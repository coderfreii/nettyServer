package org.tl.nettyServer.media.net.rtsp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.session.NettyRtspSessionFacade;

public class ConnInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Attribute<RTSPMinaConnection> attr = ctx.channel().attr(NettyRtspSessionFacade.connectionAttributeKey);
        if (attr.get() == null) {
            // create rtsp connection
            NettyRtspSessionFacade session = new NettyRtspSessionFacade();
            session.setContext(ctx);
            RTSPMinaConnection connection = new RTSPMinaConnection(session);
            session.setConnection(connection);
            attr.set(connection);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // check play stram is null
        RTSPMinaConnection conn = ctx.channel().attr(NettyRtspSessionFacade.connectionAttributeKey).get();
        if (conn != null) {
            conn.close();
        }
        super.channelInactive(ctx);
    }
}
