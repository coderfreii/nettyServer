/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tl.nettyServer.media.stream.consumer;

import lombok.extern.slf4j.Slf4j;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.messaging.*;
import org.tl.nettyServer.media.net.rtmp.Channel;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.stream.base.IClientStream;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.stream.message.ResetMessage;
import org.tl.nettyServer.media.stream.message.StatusMessage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RtmpProtocolState connection consumer.
 */
@Slf4j
public class ConnectionConsumer implements IPushableConsumer, IPipeConnectionListener {
    /**
     * Connection consumer class name
     */
    public static final String KEY = ConnectionConsumer.class.getName();
 
    private RTMPConnection conn;
 
    private Channel video;
 
    private Channel audio;
 
    private Channel data; 
    /**
     * Chunk size. Packets are sent chunk-by-chunk.
     */
    private int chunkSize = 1024; //TODO: Not sure of the best value here 
    /**
     * Whether or not the chunk size has been sent. This seems to be required for h264.
     */
    private AtomicBoolean chunkSizeSent = new AtomicBoolean(false);
  
    public ConnectionConsumer(RTMPConnection conn, Channel videoChannel, Channel audioChannel, Channel dataChannel) {
        log.debug("Channel ids - video: {} audio: {} data: {}", new Object[] { videoChannel, audioChannel, dataChannel });
        this.conn = conn;
        this.video = videoChannel;
        this.audio = audioChannel;
        this.data = dataChannel;
    }
 
    public ConnectionConsumer(Channel videoChannel, Channel audioChannel, Channel dataChannel) {
        this(null, videoChannel, audioChannel, dataChannel); 
    }

    @Override
    public void pushMessage(IPipe pipe, IMessage message) {
        //log.trace("pushMessage - type: {}", message.getMessageType());
        if (message instanceof ResetMessage) {
            //ignore
        } else if (message instanceof StatusMessage) {
            StatusMessage statusMsg = (StatusMessage) message;
            data.sendStatus(statusMsg.getBody());
        } else if (message instanceof RTMPMessage) {
            // make sure chunk size has been sent
            sendChunkSize();
            // cast to rtmp message
            RTMPMessage rtmpMsg = (RTMPMessage) message;
            IRTMPEvent msg = rtmpMsg.getBody();
            // get timestamp
            int eventTime = msg.getTimestamp();
            log.debug("Message timestamp: {}", eventTime);
            if (eventTime < 0) {
                eventTime += Integer.MIN_VALUE;
                msg.setTimestamp(eventTime);
                log.debug("Message has negative timestamp, applying {} offset: {}", Integer.MIN_VALUE, eventTime);
            }
            // get the data type
            byte dataType = msg.getDataType();
            if (log.isTraceEnabled()) {
                log.trace("Data type: {} source type: {}", dataType, ((BaseEvent) msg).getSourceType());
            }
            // create a new header for the consumer
            final Header header = new Header();
            header.setTimerBase(eventTime);
            // data buffer
            BufFacade buf = null;
            switch (dataType) {
                case Constants.TYPE_AGGREGATE:
                    //log.trace("Aggregate data");
                    data.write(msg);
                    break;
                case Constants.TYPE_AUDIO_DATA:
                    //log.trace("Audio data");
                    buf = ((AudioData) msg).getData();
                    if (buf != null) {
                        AudioData audioData = new AudioData(buf.asReadOnly());
                        audioData.setHeader(header);
                        audioData.setTimestamp(header.getTimer());
                        audioData.setSourceType(((AudioData) msg).getSourceType());
                        audio.write(audioData);
                    } else {
                        log.warn("Audio data was not found");
                    }
                    break;
                case Constants.TYPE_VIDEO_DATA:
                    //log.trace("Video data");
                    buf = ((VideoData) msg).getData();
                    if (buf != null) {
                        VideoData videoData = new VideoData(buf.asReadOnly());
                        videoData.setHeader(header);
                        videoData.setTimestamp(header.getTimer());
                        videoData.setSourceType(((VideoData) msg).getSourceType());
                        video.write(videoData);
                    } else {
                        log.warn("Video data was not found");
                    }
                    break;
                case Constants.TYPE_PING:
                    //log.trace("Ping");
                    Ping ping = (Ping) msg;
                    ping.setHeader(header);
                    conn.ping(ping);
                    break;
                case Constants.TYPE_STREAM_METADATA:
                    if (log.isTraceEnabled()) {
                        log.trace("Meta data: {}", (Notify) msg);
                    }
                    //Notify notify = new Notify(((Notify) msg).getData().asReadOnlyBuffer());
                    Notify notify = (Notify) msg;
                    notify.setHeader(header);
                    notify.setTimestamp(header.getTimer());
                    data.write(notify);
                    break;
                case Constants.TYPE_FLEX_STREAM_SEND:
                    //if (log.isTraceEnabled()) {
                        //log.trace("Flex stream send: {}", (Notify) msg);
                    //}
                    FlexStreamSend send = null;
                    if (msg instanceof FlexStreamSend) {
                        send = (FlexStreamSend) msg;
                    } else {
                        send = new FlexStreamSend(((Notify) msg).getData().asReadOnly());
                    }
                    send.setHeader(header);
                    send.setTimestamp(header.getTimer());
                    data.write(send);
                    break;
                case Constants.TYPE_BYTES_READ:
                    //log.trace("Bytes read");
                    BytesRead bytesRead = (BytesRead) msg;
                    bytesRead.setHeader(header);
                    bytesRead.setTimestamp(header.getTimer());
                    conn.createChannelIfAbsent((byte) 2).write(bytesRead);
                    break;
                default:
                    //log.trace("Default: {}", dataType);
                    data.write(msg);
            }
        } else {
            log.debug("Unhandled push message: {}", message);
            if (log.isTraceEnabled()) {
                Class<? extends IMessage> clazz = message.getClass();
                log.trace("Class info - name: {} declaring: {} enclosing: {}", new Object[] { clazz.getName(), clazz.getDeclaringClass(), clazz.getEnclosingClass() });
            }
        }
    }

