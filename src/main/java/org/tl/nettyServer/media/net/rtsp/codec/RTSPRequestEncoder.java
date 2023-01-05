package org.tl.nettyServer.media.net.rtsp.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.object.UnsignedShort;
import org.tl.nettyServer.media.net.http.message.HTTPMessage;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPacket;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.nio.charset.CharacterCodingException;

/**
 * RTSP Request Encoder
 *
 * @author pengliren
 */
public class RTSPRequestEncoder extends RTSPMessageEncoder {

    public RTSPRequestEncoder() throws CharacterCodingException {
        super();
    }


    @Override
    protected void encodeInitialLine(BufFacade buf, HTTPMessage message)
            throws Exception {

        HTTPRequest request = (HTTPRequest) message;
        buf.writeBytes(request.getMethod().toString().getBytes("ASCII"));
        buf.writeByte((byte) ' ');
        buf.writeBytes(request.getUri().getBytes("ASCII"));
        buf.writeByte((byte) ' ');
        buf.writeBytes(request.getProtocolVersion().toString().getBytes("ASCII"));
        buf.writeByte((byte) '\r');
        buf.writeByte((byte) '\n');
    }

}
