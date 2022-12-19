package org.tl.nettyServer.media.net.rtmp.consts;

public class SourceType {
    /**
     * Data originated from a file.
     */
    public static final byte SOURCE_TYPE_VOD = 0x0;

    /**
     * Data originated from a live encoder or stream.
     */
    public static final byte SOURCE_TYPE_LIVE = 0x01;
}
