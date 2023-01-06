package org.tl.nettyServer.media.buf;

import org.tl.nettyServer.media.stream.message.Releasable;

public class ReleaseUtil {
    public static boolean release(Object o) {
        if (o == null) return true;

        if (o instanceof Releasable) {
            return ((Releasable) o).release();
        } else {
            return true;
        }
    }


    public static void releaseAll(Object o) {
        if (o instanceof Releasable) {
            while (!(((Releasable) o).release())) {

            }
        }
    }


    public static boolean release(BufFacade o) {
        if (o == null) return true;
        if (o.refCnt() == 0) return true;
        o.release();
        return o.refCnt() == 0;
    }


    public static void releaseAll(BufFacade o) {
        while (!release(o)) {

        }
    }

    public static BufFacade duplicate(BufFacade o) {
        if (o == null) return null;
        BufFacade<Object> buffer = BufFacade.buffer(o.readableBytes());
        byte[] dst = new byte[o.readableBytes()];
        o.markReaderIndex();
        o.readBytes(dst);
        o.resetReaderIndex();
        o.release();
        return buffer.writeBytes(dst);
    }
}
