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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.IoConstants;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.stream.data.IStreamData;
import org.tl.nettyServer.media.stream.data.IStreamPacket;

import java.io.*;
import java.util.LinkedList;

/**
 * Aggregate data event
 */
public class Aggregate extends BaseEvent implements IoConstants, IStreamData<Aggregate>, IStreamPacket {

    private static final long serialVersionUID = 5538859593815804830L;

    private static Logger log = LoggerFactory.getLogger(Aggregate.class);

    /**
     * Data
     */
    protected BufFacade data;

    /**
     * Data type
     */
    private byte dataType = TYPE_AGGREGATE;

    /**
     * Constructs a new Aggregate.
     */
    public Aggregate() {
        this(BufFacade.buffer(0));
    }

    /**
     * Create aggregate data event with given data buffer.
     *
     * @param data data
     */
    public Aggregate(BufFacade data) {
        super(Type.STREAM_DATA);
        setData(data);
    }

    /**
     * Create aggregate data event with given data buffer.
     *
     * @param data aggregate data
     * @param copy true to use a copy of the data or false to use reference
     */
    public Aggregate(BufFacade data, boolean copy) {
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
        this.data = data;
    }

    public void setData(byte[] data) {
        this.data = BufFacade.buffer(data.length);
        this.data.readBytes(data);
    }

    /**
     * Breaks-up the aggregate into its individual parts and returns them as a list. The parts are returned based on the ordering of the aggregate itself.
     *
     * @return list of IRTMPEvent objects
     */
    public LinkedList<IRTMPEvent> getParts() {
        LinkedList<IRTMPEvent> parts = new LinkedList<IRTMPEvent>();
        do {
            try {
                // read the header
                //log.trace("Hex: {}", data.getHexDump());
                byte subType = data.readByte();
                // when we run into subtype 0 break out of here
                if (subType == 0) {
                    log.debug("Subtype 0 encountered within this aggregate, processing with exit");
                    break;
                }
                int size = data.readUnsignedMedium();
                log.debug("Data subtype: {} size: {}", subType, size);
                // TODO ensure the data contains all the bytes to support the specified size
                int timestamp = data.readUnsignedMedium();
                /*timestamp = ntohap((GETIBPOINTER(buffer) + 4)); 0x12345678 == 34 56 78 12*/
                int streamId = data.readUnsignedMedium();
                log.debug("Data timestamp: {} stream id: {}", timestamp, streamId);
                Header partHeader = new Header();
                partHeader.setCsId(header.getCsId());
                partHeader.setDataType(subType);
                partHeader.setDataSize(size);
                // use the stream id from the aggregate's header
                partHeader.setStreamId(header.getStreamId());
                partHeader.setTimer(timestamp);
                // timer delta == time stamp - timer base
                // the back pointer may be used to verify the size of the individual part
                // it will be equal to the data size + header size
                int backPointer = 0;
                switch (subType) {
                    case TYPE_AUDIO_DATA:
                        AudioData audio = new AudioData(data.readSlice(size));
                        audio.setTimestamp(timestamp);
                        audio.setHeader(partHeader);
                        log.debug("Audio header: {}", audio.getHeader());
                        parts.add(audio);
                        //log.trace("Hex: {}", data.getHexDump());
                        // ensure 4 bytes left to read an int
                        if (data.readableBytes() >= 4) {
                            backPointer = data.readInt();
                            //log.trace("Back pointer: {}", backPointer);
                            if (backPointer != (size + 11)) {
                                log.debug("Data size ({}) and back pointer ({}) did not match", size, backPointer);
                            }
                        }
                        break;
                    case TYPE_VIDEO_DATA:
                        VideoData video = new VideoData(data.readSlice(size));
                        video.setTimestamp(timestamp);
                        video.setHeader(partHeader);
                        log.debug("Video header: {}", video.getHeader());
                        parts.add(video);
                        //log.trace("Hex: {}", data.getHexDump());
                        // ensure 4 bytes left to read an int
                        if (data.readableBytes() >= 4) {
                            backPointer = data.readInt();
                            //log.trace("Back pointer: {}", backPointer);
                            if (backPointer != (size + 11)) {
                                log.debug("Data size ({}) and back pointer ({}) did not match", size, backPointer);
                            }
                        }
                        break;
                    default:
                        log.debug("Non-A/V subtype: {}", subType);
                        Unknown unk = new Unknown(subType, data.readSlice(size));
                        unk.setTimestamp(timestamp);
                        unk.setHeader(partHeader);
                        parts.add(unk);
                        // ensure 4 bytes left to read an int
                        if (data.readableBytes() >= 4) {
                            backPointer = data.readInt();
                        }
                }
            } catch (Exception e) {
                log.error("Exception decoding aggregate parts", e);
                break;
            }
            log.trace("Data position: {}", data.readerIndex());
        } while (data.readableBytes() > 0);
        log.trace("Aggregate processing complete, {} parts extracted", parts.size());
        return parts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("Aggregate - ts: %s length: %s", getTimestamp(), (data != null ? data.capacity() : '0'));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseInternal() {
        if (data != null) {
            final BufFacade localData = data;
            // null out the data first so we don't accidentally
            // return a valid reference first
            data = null;
            localData.clear();
            localData.release();
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            data = BufFacade.buffer(byteBuf.length);
            SerializeUtils.ByteArrayToByteBuffer(byteBuf, data);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (data != null) {
            out.writeObject(SerializeUtils.ByteBufferToByteArray(data));
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Duplicate this message / event.
     *
     * @return duplicated event
     */
    public Aggregate duplicate() throws IOException, ClassNotFoundException {
        Aggregate result = new Aggregate();
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

}
