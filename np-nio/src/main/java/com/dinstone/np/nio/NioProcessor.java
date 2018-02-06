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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NioProcessor {

    private static final AtomicInteger IDGEN = new AtomicInteger();

    private Selector selector;

    private ExecutorService executor;

    private volatile boolean disposing;

    private Queue<NioSession> newSessions = new ConcurrentLinkedQueue<NioSession>();

    private Queue<NioSession> removeSessions = new ConcurrentLinkedQueue<NioSession>();

    private AtomicReference<Processor> processorRef = new AtomicReference<NioProcessor.Processor>();

    public NioProcessor(ExecutorService executor) {
        this.executor = executor;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startProcessor() {
        Processor processor = processorRef.get();
        if (processor == null) {
            processor = new Processor();
            if (processorRef.compareAndSet(null, processor)) {
                String actualThreadName = getClass().getSimpleName() + '-' + IDGEN.incrementAndGet();
                executor.execute(new NamePreservingRunnable(processor, actualThreadName));
            }
        }

        selector.wakeup();
    }

    private boolean emptySessions() {
        return newSessions.isEmpty() && selector.keys().isEmpty();
    }

    private void removeSessions() throws IOException {
        for (NioSession session = removeSessions.poll(); session != null; session = removeSessions.poll()) {
            try {
                destroySession(session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void destroySession(NioSession session) throws IOException {
        SelectionKey selectionKey = session.getSelectionKey();
        if (selectionKey != null) {
            selectionKey.cancel();
        }
        session.getSocketChannel().close();
    }

    private void registerSessions() throws IOException, ClosedChannelException {
        for (NioSession session = newSessions.poll(); session != null; session = newSessions.poll()) {
            SelectableChannel channel = session.getSocketChannel().configureBlocking(false);
            session.setSelectionKey(channel.register(selector, SelectionKey.OP_READ, session));
        }
    }

    private void processSessions() throws IOException {
        for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext();) {
            SelectionKey sKey = iterator.next();
            NioSession session = (NioSession) sKey.attachment();
            NioHandler protocolHandler = session.getHandler();
            if (sKey.isReadable()) {
                SocketChannel sc = session.getSocketChannel();
                ByteBuffer dsts = ByteBuffer.allocate(512);
                int received = sc.read(dsts);
                if (received > 0) {
                    dsts.flip();
                    protocolHandler.message(session, dsts);
                } else if (received == -1) {
                    removeSessions.add(session);
                }
            }

            if (sKey.isValid() && sKey.isWritable()) {
                Queue<ByteBuffer> q = session.getWriteQueue();
                while (true) {
                    ByteBuffer wb = q.peek();
                    if (wb == null) {
                        session.setInterestedInWrite(false);
                        break;
                    }

                    int w = session.getSocketChannel().write(wb);
                    if (w > 0 && wb.hasRemaining()) {
                        session.setInterestedInWrite(true);
                        break;
                    } else {
                        q.poll();
                    }
                }
            }

            // remove from set of selected keys
            iterator.remove();
        }
    }

    public void register(NioSession session) {
        newSessions.add(session);
        startProcessor();
    }

    public void remove(NioSession ioSession) {
        removeSessions.add(ioSession);
        startProcessor();
    }

    public void dispose() {
        disposing = true;
        selector.wakeup();
    }

    class Processor implements Runnable {

        public void run() {
            while (true) {
                try {
                    // System.out.println("current managed session count is " + selector.keys().size());
                    int selected = selector.select(1000);

                    registerSessions();

                    if (selected > 0) {
                        processSessions();
                    }

                    removeSessions();

                    if (emptySessions()) {
                        break;
                    }

                    if (disposing) {
                        for (SelectionKey sk : selector.keys()) {
                            removeSessions.add((NioSession) sk.attachment());
                        }

                        selector.wakeup();
                    }

                } catch (ClosedSelectorException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                    }
                }
            }

            if (disposing) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
