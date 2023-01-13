package org.tl.nettyServer.media.net.hls.ts.message;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.util.HexDump;
import org.tl.nettyServer.media.util.SystemTimer;

import java.util.concurrent.locks.ReentrantLock;

/**
 * MPEG2TS Segment Data
 *
 * @author pengliren
 */
public class MpegTsSegment {

    // name of the segment
    private String name;

    // segment seq number
    private int sequence;

    // creation time
    private long created = SystemTimer.currentTimeMillis();

    // queue for holding data if using memory mapped i/o
    private volatile BufFacade buffer;

    // lock used when writing or slicing the buffer
    private volatile ReentrantLock lock = new ReentrantLock();

    // whether the segment is closed
    private volatile boolean closed = false;

    private String encKey;

    private byte[] encKeyBytes;

    private long segmentStartTimestamp;

    public MpegTsSegment(String name, int sequence) {
        this.name = name;
        this.sequence = sequence;
        buffer = BufFacade.buffer(1024 * 1024);
    }

    public void setSegmentStartTimestamp(long segmentStartTimestamp) {
        this.segmentStartTimestamp = segmentStartTimestamp;
    }

    public long getSegmentStartTimestamp() {
        return segmentStartTimestamp;
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
            buffer.release();
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
