package org.tl.nettyServer.servers.net.rtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.rtmp.codec.RTMP;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.servers.net.rtmp.session.NettySessionFacade;

import javax.crypto.Cipher;
import java.util.List;

@Slf4j
public class RTMPEHandler extends MessageToMessageDecoder<BufFacade<ByteBuf>> {
    @Override
    protected void decode(ChannelHandlerContext ctx, BufFacade<ByteBuf> in, List<Object> out) throws Exception {
        Attribute<NettySessionFacade> attr = ctx.channel().attr(NettySessionFacade.sessionKeyAttr);
        NettySessionFacade session = attr.get();
        RTMPConnection connection = (RTMPConnection) session.getConnection();
        RTMP state = connection.getState();


        switch (state.getState()) {
            case RTMP.STATE_CONNECTED:
                // assuming majority of connections will not be encrypted
                if (!state.isEncrypted()) {
                    out.add(in);
                } else {
                    Cipher cipher = session.getCipherIn();
                    if (cipher != null) {
                        byte[] encrypted = new byte[in.readableBytes()];
                        in.readBytes(encrypted);
                        in.clear();
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
            case RTMP.STATE_ERROR:
            case RTMP.STATE_DISCONNECTING:
            case RTMP.STATE_DISCONNECTED:
                // do nothing, really
                log.debug("Nothing to do, connection state: {}", state);
                break;
            default:
                throw new IllegalStateException("Invalid RTMP state: " + state);
        }
    }
}
