package org.tl.nettyServer.servers.net.http.response;

import org.tl.nettyServer.servers.net.http.message.HTTPMessage;

/**
 * HTTP Response Interface
 *
 * @author pengliren
 */
public interface HTTPResponse extends HTTPMessage {

    HTTPResponseStatus getStatus();

    void setStatus(HTTPResponseStatus status);
}
