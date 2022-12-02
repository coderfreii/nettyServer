package org.tl.nettyServer.servers.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;


public interface BufFacade<T> {
    static <T> BufFacade<T> buffer(int size) {
        return (BufFacade<T>) new ByteBufFacade(Unpooled.buffer(size));
    }

    static <T> BufFacade<T> wrappedBuffer(byte[] src) {
        return (BufFacade<T>) new ByteBufFacade(Unpooled.wrappedBuffer(src));
    }

    static <T> BufFacade<T> wrapper(T s) {
        return (BufFacade<T>) new ByteBufFacade((ByteBuf) s);
    }

    T getBuf();

    BufFacade<T> writeBytes(BufFacade<T> to);

    BufFacade<T> writeBytes(byte[] src);

    BufFacade<T> writeByte(int value);

    void markReaderIndex();

    short readUnsignedByte();

    int readableBytes();

    byte readByte();

    BufFacade<T> resetReaderIndex();

    BufFacade<T> readBytes(int length);

    BufFacade<T> readBytes(byte[] src);

    int writeCharSequence(CharSequence sequence, Charset charset);

    BufFacade<T> duplicate();
}
