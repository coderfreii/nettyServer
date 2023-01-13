package org.tl.nettyServer.media.media.mpeg;

public class PacketT {
    public byte sid;
    public byte codecid;
    public int flags;
    public long pts;
    public long dts;
    public byte[] data;
    public int size;
    public int capacity;
    public int vcl; // h.264/h.265 only
}
