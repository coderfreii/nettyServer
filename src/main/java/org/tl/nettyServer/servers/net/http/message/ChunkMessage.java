package org.tl.nettyServer.servers.net.http.message;

import org.tl.nettyServer.servers.buf.BufFacade;

public class ChunkMessage implements HTTPChunk {
    HTTPMessage httpMessage;


    public void setHttpMessage(HTTPMessage httpMessage) {
        this.httpMessage = httpMessage;
    }

    public HTTPMessage getHttpMessage() {
        return httpMessage;
    }

    @Override
    public boolean isLast() {
        return false;
    }

    @Override
    public BufFacade getContent() {
        return null;
    }

    @Override
    public void setContent(BufFacade content) {

    }
}
