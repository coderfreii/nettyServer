package org.tl.nettyServer.media.net.rtmp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.tl.nettyServer.media.net.rtmp.codec.RTMP;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.handler.packet.RtmpPacketHandler;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.net.rtmp.task.ReceivedMessageTask;
import org.tl.nettyServer.media.net.rtmp.task.ReceivedMessageTaskQueue;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class RtmpPacketMayAsyncDecoder extends MessageToMessageDecoder<Packet> {
    private RtmpPacketHandler handler = new RtmpPacketHandler();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        IConnection iConnection = SessionAccessor.resolveConn(ctx);
        if (iConnection != null) {
            handler.connectionClosed((RTMPConnection) iConnection);
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Packet msg, List<Object> out) throws Exception {
        RTMPConnection conn = (RTMPConnection) SessionAccessor.resolveConn(ctx);

        ThreadPoolTaskExecutor executor = conn.getExecutor();
        String sessionId = conn.getSessionId();
        int executorQueueSizeToDropAudioPackets = conn.getExecutorQueueSizeToDropAudioPackets();


        Packet packet = msg;
        if (executor != null) {
            final byte dataType = packet.getHeader().getDataType();
            switch (dataType) {
                case Constants.TYPE_PING:
                case Constants.TYPE_ABORT:
                case Constants.TYPE_BYTES_READ:
                case Constants.TYPE_CHUNK_SIZE:
                case Constants.TYPE_CLIENT_BANDWIDTH:
                case Constants.TYPE_SERVER_BANDWIDTH:
                    handler.messageReceived(conn, packet);
                    break;
                default:
                    final String messageType = getMessageType(packet);
                    try {
                        if (executorQueueSizeToDropAudioPackets > 0 && conn.getCurrentQueueSize().get() >= executorQueueSizeToDropAudioPackets) {
                            if (packet.getHeader().getDataType() == Constants.TYPE_AUDIO_DATA) {
                                //如果队列中有积压的消息。flash可能在网络拥塞后发送了一系列消息。扔掉我们可以丢弃的包。
                                log.info("Queue threshold reached. Discarding packet: session=[{}], msgType=[{}], packetNum=[{}]", sessionId, messageType, conn.getPacketSequence().get());
                                return;
                            }
                        }
                        int streamId = packet.getHeader().getStreamId().intValue();
                        if (log.isTraceEnabled()) {
                            log.trace("Handling message for streamId: {}, csId: {} Channels: {}", streamId, packet.getHeader().getCsId(), conn.getChannels());
                        }
                        // 创建任务以设置处理消息
                        addTask(streamId, new ReceivedMessageTask(sessionId, packet, handler, conn), conn);
                    } catch (Exception e) {
                        log.error("Incoming message handling failed on session=[" + sessionId + "], messageType=[" + messageType + "]", e);
                        if (log.isDebugEnabled()) {
                            log.debug("Execution rejected on {} - {}", sessionId, RTMP.states[conn.getStateCode()]);
                            log.debug("Lock permits - decode: {} encode: {}", conn.getDecoderLock().availablePermits(), conn.getEncoderLock().availablePermits());
                        }
                    }
            }
        } else {
            log.debug("Executor is null on {} state: {}", sessionId, RTMP.states[conn.getStateCode()]);
            // pass message to the handler
            try {
                //将消息传递给处理程序
                handler.messageReceived(conn, packet);
            } catch (Exception e) {
                log.error("Error processing received message {} state: {}", sessionId, RTMP.states[conn.getStateCode()], e);
            }
        }
    }


    void addTask(int streamId, ReceivedMessageTask task, RTMPConnection connection) {
        // 创建任务以设置处理消息
        task.setPacketNumber(connection.getCurrentQueueSize().incrementAndGet());
        // 创建任务队列
        ReceivedMessageTaskQueue newStreamTaskQueue = new ReceivedMessageTaskQueue(streamId, connection);
        ConcurrentMap<Integer, ReceivedMessageTaskQueue> tasksByStreams = connection.getTasksByStreams();
        // 按流映射将队列放入任务中
        ReceivedMessageTaskQueue currentStreamTaskQueue = tasksByStreams.putIfAbsent(streamId, newStreamTaskQueue);
        if (currentStreamTaskQueue != null) {
            //将任务添加到现有队列
            currentStreamTaskQueue.addTask(task);
        } else {
            // 将任务添加到新创建的和刚添加的队列中
            newStreamTaskQueue.addTask(task);
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
