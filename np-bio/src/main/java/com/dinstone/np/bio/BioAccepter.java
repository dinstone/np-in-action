
package com.dinstone.np.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class BioAccepter {

    private AtomicReference<Thread> accepterRef = new AtomicReference<Thread>();

    private BioProcessor processor;

    private HandlerInitialer handlerInitialer;

    public BioAccepter() {
        this(Integer.MAX_VALUE);
    }

    public BioAccepter(int maxClientCount) {
        this.processor = new BioProcessor(maxClientCount);
    }

    public BioAccepter handlerInitialer(HandlerInitialer handlerInitialer) {
        this.handlerInitialer = handlerInitialer;
        return this;
    }

    public BioAccepter bind(int port) {
        if (port < 0) {
            throw new IllegalArgumentException("port less than 0");
        }

        bind(new InetSocketAddress(port));

        return this;
    }

    public BioAccepter bind(InetSocketAddress address) {
        if (address == null) {
            throw new IllegalArgumentException("address is null");
        }

        if (handlerInitialer == null) {
            throw new IllegalStateException("handlerInitialer is null");
        }
        processor.handlerInitialer(handlerInitialer);

        if (accepterRef.compareAndSet(null, new Thread(new Accepter(address), "bio-accepter-" + address.getPort()))) {
            accepterRef.get().start();
        }

        return this;
    }

    public BioAccepter destroy() {

        Thread at = accepterRef.getAndSet(null);
        if (at != null) {
            at.interrupt();
            try {
                at.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        processor.destroy();

        return this;
    }

    public class Accepter implements Runnable {

        private InetSocketAddress address;

        public Accepter(InetSocketAddress address) {
            this.address = address;
        }

        public void run() {
            ServerSocket serverSocket = null;
            try {
                System.out.println("server accept thread will work on " + address);
                // create server socket and bind port
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.setSoTimeout(1000);
                serverSocket.bind(address, 10);

                while (!Thread.interrupted()) {
                    Socket clientSocket = accept(serverSocket);
                    if (clientSocket == null) {
                        continue;
                    }

                    try {
                        processor.process(clientSocket);
                    } catch (Exception e) {
                        System.out.println("can't process socket for error :" + e.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            System.out.println("server accept thread will exit on " + address);
        }

        private Socket accept(ServerSocket serverSocket) throws IOException {
            try {
                return serverSocket.accept();
            } catch (SocketTimeoutException e) {
                // expact
            }
            return null;
        }

    }
}
