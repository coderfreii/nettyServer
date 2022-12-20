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

import java.nio.ByteBuffer;

/**
 * The utility class provides conversion methods to ease the use of byte arrays, Mina BufFacades, and NIO ByteBuffers.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SerializeUtils {

    public static byte[] ByteBufferToByteArray(BufFacade buf) {
        byte[] byteBuf = new byte[buf.readableBytes()];
        buf.markReaderIndex();
        buf.readerIndex(0);
        buf.readBytes(byteBuf);
        buf.resetReaderIndex();
        return byteBuf;
    }

    public static byte[] NioByteBufferToByteArray(ByteBuffer buf) {
        byte[] byteBuf = new byte[buf.limit()];
        int pos = buf.position();
        buf.position(0);
        buf.get(byteBuf);
        buf.position(pos);
        return byteBuf;
    }

    public static void ByteArrayToByteBuffer(byte[] byteBuf, BufFacade buf) {
        buf.writeBytes(byteBuf);
    }

    public static void ByteArrayToNioByteBuffer(byte[] byteBuf, ByteBuffer buf) {
        buf.put(byteBuf);
        buf.flip();
    }

}