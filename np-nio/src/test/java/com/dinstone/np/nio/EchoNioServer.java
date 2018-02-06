
package com.dinstone.np.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class EchoNioServer {

    private static class EchoHandler implements NioHandler {

        public void message(NioSession session, ByteBuffer message) throws IOException {
            session.write(message);
        }

    }

    public static void main(String[] args) throws IOException {
        NioAcceptor acceptor = new NioAcceptor();
        acceptor.setHandler(new EchoHandler());

        acceptor.bind(new InetSocketAddress(2222));

        System.in.read();

        acceptor.dispose();
        System.out.println("serer will be closed");

    }
}
