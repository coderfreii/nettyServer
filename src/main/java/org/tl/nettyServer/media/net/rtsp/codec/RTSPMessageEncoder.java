package org.tl.nettyServer.media.net.rtsp.codec;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.object.UnsignedShort;
import org.tl.nettyServer.media.net.http.codec.HTTPMessageEncoder;
import org.tl.nettyServer.media.net.http.message.HTTPMessage;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPacket;
import org.tl.nettyServer.media.session.SessionAccessor;

/**
 * RTSP Message Encoder
 *
 * @author pengliren
 */
public abstract class RTSPMessageEncoder extends HTTPMessageEncoder {

    public RTSPMessageEncoder() {
        super();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object message, ByteBuf out) throws Exception {
        RTSPMinaConnection conn = (RTSPMinaConnection) SessionAccessor.resolveRtspConn(ctx);
        final IConnection prevConn = Red5.getConnectionLocal();
        conn.getWriteLock().lock();
        try {
            if (message instanceof HTTPMessage) {
                BufFacade buf;
                try {
                    buf = encodeBuffer(message);
                    if (buf != null) {
                        BufFacade.wrapperAndCast(out).writeBytes(buf);
                    }
                } catch (Exception e) {
//					throw new ProtocolCodecException(e);
                }
            } else if (message instanceof RTPPacket) {
                RTPPacket rtpPacket = (RTPPacket) message;
                BufFacade temp = rtpPacket.toByteBuffer();
                BufFacade buf = BufFacade.buffer(temp.readableBytes() + 4);
                buf.writeByte((byte) '$');
                // channel
                buf.writeByte((byte) rtpPacket.getChannel());
                // rtp size
                buf.writeBytes(new UnsignedShort(temp.readableBytes()).getBytes());
                buf.writeBytes(temp);

                if (buf != null) {
                    BufFacade.wrapperAndCast(out).writeBytes(buf);
                }
            }
        } finally {
            conn.getWriteLock().unlock();
            Red5.setConnectionLocal(prevConn);
        }
    }
}
