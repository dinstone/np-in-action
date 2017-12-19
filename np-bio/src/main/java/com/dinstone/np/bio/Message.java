
package com.dinstone.np.bio;

public class Message {

    private byte[] buffer;

    private int length;

    private int offset;

    public Message(byte[] buffer) {
        this(buffer, buffer.length);
    }

    public Message(byte[] buffer, int length) {
        this(buffer, length, 0);
    }

    public Message(byte[] buffer, int length, int offset) {
        this.buffer = buffer;
        this.length = length;
        this.offset = offset;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getLength() {
        return length;
    }

    public int getOffset() {
        return offset;
    }

}
