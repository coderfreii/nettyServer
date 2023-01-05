package org.tl.nettyServer.media.net.rtsp.message;

import org.tl.nettyServer.media.buf.BufFacade;

/**
 * RTSP Channe lData
 *
 * @author pengliren
 */
public class RTSPChannelData {

    private byte channel;

    private BufFacade data;

    public RTSPChannelData(byte channel, BufFacade data) {

        this.channel = channel;
        this.data = data;
    }

    public byte getChannel() {
        return channel;
    }

    public BufFacade getData() {
        return data;
    }

    @Override
    public String toString() {

        return "data : " + data.readableBytes() + " channel : " + channel;
    }
}
