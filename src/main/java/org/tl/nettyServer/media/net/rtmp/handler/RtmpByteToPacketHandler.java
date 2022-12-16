package org.tl.nettyServer.media.net.rtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.codec.RTMPProtocolDecoder;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.util.Collections;
import java.util.List;

@Slf4j
public class RtmpByteToPacketHandler extends MessageToMessageDecoder<BufFacade<ByteBuf>> {
    private RTMPProtocolDecoder decoder = new RTMPProtocolDecoder();

    private BufFacade bufFacadeStore;

    @Override
    protected void decode(ChannelHandlerContext ctx, BufFacade<ByteBuf> msg, List<Object> out) throws Exception {
        RTMPConnection connection = (RTMPConnection) SessionAccessor.resolveConn(ctx);
        //
        int length = 0;
        if (this.bufFacadeStore != null) {
            length += this.bufFacadeStore.readableBytes();
        }
        length += msg.readableBytes();
        BufFacade<ByteBuf> in = BufFacade.buffer(length);
        if (this.bufFacadeStore != null) {
            log.info("withdraw {}  bytes", this.bufFacadeStore.readableBytes());
            in.writeBytes(this.bufFacadeStore);
        }
        in.writeBytes(msg);
        msg.release();

        List<Object> objects = Collections.emptyList();
        try {
            objects = decoder.decodeBuffer(connection, in);
        } finally {
            if (in.readable()) {
                if (this.bufFacadeStore == null) {
                    this.bufFacadeStore = BufFacade.buffer(in.readableBytes());
                }
                this.bufFacadeStore.writeBytes(in);
                log.info("store {}  bytes", this.bufFacadeStore.readableBytes());
            }
            in.release();
            out.addAll(objects);
        }
    }
}
