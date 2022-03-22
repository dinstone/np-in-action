
package com.dinstone.np.netty.echo;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NioEchoServer {

    private int port;

    private NioEventLoopGroup bossGroup;

    private NioEventLoopGroup workGroup;

    public NioEchoServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1); // (1)
        workGroup = new NioEventLoopGroup();
        ServerBootstrap boot = new ServerBootstrap(); // (2)
        boot.group(bossGroup, workGroup).channel(NioServerSocketChannel.class) // (3)
            .handler(new LoggingHandler(LogLevel.INFO)).option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_BACKLOG, 128) // (5)
            .childHandler(new ChannelInitializer<SocketChannel>() { // (4)

                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    System.out.println(ch.config().getSendBufferSize());
                    ch.pipeline().addLast(new LineEncoder());
                    ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                    ch.pipeline().addLast(new EchoServerHandler());
                }
            }).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(5 * 1024, 10 * 1024))
            .childOption(ChannelOption.SO_SNDBUF, 20); // (6)

        // Bind and start to accept incoming connections.
        ChannelFuture f = boot.bind(port).sync(); // (7)
        SocketAddress address = f.channel().localAddress();
        System.out.println("echo server works on " + address);
    }

    public void stop() {
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    public class EchoServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            // ctx.channel().config().setWriteBufferHighWaterMark(10 * 1024 * 1024);

            Runnable loadRunner = new Runnable() {

                public void run() {

                    try {

                        TimeUnit.SECONDS.sleep(30);

                    } catch (InterruptedException e) {

                        e.printStackTrace();

                    }

                    ByteBuf msg = null;

                    int count = 0;
                    while (true) {
                        if (ctx.channel().isWritable()) {
                            count++;
                            msg = Unpooled.wrappedBuffer("1234567890".getBytes());
                            ctx.writeAndFlush(msg);

                            System.out.println("write count = " + count);
                        } else {
                            // msg = Unpooled.wrappedBuffer("my gold".getBytes());
                            // ctx.writeAndFlush(msg);

                            System.out.println("write count = " + count + ", queue = "
                                    + ctx.channel().unsafe().outboundBuffer().totalPendingWriteBytes());

                        }

                    }

                }

            };

            new Thread(loadRunner, "LoadRunner-Thread").start();
        }

        public void exceptionCaught(ChannelHandlerContext context, Throwable arg1) throws Exception {
            context.close();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, final Object msg) throws Exception {
            System.out.println("receive: " + msg);

            System.out.println("isWritable  " + ctx.channel().isWritable());

            ChannelFuture f = ctx.writeAndFlush(msg);

            f.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        System.out.println("send: " + ((ByteBuf) msg).readableBytes());
                    }
                }
            });

        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            System.out.println("status is  " + ctx.channel().isWritable());
        }
    }

    public static void main(String[] args) throws Exception {
        NioEchoServer server = new NioEchoServer(2222);
        server.start();

        System.in.read();

        server.stop();
    }

}
