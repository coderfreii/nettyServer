package org.tl.nettyServer.media.net.rtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.codec.RTMPProtocolDecoder;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.util.List;

@Slf4j
public class RtmpByteToPacketHandler extends MessageToMessageDecoder<BufFacade<ByteBuf>> {
    private RTMPProtocolDecoder decoder = new RTMPProtocolDecoder();

    //放到connection里面
    private BufFacade bufFacadeStore = BufFacade.buffer(0);

    @Override
    protected void decode(ChannelHandlerContext ctx, BufFacade<ByteBuf> msg, List<Object> out) throws Exception {
        RTMPConnection connection = (RTMPConnection) SessionAccessor.resolveConn(ctx);
        if (bufFacadeStore.readable()) {
            log.info("left {}", bufFacadeStore.readableBytes());
        }
        bufFacadeStore.writeBytes(msg);
        List<Object> objects = decoder.decodeBuffer(connection, bufFacadeStore);
        out.addAll(objects);
    }
}
