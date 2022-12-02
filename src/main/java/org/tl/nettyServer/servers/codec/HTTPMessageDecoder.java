package org.tl.nettyServer.servers.codec;


import io.netty.handler.codec.ByteToMessageDecoder;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.message.*;
import org.tl.nettyServer.servers.response.HTTPResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.tl.nettyServer.servers.message.HTTPHeaders.Names.CONTENT_TYPE;


public abstract class HTTPMessageDecoder extends ByteToMessageDecoder {

    private HTTPMessage message;
    private BufFacade content;
    private long chunkSize;
    private int headerSize;
    private long maxChunkSize = 4096;
    private State state;

    private final StringBuilder sb = new StringBuilder(128);

    /**
     * The internal state of {@link HTTPMessageDecoder}.
     * <em>Internal use only</em>.
     */
    protected enum State {
        /**
         * 跳过控制字符
         *//* 跳过控制字符 */
        SKIP_CONTROL_CHARS,
        /**
         * 读取初始
         *//* 开始 读取 */
        READ_INITIAL,
        /**
         * 读头
         *//* 头 读取 */
        READ_HEADER,
        /**
         * 阅读可变长度内容
         *//* 读取可变的内容长度 */
        READ_VARIABLE_LENGTH_CONTENT,
        /**
         * 读可变长度内容块
         *//* 读取可变的内容长度的块 */
        READ_VARIABLE_LENGTH_CONTENT_AS_CHUNKS,
        /**
         * 阅读固定长度内容
         *//* 读取固定的内容长度 */
        READ_FIXED_LENGTH_CONTENT,
        /**
         * 读固定长度内容块
         *//* 读取可变的内容长度的块 */
        READ_FIXED_LENGTH_CONTENT_AS_CHUNKS,
        /**
         * 读块大小
         *//* 读取块大小 */
        READ_CHUNK_SIZE,
        /**
         * 阅读分块内容
         *//* 读取分块内容 */
        READ_CHUNKED_CONTENT,
        /**
         * 读分块内容块
         *//* 读取分块内容为块 */
        READ_CHUNKED_CONTENT_AS_CHUNKS,
        /**
         * 读块分隔符
         *//* 读取分块分隔符 */
        READ_CHUNK_DELIMITER,
        /**
         * 读块页脚
         *//* 读取块尾 */
        READ_CHUNK_FOOTER
    }

    public HTTPMessageDecoder() {
        //initial state as skip control chars
        state = State.SKIP_CONTROL_CHARS;
    }

