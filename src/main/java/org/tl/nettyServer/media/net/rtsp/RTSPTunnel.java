package org.tl.nettyServer.media.net.rtsp;

import io.netty.buffer.ByteBuf;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.codec.DecodeState;
import org.tl.nettyServer.media.net.http.codec.HTTPCodecUtil;
import org.tl.nettyServer.media.net.http.message.HTTPHeaders;
import org.tl.nettyServer.media.net.http.message.HTTPVersion;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.DefaultHttpResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.net.rtsp.codec.RTSPRequestDecoder;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static org.tl.nettyServer.media.net.http.response.HTTPResponseStatus.OK;
import static org.tl.nettyServer.media.net.rtsp.message.RTSPVersions.RTSP_1_0;


/**
 * RTSP Over HTTP Tunnel
 *
 * @author pengliren
 */
public class RTSPTunnel {

    private static Logger log = LoggerFactory.getLogger(RTSPTunnel.class);

    public static final String RTSP_TUNNELLED = "application/x-rtsp-tunnelled";

    public static ConcurrentHashMap<String, RTSPMinaConnection> RTSP_TUNNEL_CONNS = new ConcurrentHashMap<String, RTSPMinaConnection>();

    /**
     * Tunnelling RTSP and RTP through HTTP GET Request
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public static void get(HTTPRequest request, HTTPResponse response) throws IOException {

        // get connection
        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();

        // init DefaultHttpResponse
        response = new DefaultHttpResponse(HTTPVersion.HTTP_1_0, OK);
        response.setHeader(HTTPHeaders.Names.SERVER, "Red5");
        response.setHeader(HTTPHeaders.Names.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader(HTTPHeaders.Names.CONNECTION, "close");
        response.setHeader(HTTPHeaders.Names.CONTENT_TYPE, RTSP_TUNNELLED);

        // Version
        // All requests are made using HTTP version 1.0. This is to get through as many firewalls as possible.
        HTTPVersion version = request.getProtocolVersion();

        // Binding the Channels
        //Each client HTTP request must include a “x-sessioncookie” header with an ID as its value.
        //This makes it possible for the server to unambiguously bind the 2 channels.
        //This protocol uses the value as a simple opaque token.
        //Tokens must be unique to the server, but do not need to be globally unique.
        String sessioncookie = request.getHeader("x-sessioncookie");

        if (!version.equals(HTTPVersion.HTTP_1_0) || StringUtils.isEmpty(sessioncookie)) {
            response.setStatus(HTTPResponseStatus.BAD_REQUEST);
            log.info("rtsp tunnel version: {} and sessioncookie: {}", version, sessioncookie);
        } else {
            // we must bind this channel
            RTSP_TUNNEL_CONNS.put(sessioncookie, conn);
            conn.setAttribute("sessioncookie", sessioncookie);
            log.info("rtsp tunnel session cookie {}", sessioncookie);
        }

        conn.write(response);
    }

    /**
     * @param request
     * @param response
     * @throws Exception
     */
    public static void post(HTTPRequest request, HTTPResponse response) throws Exception {

        // get Version
        HTTPVersion version = request.getProtocolVersion();

        // get sessioncookie
        String sessioncookie = request.getHeader("x-sessioncookie");

        RTSPMinaConnection getConn = null;
        RTSPMinaConnection postConn = (RTSPMinaConnection) Red5.getConnectionLocal();

        if (!version.equals(HTTPVersion.HTTP_1_0) || StringUtils.isEmpty(sessioncookie)) {
            response.setStatus(HTTPResponseStatus.BAD_REQUEST);
        } else {
            // get bind channel
            getConn = RTSP_TUNNEL_CONNS.get(sessioncookie);
        }

        // check bind conn
        if (getConn == null) {
            postConn.close();
            return;
        }

        BufFacade lastBuffer = null;
        if (postConn.hasAttribute("rtsppostpkt")) {
            lastBuffer = (BufFacade) postConn.getAttribute("rtsppostpkt");
        }

        // set local conn is getConn
        Red5.setConnectionLocal(getConn);
        String content = HTTPCodecUtil.decodeBody((ByteBuf) request.getContent().getBuf());
		BufFacade buffer = BufFacade.wrappedBuffer(Base64.decodeBase64(content));
        RTSPRequestDecoder decoder = new RTSPRequestDecoder();

        if (lastBuffer != null) {
            lastBuffer.writeBytes(buffer);
        } else {
            lastBuffer = buffer;
        }

        int pos = 0;
        DecodeState obj = null;
        while (lastBuffer.readable()) {
            pos = lastBuffer.readerIndex();
            obj = decoder.decodeBuffer(lastBuffer);
            // we must rtsp packet
            if (obj.getState() == DecodeState.ENOUGH) {
                HTTPRequest req = (HTTPRequest) obj.getObject();
                HTTPResponse resp = new DefaultHttpResponse(RTSP_1_0, OK);
                RTSPCore.handleRtspMethod(req, resp);
                getConn.write(resp);
            } else {
                // current data not enough we must reset and wait next data
                log.info("data not enough ? ");
                lastBuffer.readerIndex(pos);
				BufFacade temp = BufFacade.buffer(lastBuffer.readableBytes());
                temp.writeBytes(lastBuffer);
                postConn.setAttribute("rtsppostpkt", temp);
                break;
            }
        }

        // check last data is enough we must clear lastbuffer data
        if (obj != null && obj.getState() == DecodeState.ENOUGH) {
            postConn.removeAttribute("rtsppostpkt");
        }
    }
}
