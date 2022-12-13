package org.tl.nettyServer.media.net.http.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.message.*;

import java.util.ArrayList;
import java.util.List;

public class ChunkM2MDecoder extends MessageToMessageDecoder<HTTPChunk> {
    List<HTTPChunk> chunkList = new ArrayList();

    @Override
    protected void decode(ChannelHandlerContext ctx, HTTPChunk msg, List<Object> out) throws Exception {
        chunkList.add(msg);
        if (msg instanceof HTTPChunkTrailer) {
            ChunkMessage chunkMessage = (ChunkMessage) chunkList.get(0);
            HTTPMessage httpMessage = chunkMessage.getHttpMessage();
            //这里CONTENT_LENGTH作为参考
            String header = httpMessage.getHeader(HTTPHeaders.Names.CONTENT_LENGTH);
            BufFacade<Object> buffer;
            if (header != null) {
                buffer = BufFacade.buffer(Integer.valueOf(header));
            } else {
                buffer = BufFacade.buffer(0);
            }
            httpMessage.setContent(buffer);

            for (int i = 1; i < chunkList.size() - 1; i++) {
                HTTPChunk httpChunk = chunkList.get(i);
                BufFacade content = httpChunk.getContent();
                buffer.writeBytes(content);
            }
            chunkList.clear();
            out.add(chunkMessage.getHttpMessage());
        }
    }
}
