package org.tl.nettyServer.media.media.ts;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.util.HexDump;

import java.util.concurrent.locks.ReentrantLock;

/**
 * MPEG2TS Segment Data
 *
 * @author pengliren
 */
public class MpegtsSegment {

    // name of the segment
    private String name;

    // segment seq number
    private int sequence;

    // creation time
    private long created = System.currentTimeMillis();

    // queue for holding data if using memory mapped i/o
    private volatile BufFacade buffer;

    // lock used when writing or slicing the buffer
    private volatile ReentrantLock lock = new ReentrantLock();

    // whether or not the segment is closed
    private volatile boolean closed = false;

    private String encKey;

    private byte[] encKeyBytes;

    public MpegtsSegment(String name, int sequence) {
        this.name = name;
        this.sequence = sequence;
        buffer = BufFacade.buffer(1024 * 1024);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getSequence() {
        return sequence;
    }

    public long getCreated() {
        return created;
    }

    public void setBuffer(BufFacade buf) {
        buffer = buf;
    }

    public BufFacade getBuffer() {
        return buffer;
    }

    public String getEncKey() {
        return encKey;
    }

    public void setEncKey(String encKey) {
        this.encKey = encKey;
        if (encKey != null) this.encKeyBytes = HexDump.decodeHexString(encKey);
    }

    public byte[] getEncKeyBytes() {
        return encKeyBytes;
    }

    public boolean close() {
        boolean result = false;
        if (buffer != null) {
            lock.lock();
            closed = true;
            try {

                result = true;
            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    /**
     * Should be called only when we are completely finished with this segment
     * and no longer want it to be available.
     */
    public void dispose() {
        if (buffer != null) {
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
