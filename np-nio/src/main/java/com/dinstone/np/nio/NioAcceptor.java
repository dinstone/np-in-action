/*
 * Copyright (C) 2011~2012 dinstone <dinstone@163.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.dinstone.np.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NioAcceptor {

    private static final AtomicInteger ID = new AtomicInteger(0);

    private AtomicReference<Acceptor> acceptorRef = new AtomicReference<Acceptor>();

    private Queue<InetSocketAddress> bindQueue = new ConcurrentLinkedQueue<InetSocketAddress>();

    private Queue<SelectionKey> cancelQueue = new ConcurrentLinkedQueue<SelectionKey>();

    private volatile boolean selectable;

    private volatile boolean disposing;

    private ExecutorService executor;

    private NioProcessor[] processorPool;

    private Selector selector;

    private NioHandler handler;

    public NioAcceptor() {
        // 初始化线程池
        executor = Executors.newCachedThreadPool();

        // 初始化IoProcessor池
        int size = Runtime.getRuntime().availableProcessors() + 1;
        processorPool = new NioProcessor[size];
        for (int i = 0; i < size; i++) {
            processorPool[i] = new NioProcessor(executor);
        }

        // 初始化选择器
        try {
            selector = Selector.open();
            selectable = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize IoAcceptor.", e);
        }
    }

    public void setHandler(NioHandler handler) {
        this.handler = handler;
    }

    public void bind(InetSocketAddress localAddress) throws IOException {
        // 检查参数
        if (localAddress == null) {
            throw new IllegalArgumentException("localAddress");
        }

        // 检查handler是否设置
        if (handler == null) {
            throw new IllegalStateException("handler is not set.");
        }

        // bind
        this.bindQueue.add(localAddress);

        startAcceptor();

        System.out.println("Server is listening " + localAddress);
    }

    public void dispose() {
        try {
            disposing = true;
            selector.wakeup();

            executor.shutdownNow();
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAcceptor() {
        Acceptor acceptor = acceptorRef.get();
        if (acceptor == null) {
            acceptor = new Acceptor();
            if (acceptorRef.compareAndSet(null, acceptor)) {
                String actualThreadName = getClass().getSimpleName() + '-' + ID.incrementAndGet();
                executor.execute(new NamePreservingRunnable(acceptor, actualThreadName));
            }
        }

        selector.wakeup();
    }

    private void destroy() throws IOException {
        if (selector != null) {
            selector.close();
        }
    }

    private NioProcessor getProcessor(NioSession session) {
        NioProcessor processor = session.getProcessor();
        if (processor == null) {
            int index = Math.abs((int) session.getId() % processorPool.length);
            processor = processorPool[index];
            session.setProcessor(processor);
        }

        return processor;
    }

    private void registerChannels() throws IOException {
        for (InetSocketAddress address = bindQueue.poll(); address != null; address = bindQueue.poll()) {
            // create listening socket channel and register selector
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.socket().bind(address, 500);
            ssc.configureBlocking(false);

            ssc.register(selector, SelectionKey.OP_ACCEPT);
        }
    }

    private void removeChannels() {
        for (SelectionKey selectionKey = cancelQueue.poll(); selectionKey != null; selectionKey = cancelQueue.poll()) {
            try {
                destroyChannel(selectionKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void destroyChannel(SelectionKey selectionKey) throws IOException {
        if (selectionKey != null) {
            selectionKey.cancel();
        }
        selectionKey.channel().close();
    }

    private void processChannels() throws IOException {
        Set<SelectionKey> skeys = selector.selectedKeys();
        // System.out.println("accepted socket count is " + skeys.size());
        for (Iterator<SelectionKey> iterator = skeys.iterator(); iterator.hasNext();) {
            SelectionKey sKey = iterator.next();

            if (sKey.isAcceptable()) {
                SocketChannel sc = ((ServerSocketChannel) sKey.channel()).accept();
                NioSession session = new NioSession(handler, sc);
                getProcessor(session).register(session);
            }

            // remove from set of selected keys
            iterator.remove();
        }
    }

    class Acceptor implements Runnable {

        public void run() {
            while (selectable) {
                try {
                    int selected = selector.select();

                    registerChannels();

                    if (emptyChannels()) {
                        break;
                    }

                    if (selected > 0) {
                        processChannels();
                    }

                    removeChannels();

                    if (disposing) {
                        for (SelectionKey sk : selector.keys()) {
                            cancelQueue.add(sk);
                        }

                        selector.wakeup();
                    }
                } catch (ClosedChannelException e) {
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 清理资源
            if (selectable && disposing) {
                selectable = false;
                try {
                    for (int i = 0; i < processorPool.length; i++) {
                        processorPool[i].dispose();
                    }
                } finally {
                    try {
                        destroy();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private boolean emptyChannels() {
            return bindQueue.isEmpty() && selector.keys().isEmpty();
        }
    }

}
