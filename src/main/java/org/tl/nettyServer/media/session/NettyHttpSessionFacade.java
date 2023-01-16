package org.tl.nettyServer.media.session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.AttributeKey;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;

import javax.crypto.Cipher;
import java.net.SocketAddress;

public class NettyHttpSessionFacade implements SessionFacade<ChannelHandlerContext, HTTPConnection> {
    public static AttributeKey<HTTPConnection> connectionAttributeKey = AttributeKey.valueOf(sessionKey);

    /**
     * 框架上下文
     */
    private ChannelHandlerContext context;

    /**
     * 连接
     */
    private HTTPConnection connection;


    @Override
    public void setContext(ChannelHandlerContext channelHandlerContext) {
        this.context = channelHandlerContext;
    }

    @Override
    public String getSessionId() {
        return this.connection.getSessionId();
    }

    @Override
    public void setConnection(HTTPConnection connection) {
        this.connection = connection;
    }

    @Override
    public HTTPConnection getConnection() {
        return this.connection;
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
    public void setCipherIn(Cipher cipherIn) {

    }

    @Override
    public void setCipherOut(Cipher cipherOut) {

    }

    @Override
    public Cipher getCipherIn() {
        return null;
    }

    @Override
    public Cipher getCipherOut() {
        return null;
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
