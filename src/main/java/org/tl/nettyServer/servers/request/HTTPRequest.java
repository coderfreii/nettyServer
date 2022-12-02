package org.tl.nettyServer.servers.request;


import org.tl.nettyServer.servers.message.HTTPMessage;
import org.tl.nettyServer.servers.message.HTTPMethod;

/**
 * http netty请求
 *
 * @author TL
 * @date 2022/12/02
 */
public interface HTTPRequest extends HTTPMessage {

    HTTPMethod getMethod();

    void setMethod(HTTPMethod method);

    String getUri();

    void setUri(String uri);

    String getPath();

    void setPath(String path);
}