    public DecodeState decodeBuffer(BufFacade buffer) throws Exception {
        DecodeState decodeState = new DecodeState();
        switch (state) {
            case SKIP_CONTROL_CHARS: {
                skipControlCharacters(buffer);
                state = State.READ_INITIAL;
            }
            case READ_INITIAL: {  //初始行包括  请求方法, 请求url 以及 HTT版本
                //读取一行
                String line = readLine(buffer);
                if (line == null) {
                    decodeState.setState(DecodeState.NOT_ENOUGH);
                    return decodeState;
                }
                String[] initialLine = splitInitialLine(line);
                if (initialLine.length < 3) {
                    // Invalid initial line - ignore.
                    state = State.SKIP_CONTROL_CHARS;
                    return decodeState;
                }
                //子类解析读取到的
                message = createMessage(initialLine);
                state = State.READ_HEADER;
            }
            case READ_HEADER: {
                State nextState = readHeaders(buffer);
                if (nextState == null) {
                    decodeState.setState(DecodeState.NOT_ENOUGH);
                    return decodeState;
                }
                state = nextState;
                if (nextState == State.READ_CHUNK_SIZE) {
                    // Chunked encoding
                    message.setChunked(true);
                    // Generate HttpMessage first.  HttpChunks will follow.
                    decodeState.setObject(message);
                    return decodeState;
                } else if (nextState == State.SKIP_CONTROL_CHARS) {
                    // No content is expected.
                    // Remove the headers which are not supposed to be present not
                    // to confuse subsequent handlers.
                    message.removeHeader(HTTPHeaders.Names.TRANSFER_ENCODING);
                    decodeState.setObject(message);
                    return decodeState;
                } else {
                    long contentLength = HTTPHeaders.getContentLength(message, -1);
                    if (contentLength == 0 || contentLength == -1) {
                        content = null;
                        return reset(decodeState);
                    }

                    switch (nextState) {
                        case READ_FIXED_LENGTH_CONTENT:
                            if (HTTPHeaders.is100ContinueExpected(message)) {
                                // Generate HttpMessage first.  HttpChunks will follow.
                                state = State.READ_FIXED_LENGTH_CONTENT_AS_CHUNKS;
                                message.setChunked(true);
                                // chunkSize will be decreased as the READ_FIXED_LENGTH_CONTENT_AS_CHUNKS
                                // state reads data chunk by chunk.
                                chunkSize = HTTPHeaders.getContentLength(message, -1);
                                decodeState.setObject(message);
                                return decodeState;
                            }
                            break;
                        case READ_VARIABLE_LENGTH_CONTENT:
                            if (HTTPHeaders.is100ContinueExpected(message)) {
                                // Generate HttpMessage first.  HttpChunks will follow.
                                state = State.READ_VARIABLE_LENGTH_CONTENT_AS_CHUNKS;
                                message.setChunked(true);
                                decodeState.setObject(message);
                                return decodeState;
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state: " + nextState);
                    }
                }
                // We return null here, this forces decode to be called again where we will decode the content
                return decodeState;
            }
            case READ_VARIABLE_LENGTH_CONTENT: {
                if (content == null) {
                    content = BufFacade.buffer(2048);
                }
                //this will cause a replay error until the channel is closed where this will read what's left in the buffer
                content.writeBytes(buffer);
                return reset(decodeState);
            }
            case READ_VARIABLE_LENGTH_CONTENT_AS_CHUNKS: {
                // Keep reading data as a chunk until the end of connection is reached.
                byte[] chunkData = new byte[(int) chunkSize];
                buffer.readBytes(chunkData);
                HTTPChunk chunk = new DefaultHttpChunk(BufFacade.wrappedBuffer(chunkData));
                if (buffer.readableBytes() == 0) {
                    // Reached to the end of the connection.
                    reset(decodeState);
                    if (!chunk.isLast()) {
                        // Append the last chunk.
                        decodeState.setObject(new Object[]{chunk, HTTPChunk.LAST_CHUNK});
                        return decodeState;
                    }
                }
                decodeState.setObject(chunk);
                return decodeState;
            }
            case READ_FIXED_LENGTH_CONTENT: {
                //we have a content-length so we just read the correct number of bytes
                AtomicBoolean reset = new AtomicBoolean(true);
                if (!readFixedLengthContent(buffer, reset)) {
                    decodeState.setState(DecodeState.NOT_ENOUGH);
                    return decodeState;
                }
                if (reset.get()) return reset(decodeState);
                else return noreset(decodeState);
            }
            case READ_FIXED_LENGTH_CONTENT_AS_CHUNKS: {
                long chunkSize = this.chunkSize;
                HTTPChunk chunk;
                assert chunkSize <= Integer.MAX_VALUE;
                byte[] chunkData = new byte[(int) chunkSize];
                buffer.readBytes(chunkData);
                chunk = new DefaultHttpChunk(BufFacade.wrappedBuffer(chunkData));
                chunkSize = 0;
                this.chunkSize = chunkSize;
                if (chunkSize == 0) {
                    // Read all content.
                    reset(decodeState);
                    if (!chunk.isLast()) {
                        // Append the last chunk.
                        decodeState.setObject(new Object[]{chunk, HTTPChunk.LAST_CHUNK});
                        return decodeState;
                    }
                }
                decodeState.setObject(chunk);
                return decodeState;
            }
            case READ_CHUNK_SIZE: {
                String line = readLine(buffer);
                if (line == null) {
                    decodeState.setState(DecodeState.NOT_ENOUGH);
                    return decodeState;
                }
                int chunkSize = getChunkSize(line);
                this.chunkSize = chunkSize;
                if (chunkSize == 0) {
                    state = State.READ_CHUNK_FOOTER;
                    return decodeState;
                } else if (chunkSize > maxChunkSize) {
                    // A chunk is too large. Split them into multiple chunks again.
                    state = State.READ_CHUNKED_CONTENT_AS_CHUNKS;
                } else {
                    state = State.READ_CHUNKED_CONTENT;
                }
            }
            case READ_CHUNKED_CONTENT: {
                assert chunkSize <= Integer.MAX_VALUE;
                byte[] chunkData = new byte[(int) chunkSize];
                if (buffer.readableBytes() < chunkSize) {
                    decodeState.setState(DecodeState.NOT_ENOUGH);
                    return decodeState;
                }
                buffer.readBytes(chunkData);
                HTTPChunk chunk = new DefaultHttpChunk(BufFacade.wrappedBuffer(chunkData));
                state = State.READ_CHUNK_DELIMITER;
                decodeState.setObject(chunk);
                return decodeState;
            }
            case READ_CHUNKED_CONTENT_AS_CHUNKS: {
                long chunkSize = this.chunkSize;
                HTTPChunk chunk;
                if (chunkSize > maxChunkSize) {
                    byte[] chunkData = new byte[(int) maxChunkSize];
                    if (buffer.readableBytes() < maxChunkSize) {
                        decodeState.setState(DecodeState.NOT_ENOUGH);
                        return decodeState;
                    }
                    buffer.readBytes(chunkData);
                    chunk = new DefaultHttpChunk(BufFacade.wrappedBuffer(chunkData));
                    chunkSize -= maxChunkSize;
                } else {
                    assert chunkSize <= Integer.MAX_VALUE;
                    byte[] chunkData = new byte[(int) chunkSize];
                    if (buffer.readableBytes() < chunkSize) {
                        decodeState.setState(DecodeState.NOT_ENOUGH);
                        return decodeState;
                    }
                    buffer.readBytes(chunkData);
                    chunk = new DefaultHttpChunk(BufFacade.wrappedBuffer(chunkData));
                    chunkSize = 0;
                }
                this.chunkSize = chunkSize;

                if (chunkSize == 0) {
                    // Read all content.
                    state = State.READ_CHUNK_DELIMITER;
                }

                if (!chunk.isLast()) {
                    decodeState.setObject(chunk);
                    return decodeState;
                }
            }
            case READ_CHUNK_DELIMITER: {
                for (; ; ) {
                    if (buffer.readableBytes() < 2) {
                        decodeState.setState(DecodeState.NOT_ENOUGH);
                        return decodeState;
                    }
                    byte next = buffer.readByte();
                    if (next == HTTPCodecUtil.CR) {
                        if (buffer.readByte() == HTTPCodecUtil.LF) {
                            state = State.READ_CHUNK_SIZE;
                            return decodeState;
                        }
                    } else if (next == HTTPCodecUtil.LF) {
                        state = State.READ_CHUNK_SIZE;
                        return decodeState;
                    }
                }
            }
            case READ_CHUNK_FOOTER: {
                HTTPChunkTrailer trailer = readTrailingHeaders(buffer);
                if (trailer == null) return null;
                if (maxChunkSize == 0) {
                    // Chunked encoding disabled.
                    return reset(decodeState);
                } else {
                    reset(decodeState);
                    decodeState.setObject(trailer);
                    // The last chunk, which is empty
                    return decodeState;
                }
            }
            default: {
                throw new Error("Shouldn't reach here.");
            }
        }
    }

    protected abstract HTTPMessage createMessage(String[] initialLine) throws Exception;

    private DecodeState reset(DecodeState decodeState) {
        HTTPMessage message = this.message;
        BufFacade content = this.content;

        if (content != null) {
            message.setContent(content);
            this.content = null;
        }
        this.message = null;
        decodeState.setState(DecodeState.ENOUGH);
        decodeState.setObject(message);
        state = State.SKIP_CONTROL_CHARS;
        return decodeState;
    }

    private DecodeState noreset(DecodeState decodeState) {

        if (content != null) {
            message.setContent(content);
            this.content = null;
        }
        decodeState.setState(DecodeState.ENOUGH);
        decodeState.setObject(message);
        return decodeState;
    }

    private boolean readFixedLengthContent(BufFacade buffer, AtomicBoolean reset) {
        long length = HTTPHeaders.getContentLength(message, -1);
        assert length <= Integer.MAX_VALUE;

        byte[] temp;
        if (length > buffer.readableBytes()) {
            // fix Content-Length header value of 32767 and accept application/x-rtsp-tunnelled
            // and we not reset current parse state
            if (message.getHeader(CONTENT_TYPE).equalsIgnoreCase("application/x-rtsp-tunnelled")) {
                length = buffer.readableBytes(); //Ignore content-length
                temp = new byte[(int) length];
                buffer.readBytes(temp);
                reset.set(false);
            } else {
                return false;
            }
        } else {
            temp = new byte[(int) length];
            buffer.readBytes(temp);
        }

        if (content == null) {
            content = BufFacade.wrappedBuffer(temp);
        } else {
            content.readBytes(temp);
        }
        return true;
    }

    private int getChunkSize(String hex) {
        hex = hex.trim();
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                hex = hex.substring(0, i);
                break;
            }
        }

