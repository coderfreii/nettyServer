package org.tl.nettyServer.media.session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;

import javax.crypto.Cipher;
import java.net.SocketAddress;

@Data
@Slf4j
public class NettyRtspSessionFacade implements SessionFacade<ChannelHandlerContext, RTSPMinaConnection> {
    public static AttributeKey<RTSPMinaConnection> connectionAttributeKey = AttributeKey.valueOf(sessionKey);

    /**
     * 框架上下文
     */
    private ChannelHandlerContext context;

    /**
     * 连接
     */
    private RTSPMinaConnection connection;

    /**
     * 密码在
     */
    private Cipher cipherIn;
    /**
     * 算出
     */
    private Cipher cipherOut;


    @Override
    public void setContext(ChannelHandlerContext channelHandlerContext) {
        this.context = channelHandlerContext;
    }

    @Override
    public String getSessionId() {
        long l = Long.parseLong(this.context.channel().id().asShortText(), 16);
        return String.valueOf(l);
    }

    @Override
    public boolean isConnected() {
        return this.context.channel().isActive();


    }

    @Override
    public SocketAddress getRemoteAddress() {
        return context.channel().remoteAddress();
    }

    @Override
    public SocketAddress getLocalAddress() {
        return context.channel().localAddress();
    }

    @Override
    public ChannelFuture write(Object o) {
        return context.writeAndFlush(o);
    }

    @Override
    public void closeOnFlush() {
        this.context.flush();
        this.context.close();
    }

    private TrafficCounter trafficCounter;
    @Override
    public void setTrafficCounter(TrafficCounter trafficCounter) {
        this.trafficCounter = trafficCounter;
    }

    @Override
    public TrafficCounter getTrafficCounter() {
        return this.trafficCounter;
    }
}
