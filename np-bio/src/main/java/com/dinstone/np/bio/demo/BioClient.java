package com.dinstone.np.bio.demo;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class BioClient {

    private static final Logger LOG = LoggerFactory.getLogger(BioClient.class);

    public static void main(String[] args) throws Exception {
        Socket clientSocket = new Socket("127.0.0.1", 8484);
        clientSocket.setSoTimeout(100);

        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream();

        int count = 0;
        while (count < 3) {
            String m = "hi, U" + count;
            outputStream.write(m.getBytes());

            int received = 0;
            byte[] readBuffer = new byte[1024];
            try {
                // Receive until client closes connection, indicated by -1
                while ((received = inputStream.read(readBuffer)) != -1) {
                    LOG.info(new String(readBuffer, 0, received));
                }
            } catch (SocketTimeoutException e) {
                // ignore
            }

            count++;
            Thread.sleep(1000);
        }

        clientSocket.close();
    }

}
