package org.tl.nettyServer.servers.net.rtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnManager;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.servers.net.rtmp.session.NettySessionFacade;

public class ConnInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }


    // session -> ctx, conn
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Attribute<NettySessionFacade> attr = ctx.channel().attr(NettySessionFacade.sessionKeyAttr);
        if (attr.get() == null) {
            RTMPConnection connection = RTMPConnManager.getInstance().createConnection(RTMPConnection.class);
            NettySessionFacade nettySessionFacade = new NettySessionFacade();
            nettySessionFacade.setSession(ctx);
            nettySessionFacade.setConnection(connection);
            connection.setSession(nettySessionFacade);
        }
        super.channelRead(ctx, BufFacade.wrapperAndCast(msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
