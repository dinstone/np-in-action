package com.dinstone.np.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class NioClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(NioClientTest.class);

    static byte[] dsts = new byte[1024];

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < dsts.length; i++) {
            dsts[i] = (byte) i;
        }

        SocketChannel sc = SocketChannel.open();
        // config non blocking
        sc.configureBlocking(false);
        LOG.info("SendBufferSize = {}, ReceiveBufferSize = {}", sc.socket().getSendBufferSize(),
                sc.socket().getReceiveBufferSize());
        // connect non blocking
        if (!sc.connect(new InetSocketAddress("127.0.0.1", 2222))) {
            LOG.info("connecting... ");
            while (!sc.finishConnect()) {
                Thread.sleep(1000);
            }
        }
        LOG.info("connection is ok");

        while (true) {
            int r = System.in.read();
            if ((char) r == 'q') {
                break;
            }
            if (r == 10) {
                writeData(sc);
            }
            readData(sc);
        }
        sc.close();
    }

    private static void readData(SocketChannel sc) {
        ByteBuffer dsts = ByteBuffer.allocate(1024);
        while (true) {
            try {
                int l = sc.read(dsts);
                LOG.info("read len = " + l);
                if (l == 0) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void writeData(SocketChannel sc) {
        ByteBuffer src = ByteBuffer.wrap(dsts);
        try {
            int l = sc.write(src);
            LOG.info("wirte len= {}", l);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
