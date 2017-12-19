
package com.dinstone.np.netty.echo;

import java.net.SocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;

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
            .childHandler(new ChannelInitializer<SocketChannel>() { // (4)

                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LineEncoder());
                    ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                    ch.pipeline().addLast(new EchoServerHandler());
                }
            }).option(ChannelOption.SO_BACKLOG, 128) // (5)
            .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

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

        public void exceptionCaught(ChannelHandlerContext context, Throwable arg1) throws Exception {
            context.close();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ctx.writeAndFlush(msg);
        }
    }

    public static void main(String[] args) throws Exception {
        NioEchoServer server = new NioEchoServer(2222);
        server.start();

        System.in.read();

        server.stop();
    }

}
