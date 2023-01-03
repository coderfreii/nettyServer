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
import org.tl.nettyServer.media.io.IoConstants;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.stream.data.IStreamData;
import org.tl.nettyServer.media.stream.data.IStreamPacket;

import java.io.*;

/**
 * Video data event
 */
public class VideoData extends BaseEvent implements IoConstants, IStreamData<VideoData>, IStreamPacket {

    private static final long serialVersionUID = 5538859593815804830L;

    /**
     * Videoframe type
     */
    public static enum FrameType {
        UNKNOWN, KEYFRAME, INTERFRAME, DISPOSABLE_INTERFRAME, END_OF_SEQUENCE
    }

    /**
     * Video data
     */
    protected BufFacade data;

    /**
     * Data type
     */
    private byte dataType = TYPE_VIDEO_DATA;

    /**
     * Frame type, unknown by default
     */
    protected FrameType frameType = FrameType.UNKNOWN;

    /**
     * The codec id
     */
    protected int codecId = -1;

    /**
     * True if this is configuration data and false otherwise
     */
    protected boolean config;

    /**
     * True if this indicates an end-of-sequence and false otherwise
     */
    protected boolean endOfSequence;

    /**
     * Constructs a new VideoData.
     */
    public VideoData() {
        this(BufFacade.buffer(0));
    }

    /**
     * Create video data event with given data buffer
     *
     * @param data Video data
     */
    public VideoData(BufFacade data) {
        super(Type.STREAM_DATA);
        setData(data);
    }

    /**
     * Create video data event with given data buffer
     *
     * @param data Video data
     * @param copy true to use a copy of the data or false to use reference
     */
    public VideoData(BufFacade data, boolean copy) {
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
        if (this.data != null) {
            ReleaseUtil.releaseAll(this.data);
        }

        this.data = data;
        if (data != null && data.readableBytes() > 0) {
            data.markReaderIndex();
            int firstByte = data.readByte() & 0xff;
            codecId = firstByte & ITag.MASK_VIDEO_CODEC;
            if (codecId == VideoCodec.AVC.getId()) {
                int secondByte = data.readByte() & 0xff;
                config = (secondByte == 0);
                endOfSequence = (secondByte == 2);
            }
            data.resetReaderIndex();
            int frameType = (firstByte & MASK_VIDEO_FRAMETYPE) >> 4;
            if (frameType == FLAG_FRAMETYPE_KEYFRAME) {
                this.frameType = FrameType.KEYFRAME;
            } else if (frameType == FLAG_FRAMETYPE_INTERFRAME) {
                this.frameType = FrameType.INTERFRAME;
            } else if (frameType == FLAG_FRAMETYPE_DISPOSABLE) {
                this.frameType = FrameType.DISPOSABLE_INTERFRAME;
            } else {
                this.frameType = FrameType.UNKNOWN;
            }
        }
    }

    public void setData(byte[] data) {
        setData(BufFacade.wrappedBuffer(data));
        //this.data = BufFacade.allocate(data.length);
        //this.data.put(data).flip();
    }

    /**
     * Getter for frame type
     *
     * @return Type of video frame
     */
    public FrameType getFrameType() {
        return frameType;
    }

    public int getCodecId() {
        return codecId;
    }

    public boolean isConfig() {
        return config;
    }

    public boolean isEndOfSequence() {
        return endOfSequence;
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
        frameType = (FrameType) in.readObject();
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            setData(byteBuf);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(frameType);
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
    public VideoData duplicate() throws IOException, ClassNotFoundException {
        VideoData result = new VideoData();
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
        return String.format("Video - ts: %s length: %s", getTimestamp(), (data != null ? data.capacity() : '0'));
    }

}
