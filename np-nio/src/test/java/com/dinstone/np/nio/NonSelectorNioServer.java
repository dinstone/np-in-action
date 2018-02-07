
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

        private SocketChannel channel;

        private EchoHandler handler;

        public Channel(SocketChannel socketChannel, EchoHandler handler) {
            this.channel = socketChannel;
            this.handler = handler;
        }

        public EchoHandler getHandler() {
            return handler;
        }

    }

    private static class EchoHandler {

        private Queue<ByteBuffer> wq = new ConcurrentLinkedQueue<ByteBuffer>();

        private SocketChannel channel;

        public EchoHandler(SocketChannel socketChannel) {
            this.channel = socketChannel;
        }

        public void write() throws IOException {
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
        }

        public void read() throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(512);
            long bytesRead = channel.read(buf);
            if (bytesRead == -1) {
                channel.shutdownInput();
                throw new IOException("channel is closed");
            } else if (bytesRead > 0) {
                buf.flip();
                System.out.println(buf.asCharBuffer().toString());

                wq.add(buf);
            }
        }

    }

    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(2222));
        ssc.configureBlocking(false);

        Queue<Channel> channels = new ConcurrentLinkedQueue<Channel>();

        while (true) {
            for (Channel channel : channels) {
                channel.getHandler().write();
                channel.getHandler().read();
            }

            SocketChannel socketChannel = ssc.accept();
            if (socketChannel != null) {
                socketChannel.configureBlocking(false);
                EchoHandler handler = new EchoHandler(socketChannel);
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
