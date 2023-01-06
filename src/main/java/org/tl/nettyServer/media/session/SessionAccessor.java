package org.tl.nettyServer.media.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;

public class SessionAccessor {
    static public IConnection resolveRtmpConn(ChannelHandlerContext ctx) {
        Attribute<RTMPConnection> attr = ctx.channel().attr(NettyRtmpSessionFacade.connectionAttributeKey);
        return attr.get();
    }


    static public IConnection resolveRtspConn(ChannelHandlerContext ctx) {
        Attribute<RTSPMinaConnection> attr = ctx.channel().attr(NettyRtspSessionFacade.connectionAttributeKey);
        return attr.get();
    }
}
