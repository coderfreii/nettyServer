package org.tl.nettyServer.servers.buf;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

public class ByteBufFacade implements BufFacade<ByteBuf> {
    private ByteBuf target;

    @Override
    public ByteBuf getBuf() {
        return target;
    }

    ByteBufFacade(ByteBuf src) {
        this.target = src;
    }

    public static BufFacade<ByteBuf> wrapper(Object s) {
        if (s instanceof ByteBuf) {
            return new ByteBufFacade((ByteBuf) s);
        }
        return null;
    }

    public static BufFacade<ByteBuf> wrapper(ByteBuf s) {
        return new ByteBufFacade(s);
    }

    @Override
    public BufFacade<ByteBuf> writeBytes(BufFacade<ByteBuf> src) {
        ByteBuf buf = src.getBuf();
        this.target.writeBytes(buf);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeBytes(BufFacade<ByteBuf> src, int length) {
        ByteBuf buf = src.getBuf();
        this.target.writeBytes(buf, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeBytes(BufFacade<ByteBuf> src, int srcIndex, int length) {
        ByteBuf buf = src.getBuf();
        this.target.writeBytes(buf, srcIndex, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeBytes(byte[] src) {
        this.target.writeBytes(src);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeBytes(byte[] src, int srcIndex, int length) {
        this.target.writeBytes(src, srcIndex, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeBytes(ByteBuffer src) {
        this.target.writeBytes(src);
        return this;
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        return this.target.writeBytes(in, length);
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return this.target.writeBytes(in, length);
    }

    @Override
    public int writeBytes(FileChannel in, long position, int length) throws IOException {
        return this.target.writeBytes(in, position, length);
    }

    @Override
    public BufFacade<ByteBuf> writeZero(int length) {
        this.target.writeZero(length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeByte(int value) {
        this.target.writeByte(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeShort(int value) {
        this.target.writeShort(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeShortLE(int value) {
        this.target.writeShortLE(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeMedium(int value) {
        this.target.writeMedium(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeMediumLE(int value) {
        this.target.writeMediumLE(value);
        return this;
    }

    @Override
    public void markReaderIndex() {
        this.target.markReaderIndex();
    }

    @Override
    public short readUnsignedByte() {
        return this.target.readUnsignedByte();
    }

    @Override
    public int readableBytes() {
        return this.target.readableBytes();
    }

    @Override
    public byte readByte() {
        return this.target.readByte();
    }

    @Override
    public BufFacade<ByteBuf> resetReaderIndex() {
        this.target.resetReaderIndex();
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(int length) {
        this.target.readBytes(length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(BufFacade<ByteBuf> dst) {
        ByteBuf buf = dst.getBuf();
        this.target.readBytes(buf);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(BufFacade<ByteBuf> dst, int length) {
        ByteBuf buf = dst.getBuf();
        this.target.readBytes(buf, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(BufFacade<ByteBuf> dst, int dstIndex, int length) {
        ByteBuf buf = dst.getBuf();
        this.target.readBytes(buf, dstIndex, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(byte[] src) {
        this.target.readBytes(src);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(byte[] dst, int dstIndex, int length) {
        this.target.readBytes(dst, dstIndex, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(ByteBuffer dst) {
        this.target.readBytes(dst);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> readBytes(OutputStream out, int length) throws IOException {
        this.target.readBytes(out, length);
        return this;
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return this.target.readBytes(out, length);
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return this.target.writeCharSequence(sequence, charset);
    }

    @Override
    public BufFacade<ByteBuf> ensureWritable(int minWritableBytes) {
        this.target.ensureWritable(minWritableBytes);
        return this;
    }

    @Override
    public OutputStream asOutputStream() {
        return new OutputStream() {
            @Override
            public void write(byte[] b, int off, int len) {
                ByteBufFacade.this.writeBytes(b, off, len);
            }

            @Override
            public void write(int b) {
                ByteBufFacade.this.writeByte((byte) b);
            }
        };
    }

    @Override
    public InputStream asInputStream() {
        return new InputStream() {
            @Override
            public int available() {
                return ByteBufFacade.this.readableBytes();
            }

            @Override
            public synchronized void mark(int readlimit) {
                ByteBufFacade.this.markReaderIndex();
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int read() {
                if (ByteBufFacade.this.readableBytes() > 0) {
                    return ByteBufFacade.this.readByte() & 0xff;
                }
                return -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int remaining = ByteBufFacade.this.readableBytes();
                if (remaining > 0) {
                    int readBytes = Math.min(remaining, len);
                    ByteBufFacade.this.readBytes(b, off, readBytes);
                    return readBytes;
                }

                return -1;
            }

            @Override
            public synchronized void reset() {
                ByteBufFacade.this.setIndex(0, ByteBufFacade.this.writerIndex());
            }

            @Override
            public long skip(long n) {
                int bytes;
                if (n > Integer.MAX_VALUE) {
                    bytes = ByteBufFacade.this.readableBytes();
                } else {
                    bytes = Math.min(ByteBufFacade.this.readableBytes(), (int) n);
                }
                ByteBufFacade.this.skipBytes(bytes);
                return bytes;
            }
        };
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.target.nioBuffer();
    }

    @Override
    public BufFacade<ByteBuf> duplicate() {
        ByteBuf duplicate = this.target.duplicate();
        return wrapper(duplicate);
    }

    @Override
    public BufFacade retainedDuplicate() {
        return wrapper(this.target.retainedDuplicate());
    }

    @Override
    public BufFacade retain(int increment) {
        return wrapper(this.target.retain(increment));
    }

    @Override
    public BufFacade retain() {
        return wrapper(this.target.retain());
    }

    @Override
    public BufFacade readSlice(int length) {
        return wrapper(this.target.readSlice(length));
    }

    @Override
    public BufFacade readRetainedSlice(int length) {
        return wrapper(this.target.readRetainedSlice(length));
    }


    //get -----------------------------------------------------------------

    @Override
    public byte getByte(int index) {
        return this.target.getByte(index);
    }

    @Override
    public short getUnsignedByte(int index) {
        return this.target.getUnsignedByte(index);
    }

    @Override
    public short getShort(int index) {
        return this.target.getShort(index);
    }

    @Override
    public short getShortLE(int index) {
        return this.target.getShortLE(index);
    }

    @Override
    public int getUnsignedShort(int index) {
        return this.target.getUnsignedShort(index);
    }

    @Override
    public int getUnsignedShortLE(int index) {
        return this.target.getUnsignedShortLE(index);
    }

    @Override
    public int getMedium(int index) {
        return this.target.getMedium(index);
    }

    @Override
    public int getMediumLE(int index) {
        return this.target.getMediumLE(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        return this.target.getUnsignedMedium(index);
    }

    @Override
    public int getUnsignedMediumLE(int index) {
        return this.target.getUnsignedMediumLE(index);
    }

    @Override
    public int getInt(int index) {
        return this.target.getInt(index);
    }

    @Override
    public int getIntLE(int index) {
        return this.target.getIntLE(index);
    }

    @Override
    public long getUnsignedInt(int index) {
        return this.target.getUnsignedInt(index);
    }

    @Override
    public long getUnsignedIntLE(int index) {
        return this.target.getUnsignedIntLE(index);
    }

    @Override
    public long getLong(int index) {
        return this.target.getLong(index);
    }

    @Override
    public long getLongLE(int index) {
        return this.target.getLongLE(index);
    }

    @Override
    public char getChar(int index) {
        return this.target.getChar(index);
    }

    @Override
    public float getFloat(int index) {
        return this.target.getFloat(index);
    }

    @Override
    public float getFloatLE(int index) {
        return this.target.getFloatLE(index);
    }

    @Override
    public double getDouble(int index) {
        return this.target.getDouble(index);
    }

    @Override
    public double getDoubleLE(int index) {
        return this.target.getDoubleLE(index);
    }

    @Override
    public BufFacade<ByteBuf> getBytes(int index, ByteBuf dst) {
        this.target.getBytes(index, dst);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> getBytes(int index, ByteBuf dst, int length) {
        this.target.getBytes(index, dst, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        this.target.getBytes(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> getBytes(int index, byte[] dst) {
        this.target.getBytes(index, dst);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> getBytes(int index, byte[] dst, int dstIndex, int length) {
        this.target.getBytes(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> getBytes(int index, ByteBuffer dst) {
        this.target.getBytes(index, dst);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> getBytes(int index, OutputStream out, int length) throws IOException {
        this.target.getBytes(index, out, length);
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return this.target.getBytes(index, out, length);
    }

    @Override
    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        return this.target.getBytes(index, out, length);
    }

    @Override
    public CharSequence getCharSequence(int index, int length, Charset charset) {
        return this.target.getCharSequence(index, length, charset);
    }

    @Override
    public boolean writeable() {
        return this.target.isWritable();
    }

    @Override
    public BufFacade<ByteBuf> writeBoolean(boolean value) {
        this.target.writeBoolean(value);
        return this;
    }

    @Override
    public boolean hasArray() {
        return this.target.hasArray();
    }

    @Override
    public int capacity() {
        return this.target.capacity();
    }

    @Override
    public BufFacade<ByteBuf> capacity(int newCapacity) {
        this.target.capacity(newCapacity);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> clear() {
        this.target.clear();
        return this;
    }

    @Override
    public boolean release() {
        return this.target.release();
    }

    @Override
    public BufFacade<ByteBuf> writeInt(int value) {
        this.target.writeInt(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeIntLE(int value) {
        this.target.writeIntLE(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeLong(long value) {
        this.target.writeLong(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeLongLE(long value) {
        this.target.writeLongLE(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeChar(int value) {
        this.target.writeChar(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeFloat(float value) {
        this.target.writeFloat(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeFloatLE(float value) {
        this.target.writeFloatLE(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeDouble(double value) {
        this.target.writeDouble(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeDoubleLE(double value) {
        this.target.writeDoubleLE(value);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> setIndex(int ri, int wi) {
        this.target.setIndex(ri, wi);
        return this;
    }

    @Override
    public int readerIndex() {
        return this.target.readerIndex();
    }

    @Override
    public int writerIndex() {
        return this.target.writerIndex();
    }

    @Override
    public BufFacade<ByteBuf> skipBytes(int count) {
        this.target.skipBytes(count);
        return this;
    }

    @Override
    public byte[] array() {
        return this.target.array();
    }

    @Override
    public boolean readable() {
        return this.target.isReadable();
    }

    @Override
    public boolean isReadOnly() {
        return this.target.isReadOnly();
    }

    @Override
    public BufFacade<ByteBuf> asReadOnly() {
        if (isReadOnly()) {
            return this;
        }
        return wrapper(this.target.asReadOnly());
    }

    @Override
    public CharSequence readCharSequence(int length, Charset charset) {
        return this.target.readCharSequence(length, charset);
    }

    @Override
    public boolean readBoolean() {
        return this.target.readBoolean();
    }

    @Override
    public short readShort() {
        return this.target.readShort();
    }

    @Override
    public short readShortLE() {
        return this.target.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return this.target.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return this.target.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return this.target.readMedium();
    }

    @Override
    public int readMediumLE() {
        return this.target.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return this.target.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return this.target.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return this.target.readInt();
    }

    @Override
    public int readIntLE() {
        return this.target.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return this.target.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return this.target.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return this.target.readLong();
    }

    @Override
    public long readLongLE() {
        return this.target.readLongLE();
    }

    @Override
    public char readChar() {
        return this.target.readChar();
    }

    @Override
    public float readFloat() {
        return this.target.readFloat();
    }

    @Override
    public float readFloatLE() {
        return this.target.readFloatLE();
    }

    @Override
    public double readDouble() {
        return this.target.readDouble();
    }

    @Override
    public double readDoubleLE() {
        return this.target.readDoubleLE();
    }

    @Override
    public BufFacade slice() {
        return wrapper(this.target.slice());
    }

    @Override
    public BufFacade retainedSlice() {
        return wrapper(this.target.retainedSlice());
    }

    @Override
    public BufFacade slice(int index, int length) {
        return wrapper(this.target.slice(index, length));
    }

    @Override
    public BufFacade retainedSlice(int index, int length) {
        return wrapper(this.target.retainedSlice(index, length));
    }

}
