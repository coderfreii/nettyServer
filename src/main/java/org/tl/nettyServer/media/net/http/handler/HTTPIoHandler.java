package org.tl.nettyServer.media.net.http.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.HTTPApplicationAdapter;
import org.tl.nettyServer.media.net.http.IHTTPApplicationAdapter;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.message.HTTPHeaders;
import org.tl.nettyServer.media.net.http.message.HTTPVersion;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.DefaultHttpResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.net.http.service.HttpServiceResolver;
import org.tl.nettyServer.media.net.http.service.IHTTPService;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.session.SessionAccessor;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.nio.charset.StandardCharsets;

/**
 * HTTP Mina IO Handler
 *
 * @author tl
 */
@Slf4j
public class HTTPIoHandler extends SimpleChannelInboundHandler<HTTPRequest> {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("channel exceptionCaught:" + ctx.channel().remoteAddress(), cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HTTPRequest req) throws Exception {
        HTTPConnection conn = SessionAccessor.resolveHttpConn(ctx);
        Red5.setConnectionLocal(conn);

        HTTPResponse resp = new DefaultHttpResponse(HTTPVersion.HTTP_1_1, HTTPResponseStatus.OK);
        AppRequestResolver appRequestResolver = new AppRequestResolver();
        AppAndReq resolve = appRequestResolver.resolve(req);
        IScope scope = ScopeUtils.getScope(resolve.getAppName());
        if (scope != null) {
            if (scope.isConnectionAllowed(conn)) {
                IHTTPApplicationAdapter httpApplicationAdapter = new HTTPApplicationAdapter();
                httpApplicationAdapter.setScope(scope);
                conn.setApplicationAdapter(httpApplicationAdapter);
                httpApplicationAdapter.onHTTPRequest(req, resp);
            } else {
                IHTTPService defaultHttpService = HttpServiceResolver.getDefaultHttpService();
                resp.setHeader(HTTPHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8");
                defaultHttpService.commitResponse(req, resp, BufFacade.wrappedBuffer("{'message':'访问被拒绝'}".getBytes(StandardCharsets.UTF_8)));
            }
        }
        Red5.setConnectionLocal(null);
    }

    private String resolveScopeName(String path) {
        path = path.substring(1);
        String[] segments = path.split("/");
        if (segments.length > 0) {
            return segments[0];
        }
        return "root";
    }

    private String resolveNoAppPath(String uri) {
        return uri.replaceFirst(String.format("/%s", resolveScopeName(uri)), "");
    }


    public static class AppAndReq {
        HTTPRequest httpReq;

        String appName;


        AppAndReq() {

        }

        public AppAndReq(HTTPRequest httpReq, String httpName) {
            this.appName = httpName;
            this.httpReq = httpReq;
        }

        public HTTPRequest getHttpReq() {
            return httpReq;
        }

        public String getAppName() {
            return appName;
        }
    }

    public static class AppRequestResolver {
        public AppAndReq resolve(HTTPRequest req) {
            String noAppPath;
            String scopeName = resolveScopeName(req.getUri());
            noAppPath = resolveNoAppPath(req.getUri());
            req.setPath(noAppPath);

            return new AppAndReq(req, scopeName);
        }

        private String resolveScopeName(String path) {
            path = path.substring(1);
            String[] segments = path.split("/");
            if (segments.length > 0) {
                return segments[0];
            }
            return "root";
        }

        private String resolveNoAppPath(String uri) {
            return uri.replaceFirst(String.format("/%s", resolveScopeName(uri)), "");
        }
    }
}
