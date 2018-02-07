
package com.dinstone.np.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BioProcessor {

    public static final int MAX_INPUT = 1024;

    private final AtomicInteger counter = new AtomicInteger();

    private ExecutorService executorService;

    private HandlerInitialer handlerInitialer;

    private int maxClientCount;

    public BioProcessor(int maxClientCount) {
        if (maxClientCount < 0) {
            throw new IllegalArgumentException("maxClientCount less than 0");
        }

        this.maxClientCount = maxClientCount;

        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory("bio-processor-"));
    }

    private static String link(Socket socket) {
        return "[" + extractAddress(socket.getRemoteSocketAddress()) + "]->["
                + extractAddress(socket.getLocalSocketAddress()) + "]";
    }

    private static String extractAddress(SocketAddress socketAddress) {
        InetSocketAddress isa = (InetSocketAddress) socketAddress;
        return isa.getAddress() + ":" + isa.getPort();
    }

    public void process(Socket clientSocket) throws Exception {
        if (counter.get() >= maxClientCount) {
            System.out.println("can't process more client than " + maxClientCount + " for " + link(clientSocket));
            clientSocket.close();
        } else {
            counter.incrementAndGet();
            executorService.execute(new Processor(clientSocket));
        }
    }

    public void destroy() {
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void handlerInitialer(HandlerInitialer handlerInitialer) {
        this.handlerInitialer = handlerInitialer;
    }

    private class Processor implements Runnable {

        private Socket clientSocket;

        private String clientSession;

        public Processor(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientSession = link(clientSocket);
        }

        public void run() {
            try {
                clientSocket.setReuseAddress(true);
                clientSocket.setSoTimeout(1000);
                System.out.println("socket open with " + clientSession);

                Handler handler = handlerInitialer.initial(clientSocket);

                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();

                int received = 0;
                byte[] readBuffer = new byte[MAX_INPUT];
                // Receive until client closes connection, indicated by -1
                while (!Thread.interrupted() && (received = read(inputStream, readBuffer)) != -1) {
                    if (received == 0) {
                        continue;
                    }

                    Message message = handler.handle(new Message(readBuffer, received));
                    if (message != null) {
                        outputStream.write(message.getBuffer(), message.getOffset(), message.getLength());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // ignore
                }
                counter.decrementAndGet();
            }
            System.out.println("socket closed with " + clientSession);
        }

        private int read(InputStream inputStream, byte[] readBuffer) throws IOException {
            try {
                return inputStream.read(readBuffer);
            } catch (SocketTimeoutException e) {
            }
            return 0;
        }

    }

}
