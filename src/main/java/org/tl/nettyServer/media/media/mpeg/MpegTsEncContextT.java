package org.tl.nettyServer.media.media.mpeg;

public class MpegTsEncContextT {
    public PatT pat;
    public int h264_h265_with_aud;

    public long sdt_period;
    public long pat_period;
    public long pcr_period;
    public long pcr_clock; // last pcr time

    public int pat_cycle;
    public char pid;

    public MpegTsFuncT func;
    public MpegMuxerT param;

    public byte[] payload = new byte[1024]; // maximum PAT/PMT payload length
}
