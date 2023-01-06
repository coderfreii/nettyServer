package org.tl.nettyServer.media.net.rtmp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.codec.RtmpProtocolState;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnManager;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.session.SessionAccessor;

@Slf4j
public class MessageSendHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        messageSend((RTMPConnection) SessionAccessor.resolveRtmpConn(ctx), msg);
        super.write(ctx, msg, promise);
    }

    private void messageSend(RTMPConnection conn, Object message) {
        log.trace("messageSent session: {} message: {}", conn, message);
        String sessionId = conn.getSessionId();
        if (log.isTraceEnabled()) {
            log.trace("Message sent on session: {} id: {}", sessionId, sessionId);
        }
        if (sessionId != null) {
            conn = (RTMPConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn != null) {
                final byte state = conn.getStateCode();
                switch (state) {
                    case RtmpProtocolState.STATE_CONNECTED:
                        if (message instanceof Packet) {
                            conn.messageSent((Packet) message);
                        } else if (log.isDebugEnabled()) {
                            log.debug("Message was not of Packet type; its type: {}", message != null ? message.getClass().getName() : "null");
                        }
                        break;
                    case RtmpProtocolState.STATE_CONNECT:
                    case RtmpProtocolState.STATE_HANDSHAKE:
                        if (log.isTraceEnabled()) {
                            log.trace("messageSent: {}", ((BufFacade) message).hex());
                        }
                        break;
                    case RtmpProtocolState.STATE_DISCONNECTING:
                    case RtmpProtocolState.STATE_DISCONNECTED:
                    default:
                }
            } else {
                log.warn("Destination connection was null, it is already disposed. Session id: {}", sessionId);
            }
        }
    }
}
