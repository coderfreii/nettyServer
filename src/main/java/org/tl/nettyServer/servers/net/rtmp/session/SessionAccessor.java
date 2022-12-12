package org.tl.nettyServer.servers.net.rtmp.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.tl.nettyServer.servers.net.rtmp.conn.IConnection;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnection;

public class SessionAccessor {
    static public IConnection resolveConn(ChannelHandlerContext ctx) {
        Attribute<NettySessionFacade> attr = ctx.channel().attr(NettySessionFacade.sessionKeyAttr);
        NettySessionFacade nettySessionFacade = attr.get();
        RTMPConnection connection = (RTMPConnection) nettySessionFacade.getConnection();
        return connection;
    }

    static public SessionFacade resolveSession(ChannelHandlerContext ctx) {
        Attribute<NettySessionFacade> attr = ctx.channel().attr(NettySessionFacade.sessionKeyAttr);
        SessionFacade nettySessionFacade = attr.get();
        return nettySessionFacade;
    }
}