        return Integer.parseInt(hex, 16);
    }

    private HTTPChunkTrailer readTrailingHeaders(BufFacade buffer) {
        headerSize = 0;
        buffer.markReaderIndex();
        String line = readHeader(buffer);
        String lastHeader = null;
        HTTPChunkTrailer trailer = null;
        if (line != null && line.length() != 0) {
            trailer = new DefaultHttpChunkTrailer();
            do {
                char firstChar = line.charAt(0);
                if (lastHeader != null && (firstChar == ' ' || firstChar == '\t')) {
                    List<String> current = trailer.getHeaders(lastHeader);
                    if (current.size() != 0) {
                        int lastPos = current.size() - 1;
                        String newString = current.get(lastPos) + line.trim();
                        current.set(lastPos, newString);
                    } else {
                        // Content-Length, Transfer-Encoding, or Trailer
                    }
                } else {
                    String[] header = splitHeader(line);
                    String name = header[0];
                    if (!name.equalsIgnoreCase(HTTPHeaders.Names.CONTENT_LENGTH) &&
                            !name.equalsIgnoreCase(HTTPHeaders.Names.TRANSFER_ENCODING) &&
                            !name.equalsIgnoreCase(HTTPHeaders.Names.TRAILER)) {
                        trailer.addHeader(name, header[1]);
                    }
                    lastHeader = name;
                }

                line = readHeader(buffer);
            } while (line != null && line.length() != 0);
        }
        if (line == null) {
            buffer.resetReaderIndex();
            return null;
        }
        if (trailer != null)
            return trailer;
        return HTTPChunk.LAST_CHUNK;
    }

    private State readHeaders(BufFacade buffer) {
        headerSize = 0;
        final HTTPMessage message = this.message;
        String line = readHeader(buffer);
        String name = null;
        String value = null;
        if (line != null && line.length() != 0) {
            message.clearHeaders();
            do {
                char firstChar = line.charAt(0);
                if (name != null && (firstChar == ' ' || firstChar == '\t')) {  //the next is also can be value of last key name
                    value = value + ' ' + line.trim();
                } else {
                    if (name != null) {
                        message.addHeader(name, value);
                    }
                    String[] header = splitHeader(line);
                    name = header[0];
                    value = header[1];
                }
                line = readHeader(buffer);
            } while (line != null && line.length() != 0);


            // Add the last header.
            if (name != null) {
                message.addHeader(name, value);
            } else {
                return null;
            }
        }

        if (line == null) return null;

        State nextState;

        if (isContentAlwaysEmpty(message)) {
            //后续内容为空 重置到初始状态
            nextState = State.SKIP_CONTROL_CHARS;
        } else if (message.isChunked()) {
            // HttpMessage.isChunked() returns true when either:
            // 1) HttpMessage.setChunked(true) was called or
            // 2) 'Transfer-Encoding' is 'chunked'.
            // Because this decoder did not call HttpMessage.setChunked(true)
            // yet, HttpMessage.isChunked() should return true only when
            // 'Transfer-Encoding' is 'chunked'.
            nextState = State.READ_CHUNK_SIZE;
        } else if (HTTPHeaders.getContentLength(message, -1) >= 0) {
            nextState = State.READ_FIXED_LENGTH_CONTENT;
        } else {
            nextState = State.READ_VARIABLE_LENGTH_CONTENT;
        }
        return nextState;
    }

    /**
     * actual read a Line and record headerSize
     *
     * @param buffer
     * @return
     */
    private String readHeader(BufFacade buffer) {
        StringBuilder sb = this.sb;
        sb.setLength(0);
        int headerSize = this.headerSize;
        buffer.markReaderIndex();
        loop:
        for (; ; ) {
            if (buffer.readableBytes() > 1) {
                char nextByte = (char) buffer.readByte();
                headerSize++;

                switch (nextByte) {
                    case HTTPCodecUtil.CR:
                        nextByte = (char) buffer.readByte();
                        headerSize++;
                        if (nextByte == HTTPCodecUtil.LF) {
                            break loop;
                        }
                        break;
                    case HTTPCodecUtil.LF:
                        break loop;
                }
                sb.append(nextByte);
            } else {
                buffer.resetReaderIndex();
                return null;
            }
        }
        this.headerSize = headerSize;
        return sb.toString();
    }

    private String[] splitHeader(String sb) {
        final int length = sb.length();
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;

        nameStart = findNonWhitespaceOffset(sb, 0);
        for (nameEnd = nameStart; nameEnd < length; nameEnd++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }

        for (colonEnd = nameEnd; colonEnd < length; colonEnd++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd++;
                break;
            }
        }

        valueStart = findNonWhitespaceOffset(sb, colonEnd);
        if (valueStart == length) {
            return new String[]{
                    sb.substring(nameStart, nameEnd),
                    ""
            };
        }

        valueEnd = findEndOfString(sb);
        return new String[]{
                sb.substring(nameStart, nameEnd),
                sb.substring(valueStart, valueEnd)
        };
    }

    /**
     * 是否返回内容为空
     *
     * @param msg 味精
     * @return boolean
     */
    protected boolean isContentAlwaysEmpty(HTTPMessage msg) {
        if (msg instanceof HTTPResponse) {
            HTTPResponse res = (HTTPResponse) msg;
            int code = res.getStatus().getCode();

            // Correctly handle return codes of 1xx.
            //
            // See:
            //     - http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html Section 4.4
            //     - https://github.com/netty/netty/issues/222
            if (code >= 100 && code < 200) {
                if (code == 101 && !res.containsHeader(HTTPHeaders.Names.SEC_WEBSOCKET_ACCEPT)) {
                    // It's Hixie 76 websocket handshake response
                    return false;
                }
                return true;
            }

            switch (code) {
                case 204:
                case 205:
                case 304:
                    return true;
            }
        }
        return false;
    }

    private void skipControlCharacters(BufFacade buffer) {
        for (; ; ) {
            buffer.markReaderIndex();
            char c = (char) buffer.readUnsignedByte();
            boolean dontNeedSkip = !Character.isISOControl(c) &&
                    !Character.isWhitespace(c);
            if (dontNeedSkip) {
                buffer.resetReaderIndex();
                break;
            }
        }
    }

    /**
     * each line of http protocol as small part of message ,so we often read each line to decode buff
     * <p>
     * 读后重置指针
     *
     * @param buffer
     * @return
     */
    private String readLine(BufFacade buffer) {
        StringBuilder sb = this.sb;
        sb.setLength(0);
        // store readerIndex of this buff, so we can reset after we do reading operations
        buffer.markReaderIndex();
        while (buffer.readableBytes() > 1) {
            byte nextByte = buffer.readByte();
            //回车换行符
            if (nextByte == HTTPCodecUtil.CR) {
                nextByte = buffer.readByte();
                if (nextByte == HTTPCodecUtil.LF) {
                    //after reading CR read the line feed, so we can know this time reading reaches the end of line
                    return sb.toString();
                }
            } else if (nextByte == HTTPCodecUtil.LF) {
                return sb.toString();
            } else {  // read char to String
                sb.append((char) nextByte);
            }
        }
        //如果读完,都读不到换行符则重置
        buffer.resetReaderIndex();
        return null;
    }

    private String[] splitInitialLine(String sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;

        aStart = findNonWhitespaceOffset(sb, 0);
        aEnd = findWhitespaceOffset(sb, aStart);

        bStart = findNonWhitespaceOffset(sb, aEnd);
        bEnd = findWhitespaceOffset(sb, bStart);

        cStart = findNonWhitespaceOffset(sb, bEnd);
        cEnd = findEndOfString(sb);

        return new String[]{
                sb.substring(aStart, aEnd),
                sb.substring(bStart, bEnd),
                cStart < cEnd ? sb.substring(cStart, cEnd) : ""};
    }

    /**
     * @param sb
     * @param offset
     * @return
     */
    private int findNonWhitespaceOffset(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    private int findWhitespaceOffset(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    private int findEndOfString(String sb) {
        int result;
        for (result = sb.length(); result > 0; result--) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }
}
