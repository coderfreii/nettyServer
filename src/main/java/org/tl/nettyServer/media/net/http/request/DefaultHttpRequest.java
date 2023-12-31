package org.tl.nettyServer.media.net.http.request;


import io.netty.util.internal.StringUtil;
import org.tl.nettyServer.media.net.http.message.DefaultHttpMessage;
import org.tl.nettyServer.media.net.http.message.HTTPMethod;
import org.tl.nettyServer.media.net.http.message.HTTPVersion;

/**
 * Default Http Request
 *
 * @author pengliren
 */
public class DefaultHttpRequest extends DefaultHttpMessage implements HTTPRequest {

    private HTTPMethod method;
    private String uri;
    private String path;

    /**
     * Creates a new instance.
     *
     * @param httpVersion the HTTP version of the request
     * @param method      the HTTP method of the request
     * @param uri         the URI or path of the request
     */
    public DefaultHttpRequest(HTTPVersion httpVersion, HTTPMethod method, String uri) {
        super(httpVersion);
        setMethod(method);
        setUri(uri);
    }

    @Override
    public HTTPMethod getMethod() {
        return method;
    }

    @Override
    public void setMethod(HTTPMethod method) {
        if (method == null) {
            throw new NullPointerException("method");
        }
        this.method = method;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append("(chunked: ");
        buf.append(isChunked());
        buf.append(')');
        buf.append(StringUtil.NEWLINE);
        buf.append(getMethod().toString());
        buf.append(' ');
        buf.append(getUri());
        buf.append(' ');
        buf.append(getPath());
        buf.append(' ');
        buf.append(getProtocolVersion().getText());
        buf.append(StringUtil.NEWLINE);
        appendHeaders(buf);

        // Remove the last newline.
        buf.setLength(buf.length() - StringUtil.NEWLINE.length());
        return buf.toString();
    }
}
