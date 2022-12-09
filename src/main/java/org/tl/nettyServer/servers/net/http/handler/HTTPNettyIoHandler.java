package org.tl.nettyServer.servers.net.http.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.tl.nettyServer.servers.net.http.message.HTTPHeaders;
import org.tl.nettyServer.servers.net.http.message.HTTPVersion;
import org.tl.nettyServer.servers.net.http.request.HTTPRequest;
import org.tl.nettyServer.servers.net.http.response.DefaultHttpResponse;
import org.tl.nettyServer.servers.net.http.response.HTTPResponse;
import org.tl.nettyServer.servers.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.servers.net.http.service.BaseHTTPService;


/**
 * HTTP Mina IO Handler
 *
 * @author tl
 */
public class HTTPNettyIoHandler extends SimpleChannelInboundHandler<HTTPRequest> {


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HTTPRequest req) throws Exception {
        HTTPResponse resp = new DefaultHttpResponse(HTTPVersion.HTTP_1_1, HTTPResponseStatus.OK);
        resp.setHeader(HTTPHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8");
        new BaseHTTPService() {
            @Override
            public void setHeader(HTTPResponse resp) {

            }
        }.commitResponse(req, resp, req.getContent());
        ctx.writeAndFlush(resp);
    }


}
