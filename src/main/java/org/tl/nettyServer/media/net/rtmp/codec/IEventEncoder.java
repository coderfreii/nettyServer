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

package org.tl.nettyServer.media.net.rtmp.codec;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.so.ISharedObjectMessage;

/**
 * Encodes events to byte buffer.
 */
public interface IEventEncoder {
    /**
     * Encodes Notify event to byte buffer.
     *
     * @param notify
     *            Notify event
     * @return Byte buffer
     */
    public abstract BufFacade encodeNotify(Notify notify);

    /**
     * Encodes Invoke event to byte buffer.
     *
     * @param invoke
     *            Invoke event
     * @return Byte buffer
     */
    public abstract BufFacade encodeInvoke(Invoke invoke);

    /**
     * Encodes Ping event to byte buffer.
     *
     * @param ping
     *            Ping event
     * @return Byte buffer
     */
    public abstract BufFacade encodePing(Ping ping);

    /**
     * Encodes BytesRead event to byte buffer.
     *
     * @param streamBytesRead
     *            BytesRead event
     * @return Byte buffer
     */
    public abstract BufFacade encodeBytesRead(BytesRead streamBytesRead);

    /**
     * Encodes Aggregate event to byte buffer.
     *
     * @param aggregate
     *            Aggregate event
     * @return Byte buffer
     */
    public abstract BufFacade encodeAggregate(Aggregate aggregate);

    /**
     * Encodes AudioData event to byte buffer.
     *
     * @param audioData
     *            AudioData event
     * @return Byte buffer
     */
    public abstract BufFacade encodeAudioData(AudioData audioData);

    /**
     * Encodes VideoData event to byte buffer.
     *
     * @param videoData
     *            VideoData event
     * @return Byte buffer
     */
    public abstract BufFacade encodeVideoData(VideoData videoData);

    /**
     * Encodes Unknown event to byte buffer.
     *
     * @param unknown
     *            Unknown event
     * @return Byte buffer
     */
    public abstract BufFacade encodeUnknown(Unknown unknown);

    /**
     * Encodes ChunkSize event to byte buffer.
     *
     * @param chunkSize
     *            ChunkSize event
     * @return Byte buffer
     */
    public abstract BufFacade encodeChunkSize(ChunkSize chunkSize);

    /**
     * Encodes SharedObjectMessage event to byte buffer.
     *
     * @param so
     *            ISharedObjectMessage event
     * @return Byte buffer
     */
    public abstract BufFacade encodeSharedObject(ISharedObjectMessage so);

    /**
     * Encodes SharedObjectMessage event to byte buffer using AMF3 encoding.
     *
     * @param so
     *            ISharedObjectMessage event
     * @return Byte buffer
     */
    public BufFacade encodeFlexSharedObject(ISharedObjectMessage so);
}
