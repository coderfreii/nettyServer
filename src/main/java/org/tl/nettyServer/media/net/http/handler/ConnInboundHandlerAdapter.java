package org.tl.nettyServer.media.net.http.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.session.NettyHttpSessionFacade;
import org.tl.nettyServer.media.session.NettyRtspSessionFacade;
import org.tl.nettyServer.media.session.SessionAccessor;

public class ConnInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Attribute<HTTPConnection> attr = ctx.channel().attr(NettyHttpSessionFacade.connectionAttributeKey);
        if (attr.get() == null) {
            // create http connection
            NettyHttpSessionFacade session = new NettyHttpSessionFacade();
            session.setContext(ctx);
            HTTPConnection connection = new HTTPConnection(session);
            session.setConnection(connection);
            attr.set(connection);
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // check play stram is null
        HTTPConnection conn = SessionAccessor.resolveHttpConn(ctx);
        if (conn != null) {
            conn.close();
        }
        super.channelInactive(ctx);
    }
}
