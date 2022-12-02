package org.tl.nettyServer.servers.response;

import org.tl.nettyServer.servers.message.HTTPMessage;

/**
 * HTTP Response Interface
 *
 * @author pengliren
 */
public interface HTTPResponse extends HTTPMessage {

    HTTPResponseStatus getStatus();

    void setStatus(HTTPResponseStatus status);
}
