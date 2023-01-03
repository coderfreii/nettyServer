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

package org.tl.nettyServer.media.net.rtmp.event;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.buf.ReleaseUtil;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.stream.data.IStreamData;
import org.tl.nettyServer.media.stream.data.IStreamPacket;

import java.io.*;

public class AudioData extends BaseEvent implements IStreamData<AudioData>, IStreamPacket {

    private static final long serialVersionUID = -4102940670913999407L;

    protected BufFacade data;

    /**
     * Data type
     */
    private byte dataType = TYPE_AUDIO_DATA;

    /**
     * The codec id
     */
    protected int codecId = -1;

    /**
     * True if this is configuration data and false otherwise
     */
    protected boolean config;

    /**
     * Constructs a new AudioData.
     */
    public AudioData() {
        this(BufFacade.buffer(0));
    }

    public AudioData(BufFacade data) {
        super(Type.STREAM_DATA);
        setData(data);
    }

    /**
     * Create audio data event with given data buffer
     *
     * @param data Audio data
     * @param copy true to use a copy of the data or false to use reference
     */
    public AudioData(BufFacade data, boolean copy) {
        super(Type.STREAM_DATA);
        if (copy) {
            byte[] array = new byte[data.readableBytes()];
            data.markReaderIndex();
            data.readBytes(array);
            data.resetReaderIndex();
            setData(array);
        } else {
            setData(data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getDataType() {
        return dataType;
    }

    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade getData() {
        return data;
    }

    public void setData(BufFacade data) {
        if (data != null && data.capacity() > 0) {
            data.markReaderIndex();
            codecId = ((data.readByte() & 0xff) & ITag.MASK_SOUND_FORMAT) >> 4;
            if (codecId == AudioCodec.AAC.getId()) {
                config = (data.readByte() == 0);
            }
            data.resetReaderIndex();
        }
        if (this.data != null) {
            ReleaseUtil.releaseAll(this.data);
        }
        this.data = data;
    }

    public void setData(byte[] data) {
        setData(BufFacade.wrappedBuffer(data));
        //this.data = BufFacade.allocate(data.length);
        //this.data.put(data).flip();
    }

    public int getCodecId() {
        return codecId;
    }

    public boolean isConfig() {
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean releaseInternal() {
        if (data != null) {
            if (ReleaseUtil.release(data)) {
                data = null;
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            setData(byteBuf);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (data != null) {
            if (data.hasArray()) {
                out.writeObject(data.array());
            } else {
                byte[] array = new byte[data.readableBytes()];
                data.markReaderIndex();
                data.readBytes(array);
                data.resetReaderIndex();
                out.writeObject(array);
            }
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Duplicate this message / event.
     *
     * @return duplicated event
     */
    public IStreamData<AudioData> duplicate() throws IOException, ClassNotFoundException {
        AudioData result = new AudioData();
        // serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        writeExternal(oos);
        oos.close();
        // convert to byte array
        byte[] buf = baos.toByteArray();
        baos.close();
        // create input streams
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        // deserialize
        result.readExternal(ois);
        ois.close();
        bais.close();
        // clone the header if there is one
        if (header != null) {
            result.setHeader(header.clone());
        }
        result.setSourceType(sourceType);
        result.setSource(source);
        result.setTimestamp(timestamp);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("Audio - ts: %s length: %s", getTimestamp(), (data != null ? data.capacity() : '0'));
    }

}
