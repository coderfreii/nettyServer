package org.tl.nettyServer.media;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.tl.nettyServer.media.net.rtsp.codec.RTSPRequestDecoder;
import org.tl.nettyServer.media.net.rtsp.codec.RTSPResponseEncoder;
import org.tl.nettyServer.media.net.rtsp.handler.ConnInboundHandlerAdapter;
import org.tl.nettyServer.media.net.rtsp.handler.RTSPChannelDataHandler;
import org.tl.nettyServer.media.net.rtsp.handler.RTSPRequestHandler;
import org.tl.nettyServer.media.util.CustomizableThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RtspServer {
    static private EventLoopGroup bossGroup;
    static private EventLoopGroup workerGroup;


    static ExecutorService executor = Executors.newFixedThreadPool(4, new CustomizableThreadFactory("UdpWorkerExecutor-"));

    public static Bootstrap RTP_VIDEO_ACCEPTOR = d();
    public static Bootstrap RTCP_VIDEO_ACCEPTOR = d();
    public static Bootstrap RTP_AUDIO_ACCEPTOR = d();
    public static Bootstrap RTCP_AUDIO_ACCEPTOR = d();


    public static Channel RTP_VIDEO_ACCEPTOR_CHANNEL;
    public static Channel RTCP_VIDEO_ACCEPTOR_CHANNEL;
    public static Channel RTP_AUDIO_ACCEPTOR_CHANNEL;
    public static Channel RTCP_AUDIO_ACCEPTOR_CHANNEL;


    static {


        RTP_VIDEO_ACCEPTOR.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
            }
        });


        RTCP_VIDEO_ACCEPTOR.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
            }
        });


        RTP_AUDIO_ACCEPTOR.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
            }
        });


        RTCP_AUDIO_ACCEPTOR.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
            }
        });
    }

    public static void main(String[] args) {
        ChannelHandler test = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                socketChannel.pipeline()
                        .addLast(new RTSPResponseEncoder())
                        .addLast(new ConnInboundHandlerAdapter())
                        .addLast(new RTSPRequestDecoder())
                        .addLast(new RTSPRequestHandler())
                        .addLast(new RTSPChannelDataHandler())
                ;
            }
        };


        //创建两个线程组 boosGroup、workerGroup
        bossGroup = new NioEventLoopGroup(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "boss");
            }
        });
        workerGroup = new NioEventLoopGroup(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "worker");
            }
        });

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
                //使用匿名内部类的形式初始化通道对象
                .childHandler(test);//给workerGroup的EventLoop对应的管道设置处理器

        //绑定端口号，启动服务端
        ChannelFuture channelFuture = bootstrap.bind(5541);

        //添加监听器
        channelFuture.addListener(new ChannelFutureListener() {
            //使用匿名内部类，ChannelFutureListener接口
            //重写operationComplete方法
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //判断是否操作成功
                if (future.isSuccess()) {
                    System.out.println("rtsp 连接成功 5541");
                } else {
                    System.out.println("rtsp 连接失败 5541");
                }
            }
        });

        //对关闭通道进行监听
        ChannelFuture channelCloseFuture = channelFuture.channel().closeFuture();
        channelCloseFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
    }


    static Bootstrap d() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    // 主线程处理
                    .channel(NioDatagramChannel.class)
                    // 广播
                    .option(ChannelOption.SO_BROADCAST, true)
                    // 设置读缓冲区为2M
                    .option(ChannelOption.SO_RCVBUF, 2048 * 1024)
                    // 设置写缓冲区为1M
                    .option(ChannelOption.SO_SNDBUF, 1024 * 1024);

            return bootstrap;
        } finally {
//            group.shutdownGracefully();
        }
    }
}
