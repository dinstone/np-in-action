package com.dinstone.np.bio.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class BioServer {

    private static final Logger LOG = LoggerFactory.getLogger(BioServer.class);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8484);
        while (!Thread.interrupted()) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        process(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        serverSocket.close();
    }

    private static void process(Socket clientSocket) throws IOException {
        int received = 0;
        byte[] readBuffer = new byte[1024];
        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream();
        // Receive until client closes connection, indicated by -1
        while ((received = inputStream.read(readBuffer)) != -1) {
            LOG.info(new String(readBuffer, 0, received));
            outputStream.write(readBuffer, 0, received);
        }
        clientSocket.close();
    }

}
