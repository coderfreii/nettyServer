package org.tl.nettyServer.media.net.rtmp.handler.packet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.Attribute;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.session.NettyRtmpSessionFacade;

public class ChannelTrafficShapingSessionHandler extends ChannelTrafficShapingHandler {
    public ChannelTrafficShapingSessionHandler(long writeLimit, long readLimit, long checkInterval, long maxTime) {
        super(writeLimit, readLimit, checkInterval, maxTime);
    }

    public ChannelTrafficShapingSessionHandler(long writeLimit, long readLimit, long checkInterval) {
        super(writeLimit, readLimit, checkInterval);
    }

    public ChannelTrafficShapingSessionHandler(long writeLimit, long readLimit) {
        super(writeLimit, readLimit);
    }

    public ChannelTrafficShapingSessionHandler(long checkInterval) {
        super(checkInterval);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Attribute<RTMPConnection> attr = ctx.channel().attr(NettyRtmpSessionFacade.connectionAttributeKey);
        if (attr.get() != null) {
            TrafficCounter trafficCounter = trafficCounter();
            NettyRtmpSessionFacade session = new NettyRtmpSessionFacade();
            session.setContext(ctx);
            session.setConnection(attr.get());
            session.setTrafficCounter(trafficCounter);
            attr.get().setSession(session);
        }
        super.channelActive(ctx);
    }

    @Override
    protected long calculateSize(Object msg) {
        if (msg instanceof BufFacade) {
            return ((BufFacade<?>) msg).readableBytes();
        }
        return super.calculateSize(msg);
    }
}
