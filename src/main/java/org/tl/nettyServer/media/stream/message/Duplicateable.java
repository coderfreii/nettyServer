package org.tl.nettyServer.media.stream.message;

import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.stream.data.IStreamData;

import java.io.IOException;

public interface Duplicateable<T> {
    T duplicate() throws IOException, ClassNotFoundException;


    static IRTMPEvent doDuplicate(IRTMPEvent rtmpEvent) {
        if (rtmpEvent instanceof IStreamData) {
            try {
                rtmpEvent = (IRTMPEvent) ((IStreamData) rtmpEvent).duplicate(false);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return rtmpEvent;
    }


    static RTMPMessage doDuplicate(RTMPMessage rtmpMessage) {
        IRTMPEvent body = rtmpMessage.getBody();
        IRTMPEvent duplicate = doDuplicate(body);
        return RTMPMessage.build(duplicate);
    }
}
