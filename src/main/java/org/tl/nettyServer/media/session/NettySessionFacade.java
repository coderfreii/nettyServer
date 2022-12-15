package org.tl.nettyServer.media.session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;

import javax.crypto.Cipher;
import java.net.SocketAddress;

@Data
@Slf4j
public class NettySessionFacade implements SessionFacade<ChannelHandlerContext, RTMPConnection> {
    public static AttributeKey<RTMPConnection> connectionAttributeKey = AttributeKey.valueOf(sessionKey);

    /**
     * 框架上下文
     */
    private ChannelHandlerContext context;

    /**
     * 连接
     */
    private RTMPConnection connection;

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
        return this.connection.getSessionId();
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
    public ChannelFuture write(Object o) {
        return context.writeAndFlush(o);
    }

    @Override
    public void closeOnFlush() {
        this.context.flush();
        this.context.close();
    }
}
