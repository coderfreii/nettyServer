package org.tl.nettyServer.servers.net.http.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.tl.nettyServer.servers.net.http.message.HTTPHeaders;
import org.tl.nettyServer.servers.net.http.message.HTTPMessage;

import java.util.List;

public class ContentTypeDecoder extends MessageToMessageDecoder<HTTPMessage> {


    @Override
    protected void decode(ChannelHandlerContext ctx, HTTPMessage msg, List<Object> out) throws Exception {
        if (msg.getHeaders(HTTPHeaders.Names.CONTENT_TYPE).contains("application/json")) {
            try {
//                ContentTypeJson jsonObjectDecoder = new ContentTypeJson();
//                jsonObjectDecoder.decode(ctx, BufFacade.transfer(msg.getContent().getBuf()), out);
//                if (out.size() > 0) {
//                    BufFacade<Object> wrapperAndCast = BufFacade.wrapperAndCast(out.get(0));
//                    if (wrapperAndCast != null) {
//                        wrapperAndCast.markReaderIndex();
//                        byte[] bytes = new byte[wrapperAndCast.readableBytes()];
//                        wrapperAndCast.readBytes(bytes);
//                        String s = new String(bytes);
//                        System.out.println(s);
//                    }
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        out.add(msg);
    }
}
