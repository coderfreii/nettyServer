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

package org.tl.nettyServer.servers.io.amf3;



import org.springframework.core.annotation.Order;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.io.amf.AMF;
import org.tl.nettyServer.servers.io.object.Deserializer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Implementation of the IDataInput interface. Can be used to load an IExternalizable object.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * 
 */
public class DataInput implements IDataInput {

    /** The input stream. */
    private Input3 input;

    /** Raw data of input source. */
    private BufFacade buffer;

    /**
     * Create a new DataInput.
     * 
     * @param input
     *            input to use
     */
    protected DataInput(Input3 input) {
        this.input = input;
        buffer = input.getBuffer();
    }

    /** {@inheritDoc} */
    @Override
    public boolean readBoolean() {
        return (buffer.readByte() != 0);
    }

    /** {@inheritDoc} */
    @Override
    public byte readByte() {
        return buffer.readByte();
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes) {
        buffer.readBytes(bytes);
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes, int offset) {
        buffer.readBytes(bytes, offset, bytes.length - offset);
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        buffer.readBytes(bytes, offset, length);
    }

    /** {@inheritDoc} */
    @Override
    public double readDouble() {
        return buffer.readDouble();
    }

    /** {@inheritDoc} */
    @Override
    public float readFloat() {
        return buffer.readFloat();
    }

    /** {@inheritDoc} */
    @Override
    public int readInt() {
        return buffer.readInt();
    }

    /** {@inheritDoc} */
    @Override
    public String readMultiByte(int length, String charSet) {
        final Charset cs = Charset.forName(charSet);
        int limit = buffer.capacity();
        final ByteBuffer strBuf = buffer.nioBuffer();
        strBuf.limit(strBuf.position() + length);
        final String string = cs.decode(strBuf).toString();
        buffer.capacity(limit); // Reset the limit
        return string;
    }

    /** {@inheritDoc} */
    @Override
    public Object readObject() {
        return Deserializer.deserialize(input, Object.class);
    }

    /** {@inheritDoc} */
    @Override
    public short readShort() {
        return buffer.readShort();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedByte() {
        return buffer.readUnsignedByte();
    }

    /** {@inheritDoc} */
    @Override
    public long readUnsignedInt() {
        return buffer.readUnsignedInt();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedShort() {
        return buffer.readShort() & 0xffff; //buffer.readUnsignedShort();
    }

    /** {@inheritDoc} */
    @Override
    public String readUTF() {
        int length = buffer.readShort() & 0xffff; //buffer.readUnsignedShort();
        return readUTFBytes(length);
    }

    /** {@inheritDoc} */
    @Override
    public String readUTFBytes(int length) {
        int limit = buffer.capacity();
        final ByteBuffer strBuf = buffer.nioBuffer();
        strBuf.limit(strBuf.position() + length);
        final String string = AMF.CHARSET.decode(strBuf).toString();
        buffer.capacity(limit); // Reset the limit
        return string;
    }

}
