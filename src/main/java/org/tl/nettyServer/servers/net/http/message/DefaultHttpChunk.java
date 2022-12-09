package org.tl.nettyServer.servers.net.http.message;

import org.tl.nettyServer.servers.buf.BufFacade;

/**
 * Default Http Chunk
 *
 * @author pengliren
 */
public class DefaultHttpChunk implements HTTPChunk {

    private BufFacade content;
    private boolean last;

    /**
     * Creates a new instance with the specified chunk content. If an empty
     * buffer is specified, this chunk becomes the 'end of content' marker.
     */
    public DefaultHttpChunk(BufFacade content) {
        setContent(content);
    }

    @Override
    public BufFacade getContent() {
        return content;
    }

    @Override
    public void setContent(BufFacade content) {
        if (content == null) {
            throw new NullPointerException("content");
        }
        last = content.readableBytes() == 0;
        this.content = content;
    }

    @Override
    public boolean isLast() {
        return last;
    }
}
