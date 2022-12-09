package org.tl.nettyServer.servers.net.http.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.tl.nettyServer.servers.net.http.message.HTTPHeaders;
import org.tl.nettyServer.servers.net.http.message.HTTPMessage;

import java.nio.charset.Charset;
import java.util.List;

/**
 * HTTP Codec Util
 *
 * @author pengliren
 */
public class HTTPCodecUtil {

    //space ' '
    public static final byte SP = 32;

    //tab ' '
    public static final byte HT = 9;

    /**
     * Carriage return
     */
    public static final byte CR = 13;

    /**
     * Equals '='
     */
    public static final byte EQUALS = 61;

    /**
     * Line feed character
     */
    public static final byte LF = 10;

    /**
     * carriage return line feed
     */
    public static final byte[] CRLF = {CR, LF};

    /**
     * Colon ':'
     */
    public static final byte COLON = 58;

    /**
     * Semicolon ';'
     */
    public static final byte SEMICOLON = 59;

    /**
     * comma ','
     */
    public static final byte COMMA = 44;

    public static final byte DOUBLE_QUOTE = '"';

    public static final Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;

    private HTTPCodecUtil() {
    }

    public static void validateHeaderName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c > 127) {
                throw new IllegalArgumentException(
                        "name contains non-ascii character: " + name);
            }

            // Check prohibited characters.
            switch (c) {
                case '\t':
                case '\n':
                case 0x0b:
                case '\f':
                case '\r':
                case ' ':
                case ',':
                case ':':
                case ';':
                case '=':
                    throw new IllegalArgumentException(
                            "name contains one of the following prohibited characters: " +
                                    "=,;: \\t\\r\\n\\v\\f: " + name);
            }
        }
    }

    public static void validateHeaderValue(String value) {
        if (value == null) {
            throw new NullPointerException("value");
        }

        // 0 - the previous character was neither CR nor LF
        // 1 - the previous character was CR
        // 2 - the previous character was LF
        int state = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            // Check the absolutely prohibited characters.
            switch (c) {
                case 0x0b: // Vertical tab
                    throw new IllegalArgumentException(
                            "value contains a prohibited character '\\v': " + value);
                case '\f':
                    throw new IllegalArgumentException(
                            "value contains a prohibited character '\\f': " + value);
            }

            // Check the CRLF (HT | SP) pattern
            switch (state) {
                case 0:
                    switch (c) {
                        case '\r':
                            state = 1;
                            break;
                        case '\n':
                            state = 2;
                            break;
                    }
                    break;
                case 1:
                    switch (c) {
                        case '\n':
                            state = 2;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Only '\\n' is allowed after '\\r': " + value);
                    }
                    break;
                case 2:
                    switch (c) {
                        case '\t':
                        case ' ':
                            state = 0;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Only ' ' and '\\t' are allowed after '\\n': " + value);
                    }
            }
        }

        if (state != 0) {
            throw new IllegalArgumentException(
                    "value must not end with '\\r' or '\\n':" + value);
        }
    }

    public static boolean isTransferEncodingChunked(HTTPMessage m) {
        List<String> chunked = m.getHeaders(HTTPHeaders.Names.TRANSFER_ENCODING);
        if (chunked.isEmpty()) {
            return false;
        }

        for (String v : chunked) {
            if (v.equalsIgnoreCase(HTTPHeaders.Values.CHUNKED)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTransferEncodingChunkedNetty(HTTPMessage m) {
        List<String> chunked = m.getHeaders(HTTPHeaders.Names.TRANSFER_ENCODING);
        if (chunked.isEmpty()) {
            return false;
        }

        for (String v : chunked) {
            if (v.equalsIgnoreCase(HTTPHeaders.Values.CHUNKED)) {
                return true;
            }
        }
        return false;
    }

    public static ByteBuf encodeBody(String body) {
        ByteBuf buffer = Unpooled.buffer(512);

        buffer.writeCharSequence(body, DEFAULT_CHARSET);
        return buffer;
    }

    public static String decodeBody(ByteBuf buffer) {

        if (buffer == null) return null;
        String body = null;

        body = buffer.readCharSequence(buffer.readableBytes(), DEFAULT_CHARSET).toString();

        return body;
    }
}
