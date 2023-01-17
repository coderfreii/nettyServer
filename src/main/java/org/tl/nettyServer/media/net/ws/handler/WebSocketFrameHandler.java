package org.tl.nettyServer.media.net.ws.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.ws.WebSocketHandler;
import org.tl.nettyServer.media.net.ws.message.*;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.util.List;

public class WebSocketFrameHandler extends MessageToMessageDecoder<WebSocketFrame> {
    private WebSocketHandler webSocketHandler = new WebSocketHandler() {
        @Override
        public void onOpen(HTTPConnection session) {

        }

        @Override
        public void onClose(HTTPConnection session) {

        }

        @Override
        public void onMessage(HTTPConnection session, TextMessageFrame textMessageFrame) {

        }

        @Override
        public void onMessage(HTTPConnection session, BinMessageFrame binMessageFrame) {

        }

        @Override
        public void onError(HTTPConnection session) {

        }

        @Override
        public void onIdle(HTTPConnection session) {

        }

        @Override
        public void onPing(HTTPConnection session) {

        }

        @Override
        public void onPong(HTTPConnection session) {

        }
    };


    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame message, List<Object> out) throws Exception {
        HTTPConnection conn = SessionAccessor.resolveHttpConn(ctx);
        if (message instanceof TextMessageFrame) {
            webSocketHandler.onMessage(conn, (TextMessageFrame) message);
        } else if (message instanceof BinMessageFrame) {
            webSocketHandler.onMessage(conn, (BinMessageFrame) message);
        } else if (message instanceof CloseFrame) {
            //关闭帧，收到断开TCP连接
            conn.close();
        } else if (message instanceof PongFrame) {
            webSocketHandler.onPong(conn);
        } else if (message instanceof PingFrame) {
            webSocketHandler.onPing(conn);
        }
    }
}
