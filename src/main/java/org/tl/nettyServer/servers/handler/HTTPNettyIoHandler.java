package org.tl.nettyServer.servers.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.message.HTTPChunk;
import org.tl.nettyServer.servers.message.HTTPHeaders;
import org.tl.nettyServer.servers.message.HTTPVersion;
import org.tl.nettyServer.servers.request.HTTPRequest;
import org.tl.nettyServer.servers.response.DefaultHttpResponse;
import org.tl.nettyServer.servers.response.HTTPResponse;
import org.tl.nettyServer.servers.response.HTTPResponseStatus;
import org.tl.nettyServer.servers.service.BaseHTTPService;

import java.nio.charset.StandardCharsets;

/**
 * HTTP Mina IO Handler
 *
 * @author tl
 */
public class HTTPNettyIoHandler extends SimpleChannelInboundHandler<Object> {


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
    protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
        if (message instanceof HTTPRequest) {
            HTTPRequest req = (HTTPRequest) message;
            HTTPResponse resp = new DefaultHttpResponse(HTTPVersion.HTTP_1_1, HTTPResponseStatus.OK);
            resp.setHeader(HTTPHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8");
            new BaseHTTPService() {
                @Override
                public void setHeader(HTTPResponse resp) {

                }
            }.commitResponse(req, resp, BufFacade.wrappedBuffer("{'message':'访问被拒绝'}".getBytes(StandardCharsets.UTF_8)));
            resp.setContent(BufFacade.wrappedBuffer("{'message':'访问被拒绝'}".getBytes(StandardCharsets.UTF_8)));
            ctx.writeAndFlush(resp);
        } else if (message instanceof HTTPChunk) {
            ctx.flush();
            ctx.close();
        } else {
            ctx.flush();
            ctx.close();
        }
    }


}
