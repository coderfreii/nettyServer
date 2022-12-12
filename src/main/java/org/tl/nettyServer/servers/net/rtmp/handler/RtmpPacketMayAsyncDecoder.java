package org.tl.nettyServer.servers.net.rtmp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.tl.nettyServer.servers.net.rtmp.codec.RTMP;
import org.tl.nettyServer.servers.net.rtmp.message.Constants;
import org.tl.nettyServer.servers.net.rtmp.message.Header;
import org.tl.nettyServer.servers.net.rtmp.message.Packet;
import org.tl.nettyServer.servers.net.rtmp.session.SessionAccessor;
import org.tl.nettyServer.servers.net.rtmp.session.SessionFacade;
import org.tl.nettyServer.servers.net.rtmp.task.ReceivedMessageTask;
import org.tl.nettyServer.servers.net.rtmp.task.ReceivedMessageTaskQueue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RtmpPacketMayAsyncDecoder extends MessageToMessageDecoder<Packet> {


    /**
     * Packet sequence number
     */
    private final AtomicLong packetSequence = new AtomicLong();

    /**
     * Keep track of current queue size
     * 跟踪当前队列大小
     */
    private final AtomicInteger currentQueueSize = new AtomicInteger();

    @Override
    protected void decode(ChannelHandlerContext ctx, Packet msg, List<Object> out) throws Exception {
        SessionFacade nettySessionFacade = SessionAccessor.resolveSession(ctx);
        ThreadPoolTaskExecutor executor = nettySessionFacade.getExecutor();
        int executorQueueSizeToDropAudioPackets = nettySessionFacade.getExecutorQueueSizeToDropAudioPackets();
        String sessionId = nettySessionFacade.getConnection().getSessionId();

        Packet packet = msg;
        if (executor != null) {
            final byte dataType = packet.getHeader().getDataType();
            //将这些类型路由到执行器之外
            switch (dataType) {
                case Constants.TYPE_PING:
                case Constants.TYPE_ABORT:
                case Constants.TYPE_BYTES_READ:
                case Constants.TYPE_CHUNK_SIZE:
                case Constants.TYPE_CLIENT_BANDWIDTH:
                case Constants.TYPE_SERVER_BANDWIDTH:
                    //将消息传递给处理程序
                    out.add(packet);
                    break;
                default:
                    final String messageType = getMessageType(packet);
                    try {
                        // 增加包数
                        final long packetNumber = packetSequence.incrementAndGet();
                        if (executorQueueSizeToDropAudioPackets > 0 && currentQueueSize.get() >= executorQueueSizeToDropAudioPackets) {
                            if (packet.getHeader().getDataType() == Constants.TYPE_AUDIO_DATA) {
                                //如果队列中有积压的消息。flash可能在网络拥塞后发送了一系列消息。扔掉我们可以丢弃的包。
                                log.info("Queue threshold reached. Discarding packet: session=[{}], msgType=[{}], packetNum=[{}]", sessionId, messageType, packetNumber);
                                return;
                            }
                        }
                        int streamId = packet.getHeader().getStreamId().intValue();
                        if (log.isTraceEnabled()) {
                            log.trace("Handling message for streamId: {}, channelId: {} Channels: {}", streamId, packet.getHeader().getChannelId(), channels);
                        }
                        // 创建任务以设置处理消息
                        ReceivedMessageTask task = new ReceivedMessageTask(sessionId, packet, handler, this);
                        task.setPacketNumber(packetNumber);
                        // 创建任务队列
                        ReceivedMessageTaskQueue newStreamTasks = new ReceivedMessageTaskQueue(streamId, this);
                        // 按流映射将队列放入任务中
                        ReceivedMessageTaskQueue currentStreamTasks = tasksByStreams.putIfAbsent(streamId, newStreamTasks);
                        if (currentStreamTasks != null) {
                            //将任务添加到现有队列
                            currentStreamTasks.addTask(task);
                        } else {
                            // 将任务添加到新创建的和刚添加的队列中
                            newStreamTasks.addTask(task);
                        }
                    } catch (Exception e) {
                        log.error("Incoming message handling failed on session=[" + sessionId + "], messageType=[" + messageType + "]", e);
                        if (log.isDebugEnabled()) {
                            log.debug("Execution rejected on {} - {}", sessionId, RTMP.states[getStateCode()]);
                            log.debug("Lock permits - decode: {} encode: {}", decoderLock.availablePermits(), encoderLock.availablePermits());
                        }
                    }
            }
        } else {
            log.debug("Executor is null on {} state: {}", sessionId, RTMP.states[getStateCode()]);
            // pass message to the handler
            try {
                out.add(packet);
            } catch (Exception e) {
                log.error("Error processing received message {} state: {}", sessionId, RTMP.states[getStateCode()], e);
            }
        }
    }


    private String getMessageType(Packet packet) {
        final Header header = packet.getHeader();
        final byte headerDataType = header.getDataType();
        return messageTypeToName(headerDataType);
    }

    public String messageTypeToName(byte headerDataType) {
        switch (headerDataType) {
            case Constants.TYPE_AGGREGATE:
                return "TYPE_AGGREGATE";
            case Constants.TYPE_AUDIO_DATA:
                return "TYPE_AUDIO_DATA";
            case Constants.TYPE_VIDEO_DATA:
                return "TYPE_VIDEO_DATA";
            case Constants.TYPE_FLEX_SHARED_OBJECT:
                return "TYPE_FLEX_SHARED_OBJECT";
            case Constants.TYPE_SHARED_OBJECT:
                return "TYPE_SHARED_OBJECT";
            case Constants.TYPE_INVOKE:
                return "TYPE_INVOKE";
            case Constants.TYPE_FLEX_MESSAGE:
                return "TYPE_FLEX_MESSAGE";
            case Constants.TYPE_NOTIFY:
                return "TYPE_NOTIFY";
            case Constants.TYPE_FLEX_STREAM_SEND:
                return "TYPE_FLEX_STREAM_SEND";
            case Constants.TYPE_PING:
                return "TYPE_PING";
            case Constants.TYPE_BYTES_READ:
                return "TYPE_BYTES_READ";
            case Constants.TYPE_CHUNK_SIZE:
                return "TYPE_CHUNK_SIZE";
            case Constants.TYPE_CLIENT_BANDWIDTH:
                return "TYPE_CLIENT_BANDWIDTH";
            case Constants.TYPE_SERVER_BANDWIDTH:
                return "TYPE_SERVER_BANDWIDTH";
            default:
                return "UNKNOWN [" + headerDataType + "]";

        }
    }
}
