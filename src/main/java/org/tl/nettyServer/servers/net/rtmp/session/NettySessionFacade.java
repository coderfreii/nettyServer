package org.tl.nettyServer.servers.net.rtmp.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.tl.nettyServer.servers.net.rtmp.conn.IConnection;

import javax.crypto.Cipher;

public class NettySessionFacade implements SessionFacade<ChannelHandlerContext> {
    private ChannelHandlerContext context;
    private IConnection connection;
    public static AttributeKey<NettySessionFacade> sessionKeyAttr = AttributeKey.valueOf(sessionKey);

    private Cipher cipherIn;
    private Cipher cipherOut;

    @Override
    public void setSession(ChannelHandlerContext channelHandlerContext) {
        this.context = channelHandlerContext;
        this.context.channel().attr(sessionKeyAttr).set(this);
    }

    @Override
    public void setConnection(IConnection connection) {
        this.connection = connection;
    }

    @Override
    public void setCipherIn(Cipher cipherIn) {
        this.cipherIn = cipherIn;
    }

    @Override
    public void setCipherOut(Cipher cipherOut) {
        this.cipherOut = cipherOut;
    }

    @Override
    public Cipher getCipherIn() {
        return this.cipherIn;
    }

    @Override
    public Cipher getCipherOut() {
        return this.cipherOut;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }

    public IConnection getConnection() {
        return connection;
    }
}
