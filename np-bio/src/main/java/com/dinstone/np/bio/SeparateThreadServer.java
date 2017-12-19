
package com.dinstone.np.bio;

import java.io.IOException;
import java.net.Socket;

/**
 * @author dinstone
 * @version 1.0.0
 */
public class SeparateThreadServer {

    public static void main(String[] args) throws IOException {
        BioAccepter server = new BioAccepter(2, 3).handlerInitialer(new HandlerInitialer() {

            public Handler initial(Socket socket) {
                return new EchoHandler();
            }
        });
        server.bind(2222);

        System.in.read();

        server.destroy();
    }
}
