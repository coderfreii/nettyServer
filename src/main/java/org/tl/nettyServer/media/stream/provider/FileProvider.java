/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 *
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.tl.nettyServer.media.stream.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.flv.IKeyFrameDataAnalyzer;
import org.tl.nettyServer.media.messaging.*;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.IStreamableFileService;
import org.tl.nettyServer.media.service.IStreamableFileServiceFactory;
import org.tl.nettyServer.media.service.StreamableFileServiceFactory;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Pullable provider for files
 */
public class FileProvider implements IPassive, ISeekableProvider, IPullableProvider, IPipeConnectionListener, IStreamTypeAwareProvider {
    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(FileProvider.class);

    /**
     * Class name
     */
    public static final String KEY = FileProvider.class.getName();

    /**
     * Provider scope
     */
    private IScope scope;

    /**
     * Source file
     */
    private File file;

    /**
     * Consumer pipe
     */
    private IPipe pipe;

    /**
     * Tag reader
     */
    private ITagReader reader;

    /**
     * Keyframe metadata
     */
    private IKeyFrameDataAnalyzer.KeyFrameMeta keyFrameMeta;

    /**
     * Position at start
     */
    private int start;

    /**
     * Create file provider for given file and scope
     *
     * @param scope
     *            Scope
     * @param file
     *            File
     */
    public FileProvider(IScope scope, File file) {
        this.scope = scope;
        this.file = file;
    }

    /**
     * Setter for start position
     *
     * @param start
     *            Start position
     */
    public void setStart(int start) {
        this.start = start;
    }

    /** {@inheritDoc} */
    public boolean hasVideo() {
        return (reader != null && reader.hasVideo());
    }

    /** {@inheritDoc} */
    public IMessage pullMessage(IPipe pipe) throws IOException {
        // there is no need for sync here, the readers use semaphore locks
        if (this.pipe == pipe) {
            if (reader == null) {
                init();
            }

            if (reader == null) {
                throw new FileNotFoundException(file.getPath());
            }

            if (reader.hasMoreTags()) {
                IRTMPEvent msg = null;
                ITag tag = reader.readTag();
                if (tag != null) {
                    int timestamp = tag.getTimestamp();
                    switch (tag.getDataType()) {
                        case Constants.TYPE_AUDIO_DATA:
                            msg = new AudioData(tag.getBody());
                            break;
                        case Constants.TYPE_VIDEO_DATA:
                            msg = new VideoData(tag.getBody());
                            break;
                        case Constants.TYPE_INVOKE:
                            msg = new Invoke(tag.getBody());
                            break;
                        case Constants.TYPE_NOTIFY:
                            msg = new Notify(tag.getBody());
                            break;
                        case Constants.TYPE_FLEX_STREAM_SEND:
                            msg = new FlexStreamSend(tag.getBody());
                            break;
                        default:
                            log.warn("Unexpected type? {}", tag.getDataType());
                            msg = new Unknown(tag.getDataType(), tag.getBody());
                    }
                    msg.setTimestamp(timestamp);
                    RTMPMessage rtmpMsg = RTMPMessage.build(msg);
                    return rtmpMsg;
                } else {
                    log.debug("Tag was null");
                }
            } else {
                // TODO send OOBCM to notify EOF
                // Do not unsubscribe if there aren't any more tags, as this kills VOD seek while in buffer
                // this.pipe.unsubscribe(this);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public IMessage pullMessage(IPipe pipe, long wait) throws IOException {
        return pullMessage(pipe);
    }

    /** {@inheritDoc} */
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        switch (event.getType()) {
            case PROVIDER_CONNECT_PULL:
                if (pipe == null) {
                    pipe = (IPipe) event.getSource();
                }
                break;
            case PROVIDER_DISCONNECT:
                if (pipe == event.getSource()) {
                    pipe = null;
                    uninit();
                }
                break;
            case CONSUMER_DISCONNECT:
                if (pipe == event.getSource()) {
                    uninit();
                }
            default:
        }
    }

    /** {@inheritDoc} */
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
        String serviceName = oobCtrlMsg.getServiceName();
        String target = oobCtrlMsg.getTarget();
        log.debug("onOOBControlMessage - service name: {} target: {}", serviceName, target);
        if (serviceName != null) {
            if (IPassive.KEY.equals(target)) {
                if ("init".equals(serviceName)) {
                    Integer startTS = (Integer) oobCtrlMsg.getServiceParamMap().get("startTS");
                    setStart(startTS);
                }
            } else if (ISeekableProvider.KEY.equals(target)) {
                if ("seek".equals(serviceName)) {
                    Integer position = (Integer) oobCtrlMsg.getServiceParamMap().get("position");
                    int seekPos = seek(position.intValue());
                    // Return position we seeked to
                    oobCtrlMsg.setResult(seekPos);
                }
            } else if (IStreamTypeAwareProvider.KEY.equals(target)) {
                if ("hasVideo".equals(serviceName)) {
                    oobCtrlMsg.setResult(hasVideo());
                }
            }
        }
    }

    /**
     * Initializes file provider. Creates streamable file factory and service, seeks to start position
     */
    private void init() throws IOException {
        IStreamableFileServiceFactory factory = (IStreamableFileServiceFactory) ScopeUtils.getScopeService(scope, IStreamableFileServiceFactory.class, StreamableFileServiceFactory.class);
        IStreamableFileService service = factory.getService(file);
        if (service == null) {
            log.error("No service found for {}", file.getAbsolutePath());
            return;
        }
        IStreamableFile streamFile = service.getStreamableFile(file);
        reader = streamFile.getReader();
        if (start > 0) {
            seek(start);
        }
    }

    /**
     * Reset
     */
    private void uninit() {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }

    /** {@inheritDoc} */
    public int seek(int ts) {
        log.trace("Seek ts: {}", ts);
        if (keyFrameMeta == null) {
            if (!(reader instanceof IKeyFrameDataAnalyzer)) {
                // Seeking not supported
                return ts;
            }
            keyFrameMeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();
        }
        if (keyFrameMeta.positions.length == 0) {
            // no video keyframe metainfo, it's an audio-only FLV we skip the seek for now.
            // TODO add audio-seek capability
            return ts;
        }
        if (ts >= keyFrameMeta.duration) {
            // Seek at or beyond EOF
            reader.position(Long.MAX_VALUE);
            return (int) keyFrameMeta.duration;
        }
        int frame = -1;
        for (int i = 0; i < keyFrameMeta.positions.length; i++) {
            if (keyFrameMeta.timestamps[i] >= ts) {
                frame = i;
                break;
            }
        }
        if (frame > -1) {
            reader.position(keyFrameMeta.positions[frame]);
            return keyFrameMeta.timestamps[frame];
        } else {
            // Seek at or beyond EOF
            reader.position(Long.MAX_VALUE);
            return (int) keyFrameMeta.duration;
        }
    }
}
