package org.tl.nettyServer.servers.net.rtmp.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.Data;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.tl.nettyServer.servers.net.rtmp.conn.IConnection;
import org.tl.nettyServer.servers.net.rtmp.message.Packet;

import javax.crypto.Cipher;

@Data
public class NettySessionFacade implements SessionFacade<ChannelHandlerContext> {
    public static AttributeKey<NettySessionFacade> sessionKeyAttr = AttributeKey.valueOf(sessionKey);

    /**
     * 框架上下文
     */
    private ChannelHandlerContext context;

    /**
     * 连接
     */
    private IConnection connection;

    /**
     * 密码在
     */
    private Cipher cipherIn;
    /**
     * 算出
     */
    private Cipher cipherOut;

    /**
     * 连接
     */
    private boolean connected = false;

    /**
     * Thread pool for guarding deadlocks.
     * 用于保护死锁的线程池
     */
    protected transient ThreadPoolTaskScheduler deadlockGuardScheduler;

    /**
     * Scheduling service
     */
    protected transient ThreadPoolTaskScheduler scheduler;


    /**
     * Thread pool for message handling.
     */
    protected transient ThreadPoolTaskExecutor executor;


    protected int defaultServerBandwidth = 10000000;


    protected int defaultClientBandwidth = 10000000;


    /**
     * Bandwidth limit type / enforcement. (0=hard,1=soft,2=dynamic)
     * 带宽限制类型/强制
     */
    protected int limitType = 0;

    protected boolean bandwidthDetection = true;

    /**
     * Initial channel capacity
     * 	初始信道容量
     */
    private int channelsInitialCapacity = 3;

    /**
     * Specify the size of queue that will trigger audio packet dropping, disabled if it's 0
     *	 指定将触发音频数据包丢弃的队列的大小，如果为0，则禁用
     * */
    private Integer executorQueueSizeToDropAudioPackets = 0;


    /**
     * Concurrency level for channels collection
     * 	通道集合的并发级别
     */
    private int channelsConcurrencyLevel = 1;

    /**
     * Initial streams capacity
     * 	初始流容量
     */
    private int streamsInitialCapacity = 1;


    /**
     * Concurrency level for streams collection
     * 	流集合的并发级别
     */
    private int streamsConcurrencyLevel = 1;


    /**
     * Initial pending calls capacity
     * 	初始挂起回调容量
     */
    private int pendingCallsInitialCapacity = 3;


    /**
     * Concurrency level for pending calls collection
     * 	挂起回到集合级别
     */
    private int pendingCallsConcurrencyLevel = 1;


    /**
     * Initial reserved streams capacity
     * 	初始预留流容量
     */
    private int reservedStreamsInitialCapacity = 1;

    /**
     * Concurrency level for reserved streams collection
     * 	保留流集合的并发级别
     */
    private int reservedStreamsConcurrencyLevel = 1;


    /**
     * Maximum time in milliseconds allowed to process received message
     * 允许处理接收消息的最长时间（毫秒）
     */
    protected long maxHandlingTimeout = 500L;

    /**
     * Maximum time in milliseconds to wait for a valid handshake.
     * 等待有效握手的最长时间（毫秒）。
     */
    private int maxHandshakeTimeout = 10000;

    /**
     * Ping interval in ms to detect dead clients.
     * 以毫秒为单位的ping间隔，用于检测死机。
     */
    private volatile int pingInterval = 5000;

    /**
     * Maximum time in ms after which a client is disconnected because of inactivity.
     * 客户端因不活动而断开连接的最长时间（毫秒）。
     */
    protected volatile int maxInactivity = 60000;


    @Override
    public void setSession(ChannelHandlerContext channelHandlerContext) {
        this.context = channelHandlerContext;
        this.context.channel().attr(sessionKeyAttr).set(this);
    }

    @Override
    public String getSessionId() {
        return this.connection.getSessionId();
    }

    @Override
    public void write(Packet out) {
        this.context.writeAndFlush(out);
    }
}
