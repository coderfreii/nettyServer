package org.tl.nettyServer.media.net.rtsp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.net.http.message.HTTPHeaders;
import org.tl.nettyServer.media.net.http.message.HTTPMethod;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.DefaultHttpResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.rtsp.RTSPTunnel;
import org.tl.nettyServer.media.net.rtsp.RtspHandler;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.message.RTSPResponseStatuses;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.net.ProtocolException;
import java.util.List;

import static org.tl.nettyServer.media.net.rtsp.message.RTSPResponseStatuses.OK;
import static org.tl.nettyServer.media.net.rtsp.message.RTSPVersions.RTSP_1_0;

/**
 * RTSP Mina Io Handler
 *
 * @author pengliren
 */
public class RTSPRequestHandler extends MessageToMessageDecoder<HTTPRequest> {

    private static Logger log = LoggerFactory.getLogger(RTSPRequestHandler.class);


    private RtspHandler rtspHandler = new RtspHandler();

    public RTSPRequestHandler() {

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HTTPRequest message, List<Object> out) throws Exception {
        RTSPMinaConnection conn = (RTSPMinaConnection) SessionAccessor.resolveRtspConn(ctx);
        Red5.setConnectionLocal(conn);
        // handle rtsp method
        if (message instanceof HTTPRequest) {
            boolean isResponse = true; // need send response ? rtsp default send if rtsp tunnelled is not send
            HTTPRequest request = (HTTPRequest) message;
            HTTPResponse response = new DefaultHttpResponse(RTSP_1_0, OK);

            // first handle rtsp method
            boolean flag = rtspHandler.handleRtspMethod(request, response);

            if (!flag) {
                // second check rtsp over http tunnel
                if (request.getMethod().equals(HTTPMethod.GET)
                        && request.getHeader(HTTPHeaders.Names.ACCEPT) != null
                        && request.getHeader(HTTPHeaders.Names.ACCEPT).equalsIgnoreCase(
                        RTSPTunnel.RTSP_TUNNELLED)) {
                    isResponse = false;
                    // rtsp over http for get
                    RTSPTunnel.get(request, response);
                } else if (request.getMethod().equals(HTTPMethod.POST)
                        && request.getHeader(HTTPHeaders.Names.CONTENT_TYPE) != null
                        && request.getHeader(HTTPHeaders.Names.CONTENT_TYPE).equalsIgnoreCase(
                        RTSPTunnel.RTSP_TUNNELLED)) {
                    isResponse = false;
                    // rtsp over http for post
                    RTSPTunnel.post(request, response);
                } else {
                    log.info("not support method {}", request);
                    response.setStatus(RTSPResponseStatuses.BAD_REQUEST);
                }
            }
            if (isResponse) conn.write(response);
        }
        Red5.setConnectionLocal(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RTSPMinaConnection conn = (RTSPMinaConnection) SessionAccessor.resolveRtspConn(ctx);
        if (conn != null) conn.close();

        if (cause instanceof ProtocolException) {
            log.warn("Exception caught {}", cause.getMessage());
        } else {
            log.error("Exception caught {}", cause.getMessage());
        }
        super.exceptionCaught(ctx, cause);
    }


    public RtspHandler getRtspHandler() {
        return rtspHandler;
    }

    public void setRtspHandler(RtspHandler rtspHandler) {
        this.rtspHandler = rtspHandler;
    }
}
