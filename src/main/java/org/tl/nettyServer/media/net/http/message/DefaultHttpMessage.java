package org.tl.nettyServer.media.net.http.message;

import io.netty.util.internal.StringUtil;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.codec.HTTPCodecUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class DefaultHttpMessage implements HTTPMessage {

    private final HTTPHeaders headers = new HTTPHeaders();
    private HTTPVersion version;
    private BufFacade content;
    private boolean chunked;

    /**
     * Creates a new instance.
     */
    protected DefaultHttpMessage(final HTTPVersion version) {
        setProtocolVersion(version);
    }

    @Override
    public void addHeader(final String name, final Object value) {
        headers.addHeader(name, value);
    }

    @Override
    public void setHeader(final String name, final Object value) {
        headers.setHeader(name, value);
    }

    @Override
    public void setHeader(final String name, final Iterable<?> values) {
        headers.setHeader(name, values);
    }

    @Override
    public void removeHeader(final String name) {
        headers.removeHeader(name);
    }

    @Override
    public boolean isChunked() {
        if (chunked) {
            return true;
        } else {
            return HTTPCodecUtil.isTransferEncodingChunked(this);
        }
    }

    @Override
    public void setChunked(boolean chunked) {
        this.chunked = chunked;
        if (chunked) {
            setContent(null);
        }
    }

    @Override
    public void clearHeaders() {
        headers.clearHeaders();
    }

    @Override
    public void setContent(BufFacade content) {
        if (content != null && content.readableBytes() > 0 && isChunked()) {
            throw new IllegalArgumentException(
                    "non-empty content disallowed if this.chunked == true");
        }
        this.content = content;
    }

    @Override
    public String getHeader(final String name) {
        return headers.getHeader(name);
    }

    @Override
    public List<String> getHeaders(final String name) {
        return headers.getHeaders(name);
    }

    @Override
    public List<Map.Entry<String, String>> getHeaders() {
        return headers.getHeaders();
    }

    @Override
    public boolean containsHeader(final String name) {
        return headers.containsHeader(name);
    }

    @Override
    public Set<String> getHeaderNames() {
        return headers.getHeaderNames();
    }

    @Override
    public HTTPVersion getProtocolVersion() {
        return version;
    }

    @Override
    public void setProtocolVersion(HTTPVersion version) {
        if (version == null) {
            throw new NullPointerException("version");
        }
        this.version = version;
    }

    @Override
    public BufFacade getContent() {
        return content;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append("(version: ");
        buf.append(getProtocolVersion().getText());
        buf.append(", keepAlive: ");
        buf.append(HTTPHeaders.isKeepAlive(this));
        buf.append(", chunked: ");
        buf.append(isChunked());
        buf.append(')');
        buf.append(StringUtil.NEWLINE);
        appendHeaders(buf);

        // Remove the last newline.
        buf.setLength(buf.length() - StringUtil.NEWLINE.length());
        return buf.toString();
    }

    public void appendHeaders(StringBuilder buf) {
        for (Map.Entry<String, String> e : getHeaders()) {
            buf.append(e.getKey());
            buf.append(": ");
            buf.append(e.getValue());
            buf.append(StringUtil.NEWLINE);
        }
    }
}
