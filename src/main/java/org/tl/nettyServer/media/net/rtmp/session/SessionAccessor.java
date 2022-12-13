package org.tl.nettyServer.media.net.rtmp.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;

public class SessionAccessor {
    static public IConnection resolveConn(ChannelHandlerContext ctx) {
        Attribute<RTMPConnection> attr = ctx.channel().attr(NettySessionFacade.connectionAttributeKey);
        return attr.get();
    }
}
