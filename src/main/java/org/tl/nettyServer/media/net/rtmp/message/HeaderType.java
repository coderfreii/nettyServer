package org.tl.nettyServer.media.net.rtmp.message;

public enum HeaderType {
    HEADER_NEW, HEADER_SAME_SOURCE, HEADER_TIMER_CHANGE, HEADER_CONTINUE;

    public static class HeaderTypeValues {
        public static final HeaderType[] values = HeaderType.HeaderTypeValues.values;
    }
}
