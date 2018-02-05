
package com.dinstone.np.bio;

import java.net.Socket;

public class BioEchoServer {

    private int port;

    private BioAccepter accepter;

    public BioEchoServer(int port) {
        this.port = port;
    }

    public void start() {
        accepter = new BioAccepter(2).handlerInitialer(new HandlerInitialer() {

            public Handler initial(Socket socket) {
                return new EchoHandler();
            }
        }).bind(port);
    }

    public void stop() {
        accepter.destroy();
    }

    public class EchoHandler implements Handler {

        public Message handle(Message message) {
            return message;
        }

    }

    public static void main(String[] args) throws Exception {
        BioEchoServer server = new BioEchoServer(2222);
        server.start();

        System.in.read();

        server.stop();
    }
}
