package com.dinstone.np.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

public class NioServer {

    public static void main(String[] args) throws IOException {
        int BUFSIZE = 256; // Buffer size (bytes)
        final int TIMEOUT = 3000; // Wait timeout (milliseconds)
        // Create a selector to multiplex listening sockets and connections
        Selector selector = Selector.open();
        // Create listening socket channel for each port and register selector
        ServerSocketChannel listnChannel = ServerSocketChannel.open();
        listnChannel.socket().bind(new InetSocketAddress(8585));
        listnChannel.configureBlocking(false); // must be nonblocking to register
        // Register selector with channel. The returned key is ignored
        listnChannel.register(selector, SelectionKey.OP_ACCEPT);
        // Create a handler that will implement the protocol
        TCPProtocol protocol = new EchoSelectorProtocol(BUFSIZE);
        while (true) { // Run forever, processing available I/O operations
            // Wait for some channel to be ready (or timeout)
            if (selector.select(TIMEOUT) == 0) { // returns # of ready chans
                System.out.print(".");
                continue;
            }

            // Get iterator on set of keys with I/O to process
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next(); // Key is bit mask
                // Server socket channel has pending connection requests?
                if (key.isAcceptable()) {
                    protocol.handleAccept(key);
                }
                // Client socket channel has pending data?
                if (key.isReadable()) {
                    protocol.handleRead(key);
                }
                // Client socket channel is available for writing and
                // key is valid (i.e., channel not closed)?
                if (key.isValid() && key.isWritable()) {
                    protocol.handleWrite(key);
                }
                keyIter.remove(); // remove from set of selected keys
            }
        }
    }
}
