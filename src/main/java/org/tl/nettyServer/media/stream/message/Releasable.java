package org.tl.nettyServer.media.stream.message;

public interface Releasable {
    boolean release();

    void clear();
}
