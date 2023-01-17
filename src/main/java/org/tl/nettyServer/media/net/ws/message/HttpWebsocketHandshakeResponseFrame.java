package org.tl.nettyServer.media.net.ws.message;

import org.tl.nettyServer.media.net.http.message.HTTPVersion;
import org.tl.nettyServer.media.net.http.response.DefaultHttpResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;

/**
 * http握手响应帧
 *
 * @author chao
 * @data 2017-10-27
 */
public class HttpWebsocketHandshakeResponseFrame extends DefaultHttpResponse {
    private String secWebsocketAccept = "";
    private String secWebsocketExtensions = "";

    /**
     * Creates a new instance.
     *
     * @param version the HTTP version of this response
     * @param status  the status of this response
     */
    public HttpWebsocketHandshakeResponseFrame(HTTPVersion version, HTTPResponseStatus status) {
        super(version, status);
    }


    /**
     * Hypertext Transfer Protocol
     * HTTP/1.1 101 \r\n
     * Upgrade: websocket\r\n
     * Connection: upgrade\r\n
     * Sec-WebSocket-Accept: 1CBbdVUHx2vwNyVlvn3NtOfP1rs=\r\n
     * Sec-WebSocket-Extensions: permessage-deflate;client_max_window_bits=15\r\n
     * Date: Tue, 24 Oct 2017 15:37:40 GMT\r\n
     * \r\n
     * 生成HTTP协议帧
     *
     * @return
     */


    public String getSecWebsocketAccept() {
        return secWebsocketAccept;
    }

    public void setSecWebsocketAccept(String secWebsocketAccept) {
        this.secWebsocketAccept = secWebsocketAccept;
    }

    public String getSecWebsocketExtensions() {
        return secWebsocketExtensions;
    }

    public void setSecWebsocketExtensions(String secWebsocketExtensions) {
        this.secWebsocketExtensions = secWebsocketExtensions;
    }

}