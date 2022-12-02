
package org.tl.nettyServer.servers.response;


import io.netty.util.internal.StringUtil;
import org.tl.nettyServer.servers.message.DefaultHttpMessage;
import org.tl.nettyServer.servers.message.HTTPHeaders;
import org.tl.nettyServer.servers.message.HTTPVersion;


/**
 * Default Http Response
 *
 * @author pengliren
 */
public class DefaultHttpResponse extends DefaultHttpMessage implements HTTPResponse {

    private HTTPResponseStatus status;

    /**
     * Creates a new instance.
     *
     * @param version the HTTP version of this response
     * @param status  the status of this response
     */
    public DefaultHttpResponse(HTTPVersion version, HTTPResponseStatus status) {
        super(version);
        setStatus(status);
        //添加跨域请求
        addHeader(HTTPHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    }

    public HTTPResponseStatus getStatus() {
        return status;
    }


    @Override
    public void setStatus(HTTPResponseStatus status) {
        if (status == null) {
            throw new NullPointerException("status");
        }
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append("(chunked: ");
        buf.append(isChunked());
        buf.append(')');
        buf.append(StringUtil.NEWLINE);
        buf.append(getProtocolVersion().getText());
        buf.append(' ');
        buf.append(getStatus().toString());
        buf.append(StringUtil.NEWLINE);
        appendHeaders(buf);
        // Remove the last newline.
        buf.setLength(buf.length() - StringUtil.NEWLINE.length());
        return buf.toString();
    }
}
