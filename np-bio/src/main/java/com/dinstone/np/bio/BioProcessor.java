
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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BioProcessor {

    public class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger index = new AtomicInteger();

        private String prex;

        public NamedThreadFactory(String prex) {
            this.prex = prex;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, prex + index.incrementAndGet());
        }

    }

    public static final int MAX_INPUT = 1024;

    private final AtomicInteger counter = new AtomicInteger();

    private ExecutorService executorService;

    private HandlerInitialer handlerInitialer;

    private int maxClientCount;

    public class Processor implements Runnable {

        private Socket socket;

        private String session;

        public Processor(Socket socket) {
            this.socket = socket;
            this.session = link(socket);
        }

        public void run() {
            try {
                System.out.println("socket open with " + session);
                Handler handler = handlerInitialer.initial(socket);

                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

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
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
                counter.decrementAndGet();
            }
            System.out.println("socket closed with " + session);
        }

        private int read(InputStream inputStream, byte[] readBuffer) throws IOException {
            try {
                return inputStream.read(readBuffer);
            } catch (SocketTimeoutException e) {
            }
            return 0;
        }

    }

    public BioProcessor(int processCount, int maxClientCount) {
        if (processCount > 0) {
            executorService = Executors.newFixedThreadPool(processCount, new NamedThreadFactory("bio-processor-"));
        } else {
            executorService = Executors.newCachedThreadPool(new NamedThreadFactory("bio-processor-"));
        }

        this.maxClientCount = maxClientCount;
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
            clientSocket.setSoTimeout(1000);
            executorService.execute(new Processor(clientSocket));
        }
    }

    public void destroy() {
        executorService.shutdownNow();
    }

    public void handlerInitialer(HandlerInitialer handlerInitialer) {
        this.handlerInitialer = handlerInitialer;
    }

}
