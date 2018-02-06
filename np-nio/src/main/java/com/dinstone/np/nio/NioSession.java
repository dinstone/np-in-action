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

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class NioSession {

    private static final AtomicLong IDGEN = new AtomicLong();

    private Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>();

    private Queue<ByteBuffer> wq = new ConcurrentLinkedQueue<ByteBuffer>();

    private long id;

    private NioHandler handler;

    private SocketChannel channel;

    private SelectionKey selectionKey;

    private NioProcessor processor;

    public NioSession(NioHandler handler, SocketChannel channel) {
        this.id = IDGEN.incrementAndGet();
        this.handler = handler;
        this.channel = channel;
    }

    public Object getAttribute(Object name) {
        return attributes.get(name);
    }

    public void setAttribute(Object name, Object processor) {
        attributes.put(name, processor);
    }

    public long getId() {
        return id;
    }

    public SocketChannel getSocketChannel() {
        return channel;
    }

    public NioHandler getHandler() {
        return handler;
    }

    public void close() {
        processor.remove(this);
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public void setProcessor(NioProcessor processor) {
        this.processor = processor;
    }

    public NioProcessor getProcessor() {
        return processor;
    }

    public Queue<ByteBuffer> getWriteQueue() {
        return wq;
    }

    public void write(ByteBuffer message) {
        wq.add(message);
        setInterestedInWrite(true);
    }

    public void setInterestedInWrite(boolean isInterested) {
        SelectionKey key = this.getSelectionKey();

        if (key == null) {
            return;
        }

        int newInterestOps = key.interestOps();

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
            // newInterestOps &= ~SelectionKey.OP_READ;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
            // newInterestOps |= SelectionKey.OP_READ;
        }

        key.interestOps(newInterestOps);
    }

}
