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

package org.tl.nettyServer.servers.net.rtmp.codec;

import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.rtmp.event.*;
import org.tl.nettyServer.servers.so.ISharedObjectMessage;

/**
 * Event decoder decodes event objects from incoming byte buffer.
 */
public interface IEventDecoder {

    /**
     * Decodes event of Unknown type.
     *
     * @param dataType Data type
     * @param in       Byte buffer to decode
     * @return Unknown event
     */
    public abstract Unknown decodeUnknown(byte dataType, BufFacade in);

    /**
     * Decodes chunk size event.
     *
     * @param in Byte buffer to decode
     * @return ChunkSize event
     */
    public abstract ChunkSize decodeChunkSize(BufFacade in);

    /**
     * Decodes shared object message event.
     *
     * @param in Byte buffer to decode
     * @return ISharedObjectMessage event
     */
    public abstract ISharedObjectMessage decodeSharedObject(BufFacade in);

    /**
     * Decodes shared object message event from AMF3 encoding.
     *
     * @param in Byte buffer to decode
     * @return ISharedObjectMessage event
     */
    public abstract ISharedObjectMessage decodeFlexSharedObject(BufFacade in);

    /**
     * Decodes ping event.
     *
     * @param in Byte buffer to decode
     * @return Ping event
     */
    public abstract Ping decodePing(BufFacade in);

    /**
     * Decodes BytesRead event.
     *
     * @param in Byte buffer to decode
     * @return BytesRead event
     */
    public abstract BytesRead decodeBytesRead(BufFacade in);

    /**
     * Decodes the aggregated data.
     *
     * @param in Byte buffer to decode
     * @return Aggregate event
     */
    public abstract Aggregate decodeAggregate(BufFacade in);

    /**
     * Decodes audio data event.
     *
     * @param in Byte buffer to decode
     * @return AudioData event
     */
    public abstract AudioData decodeAudioData(BufFacade in);

    /**
     * Decodes video data event.
     *
     * @param in Byte buffer to decode
     * @return VideoData event
     */
    public abstract VideoData decodeVideoData(BufFacade in);

    /**
     * Decodes Flex message event.
     *
     * @param in Byte buffer to decode
     * @return FlexMessage event
     */
    public abstract FlexMessage decodeFlexMessage(BufFacade in);

}
