package org.tl.nettyServer.servers.conf;

/**
 * @author pengliren
 * @ClassName: Configuration
 * @Description: 读取配置信息
 */
public class ExtConfiguration {

    public static String HTTP_HOST = "0.0.0.0";
    public static int HTTP_PORT = 80;
    public static int HTTP_IO_THREADS = 2;
    public static int HTTP_WORKER_THREADS = 10;
    public static int HTTP_SEND_BUFFER_SIZE = 65536;
    public static int HTTP_RECEIVE_BUFFER_SIZE = 65536;
    public static boolean HTTP_TCP_NODELAY = true;
    public static int HTTP_MAX_BACKLOG = 5000;
    public static int HTTP_IDLE = 30;

    public static int HLS_SEGMENT_MAX = 3;
    public static int HLS_SEGMENT_TIME = 10;
    public static boolean HLS_ENCRYPT = false;

    public static String RTMP_HOST = "0.0.0.0";
    public static int RTMP_PORT = 1935;
    public static int RTMP_IO_THREADS = 2;
    public static int RTMP_WORKER_THREADS = 10;
    public static int RTMP_SEND_BUFFER_SIZE = 271360;
    public static int RTMP_RECEIVE_BUFFER_SIZE = 65536;
    public static int RTMP_PING_INTERVAL = 1000;
    public static int RTMP_MAX_INACTIVITY = 60000;
    public static int RTMP_MAX_HANDSHAKE_TIMEOUT = 5000;
    public static boolean RTMP_TCP_NODELAY = true;
    public static int RTMP_MAX_BACKLOG = 5000;
    public static int RTMP_DEFAULT_SERVER_BANDWIDTH = 10000000;
    public static int RTMP_DEFAULT_CLIENT_BANDWIDTH = 10000000;
    public static int RTMP_CLIENT_BANDWIDTH_LIMIT_TYPE = 2;
    public static boolean RTMP_BANDWIDTH_DETECTION = true;

    public static String RTSP_HOST = "0.0.0.0";
    public static int RTSP_PORT = 554;
    public static int RTSP_IO_THREADS = 2;
    public static int RTSP_WORKER_THREADS = 10;
    public static int RTSP_SEND_BUFFER_SIZE = 65536;
    public static int RTSP_RECEIVE_BUFFER_SIZE = 65536;
    public static boolean RTSP_TCP_NODELAY = true;
    public static int RTSP_MAX_BACKLOG = 8000;
    public static int UDP_PORT_START = 6970;

    public static String JMX_RMI_HOST = "0.0.0.0";
    public static String JMX_RMI_PORT_REMOTEOBJECTS = "";
    public static int JMX_RMI_PORT_REGISTRY = 9999;
    public static boolean JMX_RMI_ENABLE = false;

    public static long NOTIFY_SYSTIMER_TICK = 20;

    public static int FILECACHE_MAXSIZE = 500;
    public static int FILECACHE_PURGE = 10;
    public static int CACHE_INTERVAL = 10;


    public static int MULTICAST_EXECUTOR_THREADS = 4;
    public static int UNICAST_EXECUTOR_THREADS = 4;


    public static int POOL_SIZE = 8;
    public static int CORE_POOL_SIZE = 4;
    public static int MAX_POOL_SIZE = 12;
    public static int QUEUE_CAPACITY = 64;
    public static int DEAD_POOL_SIZE = 8;
    public static long BASE_TO_LERANCE = 5000;
    public static boolean DROP_LIVE_FUTURE = false;

    public static int ENTRY_MAX = 500;
    public static int SO_POOL_SIZE = 4;
    public static int INTERVAL = 5000;
    public static int TRIGGER = 100;

    public static int IO_THREADS = 8;
    public static int BUFFER_SIZE = 65536;
    public static int RECEIVE_BUFFER_SIZE = 65536;
    public static int TRAFFIC_CLASS = -1;
    public static int BACK_LOG = 32;
    public static boolean TCP_NO_DELAY = true;
    public static boolean KEEP_ALIVE = true;
    public static int THOUGH_PUT_CALC_INTERVAL = 15;
    public static boolean ENABLED_EFAULT_ACCEPTOR = true;
    public static int INITIAL_POOL_SIZE = 2;
    public static int MAX_PROCESSOR_POOL_SIZE = 8;
    public static int EXECUTOR_KEEP_ALIVE_TIME = 60000;
    public static int MINA_POLL_INTERVAL = 1000;
    public static boolean ENABLE_MINA_MONITOR = false;
    public static boolean ENABLE_MINA_LOG_FILTER = false;
    public static int PING_INTERVAL = 1000;
    public static int MAX_INACTIVITY = 60000;
    public static int MAX_HANDSHAKE_TIMEOUT = 5000;
    public static int DEFAULT_SERVER_BANDWIDTH = 10000000;
    public static int DEFAULT_CLIENT_BANDWIDTH = 10000000;
    public static int LIMIT_TYPE = 2;
    public static boolean BANDWIDTH_DETECTION = false;
    public static int MAX_HANDLING_TIMEOUT = 2000;
    public static int EXECUTOR_QUEUE_SIZE_TO_DROP_AUDIO_PACKETS = 60;
    public static int CHANNELS_INITAL_CAPACITY = 3;
    public static int CHANNELS_CONCURRENCY_LEVEL = 1;
    public static int STREAMS_INITAL_CAPACITY = 1;
    public static int STREAMS_CONCURRENCY_LEVEL = 1;
    public static int PENDING_CALLS_INITAL_CAPACITY = 3;
    public static int PENDING_CALLS_CONCURRENCY_LEVEL = 1;
    public static int RESERVED_STREAMS_INITAL_CAPACITY = 1;
    public static int RESERVED_STREAMS_CONCURREN_CYLEVEL = 1;
    public static int TARGET_RESPONSE_SIZE = 32768;
    public static int PING_INTERVALT = 5000;
    public static int MAX_INACTIVITY_T = 60000;
    public static int MAX_HANDSHAKE_TIMEOUTT = 5000;
    public static int MAX_IN_MESSAGES_PERPROCESS = 166;
    public static int MAX_QUEUE_OFFER_TIME = 125;
    public static int MAX_QUEUE_OFFER_ATTEMPTS = 4;


    public static String RTMPS_HOST = "0.0.0.0";
    public static int RTMPS_PORT = 8443;
    public static int RTMPS_PING_INTERVAL = 5000;
    public static int RTMPS_MAX_INACTIVITY = 60000;
    public static int RTMPS_MAX_KEEP_ALIVE_REQUESTS = -1;
    public static int RTMPS_MAX_THREADS = 8;
    public static int RTMPS_ACCEPTOR_THREAD_COUNT = 2;
    public static int RTMPS_PROCESSOR_CACHE = 20;
    public static String RTMPS_KEYSTOREPASS = "123456";
    public static String RTMPS_KEYSTOREFILE = "classpath:conf/keystore.jks";
    public static String RTMPS_TRUSTSTOREPASS = "123456";
    public static String RTMPS_TRUSTSTOREFILE = "classpath:conf/truststore.jks";
}
