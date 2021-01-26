
package com.dinstone.np.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class NonSelectorNioServer {

    private static final Logger LOG = LoggerFactory.getLogger(NioClientTest.class);

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
            ByteBuffer buf = ByteBuffer.allocate(4 * 1024);
            long bytesRead = channel.read(buf);
            if (bytesRead == -1) {
                channel.shutdownInput();

                inputShutdown = true;
            } else if (bytesRead > 0) {
                buf.flip();
                System.out.println(bytesRead);

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
        ssc.bind(new InetSocketAddress("127.0.0.1", 2222));

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
                Socket s = socketChannel.socket();
                s.setReceiveBufferSize(500);
                LOG.info("SendBufferSize = {}, ReceiveBufferSize = {}", s.getSendBufferSize(),
                        s.getReceiveBufferSize());

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
