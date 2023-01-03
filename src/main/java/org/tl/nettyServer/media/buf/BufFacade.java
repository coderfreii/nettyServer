package org.tl.nettyServer.media.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;


public interface BufFacade<T> {
    Class<ByteBuf> currentByteBuf = ByteBuf.class;

    static <T> BufFacade<T> buffer(int size) {
        return (BufFacade<T>) new ByteBufFacade(PooledByteBufAllocator.DEFAULT.buffer(size));
    }

    static <T> BufFacade<T> directBuffer(int size) {
        return (BufFacade<T>) new ByteBufFacade(PooledByteBufAllocator.DEFAULT.directBuffer(size));
    }

    static <T> BufFacade<T> wrappedBuffer(byte[] src) {
        BufFacade<Object> buffer = buffer(src.length);
        return (BufFacade<T>) buffer.writeBytes(src);
    }

    static <T> BufFacade<T> wrappedBuffer(ByteBuffer src) {
        return (BufFacade<T>) new ByteBufFacade(Unpooled.wrappedBuffer(src));
    }

    static <T> BufFacade<T> wrapperAndCast(T s) {
        return (BufFacade<T>) new ByteBufFacade((ByteBuf) s);
    }

    static <T> T transfer(Object target, Class<T> clazz) {
        return (T) target;
    }

    static <T> T transfer(Object target) {
        return (T) transfer(target, currentByteBuf);
    }

    String hex();

    T getBuf();

    void markReaderIndex();

    short readUnsignedByte();

    int readableBytes();

    byte readByte();

    BufFacade<T> resetReaderIndex();

    boolean hasArray();

    int capacity();

    BufFacade<T> capacity(int newCapacity);

    BufFacade<T> clear();

    boolean release();

    int refCnt();

    BufFacade<T> setIndex(int ri, int wi);

    int readerIndex();

    int writerIndex();

    BufFacade<T> skipBytes(int count);

    byte[] array();

    void rewind();

    BufFacade<T> discardReadBytes();


    /**
     * Sets the {@code readerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code readerIndex} is
     *                                   less than {@code 0} or
     *                                   greater than {@code this.writerIndex}
     */
    BufFacade<T> readerIndex(int readerIndex);


    /**
     * Sets the {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code writerIndex} is
     *                                   less than {@code this.readerIndex} or
     *                                   greater than {@code this.capacity}
     */
    BufFacade<T> writerIndex(int writerIndex);


    //read----
    boolean readable();

    boolean isReadOnly();

    /**
     * Returns a read-only version of this buffer.
     */
    BufFacade<T> asReadOnly();


    BufFacade<T> readBytes(int length);


