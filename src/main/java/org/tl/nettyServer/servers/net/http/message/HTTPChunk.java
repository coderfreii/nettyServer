package org.tl.nettyServer.servers.net.http.message;

import org.tl.nettyServer.servers.buf.BufFacade;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HTTP Chunk Interface
 *
 * @author tl
 */
public interface HTTPChunk {

    /**
     * The 'end of content' marker in chunked encoding.
     */
    HTTPChunkTrailer LAST_CHUNK = new HTTPChunkTrailer() {
        @Override
        public BufFacade getContent() {
            return null;
        }

        @Override
        public void setContent(BufFacade content) {
            throw new IllegalStateException("read-only");
        }

        @Override
        public boolean isLast() {
            return true;
        }

        @Override
        public void addHeader(String name, Object value) {
            throw new IllegalStateException("read-only");
        }

        @Override
        public void clearHeaders() {
            // NOOP
        }

        @Override
        public boolean containsHeader(String name) {
            return false;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Set<String> getHeaderNames() {
            return Collections.emptySet();
        }

        @Override
        public List<String> getHeaders(String name) {
            return Collections.emptyList();
        }

        @Override
        public List<Map.Entry<String, String>> getHeaders() {
            return Collections.emptyList();
        }

        @Override
        public void removeHeader(String name) {
            // NOOP
        }

        @Override
        public void setHeader(String name, Object value) {
            throw new IllegalStateException("read-only");
        }

        @Override
        public void setHeader(String name, Iterable<?> values) {
            throw new IllegalStateException("read-only");
        }
    };

    /**
     * Returns {@code true} if and only if this chunk is the 'end of content'
     * marker.
     */
    boolean isLast();

    /**
     * Returns the content of this chunk.  If this is the 'end of content'
     * marker,  will be returned.
     */
    BufFacade getContent();

    /**
     * Sets the content of this chunk.  If an empty buffer is specified,
     * this chunk becomes the 'end of content' marker.
     */
    void setContent(BufFacade content);
}
