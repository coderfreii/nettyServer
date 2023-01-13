package org.tl.nettyServer.media.media.mpegts;


import org.tl.nettyServer.media.event.IEvent;

public class MpegTSSection {
    int pid = 0;
    int cc = 0;
    int discontinuity = 0;
    //void (*write_packet)
    IEvent write_packet(MpegTSSection s, byte[] packet){return null;};
    byte opaque = 0;

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getCc() {
        return cc;
    }

    public void setCc(int cc) {
        this.cc = cc;
    }

    public int getDiscontinuity() {
        return discontinuity;
    }

    public void setDiscontinuity(int discontinuity) {
        this.discontinuity = discontinuity;
    }

    public byte getOpaque() {
        return opaque;
    }

    public void setOpaque(byte opaque) {
        this.opaque = opaque;
    }
}
