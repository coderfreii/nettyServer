package org.tl.nettyServer.servers.net.rtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.rtmp.codec.RTMPProtocolDecoder;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.servers.net.rtmp.session.SessionAccessor;

import java.util.List;

public class RtmpDecodeToPacketHandler extends MessageToMessageDecoder<BufFacade<ByteBuf>> {
    private RTMPProtocolDecoder decoder = new RTMPProtocolDecoder();

    @Override
    protected void decode(ChannelHandlerContext ctx, BufFacade<ByteBuf> msg, List<Object> out) throws Exception {
        RTMPConnection connection = (RTMPConnection) SessionAccessor.resolveConn(ctx);
        List<Object> objects = decoder.decodeBuffer(connection, msg);
    }
}
