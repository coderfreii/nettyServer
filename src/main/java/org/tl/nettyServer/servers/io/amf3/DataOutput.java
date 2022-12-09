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

import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.io.amf.AMF;
import org.tl.nettyServer.servers.io.object.Serializer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

;

/**
 * Implementation of the IDataOutput interface. Can be used to store an IExternalizable object.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class DataOutput implements IDataOutput {

    /**
     * The output stream
     */
    private Output3 output;

    /**
     * Raw data of output destination
     */
    private BufFacade buffer;

    /**
     * Create a new DataOutput.
     *
     * @param output destination to write to
     */
    protected DataOutput(Output3 output) {
        this.output = output;
        buffer = output.getBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBoolean(boolean value) {
        buffer.writeByte((byte) (value ? 1 : 0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(byte value) {
        buffer.writeByte(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] bytes) {
        buffer.writeBytes(bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] bytes, int offset) {
        buffer.writeBytes(bytes, offset, bytes.length - offset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        buffer.writeBytes(bytes, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDouble(double value) {
        buffer.writeDouble(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFloat(float value) {
        buffer.writeFloat(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMultiByte(String value, String encoding) {
        final Charset cs = Charset.forName(encoding);
        final ByteBuffer strBuf = cs.encode(value);
        buffer.writeBytes(strBuf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObject(Object value) {
        Serializer.serialize(output, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(short value) {
        buffer.writeShort(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUnsignedInt(long value) {
        buffer.writeInt((int) value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUTF(String value) {
        // fix from issue #97
        try {
            byte[] strBuf = value.getBytes(AMF.CHARSET.name());
            buffer.writeShort((short) strBuf.length);
            buffer.writeBytes(strBuf);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUTFBytes(String value) {
        final ByteBuffer strBuf = AMF.CHARSET.encode(value);
        buffer.writeBytes(strBuf);
    }

}
