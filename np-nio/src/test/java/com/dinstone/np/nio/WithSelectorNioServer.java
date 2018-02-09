
package com.dinstone.np.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class WithSelectorNioServer {

    private static class EchoHandler {

        private int bufSize = 512;

        public void accept(SelectionKey key) throws IOException {
            SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
            // Must be nonblocking to register
            clntChan.configureBlocking(false);
            // Register the selector with new channel for read and attach byte buffer
            clntChan.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufSize));
        }

        public void read(SelectionKey key) throws IOException {
            // Client socket channel has pending data
            SocketChannel clntChan = (SocketChannel) key.channel();
            ByteBuffer buf = (ByteBuffer) key.attachment();
            long bytesRead = clntChan.read(buf);
            if (bytesRead == -1) {
                // Did the other end close?
                clntChan.close();
            } else if (bytesRead > 0) {
                // Indicate via key that reading/writing are both of interest now.
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        }

        public void write(SelectionKey key) throws IOException {
            // Retrieve data read earlier
            ByteBuffer buf = (ByteBuffer) key.attachment();
            buf.flip(); // Prepare buffer for writing
            SocketChannel clntChan = (SocketChannel) key.channel();
            clntChan.write(buf);
            if (!buf.hasRemaining()) {
                key.interestOps(SelectionKey.OP_READ);
            }

            // Make room for more data to be read in
            buf.compact();
        }

    }

    private static final long timeout = 1000;

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        ssc.register(selector, SelectionKey.OP_ACCEPT);

        ssc.bind(new InetSocketAddress(2222));

        EchoHandler handler = new EchoHandler();

        while (true) {
            if (selector.select(timeout) == 0) {
                continue;
            }

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey sk = iterator.next();

                showInterest(sk);

                if (sk.isAcceptable()) {
                    handler.accept(sk);
                }

                if (sk.isReadable()) {
                    handler.read(sk);
                }

                /*
                 * Channel is available for writing, and key is valid (i.e., client channel not closed).
                 */
                if (sk.isValid() && sk.isWritable()) {
                    handler.write(sk);
                }

                showInterest(sk);

                iterator.remove();
            }

        }
    }

    private static void showInterest(SelectionKey sk) {
        int iops = sk.interestOps();
        boolean aio = (iops & SelectionKey.OP_ACCEPT) != 0;
        boolean rio = (iops & SelectionKey.OP_READ) != 0;
        boolean wio = (iops & SelectionKey.OP_WRITE) != 0;
        System.out.println("interest : ACCEPT=" + aio + ",READ=" + rio + ",WRITE=" + wio);
    }
}
