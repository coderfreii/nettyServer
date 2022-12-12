package org.tl.nettyServer.servers.net.rtmp.conn;

import org.tl.nettyServer.servers.event.IEvent;
import org.tl.nettyServer.servers.net.rtmp.codec.RTMP;
import org.tl.nettyServer.servers.net.rtmp.codec.RTMPDecodeState;

import java.util.Map;

public class RTMPConnection extends BaseConnection {
    private RTMP state = new RTMP();

    // 创建解码器状态
    private RTMPDecodeState decoderState;


    public RTMPConnection() {
        decoderState = new RTMPDecodeState(getSessionId());
    }

    /**
     * Marker byte for standard or non-encrypted RTMP data.
     * 标准或非加密RTMP数据的标记字节。
     */
    public static final byte RTMP_NON_ENCRYPTED = (byte) 0x03;

    /**
     * Marker byte for encrypted RTMP data.
     * 加密RTMP数据标记字节
     */
    public static final byte RTMP_ENCRYPTED = (byte) 0x06;

    /**
     * Marker byte for encrypted RTMP data XTEA. http://en.wikipedia.org/wiki/XTEA
     * 加密的rtmp数据xtea的标记字节
     */
    public static final byte RTMP_ENCRYPTED_XTEA = (byte) 0x08;

    /**
     * Marker byte for encrypted RTMP data using Blowfish. http://en.wikipedia.org/wiki/Blowfish_(cipher)
     * 使用Blowfish加密RTMP数据的标记字节
     */
    public static final byte RTMP_ENCRYPTED_BLOWFISH = (byte) 0x09;

    /**
     * Unknown type 0x0a, seen on youtube
     * 未知类型0x0a，在YouTube上看到
     */
    public static final byte RTMP_ENCRYPTED_UNK = (byte) 0x0a;

    public void setState(RTMP state) {
        this.state = state;
    }

    public RTMP getState() {
        return state;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Encoding getEncoding() {
        return null;
    }

    @Override
    public Duty getDuty() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public Map<String, Object> getConnectParams() {
        return null;
    }

    @Override
    public long getReadBytes() {
        return 0;
    }

    @Override
    public long getWrittenBytes() {
        return 0;
    }

    @Override
    public long getReadMessages() {
        return 0;
    }

    @Override
    public long getWrittenMessages() {
        return 0;
    }

    @Override
    public long getDroppedMessages() {
        return 0;
    }

    @Override
    public long getPendingMessages() {
        return 0;
    }

    @Override
    public long getClientBytesRead() {
        return 0;
    }

    @Override
    public void ping() {

    }

    @Override
    public int getLastPingTime() {
        return 0;
    }

    @Override
    public void setBandwidth(int mbits) {

    }

    @Override
    public Number getStreamId() {
        return null;
    }

    @Override
    public void setStreamId(Number id) {

    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public void dispatchEvent(IEvent event) {

    }

    @Override
    public boolean handleEvent(IEvent event) {
        return false;
    }

    @Override
    public void notifyEvent(IEvent event) {

    }


    byte getStateCode() {
        return this.getState().getState();
    }


    public RTMPDecodeState getDecoderState() {
        return this.decoderState;
    }

    public Number getStreamIdForChannelId(int channelId) {
        if (channelId < 4) {
            return 0;
        }
        Number streamId = Math.floor(((channelId - 4) / 5.0d) + 1);

        return streamId;
    }
}
