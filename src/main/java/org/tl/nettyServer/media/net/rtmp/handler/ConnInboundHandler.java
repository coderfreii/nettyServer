package org.tl.nettyServer.media.net.rtmp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.conf.ExtConfiguration;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnManager;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPNettyConnection;
import org.tl.nettyServer.media.net.rtmp.session.NettySessionFacade;

public class ConnInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    // ctx  => conn
    // session -> ctx, conn
    // conn -> session
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Attribute<RTMPConnection> attr = ctx.channel().attr(NettySessionFacade.connectionAttributeKey);
        if (attr.get() == null) {
            RTMPNettyConnection connection = (RTMPNettyConnection) RTMPConnManager.getInstance().createConnection(RTMPNettyConnection.class);
            NettySessionFacade session = new NettySessionFacade();
            session.setContext(ctx);
            session.setConnection(connection);
            connection.setSession(session);
            attr.set(connection);
            initialSession(connection);
        }
        super.channelRead(ctx, BufFacade.wrapperAndCast(msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }


    void initialSession(RTMPNettyConnection session) {
        session.setDeadlockGuardScheduler(deadlockGuardScheduler());
        session.setScheduler(rtmpScheduler());
        session.setExecutor(messageExecutor());
        session.setPingInterval(ExtConfiguration.PING_INTERVAL);
        session.setMaxInactivity(ExtConfiguration.MAX_INACTIVITY);
        session.setMaxHandshakeTimeout(ExtConfiguration.MAX_HANDSHAKE_TIMEOUT);
        session.setDefaultServerBandwidth(ExtConfiguration.DEFAULT_SERVER_BANDWIDTH);
        session.setDefaultClientBandwidth(ExtConfiguration.DEFAULT_CLIENT_BANDWIDTH);
        session.setLimitType(ExtConfiguration.LIMIT_TYPE);
        session.setBandwidthDetection(ExtConfiguration.BANDWIDTH_DETECTION);
        session.setMaxHandlingTimeout(ExtConfiguration.MAX_HANDLING_TIMEOUT);
        session.setExecutorQueueSizeToDropAudioPackets(ExtConfiguration.EXECUTOR_QUEUE_SIZE_TO_DROP_AUDIO_PACKETS);
        session.setChannelsInitialCapacity(ExtConfiguration.CHANNELS_INITAL_CAPACITY);
        session.setChannelsConcurrencyLevel(ExtConfiguration.CHANNELS_CONCURRENCY_LEVEL);
        session.setStreamsInitialCapacity(ExtConfiguration.STREAMS_INITAL_CAPACITY);
        session.setStreamsConcurrencyLevel(ExtConfiguration.STREAMS_CONCURRENCY_LEVEL);
        session.setPendingCallsInitialCapacity(ExtConfiguration.PENDING_CALLS_INITAL_CAPACITY);
        session.setPendingCallsConcurrencyLevel(ExtConfiguration.PENDING_CALLS_CONCURRENCY_LEVEL);
        session.setReservedStreamsInitialCapacity(ExtConfiguration.RESERVED_STREAMS_INITAL_CAPACITY);
        session.setReservedStreamsConcurrencyLevel(ExtConfiguration.RESERVED_STREAMS_CONCURREN_CYLEVEL);
    }


    public ThreadPoolTaskScheduler deadlockGuardScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(ExtConfiguration.DEAD_POOL_SIZE);
        threadPoolTaskScheduler.setDaemon(false);
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setThreadNamePrefix("DeadlockGuardScheduler-");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }

    public ThreadPoolTaskScheduler rtmpScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(ExtConfiguration.POOL_SIZE);
        threadPoolTaskScheduler.setDaemon(true);
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setThreadNamePrefix("RTMPConnectionScheduler-");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }

    public ThreadPoolTaskExecutor messageExecutor() {
        ThreadPoolTaskExecutor poolTaskExecutor = new ThreadPoolTaskExecutor();
        poolTaskExecutor.setCorePoolSize(ExtConfiguration.CORE_POOL_SIZE);
        poolTaskExecutor.setMaxPoolSize(ExtConfiguration.MAX_POOL_SIZE);
        poolTaskExecutor.setQueueCapacity(ExtConfiguration.QUEUE_CAPACITY);
        poolTaskExecutor.setDaemon(false);
        poolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        poolTaskExecutor.setThreadNamePrefix("RTMPConnectionExecutor-");
        poolTaskExecutor.initialize();
        return poolTaskExecutor;
    }
}
