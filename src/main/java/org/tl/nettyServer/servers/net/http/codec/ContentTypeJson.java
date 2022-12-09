package org.tl.nettyServer.servers.net.http.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.json.JsonObjectDecoder;

import java.util.List;

public class ContentTypeJson extends JsonObjectDecoder {
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        super.decode(ctx, in, out);
    }
}
