package org.tl.nettyServer.media.stream.engin;

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.amf.Output;
import org.tl.nettyServer.media.messaging.*;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtmp.status.Status;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.scheduling.ISchedulingService;
import org.tl.nettyServer.media.service.consumer.IConsumerService;
import org.tl.nettyServer.media.service.provider.IProviderService;
import org.tl.nettyServer.media.stream.client.ISubscriberStream;
import org.tl.nettyServer.media.stream.data.IStreamData;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.stream.message.ResetMessage;
import org.tl.nettyServer.media.stream.message.StatusMessage;
import org.tl.nettyServer.media.stream.playlist.IPlayItem;
import org.tl.nettyServer.media.stream.provider.ISeekableProvider;
import org.tl.nettyServer.media.stream.provider.IStreamTypeAwareProvider;
import org.tl.nettyServer.media.util.ObjectMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public abstract class BaseEngine implements IPushableConsumer {
    protected ISubscriberStream subscriberStream;
    protected ISchedulingService schedulingService;
    protected IConsumerService consumerService;
    protected IProviderService providerService;
    protected Number streamId;

    //上配置 下状态

    protected boolean pullMode;
    protected final AtomicReference<IMessageInput> msgInReference = new AtomicReference<>();
    protected final AtomicReference<IMessageOutput> msgOutReference = new AtomicReference<>();


    protected BaseEngine() {

    }


    protected BaseEngine(ISubscriberStream subscriberStream, ISchedulingService schedulingService, IConsumerService consumerService, IProviderService providerService) {
        this.subscriberStream = subscriberStream;
        this.schedulingService = schedulingService;
        this.consumerService = consumerService;
        this.providerService = providerService;
        // get the stream id
        this.streamId = subscriberStream.getStreamId();
    }

    /**
     * Decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
     */
    protected int playDecision = 3;


    /**
     * 上次发送到客户端的消息的时间戳。
     */
    protected int lastMessageTs = -1;

    /**
     * Number of bytes sent.
     * 发送bytes的数目
     */
    protected AtomicLong bytesSent = new AtomicLong(0);

    /**
     * 流播放的开始时间。不是播放流的时间，而是应该播放流的时间如果
     * 从一开始就演奏。在时间戳5s的1:00:05播放流。后场是1:00:00。
     **/
    protected volatile long playbackStart;

    /**
     * 发送第一个包的时间戳
     */
    protected AtomicInteger streamStartTS = new AtomicInteger(-1);

    /**
     * 流开始处的偏移量（毫秒）。
     */
    protected int streamOffset;


    protected AtomicReference<IPlayItem> currentItem = new AtomicReference<>();


    /**
     * Sends a status message.
     */
    protected void doPushMessage(Status status) {
        StatusMessage message = new StatusMessage();
        message.setBody(status);
        doPushMessage(message);
    }

    /**
     * Send message to output stream and handle exceptions.
     */
    protected void doPushMessage(AbstractMessage message) {
        if (log.isTraceEnabled()) {
            String msgType = message.getMessageType();
            log.trace("doPushMessage: {}", msgType);
        }

        IMessageOutput out = msgOutReference.get();
        if (out == null) {
            log.warn("Push message failed due to null output pipe");
            return;
        }

        try {
            out.pushMessage(message);

            if (message instanceof RTMPMessage) {
                IRTMPEvent body = ((RTMPMessage) message).getBody();
                //更新上次发送的消息的时间戳
                lastMessageTs = body.getTimestamp();
                BufFacade streamData = null;
                if (body instanceof IStreamData && (streamData = ((IStreamData<?>) body).getData()) != null) {
                    bytesSent.addAndGet(streamData.readableBytes());
                }
            }
        } catch (IOException err) {
            log.warn("Error while pushing message", err);
        }

    }

    /**
     * Send an RtmpProtocolState message
     */
    protected void sendMessage(RTMPMessage messageIn) {
        IRTMPEvent eventIn = messageIn.getBody();
        IRTMPEvent event;
        switch (eventIn.getDataType()) {
            case Constants.TYPE_AGGREGATE:
                event = new Aggregate(((Aggregate) eventIn).getData());
                break;
            case Constants.TYPE_AUDIO_DATA:
                event = new AudioData(((AudioData) eventIn).getData());
                break;
            case Constants.TYPE_VIDEO_DATA:
                event = new VideoData(((VideoData) eventIn).getData());
                break;
            default:
                event = new Notify(((Notify) eventIn).getData());
                break;
        }
        // 获取传入事件时间
        int eventTime = eventIn.getTimestamp();
        // 获取传入事件源类型并设置传出事件
        event.setSourceType(eventIn.getSourceType());
        // 实例化传出消息
        RTMPMessage messageOut = RTMPMessage.build(event, eventTime);
        if (log.isTraceEnabled()) {
            log.trace("Source type - in: {} out: {}", eventIn.getSourceType(), messageOut.getBody().getSourceType());
            long delta = System.currentTimeMillis() - playbackStart;
            log.trace("sendMessage: streamStartTS {}, length {}, streamOffset {}, timestamp {} last timestamp {} delta {} buffered {}", new Object[]{streamStartTS.get(), currentItem.get().getLength(), streamOffset, eventTime, lastMessageTs, delta, lastMessageTs - delta});
        }
        if (playDecision == 1) { // 1 == vod/file
            if (eventTime > 0 && streamStartTS.compareAndSet(-1, eventTime)) {
                log.debug("sendMessage: set streamStartTS");
                messageOut.getBody().setTimestamp(0);
            }
            long length = currentItem.get().getLength();
            if (length >= 0) {
                int duration = eventTime - streamStartTS.get();
                if (log.isTraceEnabled()) {
                    log.trace("sendMessage duration={} length={}", duration, length);
                }
                if (duration - streamOffset >= length) {
                    // sent enough data to client
                    stop();
                    return;
                }
            }
        } else {
            // 对于实时流，不要将streamstartts重置为0
            if (eventTime > 0 && streamStartTS.compareAndSet(-1, eventTime)) {
                log.debug("sendMessage: set streamStartTS");
            }
            //实时流的相对时间戳调整
            int startTs = streamStartTS.get();
            if (startTs > 0) {
                // 减去流开始为客户端播放时的偏移时间
                eventTime -= startTs;
                messageOut.getBody().setTimestamp(eventTime);
                if (log.isTraceEnabled()) {
                    log.trace("sendMessage (updated): streamStartTS={}, length={}, streamOffset={}, timestamp={}", new Object[]{startTs, currentItem.get().getLength(), streamOffset, eventTime});
                }
            }
        }
        doPushMessage(messageOut);
    }

    abstract void stop();

    /**
     * 发送清除ping。让客户端知道流没有更多的数据要发送。
     */
    protected void sendClearPing() {
        Ping eof = new Ping();
        eof.setEventType(Ping.STREAM_PLAYBUFFER_CLEAR);
        eof.setValue2(streamId);
        // eos
        RTMPMessage eofMsg = RTMPMessage.build(eof);
        doPushMessage(eofMsg);
    }

    /**
     * Send reset message
     */
    protected void sendReset() {
        if (pullMode) {
            Ping recorded = new Ping();
            recorded.setEventType(Ping.RECORDED_STREAM);
            recorded.setValue2(streamId);
            // recorded
            RTMPMessage recordedMsg = RTMPMessage.build(recorded);
            doPushMessage(recordedMsg);
        }
        Ping begin = new Ping();
        begin.setEventType(Ping.STREAM_BEGIN);
        begin.setValue2(streamId);
        // begin
        RTMPMessage beginMsg = RTMPMessage.build(begin);
        doPushMessage(beginMsg);
        // reset
        ResetMessage reset = new ResetMessage();
        doPushMessage(reset);
    }

    /**
     * Send reset status for item
     */
    protected void sendResetStatus(IPlayItem item) {
        Status reset = new Status(StatusCodes.NS_PLAY_RESET);
        reset.setClientid(streamId);
        reset.setDetails(item.getName());
        reset.setDesciption(String.format("Playing and resetting %s.", item.getName()));

        doPushMessage(reset);
    }

    /**
     * Send playback start status notification
     */
    protected void sendStartStatus(IPlayItem item) {
        Status start = new Status(StatusCodes.NS_PLAY_START);
        start.setClientid(streamId);
        start.setDetails(item.getName());
        start.setDesciption(String.format("Started playing %s.", item.getName()));

        doPushMessage(start);
    }

    /**
     * Send playback stoppage status notification
     */
    protected void sendStopStatus(IPlayItem item) {
        Status stop = new Status(StatusCodes.NS_PLAY_STOP);
        stop.setClientid(streamId);
        stop.setDesciption(String.format("Stopped playing %s.", item.getName()));
        stop.setDetails(item.getName());

        doPushMessage(stop);
    }

    /**
     * Sends an onPlayStatus message.
     * <p>
     * http://help.adobe.com/en_US/FlashPlatform/reference/actionscript/3/flash/events/NetDataEvent.html
     */
    protected void sendOnPlayStatus(String code, int duration, long bytes) {
        if (log.isDebugEnabled()) {
            log.debug("Sending onPlayStatus - code: {} duration: {} bytes: {}", code, duration, bytes);
        }
        // create the buffer
        BufFacade buf = BufFacade.buffer(102);
        Output out = new Output(buf);
        out.writeString("onPlayStatus");
        ObjectMap<Object, Object> args = new ObjectMap<>();
        args.put("code", code);
        args.put("level", Status.STATUS);
        args.put("duration", duration);
        args.put("bytes", bytes);
        String name = currentItem.get().getName();
        if (StatusCodes.NS_PLAY_TRANSITION_COMPLETE.equals(code)) {
            args.put("clientId", streamId);
            args.put("details", name);
            args.put("description", String.format("Transitioned to %s", name));
            args.put("isFastPlay", false);
        }
        out.writeObject(args);
        Notify event = new Notify(buf, "onPlayStatus");
        if (lastMessageTs > 0) {
            event.setTimestamp(lastMessageTs);
        } else {
            event.setTimestamp(0);
        }
        RTMPMessage msg = RTMPMessage.build(event);
        doPushMessage(msg);
    }

    /**
     * Send playlist switch status notification
     */
    protected void sendSwitchStatus() {
        // TODO: find correct duration to send
        sendOnPlayStatus(StatusCodes.NS_PLAY_SWITCH, 1, bytesSent.get());
    }

    /**
     * Send transition status notification
     */
    protected void sendTransitionStatus() {
        sendOnPlayStatus(StatusCodes.NS_PLAY_TRANSITION_COMPLETE, 0, bytesSent.get());
    }

    /**
     * Send playlist complete status notification
     */
    protected void sendCompleteStatus() {
        // may be the correct duration
        int duration = (lastMessageTs > 0) ? Math.max(0, lastMessageTs - streamStartTS.get()) : 0;
        if (log.isDebugEnabled()) {
            log.debug("sendCompleteStatus - duration: {} bytes sent: {}", duration, bytesSent.get());
        }
        sendOnPlayStatus(StatusCodes.NS_PLAY_COMPLETE, duration, bytesSent.get());
    }

    /**
     * Send seek status notification
     */
    protected void sendSeekStatus(IPlayItem item, int position) {
        Status seek = new Status(StatusCodes.NS_SEEK_NOTIFY);
        seek.setClientid(streamId);
        seek.setDetails(item.getName());
        seek.setDesciption(String.format("Seeking %d (stream ID: %d).", position, streamId));

        doPushMessage(seek);
    }

    /**
     * Send pause status notification
     */
    protected void sendPauseStatus(IPlayItem item) {
        Status pause = new Status(StatusCodes.NS_PAUSE_NOTIFY);
        pause.setClientid(streamId);
        pause.setDetails(item.getName());

        doPushMessage(pause);
    }

    /**
     * Send resume status notification
     */
    protected void sendResumeStatus(IPlayItem item) {
        Status resume = new Status(StatusCodes.NS_UNPAUSE_NOTIFY);
        resume.setClientid(streamId);
        resume.setDetails(item.getName());

        doPushMessage(resume);
    }

    /**
     * Send published status notification
     */
    protected void sendPublishedStatus(IPlayItem item) {
        Status published = new Status(StatusCodes.NS_PLAY_PUBLISHNOTIFY);
        published.setClientid(streamId);
        published.setDetails(item.getName());

        doPushMessage(published);
    }

    /**
     * Send unpublished status notification
     */
    protected void sendUnpublishedStatus(IPlayItem item) {
        Status unpublished = new Status(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY);
        unpublished.setClientid(streamId);
        unpublished.setDetails(item.getName());

        doPushMessage(unpublished);
    }

    /**
     * Stream not found status notification
     */
    protected void sendStreamNotFoundStatus(IPlayItem item) {
        Status notFound = new Status(StatusCodes.NS_PLAY_STREAMNOTFOUND);
        notFound.setClientid(streamId);
        notFound.setLevel(Status.ERROR);
        notFound.setDetails(item.getName());

        doPushMessage(notFound);
    }

    /**
     * Insufficient bandwidth notification
     */
    protected void sendInsufficientBandwidthStatus(IPlayItem item) {
        Status insufficientBW = new Status(StatusCodes.NS_PLAY_INSUFFICIENT_BW);
        insufficientBW.setClientid(streamId);
        insufficientBW.setLevel(Status.WARNING);
        insufficientBW.setDetails(item.getName());
        insufficientBW.setDesciption("Data is playing behind the normal speed.");

        doPushMessage(insufficientBW);
    }

    /**
     * Send VOD init control message
     */
    protected void sendVODInitCM(IPlayItem item) {
        OOBControlMessage oobCtrlMsg = new OOBControlMessage();
        oobCtrlMsg.setTarget(IPassive.KEY);
        oobCtrlMsg.setServiceName("init");
        Map<String, Object> paramMap = new HashMap<String, Object>(1);
        paramMap.put("startTS", (int) item.getStart());
        oobCtrlMsg.setServiceParamMap(paramMap);
        msgInReference.get().sendOOBControlMessage(this, oobCtrlMsg);
    }

    /**
     * Send VOD seek control message
     */
    protected int sendVODSeekCM(int position) {
        OOBControlMessage oobCtrlMsg = new OOBControlMessage();
        oobCtrlMsg.setTarget(ISeekableProvider.KEY);
        oobCtrlMsg.setServiceName("seek");
        Map<String, Object> paramMap = new HashMap<String, Object>(1);
        paramMap.put("position", position);
        oobCtrlMsg.setServiceParamMap(paramMap);
        msgInReference.get().sendOOBControlMessage(this, oobCtrlMsg);
        if (oobCtrlMsg.getResult() instanceof Integer) {
            return (Integer) oobCtrlMsg.getResult();
        } else {
            return -1;
        }
    }

    protected boolean sendCheckVideoCM() {
        OOBControlMessage oobCtrlMsg = new OOBControlMessage();
        oobCtrlMsg.setTarget(IStreamTypeAwareProvider.KEY);
        oobCtrlMsg.setServiceName("hasVideo");
        msgInReference.get().sendOOBControlMessage(this, oobCtrlMsg);
        if (oobCtrlMsg.getResult() instanceof Boolean) {
            return (Boolean) oobCtrlMsg.getResult();
        } else {
            return false;
        }
    }

    void setMessageOut(IMessageOutput msgOut) {
        this.msgOutReference.set(msgOut);
    }
}
