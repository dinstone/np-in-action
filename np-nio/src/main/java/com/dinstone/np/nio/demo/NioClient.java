package com.dinstone.np.nio.demo;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioClient {

    public static void main(String args[]) throws Exception {
        String host = "127.0.0.1"; // Server name or IP address
        int port = 8585;
        // Create channel and set to nonblocking
        SocketChannel clntChan = SocketChannel.open();
        clntChan.configureBlocking(false);
        // Initiate connection to server and repeatedly poll until complete
        if (!clntChan.connect(new InetSocketAddress(host, port))) {
            while (!clntChan.finishConnect()) {
                System.out.print("."); // Do something else
            }
        }

        byte[] content = "hi, world".getBytes();
        ByteBuffer writeBuf = ByteBuffer.wrap(content);
        ByteBuffer readBuf = ByteBuffer.allocate(content.length);
        int totalBytesRcvd = 0; // Total bytes received so far
        int bytesRcvd; // Bytes received in last read
        while (totalBytesRcvd < content.length) {
            if (writeBuf.hasRemaining()) {
                clntChan.write(writeBuf);
            }
            if ((bytesRcvd = clntChan.read(readBuf)) == -1) {
                throw new SocketException("Connection closed prematurely");
            }
            totalBytesRcvd += bytesRcvd;
            System.out.print("."); // Do something else
        }
        System.out.println("Received: " + new String(readBuf.array(), 0, totalBytesRcvd));
        clntChan.close();
    }
}
