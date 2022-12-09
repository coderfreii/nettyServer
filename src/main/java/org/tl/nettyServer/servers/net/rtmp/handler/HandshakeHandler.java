package org.tl.nettyServer.servers.net.rtmp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.rtmp.codec.RTMP;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.servers.net.rtmp.message.Constants;
import org.tl.nettyServer.servers.net.rtmp.session.NettySessionFacade;

import java.util.List;

@Slf4j
public class HandshakeHandler extends MessageToMessageDecoder<BufFacade<ByteBuf>> {
    private HandShake handshake = new HandShake();

    @Override
    protected void decode(ChannelHandlerContext ctx, BufFacade<ByteBuf> in, List<Object> out) throws Exception {
        Attribute<NettySessionFacade> attr = ctx.channel().attr(NettySessionFacade.sessionKeyAttr);
        NettySessionFacade nettySessionFacade = attr.get();
        RTMPConnection connection = (RTMPConnection) nettySessionFacade.getConnection();
        RTMP state = connection.getState();

        switch (state.getState()) {
            //首次连接需要为握手做准备
            case RTMP.STATE_CONNECT:
                if (in.readableBytes() >= (Constants.HANDSHAKE_SIZE + 1)) {
                    log.debug("decodeHandshakeC0C1");
                    // set handshake to match client requested type
                    // 设置握手请求类型  一个byte
                    byte connectionType = in.readByte();
                    handshake.setHandshakeType(connectionType);
                    log.trace("Incoming C0 connection type: {}", connectionType);
                    // create array for decode
                    // 初始化要解码的数组
                    byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                    // copy out 1536 bytes
                    in.readBytes(dst);
                    // set state to indicate we're waiting for C2
                    BufFacade s0s1s2 = handshake.decodeClientRequest1(BufFacade.wrappedBuffer(dst));
                    //
                    ctx.writeAndFlush(s0s1s2.getBuf());

                    //设置连接状态
                    state.setState(RTMP.STATE_HANDSHAKE);
                }
                break;
            //进行握手
            case RTMP.STATE_HANDSHAKE:
                if (in.readableBytes() >= Constants.HANDSHAKE_SIZE) {
                    log.debug("decodeHandshakeC2");
                    // create array for decode
                    byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                    // get C2 out
                    in.readBytes(dst);
                    if (handshake.decodeClientRequest2(BufFacade.wrappedBuffer(dst))) {
                        log.debug("Connected, removing handshake data and adding rtmp protocol filter");
                        // set state to indicate we're connected
                        //修改连接状态为 已连接
                        state.setState(RTMP.STATE_CONNECTED);
                        // set encryption flag the rtmp state
                        // 设置rtmp是否为加密状态
                        if (handshake.useEncryption()) {
                            log.debug("Using encrypted communications, adding ciphers to the session");
                            state.setEncrypted(true);
                            nettySessionFacade.setCipherIn(handshake.getCipherIn());
                            nettySessionFacade.setCipherOut(handshake.getCipherOut());
                        }
                        // remove handshake from session now that we are connected
                        ctx.channel().pipeline().remove(this);
                    } else {
                        log.warn("Client was rejected due to invalid handshake");
                        ctx.close();
                    }
                }
                break;
            default:
                throw new IllegalStateException("Invalid RTMP state: " + state);
        }
    }


}
