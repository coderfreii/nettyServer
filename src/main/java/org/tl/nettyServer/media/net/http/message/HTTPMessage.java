package org.tl.nettyServer.media.net.http.message;

import org.tl.nettyServer.media.buf.BufFacade;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * http Netty消息
 * <p>
 * 协议层面
 *
 * @author TL
 * @date 2022/12/02
 */
public interface HTTPMessage {

    String getHeader(String name);

    List<String> getHeaders(String name);

    List<Map.Entry<String, String>> getHeaders();

    boolean containsHeader(String name);

    Set<String> getHeaderNames();

    HTTPVersion getProtocolVersion();

    void setProtocolVersion(HTTPVersion version);

    BufFacade getContent();

    void setContent(BufFacade content);

    void addHeader(String name, Object value);

    void setHeader(String name, Object value);

    void setHeader(String name, Iterable<?> values);

    void removeHeader(String name);

    void clearHeaders();

    boolean isChunked();

    void setChunked(boolean chunked);
}
