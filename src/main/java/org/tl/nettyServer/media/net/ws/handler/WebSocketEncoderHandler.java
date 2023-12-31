package org.tl.nettyServer.media.net.ws.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.codec.HTTPResponseEncoder;
import org.tl.nettyServer.media.net.ws.message.FrameUtils;
import org.tl.nettyServer.media.net.ws.message.WebSocketFrame;

public class WebSocketEncoderHandler extends MessageToByteEncoder<Object> {
    private HTTPResponseEncoder he = new HTTPResponseEncoder();


    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        BufFacade bufFacade = null;
        if (msg instanceof WebSocketFrame) {
            bufFacade = FrameUtils.genFinalFrameByte(msg);
        } else if (msg instanceof BufFacade) {
            ctx.writeAndFlush(((BufFacade<?>) msg).getBuf());
        } else {
            bufFacade = he.encodeBuffer(msg);
        }

        if (bufFacade != null && bufFacade.readable()) {
            ctx.writeAndFlush(bufFacade.getBuf());
        }
    }
}