    BufFacade<T> readBytes(BufFacade<T> dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #readBytes(BufFacade<T>, int, int)},
     * except that this method increases the {@code writerIndex} of the
     * destination by the number of the transferred bytes (= {@code length})
     * while {@link #readBytes(BufFacade<T>, int, int)} does not.
     *
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes} or
     *                                   if {@code length} is greater than {@code dst.writableBytes}
     */
    BufFacade<T> readBytes(BufFacade<T> dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code length} is greater than {@code this.readableBytes}, or
     *                                   if {@code dstIndex + length} is greater than
     *                                   {@code dst.capacity}
     */
    BufFacade<T> readBytes(BufFacade<T> dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code dst.length}).
     *
     * @throws IndexOutOfBoundsException if {@code dst.length} is greater than {@code this.readableBytes}
     */
    BufFacade<T> readBytes(byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code length} is greater than {@code this.readableBytes}, or
     *                                   if {@code dstIndex + length} is greater than {@code dst.length}
     */
    BufFacade<T> readBytes(byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination's position
     * reaches its limit, and increases the {@code readerIndex} by the
     * number of the transferred bytes.
     *
     * @throws IndexOutOfBoundsException if {@code dst.remaining()} is greater than
     *                                   {@code this.readableBytes}
     */
    BufFacade<T> readBytes(ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException               if the specified stream threw an exception during I/O
     */
    BufFacade<T> readBytes(OutputStream out, int length) throws IOException;

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes written out to the specified channel
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    int readBytes(GatheringByteChannel out, int length) throws IOException;


    CharSequence readCharSequence(int length, Charset charset);

    boolean readBoolean();


    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    short readShort();

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    short readShortLE();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    int readUnsignedShort();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    int readUnsignedShortLE();

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    int readMedium();

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the
     * {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    int readMediumLE();

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    int readUnsignedMedium();

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    int readUnsignedMediumLE();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    int readInt();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    int readIntLE();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    long readUnsignedInt();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    long readUnsignedIntLE();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    long readLong();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    long readLongLE();

    /**
     * Gets a 2-byte UTF-16 character at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    char readChar();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    float readFloat();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    float readFloatLE();

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    double readDouble();

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    double readDoubleLE();


    /**
     * Returns a slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     */
    BufFacade slice();

    /**
     * Returns a retained slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice()}.
     * This method behaves similarly to {@code slice().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */
    BufFacade retainedSlice();

    /**
     * Returns a slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     */
    BufFacade slice(int index, int length);

    /**
     * Returns a retained slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code slice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */
    BufFacade retainedSlice(int index, int length);

    /**
     * Returns a buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * The reader and writer marks will not be duplicated. Also be aware that this method will
     * NOT call {@link #retain()} and so the reference count will NOT be increased.
     *
     * @return A buffer whose readable content is equivalent to the buffer returned by {@link #slice()}.
     * However this buffer will share the capacity of the underlying buffer, and therefore allows access to all of the
     * underlying content if necessary.
     */
    BufFacade duplicate();

    /**
     * Returns a retained buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method is identical to {@code buf.slice(0, buf.capacity())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code duplicate().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */
    BufFacade retainedDuplicate();


    BufFacade retain(int increment);


    BufFacade retain();


    /**
     * Returns a new slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     *
     * @param length the size of the new slice
     * @return the newly created slice
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    BufFacade readSlice(int length);

    /**
     * Returns a new retained slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #readSlice(int)}.
     * This method behaves similarly to {@code readSlice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     *
     * @param length the size of the new slice
     * @return the newly created slice
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    BufFacade readRetainedSlice(int length);


    //get
    byte getByte(int index);

    /**
     * Gets an unsigned byte at the specified absolute {@code index} in this
     * buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */
    short getUnsignedByte(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    short getShort(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    short getShortLE(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    int getUnsignedShort(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    int getUnsignedShortLE(int index);

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    int getMedium(int index);

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer in the Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    int getMediumLE(int index);

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    int getUnsignedMedium(int index);

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    int getUnsignedMediumLE(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    int getInt(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer with Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    int getIntLE(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    long getUnsignedInt(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    long getUnsignedIntLE(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    long getLong(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    long getLongLE(int index);

    /**
     * Gets a 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    char getChar(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    float getFloat(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    public float getFloatLE(int index);

    /**
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    double getDouble(int index);

    /**
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    public double getDoubleLE(int index);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination becomes
     * non-writable.  This method is basically same with
     * {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + dst.writableBytes} is greater than
     *                                   {@code this.capacity}
     */
    BufFacade<T> getBytes(int index, ByteBuf dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code length} is greater than {@code dst.writableBytes}
     */
    BufFacade<T> getBytes(int index, ByteBuf dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code dstIndex + length} is greater than
     *                                   {@code dst.capacity}
     */
    BufFacade<T> getBytes(int index, ByteBuf dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + dst.length} is greater than
     *                                   {@code this.capacity}
     */
    BufFacade<T> getBytes(int index, byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code dstIndex + length} is greater than
     *                                   {@code dst.length}
     */
    BufFacade<T> getBytes(int index, byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer while the destination's {@code position} will be increased.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + dst.remaining()} is greater than
     *                                   {@code this.capacity}
     */
    BufFacade<T> getBytes(int index, ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}
     * @throws IOException               if the specified stream threw an exception during I/O
     */
    BufFacade<T> getBytes(int index, OutputStream out, int length) throws IOException;

    /**
     * Transfers this buffer's data to the specified channel starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes written out to the specified channel
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    /**
     * Transfers this buffer's data starting at the specified absolute {@code index}
     * to the specified channel starting at the given file position.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.
     *
     * @param position the file position at which the transfer is to begin
     * @param length   the maximum number of bytes to transfer
     * @return the actual number of bytes written out to the specified channel
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    int getBytes(int index, FileChannel out, long position, int length) throws IOException;

    /**
     * Gets a {@link CharSequence} with the given length at the given index.
     *
     * @param length  the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    CharSequence getCharSequence(int index, int length, Charset charset);


    //write----------------

    boolean writeable();

    int writableBytes();

    /**
     * Sets the specified boolean at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * If {@code this.writableBytes} is less than {@code 1}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeBoolean(boolean value);

    /**
     * Sets the specified byte at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * The 24 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 1}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeByte(int value);

    /**
     * Sets the specified 16-bit short integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeShort(int value);

    /**
     * Sets the specified 16-bit short integer in the Little Endian Byte
     * Order at the current {@code writerIndex} and increases the
     * {@code writerIndex} by {@code 2} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeShortLE(int value);

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 3}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 3}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeMedium(int value);

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 3} in this
     * buffer.
     * If {@code this.writableBytes} is less than {@code 3}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeMediumLE(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeInt(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * in the Little Endian Byte Order and increases the {@code writerIndex}
     * by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeIntLE(int value);

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeLong(long value);

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeLongLE(long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeChar(int value);

    /**
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 4}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeFloat(float value);

    /**
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} in Little Endian Byte Order and increases
     * the {@code writerIndex} by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeFloatLE(float value);

    /**
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeDouble(double value);

    /**
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} in Little Endian Byte Order and increases
     * the {@code writerIndex} by {@code 8} in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeDoubleLE(double value);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer becomes
     * unreadable, and increases the {@code writerIndex} by the number of
     * the transferred bytes.  This method is basically same with
     * {@link #writeBytes(BufFacade<T>, int, int)}, except that this method
     * increases the {@code readerIndex} of the source buffer by the number of
     * the transferred bytes while {@link #writeBytes(BufFacade<T>, int, int)}
     * does not.
     * If {@code this.writableBytes} is less than {@code src.readableBytes},
     * {@link #ensureWritable(int)} will be called in an attempt to expand
     * capacity to accommodate.
     */
    BufFacade<T> writeBytes(BufFacade<T> src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #writeBytes(BufFacade<T>, int, int)},
     * except that this method increases the {@code readerIndex} of the source
     * buffer by the number of the transferred bytes (= {@code length}) while
     * {@link #writeBytes(BufFacade<T>, int, int)} does not.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater then {@code src.readableBytes}
     */
    BufFacade<T> writeBytes(BufFacade<T> src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is less than {@code 0}, or
     *                                   if {@code srcIndex + length} is greater than {@code src.capacity}
     */
    BufFacade<T> writeBytes(BufFacade<T> src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code src.length}).
     * If {@code this.writableBytes} is less than {@code src.length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */
    BufFacade<T> writeBytes(byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is less than {@code 0}, or
     *                                   if {@code srcIndex + length} is greater than {@code src.length}
     */
    BufFacade<T> writeBytes(byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer's position
     * reaches its limit, and increases the {@code writerIndex} by the
     * number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code src.remaining()},
     * {@link #ensureWritable(int)} will be called in an attempt to expand
     * capacity to accommodate.
     */
    BufFacade<T> writeBytes(ByteBuffer src);

    /**
     * Transfers the content of the specified stream to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified stream
     * @throws IOException if the specified stream threw an exception during I/O
     */
    int writeBytes(InputStream in, int length) throws IOException;

    /**
     * Transfers the content of the specified channel to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel
     * @throws IOException if the specified channel threw an exception during I/O
     */
    int writeBytes(ScatteringByteChannel in, int length) throws IOException;

    /**
     * Transfers the content of the specified channel starting at the given file position
     * to this buffer starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * This method does not modify the channel's position.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param position the file position at which the transfer is to begin
     * @param length   the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel
     * @throws IOException if the specified channel threw an exception during I/O
     */
    int writeBytes(FileChannel in, long position, int length) throws IOException;

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the current
     * {@code writerIndex} and increases the {@code writerIndex} by the
     * specified {@code length}.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     */
    BufFacade<T> writeZero(int length);

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes.
     * in this buffer.
     * If {@code this.writableBytes} is not large enough to write the whole sequence,
     * {@link #ensureWritable(int)} will be called in an attempt to expand capacity to accommodate.
     *
     * @param sequence to write
     * @param charset  that should be used
     * @return the written number of bytes
     */
    int writeCharSequence(CharSequence sequence, Charset charset);

    BufFacade<T> ensureWritable(int minWritableBytes);


    //set--

    /**
     * Sets the specified boolean at the specified absolute {@code index} in this
     * buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */

    BufFacade<T> setBoolean(int index, boolean value);

    /**
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */
    BufFacade<T> setByte(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  The 16 high-order bits of the specified
     * value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    BufFacade<T> setShort(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer with the Little Endian Byte Order.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    BufFacade<T> setShortLE(int index, int value);

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  Please note that the most significant
     * byte is ignored in the specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    BufFacade<T> setMedium(int index, int value);

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in the Little Endian Byte Order.
     * Please note that the most significant byte is ignored in the
     * specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    BufFacade<T> setMediumLE(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    BufFacade<T> setInt(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer with Little Endian byte order
     * .
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    BufFacade<T> setIntLE(int index, int value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    BufFacade<T> setLong(int index, long value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    BufFacade<T> setLongLE(int index, long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    BufFacade<T> setChar(int index, int value);

    /**
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    BufFacade<T> setFloat(int index, float value);

    /**
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    BufFacade<T> setFloatLE(int index, float value);

    /**
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    BufFacade<T> setDouble(int index, double value);

    /**
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    BufFacade<T> setDoubleLE(int index, double value);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer becomes
     * unreadable.  This method is basically same with
     * {@link #setBytes(int, BufFacade<T>, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, BufFacade<T>, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + src.readableBytes} is greater than
     *                                   {@code this.capacity}
     */
    BufFacade<T> setBytes(int index, BufFacade<T> src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #setBytes(int, BufFacade<T>, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, BufFacade<T>, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code length} is greater than {@code src.readableBytes}
     */
    BufFacade<T> setBytes(int index, BufFacade<T> src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code srcIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code srcIndex + length} is greater than
     *                                   {@code src.capacity}
     */
    BufFacade<T> setBytes(int index, BufFacade<T> src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + src.length} is greater than
     *                                   {@code this.capacity}
     */
    BufFacade<T> setBytes(int index, byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code srcIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code srcIndex + length} is greater than {@code src.length}
     */
    BufFacade<T> setBytes(int index, byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + src.remaining()} is greater than
     *                                   {@code this.capacity}
     */
    BufFacade<T> setBytes(int index, ByteBuffer src);

    /**
     * Transfers the content of the specified source stream to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException               if the specified stream threw an exception during I/O
     */
    int setBytes(int index, InputStream in, int length) throws IOException;

    /**
     * Transfers the content of the specified source channel to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    /**
     * Transfers the content of the specified source channel starting at the given file position
     * to this buffer starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.
     *
     * @param position the file position at which the transfer is to begin
     * @param length   the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    int setBytes(int index, FileChannel in, long position, int length) throws IOException;

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the specified
     * absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     */
    BufFacade<T> setZero(int index, int length);

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes.
     *
     * @param index    on which the sequence should be written
     * @param sequence to write
     * @param charset  that should be used.
     * @return the written number of bytes.
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is not large enough to write the whole sequence
     */
    int setCharSequence(int index, CharSequence sequence, Charset charset);

    //-----

    OutputStream asOutputStream();

    InputStream asInputStream();

    ByteBuffer nioBuffer();
}
