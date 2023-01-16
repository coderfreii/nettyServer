package org.tl.nettyServer.media.net.rtmp.conn;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.event.IEvent;
import org.tl.nettyServer.media.exception.ClientRejectedException;
import org.tl.nettyServer.media.net.rtmp.Channel;
import org.tl.nettyServer.media.net.rtmp.DeferredResult;
import org.tl.nettyServer.media.net.rtmp.codec.RTMPDecodeState;
import org.tl.nettyServer.media.net.rtmp.codec.RtmpProtocolState;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.net.rtmp.status.Status;
import org.tl.nettyServer.media.net.rtmp.task.IReceivedMessageTaskQueueListener;
import org.tl.nettyServer.media.net.rtmp.task.ReceivedMessageTask;
import org.tl.nettyServer.media.net.rtmp.task.ReceivedMessageTaskQueue;
import org.tl.nettyServer.media.scheduling.ISchedulingService;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.IPendingServiceCallback;
import org.tl.nettyServer.media.service.IServiceCapableConnection;
import org.tl.nettyServer.media.service.call.IPendingServiceCall;
import org.tl.nettyServer.media.service.call.IServiceCall;
import org.tl.nettyServer.media.service.call.PendingCall;
import org.tl.nettyServer.media.service.call.ServiceCall;
import org.tl.nettyServer.media.service.stream.IStreamCommandService;
import org.tl.nettyServer.media.service.stream.StreamCommandService;
import org.tl.nettyServer.media.so.FlexSharedObjectMessage;
import org.tl.nettyServer.media.so.ISharedObjectEvent;
import org.tl.nettyServer.media.so.SharedObjectMessage;
import org.tl.nettyServer.media.stream.OutputStream;
import org.tl.nettyServer.media.stream.base.IClientStream;
import org.tl.nettyServer.media.stream.client.*;
import org.tl.nettyServer.media.stream.conn.IStreamCapableConnection;
import org.tl.nettyServer.media.util.CustomizableThreadFactory;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.beans.ConstructorProperties;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * RtmpProtocolState connection. Stores information about client streams, data transfer channels, pending RPC calls, bandwidth configuration, AMF
 * encoding type (AMF0/AMF3), connection state (is alive, last ping time and ping result) and session.
 * RTMP连接。存储有关客户端流、数据传输通道、挂起的RPC调用、带宽配置、AMF的信息
 * 编码类型（amf0/amf3）、连接状态（活动、上次Ping时间和Ping结果）和会话。
 */
@Slf4j
public abstract class RTMPConnection extends BaseConnection implements IStreamCapableConnection, IServiceCapableConnection, IReceivedMessageTaskQueueListener, SessionConnection {
    /**
     * Marker byte for standard or non-encrypted RtmpProtocolState data.
     * 标准或非加密RTMP数据的标记字节。
     */
    public static final byte RTMP_NON_ENCRYPTED = (byte) 0x03;

    /**
     * Marker byte for encrypted RtmpProtocolState data.
     * 加密RTMP数据标记字节
     */
    public static final byte RTMP_ENCRYPTED = (byte) 0x06;

    /**
     * Marker byte for encrypted RtmpProtocolState data XTEA. http://en.wikipedia.org/wiki/XTEA
     * 加密的rtmp数据xtea的标记字节
     */
    public static final byte RTMP_ENCRYPTED_XTEA = (byte) 0x08;

    /**
     * Marker byte for encrypted RtmpProtocolState data using Blowfish. http://en.wikipedia.org/wiki/Blowfish_(cipher)
     * 使用Blowfish加密RTMP数据的标记字节
     */
    public static final byte RTMP_ENCRYPTED_BLOWFISH = (byte) 0x09;

    /**
     * Unknown type 0x0a, seen on youtube
     * 未知类型0x0a，在YouTube上看到
     */
    public static final byte RTMP_ENCRYPTED_UNK = (byte) 0x0a;

    /**
     * Cipher for RTMPE input
     * RTMPE输入密码
     */
    public static final String RTMPE_CIPHER_IN = "rtmpe.cipher.in";

    /**
     * Cipher for RTMPE output
     * RTMPE输出密码
     */
    public static final String RTMPE_CIPHER_OUT = "rtmpe.cipher.out";

    // ~320 streams seems like a sufficient max amount of streams for a single connection
    //对于单个连接，320个流似乎足够多
    public static final double MAX_RESERVED_STREAMS = 320;

    /**
     * AMF version, AMF0 by default.
     */
    private volatile Encoding encoding = Encoding.AMF0;
    /**
     * Initial channel capacity
     * 初始信道容量
     */
    private int channelsInitialCapacity = 3;

    /**
     * Concurrency level for channels collection
     * 通道集合的并发级别
     */
    private int channelsConcurrencyLevel = 1;

    /**
     * Initial streams capacity
     * 初始流容量
     */
    private int streamsInitialCapacity = 1;

    /**
     * Concurrency level for streams collection
     * 流集合的并发级别
     */
    private int streamsConcurrencyLevel = 1;

    /**
     * Initial pending calls capacity
     * 初始挂起回调容量
     */
    private int pendingCallsInitialCapacity = 3;

    /**
     * Concurrency level for pending calls collection
     * 挂起回到集合级别
     */
    private int pendingCallsConcurrencyLevel = 1;

    /**
     * Initial reserved streams capacity
     * 初始预留流容量
     */
    private int reservedStreamsInitialCapacity = 1;

    /**
     * Concurrency level for reserved streams collection
     * 保留流集合的并发级别
     */
    private int reservedStreamsConcurrencyLevel = 1;

    /**
     * Connection channels
     *
     * @see Channel
     */
    private transient ConcurrentMap<Integer, Channel> channels = new ConcurrentHashMap<>(channelsInitialCapacity, 0.9f, channelsConcurrencyLevel);

    /**
     * Queues of tasks for every channel
     * 每个通道的任务队列
     *
     * @see ReceivedMessageTaskQueue
     */
    private final transient ConcurrentMap<Integer, ReceivedMessageTaskQueue> tasksByStreams = new ConcurrentHashMap<>(streamsInitialCapacity, 0.9f, streamsConcurrencyLevel);

    /**
     * Client streams
     *
     * @see IClientStream
     */
    private transient ConcurrentMap<Number, IClientStream> streams = new ConcurrentHashMap<>(streamsInitialCapacity, 0.9f, streamsConcurrencyLevel);

