package org.tl.nettyServer.media.media.mpegts.util;

public class Bswap {

    public static int av_bswap32(int x)
    {
        return AV_BSWAP32C(x);
    }
    public static int  AV_BSWAP32C(int x){
        return (AV_BSWAP16C(x) << 16 | AV_BSWAP16C((x) >> 16));
    }
    public static int  AV_BSWAP16C(int x){
        return (((x) << 8 & 0xff00)  | ((x) >> 8 & 0x00ff));
    }
}
