package org.tl.nettyServer.servers.net.http.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.http.message.HTTPMessage;
import org.tl.nettyServer.servers.net.http.response.HTTPResponse;

import java.net.ProtocolException;

/**
 * HTTP Response Encoder
 *
 * @author pengliren
 */
public class HTTPResponseEncoder extends HTTPMessageEncoder {

    public HTTPResponseEncoder() {
        super();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HTTPMessage msg, ByteBuf out) throws Exception {
        try {
            BufFacade buf = encodeBuffer(msg);
            if (buf != null) {
                BufFacade.wrapperAndCast(out).writeBytes(buf);
            }
        } catch (Exception e) {
            throw new ProtocolException();
        }
    }


    @Override
    protected void encodeInitialLine(BufFacade buf, HTTPMessage message) throws Exception {
        HTTPResponse response = (HTTPResponse) message;
        buf.writeBytes(response.getProtocolVersion().toString().getBytes("ASCII"));
        buf.writeByte(HTTPCodecUtil.SP);
        buf.writeBytes(String.valueOf(response.getStatus().getCode()).getBytes("ASCII"));
        buf.writeByte(HTTPCodecUtil.SP);
        buf.writeBytes(String.valueOf(response.getStatus().getReasonPhrase()).getBytes("ASCII"));
        buf.writeByte(HTTPCodecUtil.CR);
        buf.writeByte(HTTPCodecUtil.LF);
    }

}
