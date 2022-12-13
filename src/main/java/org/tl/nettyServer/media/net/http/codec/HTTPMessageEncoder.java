package org.tl.nettyServer.media.net.http.codec;

import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.message.HTTPChunk;
import org.tl.nettyServer.media.net.http.message.HTTPChunkTrailer;
import org.tl.nettyServer.media.net.http.message.HTTPHeaders;
import org.tl.nettyServer.media.net.http.message.HTTPMessage;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * HTTP Message Encoder
 *
 * @author pengliren
 */
public abstract class HTTPMessageEncoder extends MessageToByteEncoder<HTTPMessage> {

    private static final BufFacade LAST_CHUNK = BufFacade.buffer(10);

    private volatile boolean chunked;

    public HTTPMessageEncoder() {
        LAST_CHUNK.writeCharSequence("0\r\n\r\n", CharsetUtil.US_ASCII);
    }

    protected BufFacade encodeBuffer(HTTPMessage msg) throws Exception {
        if (msg instanceof HTTPMessage) {
            HTTPMessage m = (HTTPMessage) msg;
            boolean chunked;
            if (m.isChunked()) {
                // check if the Transfer-Encoding is set to chunked already.
                // if not add the header to the message
                if (!HTTPCodecUtil.isTransferEncodingChunked(m)) {
                    m.addHeader(HTTPHeaders.Names.TRANSFER_ENCODING, HTTPHeaders.Values.CHUNKED);
                }
                chunked = this.chunked = true;
            } else {
                chunked = this.chunked = HTTPCodecUtil.isTransferEncodingChunked(m);
            }
            BufFacade header = BufFacade.buffer(2048);
            encodeInitialLine(header, m);
            encodeHeaders(header, m);
            header.writeByte(HTTPCodecUtil.CR);
            header.writeByte(HTTPCodecUtil.LF);

            BufFacade content = m.getContent();
            if (content == null || content.readableBytes() <= 0) {
                return header;
            } else if (chunked) {
                throw new IllegalArgumentException(
                        "HttpMessage.content must be empty " +
                                "if Transfer-Encoding is chunked.");
            } else {
                return header.writeBytes(content);
            }
        }

        if (msg instanceof HTTPChunk) {
            HTTPChunk chunk = (HTTPChunk) msg;
            if (chunked) {
                if (chunk.isLast()) {
                    chunked = false;
                    if (chunk instanceof HTTPChunkTrailer) {
                        BufFacade trailer = BufFacade.buffer(2048);
                        trailer.writeByte((byte) '0');
                        trailer.writeByte(HTTPCodecUtil.CR);
                        trailer.writeByte(HTTPCodecUtil.LF);
                        encodeTrailingHeaders(trailer, (HTTPChunkTrailer) chunk);
                        trailer.writeByte(HTTPCodecUtil.CR);
                        trailer.writeByte(HTTPCodecUtil.LF);
                        return trailer;
                    } else {
                        return LAST_CHUNK.duplicate();
                    }
                } else {
                    BufFacade content = chunk.getContent();
                    int contentLength = content.readableBytes();
                    BufFacade temp = BufFacade.buffer(2048);
                    temp.writeCharSequence(Integer.toHexString(contentLength), CharsetUtil.US_ASCII);
                    temp.writeBytes(HTTPCodecUtil.CRLF);
                    temp.writeBytes(content);
                    temp.writeBytes(HTTPCodecUtil.CRLF);
                    return temp;
                }
            } else {
                if (chunk.isLast()) {
                    return null;
                } else {
                    return chunk.getContent();
                }
            }

        }

        // Unknown message type.
        return null;
    }

    private void encodeHeaders(BufFacade buf, HTTPMessage message) {
        try {
            for (Map.Entry<String, String> h : message.getHeaders()) {
                encodeHeader(buf, h.getKey(), h.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            throw (Error) new Error().initCause(e);
        }
    }

    private void encodeTrailingHeaders(BufFacade buf, HTTPChunkTrailer trailer) {
        try {
            for (Map.Entry<String, String> h : trailer.getHeaders()) {
                encodeHeader(buf, h.getKey(), h.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            throw (Error) new Error().initCause(e);
        }
    }

    private void encodeHeader(BufFacade buf, String header, String value)
            throws UnsupportedEncodingException {
        buf.writeBytes(header.getBytes("ASCII"));
        buf.writeByte(HTTPCodecUtil.COLON);
        buf.writeByte(HTTPCodecUtil.SP);
        buf.writeBytes(value.getBytes("ASCII"));
        buf.writeByte(HTTPCodecUtil.CR);
        buf.writeByte(HTTPCodecUtil.LF);
    }

    protected abstract void encodeInitialLine(BufFacade buf, HTTPMessage message) throws Exception;
}
