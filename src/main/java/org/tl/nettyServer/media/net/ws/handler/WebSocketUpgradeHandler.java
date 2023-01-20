package org.tl.nettyServer.media.net.ws.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.SneakyThrows;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.HTTPConnectionConsumer;
import org.tl.nettyServer.media.net.http.codec.QueryStringDecoder;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.handler.HTTPIoHandler;
import org.tl.nettyServer.media.net.http.message.HTTPHeaders;
import org.tl.nettyServer.media.net.http.message.HTTPVersion;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.DefaultHttpResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.net.http.service.HttpServiceResolver;
import org.tl.nettyServer.media.net.http.service.IHTTPService;
import org.tl.nettyServer.media.net.rtsp.rtp.mina.Base64;
import org.tl.nettyServer.media.net.ws.WebSocketConnConsumer;
import org.tl.nettyServer.media.net.ws.message.FrameUtils;
import org.tl.nettyServer.media.net.ws.message.HttpWebsocketHandshakeResponseFrame;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.session.SessionAccessor;
import org.tl.nettyServer.media.stream.client.CustomSingleItemSubStream;
import org.tl.nettyServer.media.stream.consumer.ICustomPushableConsumer;
import org.tl.nettyServer.media.stream.playlist.SimplePlayItem;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class WebSocketUpgradeHandler extends MessageToMessageDecoder<HTTPRequest> {
    public static String SEC_WEBSOCKET_KEY_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    @Override
    protected void decode(ChannelHandlerContext ctx, HTTPRequest req, List<Object> out) throws Exception {
        HTTPConnection conn = SessionAccessor.resolveHttpConn(ctx);
        Red5.setConnectionLocal(conn);
        if (conn.isWebsocket()) {
            out.add(req);
            return;
        }


        HTTPResponse resp = new DefaultHttpResponse(HTTPVersion.HTTP_1_1, HTTPResponseStatus.NOT_FOUND);
        IHTTPService defaultHttpService = HttpServiceResolver.getDefaultHttpService();
        resp.setHeader(HTTPHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8");

        HTTPIoHandler.AppRequestResolver appRequestResolver = new HTTPIoHandler.AppRequestResolver();
        HTTPIoHandler.AppAndReq resolve = appRequestResolver.resolve(req);
        IScope scope = ScopeUtils.getScope(resolve.getAppName());
        if (scope != null) {
            if (scope.isConnectionAllowed(conn)) {
                try {
                    preparePlay(req, resp, conn, resolve);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                defaultHttpService.commitResponse(req, resp, BufFacade.wrappedBuffer("{'message':'访问被拒绝'}".getBytes(StandardCharsets.UTF_8)));
                return;
            }
        } else {
            defaultHttpService.commitResponse(req, resp, BufFacade.wrappedBuffer("{'message':'访问被拒绝'}".getBytes(StandardCharsets.UTF_8)));
        }
        Red5.setConnectionLocal(null);
    }

    void preparePlay(HTTPRequest req, HTTPResponse resp, HTTPConnection conn, HTTPIoHandler.AppAndReq rar) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //这直接升级
        if (req.containsHeader(HTTPHeaders.Names.CONNECTION)) {
            if ("websocket".equals(req.getHeader(HTTPHeaders.Names.UPGRADE))) {
                String sec_websocket_key = req.getHeader(HTTPHeaders.Names.SEC_WEBSOCKET_KEY);
                String sec_websocket_extensions = req.getHeader("Sec-WebSocket-Extensions");
                sec_websocket_key += SEC_WEBSOCKET_KEY_GUID;
                MessageDigest cript = MessageDigest.getInstance("SHA-1");
                cript.reset();
                cript.update(sec_websocket_key.getBytes("utf8"));
                byte[] hashedVal = cript.digest();
                Base64 base64 = new Base64();
                sec_websocket_key = new String(base64.encodeBase64(hashedVal));

                HttpWebsocketHandshakeResponseFrame hwr = new HttpWebsocketHandshakeResponseFrame(HTTPVersion.HTTP_1_1, HTTPResponseStatus.SWITCHING_PROTOCOLS);
                hwr.addHeader(HTTPHeaders.Names.CONNECTION, HTTPHeaders.Values.UPGRADE);
                hwr.addHeader(HTTPHeaders.Names.SEC_WEBSOCKET_ACCEPT, sec_websocket_key);
                hwr.addHeader("Sec-WebSocket-Extensions", sec_websocket_extensions + "=15");
                hwr.addHeader(HTTPHeaders.Names.UPGRADE, HTTPHeaders.Values.WEBSOCKET);
                hwr.addHeader(HTTPHeaders.Names.DATE, FrameUtils.getGMT());
                ChannelFuture channelFuture = conn.write(hwr);

                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            conn.setWebsocket(true);
                            startPlay(new WebSocketConnConsumer(conn), rar);
                        } else {
                            IHTTPService defaultHttpService = HttpServiceResolver.getDefaultHttpService();
                            resp.setHeader(HTTPHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8");
                            defaultHttpService.commitResponse(req, resp, BufFacade.wrappedBuffer("{'message':'访问被拒绝'}".getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                });
            } else if (req.getHeader(HTTPHeaders.Names.UPGRADE) == null) {
                http(conn, rar);
            }
        } else {
            http(conn, rar);
        }
    }


    private void http(HTTPConnection conn, HTTPIoHandler.AppAndReq rar) {
        DefaultHttpResponse response = new DefaultHttpResponse(HTTPVersion.HTTP_1_1, HTTPResponseStatus.OK);
        response.setHeader(HTTPHeaders.Names.CONNECTION, HTTPHeaders.Values.KEEP_ALIVE);
        response.setHeader(HTTPHeaders.Names.CONTENT_TYPE, "video/x-flv");
        response.setHeader(HTTPHeaders.Names.ACCEPT_RANGES, HTTPHeaders.Values.BYTES);
        response.setHeader(HTTPHeaders.Names.CACHE_CONTROL,  HTTPHeaders.Values.NO_CACHE);
        response.setHeader(HTTPHeaders.Names.TRANSFER_ENCODING, "chunked");
//        response.setHeader("access-control-allow-origin", rar.getHttpReq().getHeader(HTTPHeaders.Names.ORIGIN));
        response.setHeader("access-control-allow-origin", "*");
        response.setHeader("access-control-allow-credentials", "true");
        response.setHeader("vary", "origin");
        response.setHeader(HTTPHeaders.Names.SERVER, "tl");
        ChannelFuture write = conn.write(response);

        write.addListener((i) -> {
            if (i.isSuccess()) {
                startPlay(new HTTPConnectionConsumer(conn), rar);
            } else {

            }
        });
    }

    @SneakyThrows
    private void startPlay(ICustomPushableConsumer httpConnectionConsumer, HTTPIoHandler.AppAndReq rar) {
        IScope scope = ScopeUtils.getScope(rar.getAppName());
        httpConnectionConsumer.getConnection().connect(scope, new String[]{"1"});
        CustomSingleItemSubStream rtspStream = new CustomSingleItemSubStream(scope, httpConnectionConsumer);

        String url = rar.getHttpReq().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(url);
        URI uri = new URI(queryStringDecoder.getPath());
        String[] segments = uri.getPath().substring(1).split("/");
        if (segments.length < 2) return;
        String stream = segments[1];


        SimplePlayItem playItem = SimplePlayItem.build(stream, -2000, -1);
        rtspStream.setPlayItem(playItem);
        rtspStream.start();
        rtspStream.play();
    }
}