    @Override
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        if (event.getType().equals(PipeConnectionEvent.EventType.PROVIDER_DISCONNECT)) {
            closeChannels();
        }
    } 
    @Override
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
        if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
            String serviceName = oobCtrlMsg.getServiceName();
            log.trace("Service name: {}", serviceName);
            if ("pendingCount".equals(serviceName)) {
                oobCtrlMsg.setResult(conn.getPendingMessages());
            } else if ("pendingVideoCount".equals(serviceName)) {
                IClientStream stream = conn.getStreamByChannelId(video.getId());
                if (stream != null) {
                    oobCtrlMsg.setResult(conn.getPendingVideoMessages(stream.getStreamId()));
                } else {
                    oobCtrlMsg.setResult(0L);
                }
            } else if ("writeDelta".equals(serviceName)) {
                //TODO: Revisit the max stream value later
                long maxStream = 120 * 1024;
                // Return the current delta between sent bytes and bytes the client
                // reported to have received, and the interval the client should use
                // for generating BytesRead messages (half of the allowed bandwidth).
                oobCtrlMsg.setResult(new Long[] { conn.getWrittenBytes() - conn.getClientBytesRead(), maxStream / 2 });
            } else if ("chunkSize".equals(serviceName)) {
                int newSize = (Integer) oobCtrlMsg.getServiceParamMap().get("chunkSize");
                if (newSize != chunkSize) {
                    chunkSize = newSize;
                    chunkSizeSent.set(false);
                    sendChunkSize();
                }
            }
        }
    }
 
    private void sendChunkSize() {
        if (chunkSizeSent.compareAndSet(false, true)) {
            log.debug("Sending chunk size: {}", chunkSize);
            ChunkSize chunkSizeMsg = new ChunkSize(chunkSize);
            conn.createChannelIfAbsent((byte) 2).write(chunkSizeMsg);
        }
    } 
    private void closeChannels() {
        conn.closeChannel(video.getId());
        conn.closeChannel(audio.getId());
        conn.closeChannel(data.getId());
    }

}
