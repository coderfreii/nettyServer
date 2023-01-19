/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 *
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tl.nettyServer.media.net.rtmp.message;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.buf.ReleaseUtil;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.stream.message.Releasable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RtmpProtocolState packet. Consists of packet header, data and event context.
 */
public class Packet implements Externalizable, Releasable {

    private static final long serialVersionUID = -6415050845346626950L;

    private static final boolean noCopy = System.getProperty("packet.noCopy") == null ? false : Boolean.valueOf(System.getProperty("packet.noCopy"));

    private Header header;

    private IRTMPEvent message;

    private BufFacade data;

    private transient long expirationTime = 0L;

    private transient final AtomicBoolean processed = new AtomicBoolean(false);

    /**
     * message 是否需要等待下一个chunk 才能写入完成
     */
    private boolean unCompletedToContinue = false;


    public boolean isUnCompletedToContinue() {
        return unCompletedToContinue;
    }

    public void setUnCompletedToContinue(boolean unCompletedToContinue) {
        this.unCompletedToContinue = unCompletedToContinue;
    }

    public void updateHeader(Header header) {
        this.header = header;
    }

    public Packet() {
    }

    public Packet(Header header) {
        this.header = header;
        data = BufFacade.buffer(header.getDataSize());
    }

    public Packet(Header header, IRTMPEvent event) {
        this.header = header;
        this.message = event;
    }

    public Header getHeader() {
        return header;
    }

    public void setMessage(IRTMPEvent message) {
        this.message = message;
    }

    public IRTMPEvent getMessage() {
        return message;
    }

    public void setData(BufFacade buffer) {
        if (this.data != null) {
            this.data.release();
        }

        if (noCopy) {
            this.data = buffer;
        } else {
            // try the backing array first if it exists
            if (buffer.hasArray()) {
                byte[] copy = new byte[buffer.readableBytes()];
                buffer.markReaderIndex();
                buffer.readBytes(copy);
                buffer.resetReaderIndex();
                data = BufFacade.wrappedBuffer(copy);
            } else {
                // fallback to ByteBuffer
                System.out.println("fallback to ByteBuffer");
            }
        }
    }

    public BufFacade getData() {
        return data;
    }

    public boolean hasData() {
        return data != null;
    }

    public void clearData() {
        if (data != null) {
            data.clear();
            data.release();
            data = null;
        }
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        // if expirationTime is zero, the expiration is not used
        return expirationTime > 0L ? System.currentTimeMillis() > expirationTime : false;
    }

    public void setProcessed(boolean isProcessed) {
        this.processed.set(isProcessed);
    }

    public boolean isProcessed() {
        return this.processed.get();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        header = (Header) in.readObject();
        message = (IRTMPEvent) in.readObject();
        message.setHeader(header);
        message.setTimestamp(header.getTimer());
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(header);
        out.writeObject(message);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Packet [");
        if (header != null) {
            sb.append("[header data type=" + header.getDataType() + ", csId=" + header.getCsId() + ", timer=" + header.getTimer() + "]");
        } else {
            sb.append("[header=null]");
        }
        if (message != null) {
            sb.append(", [message timestamp=" + message.getTimestamp() + "]");
        } else {
            sb.append(", [message=null]");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean release() {
        boolean d = true;
        boolean m = true;

        if (data != null) {
            d = ReleaseUtil.release(data);
        }
        if (this.message != null) {
            m = ReleaseUtil.release(message);
        }

        if (d && m) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        ReleaseUtil.clear(data);
        ReleaseUtil.clear(message);
    }
}
