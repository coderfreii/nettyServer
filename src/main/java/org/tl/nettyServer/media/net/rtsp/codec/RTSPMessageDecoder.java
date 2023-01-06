package org.tl.nettyServer.media.net.rtsp.codec;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.codec.DecodeState;
import org.tl.nettyServer.media.net.http.codec.HTTPMessageDecoder;
import org.tl.nettyServer.media.net.http.message.HTTPMessage;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.message.RTSPChannelData;
import org.tl.nettyServer.media.net.rtsp.message.RTSPHeaders;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.util.List;

/**
 * RTSP Message Decoder
 *
 * @author pengliren
 */
public abstract class RTSPMessageDecoder extends HTTPMessageDecoder {

    @Override
    protected boolean isContentAlwaysEmpty(HTTPMessage msg) {
        // Unlike HTTP, RTSP always assumes zero-length body if Content-Length
        // header is absent.
        boolean empty = super.isContentAlwaysEmpty(msg);
        if (empty) {
            return true;
        }
        if (!msg.containsHeader(RTSPHeaders.Names.CONTENT_LENGTH)) {
            return true;
        }
        return empty;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //get the connection from the session
        RTSPMinaConnection conn = (RTSPMinaConnection) SessionAccessor.resolveRtspConn(ctx);
        conn.getWriteLock().lock();
        try {
            boolean ret = false;
            int pos = 0;
            while (in.readableBytes() > 4) {
                if (in.readByte() == 36) {
                    byte channel = in.readByte();// channel
                    int len = (int) (in.readShort() & 0xFFFF);
                    if (len > in.readableBytes()) {
                        pos = in.readerIndex();
                        in.readerIndex(pos - 4);
                        ret = false;
                        break;
                    } else {
                        byte[] temp = new byte[len];
                        in.readBytes(temp);
                        BufFacade data = BufFacade.wrappedBuffer(temp);
                        RTSPChannelData channelData = new RTSPChannelData(channel, data);
                        out.add(channelData);
                        ret = true;
                    }
                } else {  //解析http请求
                    pos = in.readerIndex();
                    in.readerIndex(pos - 1);
                    DecodeState obj = decodeBuffer(BufFacade.wrapperAndCast(in));
                    if (obj.getState() == DecodeState.ENOUGH) {
                        ret = true;
                        if (obj.getObject() != null) out.add(obj.getObject());
                    } else {
                        ret = false;
                        break;
                    }
                }
            }
//			return ret;
        } catch (Exception e) {
//			throw new ProtocolCodecException(e);
        } finally {
            conn.getWriteLock().unlock();
        }
    }
}
