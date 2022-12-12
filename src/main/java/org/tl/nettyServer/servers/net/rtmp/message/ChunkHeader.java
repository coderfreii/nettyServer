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

package org.tl.nettyServer.servers.net.rtmp.message;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.rtmp.codec.ProtocolException;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * RTMP chunk header
 *
 * <pre>
 * rtmp_specification_1.0.pdf (5.3.1.1 page 12)
 * </pre>
 */
public class ChunkHeader implements Constants, Cloneable, Externalizable {

    protected static final Logger log = LoggerFactory.getLogger(ChunkHeader.class);

    /**
     * Chunk format
     */
    private byte format;

    /**
     * Chunk size
     */
    private byte size;

    /**
     * Channel
     * chunk stream ID
     */
    private int csId;

    /**
     * Getter for format
     *
     * @return chunk format
     */
    public byte getFormat() {
        return format;
    }

    /**
     * Setter for format
     *
     * @param format format
     */
    public void setFormat(byte format) {
        this.format = format;
    }

    /**
     * Getter for channel id
     *
     * @return Channel id
     */
    public int getChannelId() {
        return csId;
    }

    /**
     * Setter for channel id
     *
     * @param csId Header channel id
     */
    public void setChannelId(int csId) {
        this.csId = csId;
    }

    /**
     * Getter for size
     *
     * @return size
     */
    public byte getSize() {
        return size;
    }

    /**
     * Setter for size
     *
     * @param size Header size
     */
    public void setSize(byte size) {
        this.size = size;
    }

    /**
     * Read chunk header from the buffer.
     *
     * @param in buffer
     * @return ChunkHeader instance
     */
    public static ChunkHeader read(BufFacade in) {
        int remaining = in.readableBytes();
        if (remaining > 0) {
            byte firstByte = in.readByte();
            ChunkHeader h = new ChunkHeader();
            // going to check highest 2 bits 0b是二进制的意思
            h.format = (byte) ((firstByte & 0xff) >> 6);
            //0x3f 十进制是63 = 3 * 16 + 15 二进制=0b111111
            int assumeCsId = (firstByte & 0x3f);
            switch (assumeCsId) {
                case 0:
                    // two byte header
                    h.size = 2;
                    if (remaining < 2) {
                        throw new ProtocolException("Bad chunk header, at least 2 bytes are expected");
                    }
                    //ff = 0b11111111 
                    //计算机内的存储都是利用二进制的补码进行存储的
                    //byte要转化为int的时候，高的24位必然会补1
                    //byte是8位二进制0xFF转化成8位二进制就是11111111其本质原因就是想保持二进制补码的一致性
                    h.csId = 64 + (in.readByte() & 0xff);
                    break;
                case 1:
                    // three byte header
                    h.size = 3;
                    if (remaining < 3) {
                        throw new ProtocolException("Bad chunk header, at least 3 bytes are expected");
                    }
                    byte b1 = in.readByte();
                    byte b2 = in.readByte();
                    h.csId = 64 + ((b2 & 0xff) << 8 | (b1 & 0xff));
                    break;
                default:
                    // single byte header
                    h.size = 1;
                    h.csId = assumeCsId;
                    break;
            }
            // check channel id is valid
            if (h.csId < 0) {
                throw new ProtocolException("Bad channel id: " + h.csId);
            }
            log.trace("CHUNK header byte {}, count {}, header {}, channel {}", String.format("%02x", firstByte), h.size, 0, h.csId);
            return h;
        } else {
            // at least one byte for valid decode
            throw new ProtocolException("Bad chunk header, at least 1 byte is expected");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ChunkHeader) {
            final ChunkHeader header = (ChunkHeader) other;
            return (header.getChannelId() == csId && header.getFormat() == format);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChunkHeader clone() {
        final ChunkHeader header = new ChunkHeader();
        header.setChannelId(csId);
        header.setSize(size);
        header.setFormat(format);
        return header;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        format = in.readByte();
        csId = in.readInt();
        size = (byte) (csId > 319 ? 3 : (csId > 63 ? 2 : 1));
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(format);
        out.writeInt(csId);
    }

    @Override
    public String toString() {
        // if its new and props are un-set, just return that message
        if ((csId + format) > 0d) {
            return "ChunkHeader [type=" + format + ", csId=" + csId + ", size=" + size + "]";
        } else {
            return "empty";
        }
    }

}
