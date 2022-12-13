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

package org.tl.nettyServer.media.util;


import org.slf4j.Logger;
import org.tl.nettyServer.media.buf.BufFacade;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Miscellaneous I/O utility methods
 *
 * @see <a href="http://www.cs.utsa.edu/~wagner/laws/Abytes.html">Unsigned bytes in Java</a>
 */
public class IOUtils {

    /**
     * UTF-8 is used
     */
    public final static Charset CHARSET = Charset.forName("UTF-8");

    /**
     * Writes integer in reverse order
     *
     * @param out
     *            Data buffer to fill
     * @param value
     *            Integer
     */
    public final static void writeReverseInt(BufFacade out, int value) {
        out.writeInt((int) ((value & 0xFF) << 24 | ((value >> 8) & 0x00FF) << 16 | ((value >>> 16) & 0x000000FF) << 8 | ((value >>> 24) & 0x000000FF)));
    }

    /**
     * Writes medium integer
     *
     * @param out
     *            Output buffer
     * @param value
     *            Integer to write
     */
    public final static void writeMediumInt(BufFacade out, int value) {
        byte[] bytes = new byte[3];
        bytes[0] = (byte) ((value >>> 16) & 0xff);
        bytes[1] = (byte) ((value >>> 8) & 0xff);
        bytes[2] = (byte) (value & 0xff);
        out.writeBytes(bytes);
    }

    /**
     * Writes medium integer
     *
     * @param out
     *            Output buffer
     * @param value
     *            Integer to write
     */
    public final static void writeMediumInt(ByteBuffer out, int value) {
        out.put((byte) ((value >>> 16) & 0xff));
        out.put((byte) ((value >>> 8) & 0xff));
        out.put((byte) (value & 0xff));
    }

    /**
     * Writes extended medium integer (equivalent to a regular integer whose most significant byte has been moved to its end, past its least significant byte)
     *
     * @param out
     *            Output buffer
     * @param value
     *            Integer to write
     */
    public final static void writeExtendedMediumInt(BufFacade out, int value) {
        value = ((value & 0xff000000) >> 24) | (value << 8);
        out.writeInt(value);
    }

    /**
     * Writes extended medium integer (equivalent to a regular integer whose most significant byte has been moved to its end, past its least significant byte)
     *
     * @param out
     *            Output buffer
     * @param value
     *            Integer to write
     */
    public final static void writeExtendedMediumInt(ByteBuffer out, int value) {
        value = ((value & 0xff000000) >> 24) | (value << 8);
        out.putInt(value);
    }

    /**
     * Writes an unsigned byte value to the supplied buffer.
     *
     * @param out
     *            Output buffer
     * @param value
     *            Byte to write
     */
    public final static void writeUnsignedByte(ByteBuffer out, byte value) {
        out.put((byte) (value & 0xff));
    }

    /**
     * Reads unsigned medium integer (3 bytes)
     *
     * @param in
     *            Unsigned medium int source
     * @return int value
     */
    public final static int readUnsignedMediumInt(ByteBuffer in) {
        return ((in.get() & 0xff) << 16) + ((in.get() & 0xff) << 8) + (in.get() & 0xff);
    }

    /**
     * Reads unsigned medium integer (3 bytes)
     *
     * @param in
     *            Unsigned medium int source
     * @return int value
     */
    public final static int readUnsignedMediumInt(BufFacade in) {
        return ((in.readByte() & 0xff) << 16) + ((in.readByte() & 0xff) << 8) + (in.readByte() & 0xff);
    }

    /**
     * Reads medium int
     *
     * @param in
     *            Source
     * @return int value
     */
    public final static int readMediumInt(ByteBuffer in) {
        return ((in.get() & 0x000000ff) << 16) + ((in.get() & 0x000000ff) << 8) + (in.get() & 0x000000ff);
    }

    /**
     * Reads medium int
     *
     * @param in
     *            Source
     * @return int value
     */
    public final static int readMediumInt(BufFacade in) {
        return ((in.readByte() & 0x000000ff) << 16) + ((in.readByte() & 0x000000ff) << 8) + (in.readByte() & 0x000000ff);
    }

    /**
     * Reads extended medium int
     *
     * @param in
     *            Source
     * @return int value
     */
    public final static int readExtendedMediumInt(BufFacade in) {
        int result = in.readInt();
        result = (result >>> 8) | ((result & 0x000000ff) << 24);
        return result;
    }

    /**
     * Reads extended medium int
     *
     * @param in
     *            Source
     * @return int value
     */
    public final static int readExtendedMediumInt(ByteBuffer in) {
        int result = in.getInt();
        result = (result >>> 8) | ((result & 0x000000ff) << 24);
        return result;
    }

    /**
     * Reads reverse int
     *
     * @param in
     *            Source
     * @return int
     */
    public final static int readReverseInt(BufFacade in) {
        int value = in.readInt();
        value = ((value & 0xFF) << 24 | ((value >> 8) & 0x00FF) << 16 | ((value >>> 16) & 0x000000FF) << 8 | ((value >>> 24) & 0x000000FF));
        return value;
    }

    /**
     * Format debug message
     *
     * @param log
     *            Logger
     * @param msg
     *            Message
     * @param buf
     *            Byte buffer to debug
     */
    public final static void debug(Logger log, String msg, BufFacade buf) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
            log.debug("Size: {}", buf.readableBytes());
//            log.debug("Data:\n{}", HexDump.formatHexDump(buf.getHexDump()));
            log.debug("\n{}\n", toString(buf));
        }
    }

    /**
     * String representation of byte buffer
     *
     * @param buf Byte buffer
     * @return String representation
     */
    public final static String toString(BufFacade buf) {
        //TODO
        BufFacade duplicate = buf.duplicate();
        final ByteBuffer strBuf = duplicate.nioBuffer();
        final String string = CHARSET.decode(strBuf).toString();
        return string;
    }

    /**
     * Returns a byte array for the given hex encoded string.
     *
     * @param s encoded hex string
     * @return byte array
     */
    public final static byte[] hexStringToByteArray(String s) {
        // remove all the whitespace first
        s = s.replaceAll("\\s+","");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static void main(String[] args) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        IOUtils.writeExtendedMediumInt(buf, 1234567890);
        buf.flip();
        System.out.println("Result: " + IOUtils.readExtendedMediumInt(buf));
    }

}
