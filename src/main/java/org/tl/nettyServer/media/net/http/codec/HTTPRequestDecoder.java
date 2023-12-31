package org.tl.nettyServer.media.net.http.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CodecException;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.message.HTTPMessage;
import org.tl.nettyServer.media.net.http.message.HTTPMethod;
import org.tl.nettyServer.media.net.http.message.HTTPVersion;
import org.tl.nettyServer.media.net.http.request.DefaultHttpRequest;

import java.util.List;

public class HTTPRequestDecoder extends HTTPMessageDecoder {

    @Override
    protected HTTPMessage createMessage(String[] initialLine) throws Exception {
        return new DefaultHttpRequest(HTTPVersion.valueOf(initialLine[2]), HTTPMethod.valueOf(initialLine[0]), initialLine[1]);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            while (in.readableBytes() > 0) {
                DecodeState obj = decodeBuffer(BufFacade.wrapperAndCast(in));
                if (obj.getState() == DecodeState.ENOUGH) {
                    if (obj.getObject() != null) {
                        out.add(obj.getObject());
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw new CodecException(e);
        }
    }
}
