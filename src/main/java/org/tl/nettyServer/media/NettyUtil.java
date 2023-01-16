package org.tl.nettyServer.media;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.ThreadFactory;

public class NettyUtil {
    public static NioEventLoopGroup getNEG(int nThreads, String name) {
        return new NioEventLoopGroup(nThreads, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, name);
            }
        });
    }
}
