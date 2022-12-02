package org.tl.nettyServer.servers.buf;

import io.netty.buffer.ByteBuf;

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
    public BufFacade<ByteBuf> writeBytes(byte[] src) {
        this.target.writeBytes(src);
        return this;
    }

    @Override
    public BufFacade<ByteBuf> writeByte(int value) {
        this.target.writeByte(value);
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
    public BufFacade<ByteBuf> readBytes(byte[] src) {
        this.target.readBytes(src);
        return this;
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return this.target.writeCharSequence(sequence, charset);
    }

    @Override
    public BufFacade<ByteBuf> duplicate() {
        ByteBuf duplicate = this.target.duplicate();
        return wrapper(duplicate);
    }
}
