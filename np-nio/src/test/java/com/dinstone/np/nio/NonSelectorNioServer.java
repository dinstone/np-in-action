
package com.dinstone.np.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NonSelectorNioServer {

    private static class Channel {

        private Queue<ByteBuffer> wq = new ConcurrentLinkedQueue<ByteBuffer>();

        private SocketChannel channel;

        private EchoHandler handler;

        private boolean inputShutdown;

        private boolean outputShutdown;

        public Channel(SocketChannel channel, EchoHandler handler) {
            this.channel = channel;
            this.handler = handler;
        }

        public void handle() throws IOException {
            if (!inputShutdown) {
                read();
            }

            if (!outputShutdown) {
                write();
            }
        }

        private void write() throws IOException {
            while (true) {
                ByteBuffer wb = wq.peek();
                if (wb == null) {
                    break;
                }

                int len = channel.write(wb);
                if (len == 0) {
                    break;
                }
                if (!wb.hasRemaining()) {
                    wq.poll();
                }
            }

            if (inputShutdown && wq.isEmpty()) {
                outputShutdown = true;
            }
        }

        private void read() throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(512);
            long bytesRead = channel.read(buf);
            if (bytesRead == -1) {
                channel.shutdownInput();

                inputShutdown = true;
            } else if (bytesRead > 0) {
                buf.flip();
                System.out.println(buf.asCharBuffer().toString());

                handler.handle(this, buf);
            }
        }

        public void write(ByteBuffer buf) {
            wq.add(buf);
        }

        public boolean isShutdown() {
            return inputShutdown && outputShutdown;
        }

    }

    private static class EchoHandler {

        public void handle(Channel channel, ByteBuffer buf) {
            channel.write(buf);
        }

    }

    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(2222));

        Queue<Channel> channels = new ConcurrentLinkedQueue<Channel>();

        while (true) {
            for (Channel channel : channels) {
                channel.handle();

                if (channel.isShutdown()) {
                    channels.remove(channel);
                }
            }

            SocketChannel socketChannel = ssc.accept();
            if (socketChannel != null) {
                socketChannel.configureBlocking(false);
                EchoHandler handler = new EchoHandler();
                channels.add(new Channel(socketChannel, handler));
            }

            if (channels.size() == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
