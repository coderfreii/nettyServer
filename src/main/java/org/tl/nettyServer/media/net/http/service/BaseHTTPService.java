package org.tl.nettyServer.media.net.http.service;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.message.HTTPHeaders;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;


/**
 * Base HTTP Service
 *
 * @author pengliren
 */
public abstract class BaseHTTPService implements IHTTPService {

    /**
     * HTTP request method to use for HTTP calls.
     */
    protected static final String REQUEST_GET_METHOD = "GET";
    protected static final String REQUEST_POST_METHOD = "POST";

    @Override
    public void sendError(HTTPRequest req, HTTPResponse resp, HTTPResponseStatus status) {
        commitResponse(req, resp, null, status);
        flush(false, resp, true);
    }

    @Override
    public void commitResponse(HTTPRequest req, HTTPResponse resp, BufFacade data) {
        commitResponse(req, resp, data, HTTPResponseStatus.OK);
    }

    @Override
    public void commitResponse(HTTPRequest req, HTTPResponse resp, BufFacade data, HTTPResponseStatus status) {
        resp.setStatus(status);

        if (data != null && data.readableBytes() > 0) {
            resp.addHeader(HTTPHeaders.Names.CONTENT_LENGTH, data.readableBytes());
            resp.setContent(data);
        }

        boolean isKeepAlive = HTTPHeaders.isKeepAlive(req);

        if (isKeepAlive) {
            resp.setHeader(HTTPHeaders.Names.CONNECTION, HTTPHeaders.Values.KEEP_ALIVE);
        }

        flush(isKeepAlive, resp, false);
    }

    protected void flush(boolean isKeepAlive, HTTPResponse resp, boolean isClose) {
        HTTPConnection conn = (HTTPConnection) Red5.getConnectionLocal();
        ChannelFuture future = conn.write(resp);

        if (isClose || !isKeepAlive) {
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    future.channel().close();
                }
            });
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }


    public void handleRequest(HTTPRequest req, HTTPResponse resp) throws Exception {

    }
}