    /**
     * Reserved stream ids. Stream id's directly relate to individual NetStream instances.
     * 保留流ID。流ID直接与单个Netstream实例相关。
     */
    private transient Set<Number> reservedStreams = Collections.newSetFromMap(new ConcurrentHashMap<Number, Boolean>(reservedStreamsInitialCapacity, 0.9f, reservedStreamsConcurrencyLevel));

    /**
     * Transaction identifier for remote commands.
     * 远程命令的事务标识符。
     */
    private AtomicInteger transactionId = new AtomicInteger(1);

    /**
     * Hash map that stores pending calls and ids as pairs.
     * 将挂起的调用和ID成对存储的哈希映射。
     */
    private transient ConcurrentMap<Integer, IPendingServiceCall> pendingCalls = new ConcurrentHashMap<>(pendingCallsInitialCapacity, 0.75f, pendingCallsConcurrencyLevel);

    /**
     * Deferred results set.
     * 延迟的结果集。
     *
     * @see DeferredResult
     */
    private transient CopyOnWriteArraySet<DeferredResult> deferredResults = new CopyOnWriteArraySet<>();

    /**
     * Last ping round trip time
     * 上次ping往返时间
     */
    private AtomicInteger lastPingRoundTripTime = new AtomicInteger(-1);

    /**
     * Timestamp when last ping command was sent.
     * 最后一次ping的发送时间
     */
    private AtomicLong lastPingSentOn = new AtomicLong(0);

    /**
     * Timestamp when last ping result was received.
     * 最后一次ping收到结果的时间
     */
    private AtomicLong lastPongReceivedOn = new AtomicLong(0);


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

    /**
     * Data read interval
     * 数据读取间隔
     */
    protected long bytesReadInterval = 1024 * 1024;

    /**
     * Number of bytes to read next.
     * 下一个要读取的字节数。
     */
    protected long nextBytesRead = 1024 * 1024;

    /**
     * Number of bytes the client reported to have received.
     * 客户端报告已接收的字节数。
     */
    private AtomicLong clientBytesRead = new AtomicLong(0L);

    /**
     * Map for pending video packets keyed by stream id.
     * 由流ID键控的挂起视频包的映射。
     */
    private transient ConcurrentMap<Number, AtomicInteger> pendingVideos = new ConcurrentHashMap<>(1, 0.9f, 1);

    /**
     * Number of (NetStream) streams used.
     * 使用的（netstream）流数。
     */
    private AtomicInteger usedStreams = new AtomicInteger(0);

    /**
     * Remembered stream buffer durations.
     * 记住流缓冲区持续时间。
     */
    private transient ConcurrentMap<Number, Integer> streamBuffers = new ConcurrentHashMap<>(1, 0.9f, 1);

    /**
     * Maximum time in milliseconds to wait for a valid handshake.
     * 等待有效握手的最长时间（毫秒）。
     */
    private int maxHandshakeTimeout = 10000;

    /**
     * Maximum time in milliseconds allowed to process received message
     * 允许处理接收消息的最长时间（毫秒）
     */
    protected long maxHandlingTimeout = 500L;

    /**
     * Bandwidth limit type / enforcement. (0=hard,1=soft,2=dynamic)
     * 带宽限制类型/强制
     */
    protected int limitType = 0;

    /**
     * Protocol state
     */
    protected RtmpProtocolState state = new RtmpProtocolState();

    // protection for the decoder when using multiple threads per connection
    // 在每个连接使用多个线程时对解码器的保护
    protected transient Semaphore decoderLock = new Semaphore(1, true);

    // protection for the encoder when using multiple threads per connection
    //在每个连接使用多个线程时保护编码器
    protected transient Semaphore encoderLock = new Semaphore(1, true);

    // keeps track of the decode state 跟踪解码状态
    protected transient RTMPDecodeState decoderState;

    /**
     * Scheduling service
     */
    protected transient ThreadPoolTaskScheduler scheduler;

    /**
     * Thread pool for message handling.
     */
    protected transient ThreadPoolTaskExecutor executor;

    /**
     * Thread pool for guarding deadlocks.
     * 用于保护死锁的线程池
     */
    protected transient ThreadPoolTaskScheduler deadlockGuardScheduler;

    /**
     * Keep-alive worker flag
     * 保持活动的工人标志
     */
    protected final AtomicBoolean running;

    /**
     * Timestamp generator
     */
    private final AtomicInteger timer = new AtomicInteger(0);

    private static Timer timerO = new HashedWheelTimer(new CustomizableThreadFactory("RtmpConnTimerExecutor-"), 1, TimeUnit.SECONDS);

    /**
     * Closing flag
     */
    private final AtomicBoolean closing = new AtomicBoolean(false);

    /**
     * Packet sequence number
     */
    private final AtomicLong packetSequence = new AtomicLong();

    /**
     * Specify the size of queue that will trigger audio packet dropping, disabled if it's 0
     * 指定将触发音频数据包丢弃的队列的大小，如果为0，则禁用
     */
    private Integer executorQueueSizeToDropAudioPackets = 0;

    /**
     * Keep track of current queue size
     * 跟踪当前队列大小
     */
    private final AtomicInteger currentQueueSize = new AtomicInteger();

    /**
     * Wait for handshake task.
     * 等待握手任务。
     */
    private ScheduledFuture<?> waitForHandshakeTask;

    /**
     * Keep alive task.
     * 继续执行任务。
     */
    private ScheduledFuture<?> keepAliveTask;

    /**
     * Creates anonymous RtmpProtocolState connection without scope.
     * 创建没有作用域的匿名RTMP连接。
     *
     * @param type Connection type
     */
    @ConstructorProperties({"type"})
    public RTMPConnection(String type) {
        // 我们从没有作用域的匿名连接开始.
        // 这些参数将在稍后调用“connect”时设置。
        super(type);
        // 创建解码器状态
        decoderState = new RTMPDecodeState(getSessionId());
        // 设置运行标识
        running = new AtomicBoolean(false);
    }

    public int getId() {
        // handle the fact that a client id is a String 处理客户机ID是字符串的事实
        return client != null ? client.getId().hashCode() : -1;
    }

