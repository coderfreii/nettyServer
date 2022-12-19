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

package org.tl.nettyServer.media.stream.lisener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.IKeyFrameMetaCache;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.scheduling.IScheduledJob;
import org.tl.nettyServer.media.scheduling.ISchedulingService;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.stream.DefaultStreamFilenameGenerator;
import org.tl.nettyServer.media.stream.IStreamFilenameGenerator;
import org.tl.nettyServer.media.stream.data.IStreamPacket;
import org.tl.nettyServer.media.stream.base.IBroadcastStream;
import org.tl.nettyServer.media.stream.consumer.FileConsumer;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stream listener for recording stream events to a file.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RecordingListener implements IRecordingListener {

    private static final Logger log = LoggerFactory.getLogger(RecordingListener.class);

    /**
     * Scheduler
     */
    private ISchedulingService scheduler;

    /**
     * Event queue worker job name
     */
    private String eventQueueJobName;

    /**
     * Whether we are recording or not
     */
    private AtomicBoolean recording = new AtomicBoolean(false);

    /**
     * Whether we are appending or not
     */
    private boolean appending;

    /**
     * FileConsumer used to output recording to disk
     */
    private FileConsumer recordingConsumer;

    /**
     * The filename we are recording to.
     */
    private String fileName;

    /**
     * Queue to hold incoming stream event packets.
     */
    private final BlockingQueue<CachedEvent> queue = new LinkedBlockingQueue<>(8192);


    /**
    * The instance holding the queued processQueue() calls
    **/
    private EventQueueJob eqj;

    /**
     * Get the file we'd be recording to based on scope and given name.
     *
     * @param scope
     *            scope
     * @param name
     *            name
     * @return file
     */
    public static File getRecordFile(IScope scope, String name) {
        // get stream filename generator
        IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
        // generate filename
        String fileName = generator.generateFilename(scope, name, ".flv", IStreamFilenameGenerator.GenerationType.RECORD);
        File file = null;
        if (generator.resolvesToAbsolutePath()) {
            file = new File(fileName);
        } else {
            Resource resource = scope.getContext().getResource(fileName);
            if (resource.exists()) {
                try {
                    file = resource.getFile();
                    log.debug("File exists: {} writable: {}", file.exists(), file.canWrite());
                } catch (IOException ioe) {
                    log.error("File error: {}", ioe);
                }
            } else {
                String appScopeName = ScopeUtils.findApplication(scope).getName();
                file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, fileName));
            }
        }
        return file;
    }

    /** {@inheritDoc} */
    public boolean init(IConnection conn, String name, boolean isAppend) {
        // get connections scope
        return init(conn.getScope(), name, isAppend);
    }

    /** {@inheritDoc} */
    public boolean init(IScope scope, String name, boolean isAppend) {
        // get the file for our filename
        File file = getRecordFile(scope, name);
        if (file != null) {
            // If append mode is on...
            if (!isAppend) {
                if (file.exists()) {
                    // when "live" or "record" is used, any previously recorded stream with the same stream URI is deleted.
                    if (!file.delete()) {
                        log.warn("Existing file: {} could not be deleted", file.getName());
                        return false;
                    }
                }
            } else {
                if (file.exists()) {
                    appending = true;
                } else {
                    // if a recorded stream at the same URI does not already exist, "append" creates the stream as though "record" was passed.
                    isAppend = false;
                }
            }
            // if the file doesn't exist yet, create it
            if (!file.exists()) {
                // Make sure the destination directory exists
                String path = file.getAbsolutePath();
                int slashPos = path.lastIndexOf(File.separator);
                if (slashPos != -1) {
                    path = path.substring(0, slashPos);
                }
                File tmp = new File(path);
                if (!tmp.isDirectory()) {
                    tmp.mkdirs();
                }
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    log.warn("New recording file could not be created for: {}", file.getName(), e);
                    return false;
                }
            }
            if (log.isDebugEnabled()) {
                try {
                    log.debug("Recording file: {}", file.getCanonicalPath());
                } catch (IOException e) {
                    log.warn("Exception getting file path", e);
                }
            }
            //remove existing meta info
            if (scope.getContext().hasBean("keyframe.cache")) {
                IKeyFrameMetaCache keyFrameCache = (IKeyFrameMetaCache) scope.getContext().getBean("keyframe.cache");
                keyFrameCache.removeKeyFrameMeta(file);
            }
            // get instance via spring
            if (scope.getContext().hasBean("fileConsumer")) {
                log.debug("Context contains a file consumer");
                recordingConsumer = (FileConsumer) scope.getContext().getBean("fileConsumer");
                recordingConsumer.setScope(scope);
                recordingConsumer.setFile(file);
            } else {
                log.debug("Context does not contain a file consumer, using direct instance");
                // get a new instance
                recordingConsumer = new FileConsumer(scope, file);
            }
            // set the mode on the consumer
            if (isAppend) {
                recordingConsumer.setMode("append");
            } else {
                recordingConsumer.setMode("record");
            }
            // set the filename
            setFileName(file.getName());
            // get the scheduler
            scheduler = (ISchedulingService) scope.getParent().getContext().getBean(ISchedulingService.BEAN_NAME);
            // set recording true
            recording.set(true);
        } else {
            log.warn("Record file is null");
        }
        // since init finished, return recording flag
        return recording.get();
    }

    /** {@inheritDoc} */
    public void start() {
        // start the worker
        eqj = new EventQueueJob();
        eventQueueJobName = scheduler.addScheduledJob(3000, eqj);
    }

    /** {@inheritDoc} */
    public void stop() {
        // set the record flag to false
        if (recording.compareAndSet(true, false)) {
            // remove the scheduled job
            scheduler.removeScheduledJob(eventQueueJobName);
            if (queue.isEmpty()) {
                log.debug("Event queue was empty on stop");
            } else {
                if (!eqj.processing.get()){
                  log.debug("Event queue was not empty on stop and it's not processing, processing...");
                  do {
                    processQueue();
                  } while (!queue.isEmpty());
                }else{
                  log.debug("Event queue was not empty on stop but it's in processing, waiting...");
                  do {
                  } while (!queue.isEmpty());
                }
                log.debug("Processing done, event queue empty, moving on");
            }
            recordingConsumer.uninit();
        } else {
            log.debug("Recording listener was already stopped");
        }
    }

    /** {@inheritDoc} */
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
        if (recording.get()) {
            // store everything we would need to perform a write of the stream data
            CachedEvent event = new CachedEvent();
            event.setData(packet.getData().duplicate());
            event.setDataType(packet.getDataType());
            event.setReceivedTime(System.currentTimeMillis());
            event.setTimestamp(packet.getTimestamp());
            // queue the event
            if (!queue.add(event)) {
                log.debug("Event packet not added to recording queue");
            }
        } else {
            log.info("A packet was received by recording listener, but it's not recording anymore. {}", stream.getPublishedName());
        }
    }

    /**
     * Process the queued items.
     */
    private void processQueue() {
        CachedEvent cachedEvent;
        try {
            IRTMPEvent event = null;
            RTMPMessage message = null;
            // get first event in the queue
            cachedEvent = queue.poll();
            if (cachedEvent != null) {
                // get the data type
                final byte dataType = cachedEvent.getDataType();
                // get the data
                BufFacade buffer = cachedEvent.getData();
                // get the current size of the buffer / data
                int bufferLimit = buffer.readableBytes();
                if (bufferLimit > 0) {
                    // create new RTMP message and push to the consumer
                    switch (dataType) {
                        case Constants.TYPE_AGGREGATE:
                            event = new Aggregate(buffer);
                            event.setTimestamp(cachedEvent.getTimestamp());
                            message = RTMPMessage.build(event);
                            break;
                        case Constants.TYPE_AUDIO_DATA:
                            event = new AudioData(buffer);
                            event.setTimestamp(cachedEvent.getTimestamp());
                            message = RTMPMessage.build(event);
                            break;
                        case Constants.TYPE_VIDEO_DATA:
                            event = new VideoData(buffer);
                            event.setTimestamp(cachedEvent.getTimestamp());
                            message = RTMPMessage.build(event);
                            break;
                        default:
                            event = new Notify(buffer);
                            event.setTimestamp(cachedEvent.getTimestamp());
                            message = RTMPMessage.build(event);
                            break;
                    }
                    // push it down to the recorder
                    recordingConsumer.pushMessage(null, message);
                } else if (bufferLimit == 0 && dataType == Constants.TYPE_AUDIO_DATA) {
                    log.debug("Stream data size was 0, sending empty audio message");
                    // allow for 0 byte audio packets
                    event = new AudioData(BufFacade.buffer(0));
                    event.setTimestamp(cachedEvent.getTimestamp());
                    message = RTMPMessage.build(event);
                    // push it down to the recorder
                    recordingConsumer.pushMessage(null, message);
                } else {
                    log.debug("Stream data size was 0, recording pipe will not be notified");
                }
            }
        } catch (Exception e) {
            log.warn("Exception while pushing to consumer", e);
        }
    }

    /** {@inheritDoc} */
    public boolean isRecording() {
        return recording.get();
    }

    /** {@inheritDoc} */
    public boolean isAppending() {
        return appending;
    }

    /** {@inheritDoc} */
    public FileConsumer getFileConsumer() {
        return recordingConsumer;
    }

    /** {@inheritDoc} */
    public void setFileConsumer(FileConsumer recordingConsumer) {
        this.recordingConsumer = recordingConsumer;
    }

    /** {@inheritDoc} */
    public String getFileName() {
        return fileName;
    }

    /** {@inheritDoc} */
    public void setFileName(String fileName) {
        log.debug("File name: {}", fileName);
        this.fileName = fileName;
    }

    private class EventQueueJob implements IScheduledJob {

        public AtomicBoolean processing = new AtomicBoolean(false);

        public void execute(ISchedulingService service) {
            if (processing.compareAndSet(false, true)) {
                if (log.isTraceEnabled()) {
                    log.trace("Event queue size: {}", queue.size());
                }
                try {
                    if (!queue.isEmpty()) {
                        while (!queue.isEmpty()) {
                            if (log.isTraceEnabled()) {
                                log.trace("Taking one more item from queue, size: {}", queue.size());
                            }
                            processQueue();
                        }
                    } else {
                        log.trace("Nothing to record");
                    }
                } catch (Exception e) {
                    log.error("Error processing queue", e);
                } finally {
                    processing.set(false);
                }
            }
        }

    }

}
