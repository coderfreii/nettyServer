package org.tl.nettyServer.media.net.rtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.codec.RtmpProtocolState;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.session.NettySessionFacade;
import org.tl.nettyServer.media.session.SessionFacade;

import javax.crypto.Cipher;
import java.util.List;

@Slf4j
public class RTMPEHandler extends MessageToMessageDecoder<BufFacade<ByteBuf>> {
    @Override
    protected void decode(ChannelHandlerContext ctx, BufFacade<ByteBuf> in, List<Object> out) throws Exception {
        Attribute<RTMPConnection> attr = ctx.channel().attr(NettySessionFacade.connectionAttributeKey);
        RTMPConnection connection = attr.get();
        SessionFacade session = connection.getSession();
        RtmpProtocolState state = connection.getState();

        switch (state.getState()) {
            case RtmpProtocolState.STATE_CONNECTED:
                // assuming majority of connections will not be encrypted
                if (!state.isEncrypted()) {
                    out.add(in);
                } else {
                    Cipher cipher = session.getCipherIn();
                    if (cipher != null) {
                        byte[] encrypted = new byte[in.readableBytes()];
                        in.readBytes(encrypted);
                        in.release();
                        byte[] plain = cipher.update(encrypted);
                        BufFacade messageDecrypted = BufFacade.wrappedBuffer(plain);
                        if (log.isDebugEnabled()) {
                            log.debug("Receiving decrypted message: {}", messageDecrypted);
                        }
                        out.add(messageDecrypted);
                    }
                }
                break;
            case RtmpProtocolState.STATE_ERROR:
            case RtmpProtocolState.STATE_DISCONNECTING:
            case RtmpProtocolState.STATE_DISCONNECTED:
                // do nothing, really
                log.debug("Nothing to do, connection state: {}", state);
                in.release();
                break;
            default:
                in.release();
                throw new IllegalStateException("Invalid RtmpProtocolState state: " + state);
        }
    }
}