    @Deprecated
    public void setId(int clientId) {
        log.warn("Setting of a client id is deprecated, use IClient to manipulate the id", new Exception("RTMPConnection.setId is deprecated"));
    }


    public RtmpProtocolState getState() {
        return state;
    }

    public byte getStateCode() {
        return state.getState();
    }

    public void setStateCode(byte code) {
        if (log.isTraceEnabled()) {
            log.trace("setStateCode: {} - {}", code, RtmpProtocolState.states[code]);
        }
        state.setState(code);
    }


    /**
     * @return the decoderLock
     */
    public Semaphore getDecoderLock() {
        return decoderLock;
    }

    /**
     * @return the decoderLock
     */
    public Semaphore getEncoderLock() {
        return encoderLock;
    }

    /**
     * @return the decoderState
     */
    public RTMPDecodeState getDecoderState() {
        return decoderState;
    }


    public void setBandwidth(int mbits) {
        // 告诉flash播放器我们需要数据的速度，以及我们发送数据的速度
        createChannelIfAbsent(2).write(new ServerBW(mbits));
        // second param is the limit type (0=hard,1=soft,2=dynamic)
        createChannelIfAbsent(2).write(new ClientBW(mbits, (byte) limitType));
    }

    /**
     * Returns a usable timestamp for written packets.
     *
     * @return timestamp
     */
    public int getTimer() {
        return timer.incrementAndGet();
    }

    /**
     * Opens the connection.
     */
    public void open() {
        if (log.isTraceEnabled()) {
            // dump memory stats
            log.trace("Memory at open - free: {}K total: {}K", Runtime.getRuntime().freeMemory() / 1024, Runtime.getRuntime().totalMemory() / 1024);
        }
    }

    @Override
    public boolean connect(IScope newScope, Object[] params) {
        if (log.isDebugEnabled()) {
            log.debug("Connect scope: {}", newScope);
        }
        try {
            boolean success = super.connect(newScope, params);
            if (success) {
                // once the handshake has completed, start needed jobs start the ping / pong keep-alive
                startRoundTripMeasurement();
            } else if (log.isDebugEnabled()) {
                log.debug("Connect failed");
            }
            return success;
        } catch (ClientRejectedException e) {
            String reason = (String) e.getReason();
            log.info("Client rejected, reason: " + ((reason != null) ? reason : "None"));
            throw e;
        }
    }

    /**
     * Starts measurement.
     */
    public void startRoundTripMeasurement() {
        if (scheduler == null) {
            log.error("startRoundTripMeasurement cannot be executed due to missing scheduler. This can happen if a connection drops before handshake is complete");
            return;
        }
        if (pingInterval <= 0) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("startRoundTripMeasurement - {}", sessionId);
        }
        try {
            // 以现在+2s的初始延迟计划，以防止在connect post过程中ping消息
            keepAliveTask = scheduler.scheduleWithFixedDelay(new KeepAliveTask(), new Date(System.currentTimeMillis() + 2000L), pingInterval);
            if (log.isDebugEnabled()) {
                log.debug("Keep alive scheduled for {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error creating keep alive job for {}", sessionId, e);
        }
    }

    /**
     * Stops measurement.
     */
    private void stopRoundTripMeasurement() {
        if (keepAliveTask != null) {
            boolean cancelled = keepAliveTask.cancel(true);
            keepAliveTask = null;
            if (cancelled && log.isDebugEnabled()) {
                log.debug("Keep alive was cancelled for {}", sessionId);
            }
        }
    }

    /**
     * Initialize connection.
     */
    public void setup(String host, String path, Map<String, Object> params) {
        this.host = host;
        this.path = path;
        this.params = params;
        if (Integer.valueOf(3).equals(params.get("objectEncoding"))) {
            if (log.isDebugEnabled()) {
                log.debug("Setting object encoding to AMF3");
            }
            state.setEncoding(Encoding.AMF3);
        }
    }


    /**
     * Initialize connection.
     */
    public void setup(String host, String path, String sessionId, Map<String, Object> params) {
        this.host = host;
        this.path = path;
        this.sessionId = sessionId;
        this.params = params;
        if (params.get("objectEncoding") == Integer.valueOf(3)) {
            log.info("Setting object encoding to AMF3");
            encoding = Encoding.AMF3;
            state.setEncoding(Encoding.AMF3);
        }
    }

    public Encoding getEncoding() {
        return state.getEncoding();
    }

    public int getNextAvailableChannelId() {
        int result = 4;
        while (isChannelUsed(result)) {
            result++;
        }
        return result;
    }

    public boolean isChannelUsed(int channelId) {
        return channels.get(channelId) != null;
    }

    public Channel createChannelIfAbsent(int channelId) {
        Channel channel = channels.putIfAbsent(channelId, new Channel(this, channelId));
        if (channel == null) {
            channel = channels.get(channelId);
        }
        return channel;
    }

    public void closeChannel(int channelId) {
        if (log.isTraceEnabled()) {
            log.trace("closeChannel: {}", channelId);
        }
        Channel chan = channels.remove(channelId);
        if (log.isTraceEnabled()) {
            log.trace("channel: {} for id: {}", chan, channelId);
            if (chan == null) {
                log.trace("Channels: {}", channels);
            }
        }
        /*
        ReceivedMessageTaskQueue queue = tasksByChannels.remove(channelId);
        if (queue != null) {
            if (isConnected()) {
                // if connected, drain and process the tasks queued-up
                log.debug("Processing remaining tasks at close for channel: {}", channelId);
                processTasksQueue(queue);
            }
            queue.removeAllTasks();
        } else if (log.isTraceEnabled()) {
            log.trace("No task queue for id: {}", channelId);
        }
        */
        chan = null;
    }

    public Collection<IClientStream> getStreams() {
        return streams.values();
    }

    public Map<Number, IClientStream> getStreamsMap() {
        return Collections.unmodifiableMap(streams);
    }

    public Number reserveStreamId() {
        double d = 1.0d;
        for (; d < MAX_RESERVED_STREAMS; d++) {
            if (reservedStreams.add(d)) {
                break;
            }
        }
        if (d == MAX_RESERVED_STREAMS) {
            throw new IndexOutOfBoundsException("Unable to reserve new stream");
        }
        return d;
    }

