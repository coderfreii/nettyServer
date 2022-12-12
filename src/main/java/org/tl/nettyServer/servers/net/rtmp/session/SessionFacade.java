package org.tl.nettyServer.servers.net.rtmp.session;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.tl.nettyServer.servers.net.rtmp.conn.IConnection;
import org.tl.nettyServer.servers.net.rtmp.message.Packet;

import javax.crypto.Cipher;

public interface SessionFacade<T> {
    static String sessionKey = "sessionId";

    void setSession(T t);

    String getSessionId();

    void setConnection(IConnection connection);

    void setCipherIn(Cipher cipherIn);

    void setCipherOut(Cipher cipherOut);

    Cipher getCipherIn();

    Cipher getCipherOut();

    public void write(Packet out);


    public IConnection getConnection();

    public boolean isConnected();

    public void setConnected(boolean connected);

    public ThreadPoolTaskScheduler getDeadlockGuardScheduler();

    public void setDeadlockGuardScheduler(ThreadPoolTaskScheduler deadlockGuardScheduler);

    public ThreadPoolTaskScheduler getScheduler();

    public void setScheduler(ThreadPoolTaskScheduler scheduler);

    public ThreadPoolTaskExecutor getExecutor();

    public void setExecutor(ThreadPoolTaskExecutor executor);

    public int getDefaultServerBandwidth();

    public void setDefaultServerBandwidth(int defaultServerBandwidth);

    public int getDefaultClientBandwidth();

    public void setDefaultClientBandwidth(int defaultClientBandwidth);

    public int getLimitType();

    public void setLimitType(int limitType);

    public boolean isBandwidthDetection();

    public void setBandwidthDetection(boolean bandwidthDetection);

    public int getChannelsInitialCapacity();

    public void setChannelsInitialCapacity(int channelsInitialCapacity);

    public Integer getExecutorQueueSizeToDropAudioPackets();

    public void setExecutorQueueSizeToDropAudioPackets(Integer executorQueueSizeToDropAudioPackets);

    public int getChannelsConcurrencyLevel();

    public void setChannelsConcurrencyLevel(int channelsConcurrencyLevel);

    public int getStreamsInitialCapacity();

    public void setStreamsInitialCapacity(int streamsInitialCapacity);

    public int getStreamsConcurrencyLevel();

    public void setStreamsConcurrencyLevel(int streamsConcurrencyLevel);

    public int getPendingCallsInitialCapacity();

    public void setPendingCallsInitialCapacity(int pendingCallsInitialCapacity);

    public int getPendingCallsConcurrencyLevel();

    public void setPendingCallsConcurrencyLevel(int pendingCallsConcurrencyLevel);

    public int getReservedStreamsInitialCapacity();

    public void setReservedStreamsInitialCapacity(int reservedStreamsInitialCapacity);

    public int getReservedStreamsConcurrencyLevel();

    public void setReservedStreamsConcurrencyLevel(int reservedStreamsConcurrencyLevel);

    public long getMaxHandlingTimeout();

    public void setMaxHandlingTimeout(long maxHandlingTimeout);

    public int getMaxHandshakeTimeout();

    public void setMaxHandshakeTimeout(int maxHandshakeTimeout);

    public int getPingInterval();

    public void setPingInterval(int pingInterval);

    public int getMaxInactivity();

    public void setMaxInactivity(int maxInactivity);
}
