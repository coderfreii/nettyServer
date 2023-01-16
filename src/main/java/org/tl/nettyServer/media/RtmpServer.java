package org.tl.nettyServer.media;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.tl.nettyServer.media.net.rtmp.handler.*;
import org.tl.nettyServer.media.net.rtmp.handler.packet.ChannelTrafficShapingSessionHandler;

import java.util.concurrent.ThreadFactory;

public class RtmpServer {
    static private EventLoopGroup bossGroup;
    static private EventLoopGroup workerGroup;

    public static void main(String[] args) {

        ChannelHandler test = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                socketChannel.pipeline()
                        .addLast(new BufFacadeDecoder())
                        .addLast(new NioEventLoopGroup(10, new ThreadFactory() {
                            @Override
                            public Thread newThread(Runnable r) {
                                return new Thread(r, "RtmpPacketToByte");
                            }
                        }), new RtmpPacketToByteHandler())
                        .addLast(new MessageSendHandler())
                        .addLast(new ByteBufEncoder())
                        .addLast(new ConnInboundHandler()) //
                        .addLast(new ChannelTrafficShapingSessionHandler(0))
                        .addLast(new HandshakeHandler())
                        //将来可能也会很耗时
                        .addLast(NettyUtil.getNEG(1, "RTMPEHandler"), new RTMPEHandler())
                        //这一步骤计算比较耗时会阻塞接收其它的   //里面有一个重试机制耗时严重  需要自己的event loop
                        .addLast(NettyUtil.getNEG(1, "RtmpByteToPacketHandler"), new RtmpByteToPacketHandler())
                        .addLast(new RtmpPacketMayAsyncDecoder())
                        .addLast(new InExceptionHandler());
            }
        };


        //创建两个线程组 boosGroup、workerGroup
        bossGroup = new NioEventLoopGroup(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "boss");
            }
        });
        workerGroup = new NioEventLoopGroup(10, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "worker");
            }
        });
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
            ChannelFuture channelFuture = bootstrap.bind(19351);

            //添加监听器
            channelFuture.addListener(new ChannelFutureListener() {
                //使用匿名内部类，ChannelFutureListener接口
                //重写operationComplete方法
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //判断是否操作成功
                    if (future.isSuccess()) {
                        System.out.println("rtmp 连接成功 19351");
                    } else {
                        System.out.println("rtmp 连接失败 19351");
                    }
                }
            });

            //对关闭通道进行监听
            ChannelFuture channelCloseFuture = channelFuture.channel().closeFuture();
            channelCloseFuture.sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
