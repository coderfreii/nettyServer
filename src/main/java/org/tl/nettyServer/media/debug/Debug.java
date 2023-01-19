package org.tl.nettyServer.media.debug;

import java.util.concurrent.atomic.AtomicInteger;

public class Debug {
    public static AtomicInteger counter = new AtomicInteger(0);

    public static boolean count(){
        System.out.println(counter.incrementAndGet());
        return false;
    }

    public static boolean reset(){
        counter.set(0);
        return false;
    }
}
