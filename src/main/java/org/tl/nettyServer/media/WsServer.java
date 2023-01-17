package org.tl.nettyServer.media;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.tl.nettyServer.media.net.http.codec.ChunkM2MDecoder;
import org.tl.nettyServer.media.net.http.codec.ContentTypeDecoder;
import org.tl.nettyServer.media.net.http.handler.ConnInboundHandlerAdapter;
import org.tl.nettyServer.media.net.ws.handler.WebSocketDecoderHandler;
import org.tl.nettyServer.media.net.ws.handler.WebSocketEncoderHandler;
import org.tl.nettyServer.media.net.ws.handler.WebSocketFrameHandler;
import org.tl.nettyServer.media.net.ws.handler.WebSocketUpgradeHandler;


public class WsServer {
    static private EventLoopGroup bossGroup;
    static private EventLoopGroup workerGroup;

    public static void main(String[] args) {
        ChannelHandler test = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                socketChannel.pipeline()
                        .addLast(NettyUtil.getNEG(5, "WebSocketEncoderHandler"), new WebSocketEncoderHandler())
                        .addLast(NettyUtil.getNEG(5, "ConnInboundHandlerAdapter"), new ConnInboundHandlerAdapter())
                        .addLast(new WebSocketDecoderHandler())
                        .addLast(new ChunkM2MDecoder())
                        .addLast(new ContentTypeDecoder())
                        .addLast(new WebSocketUpgradeHandler())
                        .addLast(new WebSocketFrameHandler());
            }
        };


        //创建两个线程组 boosGroup、workerGroup
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(10);
        try {
            //创建服务端的启动对象，设置参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            //设置两个线程组boosGroup和workerGroup
            bootstrap.group(bossGroup, workerGroup)
                    //设置服务端通道实现类型
                    .channel(NioServerSocketChannel.class)
                    //设置线程队列得到连接个数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    //设置保持活动连接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                    //使用匿名内部类的形式初始化通道对象
                    .childHandler(test);//给workerGroup的EventLoop对应的管道设置处理器

            //绑定端口号，启动服务端
            ChannelFuture channelFuture = bootstrap.bind(7412);

            //添加监听器
            channelFuture.addListener(new ChannelFutureListener() {
                //使用匿名内部类，ChannelFutureListener接口
                //重写operationComplete方法
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //判断是否操作成功
                    if (future.isSuccess()) {
                        System.out.println("http 连接成功 7412");
                    } else {
                        System.out.println("http 连接失败 7412");
                    }
                }
            });

            //对关闭通道进行监听
            ChannelFuture channelCloseFuture = channelFuture.channel().closeFuture();
        } catch (RuntimeException e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