    public Number reserveStreamId(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Reserve stream id: {}", streamId);
        }
        if (reservedStreams.add(streamId.doubleValue())) {
            return streamId;
        }
        return reserveStreamId();
    }

    public boolean isValidStreamId(Number streamId) {
        double d = streamId.doubleValue();
        if (log.isTraceEnabled()) {
            log.trace("Checking validation for streamId {}; reservedStreams: {}; streams: {}, connection: {}", new Object[]{d, reservedStreams, streams, sessionId});
        }
        if (d <= 0 || !reservedStreams.contains(d)) {
            log.warn("Stream id: {} was not reserved in connection {}", d, sessionId);
            // stream id has not been reserved before
            return false;
        }
        if (streams.get(d) != null) {
            // another stream already exists with this id
            log.warn("Another stream already exists with this id in streams {} in connection: {}", streams, sessionId);
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} is valid for connection: {}", d, sessionId);
        }
        return true;
    }

    /**
     * Returns whether or not the connection has been idle for a maximum period.
     */
    public boolean isIdle() {
        long lastPingTime = lastPingSentOn.get();
        long lastPongTime = lastPongReceivedOn.get();
        boolean idle = (lastPongTime > 0 && (lastPingTime - lastPongTime > maxInactivity));
        if (log.isTraceEnabled()) {
            log.trace("Connection {} {} idle", getSessionId(), (idle ? "is" : "is not"));
        }
        return idle;
    }

    public boolean isDisconnected() {
        return state.getState() == RtmpProtocolState.STATE_DISCONNECTED;
    }

    public IClientBroadcastStream newBroadcastStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get ClientBroadcastStream defined as a prototype in red5-common.xml
            ClientBroadcastStream cbs = (ClientBroadcastStream) scope.getContext().getBean("clientBroadcastStream");
            customizeStream(streamId, cbs);
            if (!registerStream(cbs)) {
                cbs = null;
            }
            return cbs;
        }
        return null;
    }

    public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get SingleItemSubscriberStream defined as a prototype in red5-common.xml
            SingleItemSubscriberStream siss = (SingleItemSubscriberStream) scope.getContext().getBean("singleItemSubscriberStream");
            customizeStream(streamId, siss);
            if (!registerStream(siss)) {
                siss = null;
            }
            return siss;
        }
        return null;
    }

    public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get PlaylistSubscriberStream defined as a prototype in red5-common.xml
            PlaylistSubscriberStream pss = (PlaylistSubscriberStream) scope.getContext().getBean("playlistSubscriberStream");
            customizeStream(streamId, pss);
            if (!registerStream(pss)) {
                log.trace("Stream: {} for stream id: {} failed to register", streamId);
                pss = null;
            }
            return pss;
        }
        return null;
    }

    public void addClientStream(IClientStream stream) {
        if (reservedStreams.add(stream.getStreamId().doubleValue())) {
            registerStream(stream);
        } else {
            // stream not added to registered? what to do with it?
            log.warn("Failed adding stream: {} to reserved: {}", stream, reservedStreams);
        }
    }

    public void removeClientStream(Number streamId) {
        unreserveStreamId(streamId);
    }

    protected int getUsedStreamCount() {
        return usedStreams.get();
    }

    public IClientStream getStreamById(Number streamId) {
        return streams.get(streamId.doubleValue());
    }

    public Number getStreamIdForChannelId(int channelId) {
        if (channelId < 4) {
            return 0;
        }
        Number streamId = Math.floor(((channelId - 4) / 5.0d) + 1);
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} requested for channel id: {}", streamId, channelId);
        }
        return streamId;
    }

    public IClientStream getStreamByChannelId(int channelId) {
        // channels 2 and 3 are "special" and don't have an IClientStream associated
        if (channelId < 4) {
            return null;
        }
        Number streamId = getStreamIdForChannelId(channelId);
        if (log.isTraceEnabled()) {
            log.trace("Stream requested for channel id: {} stream id: {} streams: {}", channelId, streamId, streams);
        }
        return getStreamById(streamId);
    }

    /**
     * 简单做了函数映射 或者 规则如此
     *
     * @param streamId 流id
     * @return int
     */
    public int getChannelIdForStreamId(Number streamId) {
        int channelId = (int) (streamId.doubleValue() * 5) - 1;
        if (log.isTraceEnabled()) {
            log.trace("Channel id: {} requested for stream id: {}", channelId, streamId);
        }
        return channelId;
    }

    public OutputStream createOutputStream(Number streamId) {
        int channelId = getChannelIdForStreamId(streamId);
        if (log.isTraceEnabled()) {
            log.trace("Create output - stream id: {} channel id: {}", streamId, channelId);
        }
        final Channel data = createChannelIfAbsent(channelId++);
        final Channel video = createChannelIfAbsent(channelId++);
        final Channel audio = createChannelIfAbsent(channelId++);
        if (log.isTraceEnabled()) {
            log.trace("Output stream - data: {} video: {} audio: {}", data, video, audio);
        }
        return new OutputStream(video, audio, data);
    }

    private void customizeStream(Number streamId, AbstractClientStream stream) {
        Integer buffer = streamBuffers.get(streamId.doubleValue());
        if (buffer != null) {
            stream.setClientBufferDuration(buffer);
        }
        stream.setName(createStreamName());
        stream.setConnection(this);
        stream.setScope(this.getScope());
        stream.setStreamId(streamId);
    }

    private boolean registerStream(IClientStream stream) {
        if (streams.putIfAbsent(stream.getStreamId().doubleValue(), stream) == null) {
            usedStreams.incrementAndGet();
            return true;
        }
        log.error("Unable to register stream {}, stream with id {} was already added", stream, stream.getStreamId());
        return false;
    }

    @SuppressWarnings("unused")
    private void unregisterStream(IClientStream stream) {
        if (stream != null) {
            deleteStreamById(stream.getStreamId());
        }
    }

    @Override
    public void close() {
        if (closing.compareAndSet(false, true)) {
            if (log.isDebugEnabled()) {
                log.debug("close: {}", sessionId);
            }
            stopRoundTripMeasurement();
            // update our state
            if (state != null) {
                final byte s = getStateCode();
                switch (s) {
                    case RtmpProtocolState.STATE_DISCONNECTED:
                        if (log.isDebugEnabled()) {
                            log.debug("Already disconnected");
                        }
                        return;
                    default:
                        if (log.isDebugEnabled()) {
                            log.debug("State: {}", RtmpProtocolState.states[s]);
                        }
                        setStateCode(RtmpProtocolState.STATE_DISCONNECTING);
                }
            }
            Red5.setConnectionLocal(this);
            IStreamCommandService streamService = (IStreamCommandService) ScopeUtils.getScopeService(scope, IStreamCommandService.class, StreamCommandService.class);
            if (streamService != null) {
                //in the end of call streamService.deleteStream we do streams.remove
                for (Iterator<IClientStream> it = streams.values().iterator(); it.hasNext(); ) {
                    IClientStream stream = it.next();
                    if (log.isDebugEnabled()) {
                        log.debug("Closing stream: {}", stream.getStreamId());
                    }
                    streamService.deleteStream(this, stream.getStreamId());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Stream service was not found for scope: {}", (scope != null ? scope.getName() : "null or non-existant"));
                }
            }
            // close the base connection - disconnect scopes and unregister client
            super.close();
            // kill all the collections etc
            channels.clear();
            streams.clear();
            pendingCalls.clear();
            deferredResults.clear();
            pendingVideos.clear();
            streamBuffers.clear();
            if (log.isTraceEnabled()) {
                // dump memory stats
                log.trace("Memory at close - free: {}K total: {}K", Runtime.getRuntime().freeMemory() / 1024, Runtime.getRuntime().totalMemory() / 1024);
            }
        } else if (log.isDebugEnabled()) {
            log.debug("Already closing..");
        }
    }

    @Override
    public void dispatchEvent(IEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Event notify: {}", event);
        }
        // determine if its an outgoing invoke or notify
        switch (event.getType()) {
            case CLIENT_INVOKE:
                ClientInvokeEvent cie = (ClientInvokeEvent) event;
                invoke(cie.getMethod(), cie.getParams(), cie.getCallback());
                break;
            case CLIENT_NOTIFY:
                ClientNotifyEvent cne = (ClientNotifyEvent) event;
                notify(cne.getMethod(), cne.getParams());
                break;
            default:
                log.warn("Unhandled event: {}", event);
        }
    }

    /**
     * When the connection has been closed, notify any remaining pending service calls that they have failed because the connection is
     * broken. Implementors of IPendingServiceCallback may only deduce from this notification that it was not possible to read a result for
     * this service call. It is possible that (1) the service call was never written to the service, or (2) the service call was written to
     * the service and although the remote method was invoked, the connection failed before the result could be read, or (3) although the
     * remote method was invoked on the service, the service implementor detected the failure of the connection and performed only partial
     * processing. The caller only knows that it cannot be confirmed that the callee has invoked the service call and returned a result.
     */
    public void sendPendingServiceCallsCloseError() {
        if (pendingCalls != null && !pendingCalls.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Connection calls pending: {}", pendingCalls.size());
            }
            for (IPendingServiceCall call : pendingCalls.values()) {
                call.setStatus(ServiceCall.STATUS_NOT_CONNECTED);
                for (IPendingServiceCallback callback : call.getCallbacks()) {
                    callback.resultReceived(call);
                }
            }
        }
    }


    public void unreserveStreamId(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Unreserve streamId: {}", streamId);
        }
        double d = streamId.doubleValue();
        if (d > 0.0d) {
            if (reservedStreams.remove(d)) {
                deleteStreamById(d);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to unreserve stream id: {} streams: {}", d, streams);
                }
            }
        }
    }


    public void deleteStreamById(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Delete streamId: {}", streamId);
        }
        double d = streamId.doubleValue();
        if (d > 0.0d) {
            if (streams.remove(d) != null) {
                usedStreams.decrementAndGet();
                pendingVideos.remove(d);
                streamBuffers.remove(d);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to remove stream id: {} streams: {}", d, streams);
                }
            }
        }
    }

    public void ping(Ping ping) {
        createChannelIfAbsent(2).write(ping);
    }

    public abstract void write(Packet out);

    public abstract void writeRaw(BufFacade out);

    protected void updateBytesRead() {
        if (log.isTraceEnabled()) {
            log.trace("updateBytesRead");
        }
        long bytesRead = getReadBytes();
        System.out.println(bytesRead);
        if (bytesRead >= nextBytesRead) {
            BytesRead sbr = new BytesRead((int) (bytesRead % Integer.MAX_VALUE));
            createChannelIfAbsent(2).write(sbr);
            nextBytesRead += bytesReadInterval;
        }
    }

    public void receivedBytesRead(int bytes) {
        if (log.isDebugEnabled()) {
            log.debug("Client received {} bytes, written {} bytes, {} messages pending", new Object[]{bytes, getWrittenBytes(), getPendingMessages()});
        }
        clientBytesRead.addAndGet(bytes);
    }

    public long getClientBytesRead() {
        return clientBytesRead.get();
    }

    public void invoke(IServiceCall call) {
        invoke(call, 3);
    }

    public int getTransactionId() {
        return transactionId.incrementAndGet();
    }

    public void registerPendingCall(int invokeId, IPendingServiceCall call) {
        pendingCalls.put(invokeId, call);
    }

    public void invoke(IServiceCall call, int channel) {
        // We need to use Invoke for all calls to the client
        Invoke invoke = new Invoke();
        invoke.setCall(call);
        invoke.setTransactionId(getTransactionId());
        if (call instanceof IPendingServiceCall) {
            registerPendingCall(invoke.getTransactionId(), (IPendingServiceCall) call);
        }
        createChannelIfAbsent(channel).write(invoke);
    }

    public void invoke(String method) {
        invoke(method, null, null);
    }

    public void invoke(String method, Object[] params) {
        invoke(method, params, null);
    }

    public void invoke(String method, IPendingServiceCallback callback) {
        invoke(method, null, callback);
    }

    public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
        IPendingServiceCall call = new PendingCall(method, params);
        if (callback != null) {
            call.registerCallback(callback);
        }
        invoke(call);
    }

    public void notify(IServiceCall call) {
        notify(call, 3);
    }

    public void notify(IServiceCall call, int channel) {
        Notify notify = new Notify();
        notify.setCall(call);
        createChannelIfAbsent(channel).write(notify);
    }

    public void notify(String method) {
        notify(method, null);
    }


    public void notify(String method, Object[] params) {
        IServiceCall call = new ServiceCall(method, params);
        notify(call);
    }


    public void status(Status status) {
        status(status, 3);
    }


    public void status(Status status, int channel) {
        if (status != null) {
            createChannelIfAbsent(channel).sendStatus(status);
        }
    }

    @Override
    public long getReadBytes() {
        return getSession().getTrafficCounter().cumulativeReadBytes();
    }


    @Override
    public long getWrittenBytes() {
        return 0;
    }

    public IPendingServiceCall getPendingCall(int invokeId) {
        return pendingCalls.get(invokeId);
    }

    public IPendingServiceCall retrievePendingCall(int invokeId) {
        return pendingCalls.remove(invokeId);
    }

    protected String createStreamName() {
        return UUID.randomUUID().toString();
    }

    protected void writingMessage(Packet message) {
        if (message.getMessage() instanceof VideoData) {
            Number streamId = message.getHeader().getStreamId();
            final AtomicInteger value = new AtomicInteger();
            AtomicInteger old = pendingVideos.putIfAbsent(streamId.doubleValue(), value);
            if (old == null) {
                old = value;
            }
            old.incrementAndGet();
        }
    }

    public synchronized void messageReceived() {
        if (log.isTraceEnabled()) {
            log.trace("messageReceived");
        }
        readMessages.incrementAndGet();
        // trigger generation of BytesRead messages
        updateBytesRead();
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

    @Override
    public void onTaskAdded(ReceivedMessageTaskQueue queue) {
        currentQueueSize.incrementAndGet();
        processTasksQueue(queue);
    }

    @Override
    public void onTaskRemoved(ReceivedMessageTaskQueue queue) {
        currentQueueSize.decrementAndGet();
        processTasksQueue(queue);
    }

    @SuppressWarnings("unchecked")
    private void processTasksQueue(final ReceivedMessageTaskQueue currentStreamTasks) {
        int streamId = currentStreamTasks.getStreamId();
        if (log.isTraceEnabled()) {
            log.trace("Process tasks for streamId {}", streamId);
        }
        final ReceivedMessageTask task = currentStreamTasks.getTaskToProcess();
        if (task == null) {
            if (log.isTraceEnabled()) {
                log.trace("Channel {} task queue is empty", streamId);
            }
            return;
        }
        Packet packet = task.getPacket();
        try {
            final String messageType = getMessageType(packet);
            ListenableFuture<Packet> future = (ListenableFuture<Packet>) executor.submitListenable(new ListenableFutureTask<Packet>(task));
            future.addCallback(new ListenableFutureCallback<Packet>() {

                final long startTime = System.currentTimeMillis();

                int getProcessingTime() {
                    return (int) (System.currentTimeMillis() - startTime);
                }

                public void onFailure(Throwable t) {
                    log.debug("ReceivedMessageTask failure: {}", t);
                    if (log.isWarnEnabled()) {
                        log.warn("onFailure - session: {}, msgtype: {}, processingTime: {}, packetNum: {}", sessionId, messageType, getProcessingTime(), task.getPacketNumber());
                    }
                    currentStreamTasks.removeTask(task);
                    //这里release-
                    task.release();
                }

                public void onSuccess(Packet packet) {
                    log.debug("ReceivedMessageTask success");
                    if (log.isDebugEnabled()) {
                        log.debug("onSuccess - session: {}, msgType: {}, processingTime: {}, packetNum: {}", sessionId, messageType, getProcessingTime(), task.getPacketNumber());
                    }
                    currentStreamTasks.removeTask(task);
                }

            });
        } catch (TaskRejectedException tre) {
            Throwable[] suppressed = tre.getSuppressed();
            for (Throwable t : suppressed) {
                log.warn("Suppressed exception on {}", sessionId, t);
            }
            log.info("Rejected message: {} on {}", packet, sessionId);
            currentStreamTasks.removeTask(task);
        } catch (Throwable e) {
            log.error("Incoming message handling failed on session=[" + sessionId + "]", e);
            if (log.isDebugEnabled()) {
                log.debug("Execution rejected on {} - {}", getSessionId(), RtmpProtocolState.states[getStateCode()]);
                log.debug("Lock permits - decode: {} encode: {}", decoderLock.availablePermits(), encoderLock.availablePermits());
            }
            currentStreamTasks.removeTask(task);
        }

    }

    public void messageSent(Packet message) {
        if (message.getMessage() instanceof VideoData) {
            Number streamId = message.getHeader().getStreamId();
            AtomicInteger pending = pendingVideos.get(streamId.doubleValue());
            if (log.isTraceEnabled()) {
                log.trace("Stream id: {} pending: {} total pending videos: {}", streamId, pending, pendingVideos.size());
            }
            if (pending != null) {
                pending.decrementAndGet();
            }
        }
        writtenMessages.incrementAndGet();
    }

    protected void messageDropped() {
        droppedMessages.incrementAndGet();
    }

    protected int currentQueueSize() {
        return currentQueueSize.get();
    }

    @Override
    public long getPendingVideoMessages(Number streamId) {
        AtomicInteger pendingCount = pendingVideos.get(streamId.doubleValue());
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} pendingCount: {} total pending videos: {}", streamId, pendingCount, pendingVideos.size());
        }
        return pendingCount != null ? pendingCount.intValue() : 0;
    }

    public void sendSharedObjectMessage(String name, int currentVersion, boolean persistent, Set<ISharedObjectEvent> events) {
        // create a new sync message for every client to avoid concurrent access through multiple threads
        SharedObjectMessage syncMessage = state.getEncoding() == Encoding.AMF3 ? new FlexSharedObjectMessage(null, name, currentVersion, persistent) : new SharedObjectMessage(null, name, currentVersion, persistent);
        syncMessage.addEvents(events);
        try {
            // get the channel for so updates
            Channel channel = createChannelIfAbsent(3);
            if (log.isTraceEnabled()) {
                log.trace("Send to channel: {}", channel);
            }
            channel.write(syncMessage);
        } catch (Exception e) {
            log.warn("Exception sending shared object", e);
        }
    }


    public void ping() {
        long newPingTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Send Ping: session=[{}], currentTime=[{}], lastPingTime=[{}]", new Object[]{getSessionId(), newPingTime, lastPingSentOn.get()});
        }
        if (lastPingSentOn.get() == 0) {
            lastPongReceivedOn.set(newPingTime);
        }
        Ping pingRequest = new Ping();
        pingRequest.setEventType(Ping.PING_CLIENT);
        lastPingSentOn.set(newPingTime);
        int now = (int) (newPingTime & 0xffffffffL);
        pingRequest.setValue2(now);
        ping(pingRequest);
    }

    /**
     * Marks that ping back was received.
     *
     * @param pong Ping object
     */
    public void pingReceived(Ping pong) {
        long now = System.currentTimeMillis();
        long previousPingTime = lastPingSentOn.get();
        int previousPingValue = (int) (previousPingTime & 0xffffffffL);
        int pongValue = pong.getValue2().intValue();
        if (log.isDebugEnabled()) {
            log.debug("Pong received: session=[{}] at {} with value {}, previous received at {}", new Object[]{getSessionId(), now, pongValue, previousPingValue});
        }
        if (pongValue == previousPingValue) {
            lastPingRoundTripTime.set((int) ((now - previousPingTime) & 0xffffffffL));
            if (log.isDebugEnabled()) {
                log.debug("Ping response session=[{}], RTT=[{} ms]", new Object[]{getSessionId(), lastPingRoundTripTime.get()});
            }
        } else {
            // don't log the congestion entry unless there are more than X messages waiting
            if (getPendingMessages() > 4) {
                int pingRtt = (int) ((now & 0xffffffffL)) - pongValue;
                log.info("Pong delayed: session=[{}], ping response took [{} ms] to arrive. Connection may be congested, or loopback", new Object[]{getSessionId(), pingRtt});
            }
        }
        lastPongReceivedOn.set(now);
    }

    /**
     * Difference between when the last ping was sent and when the last pong was received.
     *
     * @return last interval of ping minus pong
     */
    public int getLastPingSentAndLastPongReceivedInterval() {
        return (int) (lastPingSentOn.get() - lastPongReceivedOn.get());
    }


    public int getLastPingTime() {
        return lastPingRoundTripTime.get();
    }

    /**
     * Setter for ping interval.
     *
     * @param pingInterval Interval in ms to ping clients. Set to 0 to disable ghost detection code.
     */
    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     * Setter for maximum inactivity.
     *
     * @param maxInactivity Maximum time in ms after which a client is disconnected in case of inactivity.
     */
    public void setMaxInactivity(int maxInactivity) {
        this.maxInactivity = maxInactivity;
    }

    /**
     * Inactive state event handler.
     */
    protected abstract void onInactive();

    /**
     * Sets the scheduler.
     *
     * @param scheduler scheduling service / thread executor
     */
    public void setScheduler(ThreadPoolTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * @return the scheduler
     */
    public ThreadPoolTaskScheduler getScheduler() {
        return scheduler;
    }

    public ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    /**
     * Thread pool for guarding deadlocks
     *
     * @return the deadlockGuardScheduler
     */
    public ThreadPoolTaskScheduler getDeadlockGuardScheduler() {
        return deadlockGuardScheduler;
    }

    /**
     * Thread pool for guarding deadlocks
     *
     * @param deadlockGuardScheduler the deadlockGuardScheduler to set
     */
    public void setDeadlockGuardScheduler(ThreadPoolTaskScheduler deadlockGuardScheduler) {
        this.deadlockGuardScheduler = deadlockGuardScheduler;
    }

    /**
     * Registers deferred result.
     *
     * @param result Result to register
     */
    public void registerDeferredResult(DeferredResult result) {
        deferredResults.add(result);
    }

    /**
     * Unregister deferred result
     *
     * @param result Result to unregister
     */
    public void unregisterDeferredResult(DeferredResult result) {
        deferredResults.remove(result);
    }

    public void rememberStreamBufferDuration(int streamId, int bufferDuration) {
        streamBuffers.put(streamId, bufferDuration);
    }

    /**
     * Set maximum time to wait for valid handshake in milliseconds.
     *
     * @param maxHandshakeTimeout Maximum time in milliseconds
     */
    public void setMaxHandshakeTimeout(int maxHandshakeTimeout) {
        this.maxHandshakeTimeout = maxHandshakeTimeout;
    }

    public long getMaxHandlingTimeout() {
        return maxHandlingTimeout;
    }

    public void setMaxHandlingTimeout(long maxHandlingTimeout) {
        this.maxHandlingTimeout = maxHandlingTimeout;
    }

    public int getChannelsInitialCapacity() {
        return channelsInitialCapacity;
    }

    public void setChannelsInitialCapacity(int channelsInitialCapacity) {
        this.channelsInitialCapacity = channelsInitialCapacity;
    }

    public int getChannelsConcurrencyLevel() {
        return channelsConcurrencyLevel;
    }

    public void setChannelsConcurrencyLevel(int channelsConcurrencyLevel) {
        this.channelsConcurrencyLevel = channelsConcurrencyLevel;
    }

    public int getStreamsInitialCapacity() {
        return streamsInitialCapacity;
    }

    public void setStreamsInitialCapacity(int streamsInitialCapacity) {
        this.streamsInitialCapacity = streamsInitialCapacity;
    }

    public int getStreamsConcurrencyLevel() {
        return streamsConcurrencyLevel;
    }

    public void setStreamsConcurrencyLevel(int streamsConcurrencyLevel) {
        this.streamsConcurrencyLevel = streamsConcurrencyLevel;
    }

    public int getPendingCallsInitialCapacity() {
        return pendingCallsInitialCapacity;
    }

    public void setPendingCallsInitialCapacity(int pendingCallsInitialCapacity) {
        this.pendingCallsInitialCapacity = pendingCallsInitialCapacity;
    }

    public int getPendingCallsConcurrencyLevel() {
        return pendingCallsConcurrencyLevel;
    }

    public void setPendingCallsConcurrencyLevel(int pendingCallsConcurrencyLevel) {
        this.pendingCallsConcurrencyLevel = pendingCallsConcurrencyLevel;
    }

    public int getReservedStreamsInitialCapacity() {
        return reservedStreamsInitialCapacity;
    }

    public void setReservedStreamsInitialCapacity(int reservedStreamsInitialCapacity) {
        this.reservedStreamsInitialCapacity = reservedStreamsInitialCapacity;
    }

    public int getReservedStreamsConcurrencyLevel() {
        return reservedStreamsConcurrencyLevel;
    }

    public void setReservedStreamsConcurrencyLevel(int reservedStreamsConcurrencyLevel) {
        this.reservedStreamsConcurrencyLevel = reservedStreamsConcurrencyLevel;
    }

    /**
     * Specify the size of queue that will trigger audio packet dropping, disabled if it's 0
     *
     * @param executorQueueSizeToDropAudioPackets queue size
     */
    public void setExecutorQueueSizeToDropAudioPackets(Integer executorQueueSizeToDropAudioPackets) {
        this.executorQueueSizeToDropAudioPackets = executorQueueSizeToDropAudioPackets;
    }

    @Override
    public String getProtocol() {
        return "rtmp";
    }


    @Override
    public String toString() {
        if (log.isDebugEnabled()) {
            String id = getClient() != null ? getClient().getId() : null;
            return String.format("%1$s %2$s:%3$s to %4$s client: %5$s session: %6$s state: %7$s", new Object[]{getClass().getSimpleName(), getRemoteAddress(), getRemotePort(), getHost(), id, getSessionId(), RtmpProtocolState.states[getStateCode()]});
        } else {
            Object[] args = new Object[]{getClass().getSimpleName(), getRemoteAddress(), getReadBytes(), getWrittenBytes(), getSessionId(), RtmpProtocolState.states[getStateCode()]};
            return String.format("%1$s from %2$s (in: %3$s out: %4$s) session: %5$s state: %6$s", args);
        }
    }

    /**
     * 使连接保持活动状态并在客户端死机时断开连接的任务。
     */
    private class KeepAliveTask implements Runnable {

        private final AtomicLong lastBytesRead = new AtomicLong(0);

        private volatile long lastBytesReadTime = 0;

        @Override
        public void run() {
            //在连接状态下才能ping
            if (state.getState() != RtmpProtocolState.STATE_CONNECTED) {
                return;
            }
            // 确保作业尚未运行
            if (!running.compareAndSet(false, true)) {
                return;
            }
            if (log.isTraceEnabled()) {
                log.trace("Running keep-alive for {}", getSessionId());
            }

            try {
                // first check connected
                if (!isConnected()) {
                    if (log.isDebugEnabled()) {
                        log.debug("No longer connected, clean up connection. Connection state: {}", RtmpProtocolState.states[state.getState()]);
                    }
                    onInactive();
                    return;
                }
                long now = System.currentTimeMillis();
                //获取连接上当前读取的字节数
                long currentReadBytes = getReadBytes();
                //获取最后读取的字节数
                long previousReadBytes = lastBytesRead.get();
                if (log.isTraceEnabled()) {
                    log.trace("Time now: {} current read count: {} last read count: {}", new Object[]{now, currentReadBytes, previousReadBytes});
                }
                if (currentReadBytes > previousReadBytes) {
                    if (log.isTraceEnabled()) {
                        log.trace("Client is still alive, no ping needed");
                    }
                    // 自上次检查以来，客户端已发送数据，因此没有死机。不需要ping
                    if (lastBytesRead.compareAndSet(previousReadBytes, currentReadBytes)) {
                        //更新时间戳以匹配我们的更新
                        lastBytesReadTime = now;
                    }
                } else {
                    //客户端没有向ping命令发送响应，也没有发送数据太长时间，请断开连接
                    long lastPingTime = lastPingSentOn.get();
                    long lastPongTime = lastPongReceivedOn.get();
                    if (lastPongTime > 0 && (lastPingTime - lastPongTime > maxInactivity) && (now - lastBytesReadTime > maxInactivity)) {
                        log.warn("Closing connection - inactivity timeout: session=[{}], lastPongReceived=[{} ms ago], lastPingSent=[{} ms ago], lastDataRx=[{} ms ago]", new Object[]{getSessionId(), (lastPingTime - lastPongTime), (now - lastPingTime), (now - lastBytesReadTime)});
                        // 以下行处理一个非常常见的支持请求
                        log.warn("Client on session=[{}] has not responded to our ping for [{} ms] and we haven't received data for [{} ms]", new Object[]{getSessionId(), (lastPingTime - lastPongTime), (now - lastBytesReadTime)});
                        onInactive();
                    } else {
                        // 向客户端发送ping命令触发数据发送
                        ping();
                    }
                }

            } catch (Exception e) {
                log.warn("Exception in keepalive for {}", getSessionId(), e);
            } finally {
                // 重置运行标识
                running.compareAndSet(true, false);
            }
        }
    }

    /**
     * 10秒钟内如果握手没有成功则断开连接
     */
    private class WaitForHandshakeTask implements TimerTask, Runnable {

        public WaitForHandshakeTask() {
            if (log.isTraceEnabled()) {
                log.trace("WaitForHandshakeTask created on scheduler: {} for session: {}", scheduler, getSessionId());
            }
        }

        public void run() {
            if (log.isTraceEnabled()) {
                log.trace("WaitForHandshakeTask started for {}", getSessionId());
            }
            // check for connected state before disconnecting
            if (state.getState() != RtmpProtocolState.STATE_CONNECTED) {
                // Client didn't send a valid handshake, disconnect
                log.warn("Closing {}, due to long handshake. State: {}", getSessionId(), RtmpProtocolState.states[getStateCode()]);
                onInactive();
            }
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            waitForHandshakeTimeout = null;
            // Client didn't send a valid handshake, disconnect
            log.warn("Closing {}, with id {} due to long handshake", RTMPConnection.this, getId());
            onInactive();
        }

    }

    private Timeout waitForHandshakeTimeout;

    protected void startWaitForHandshake(ISchedulingService service) {
        waitForHandshakeTimeout = timerO.newTimeout(new WaitForHandshakeTask(), maxHandshakeTimeout, TimeUnit.MILLISECONDS);
    }

    //------------

    public Integer getExecutorQueueSizeToDropAudioPackets() {
        return executorQueueSizeToDropAudioPackets;
    }


    public AtomicInteger getCurrentQueueSize() {
        return currentQueueSize;
    }


    public AtomicLong getPacketSequence() {
        return packetSequence;
    }


    public ConcurrentMap<Integer, ReceivedMessageTaskQueue> getTasksByStreams() {
        return tasksByStreams;
    }

    public ConcurrentMap<Integer, Channel> getChannels() {
        return channels;
    }
}
