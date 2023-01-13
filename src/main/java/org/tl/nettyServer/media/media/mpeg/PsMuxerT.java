package org.tl.nettyServer.media.media.mpeg;

public class PsMuxerT {
    public PsmT psm;
    public PsPackHeaderT pack;
    public PsSystemHeaderT system;
    public int h264_h265_with_aud;
    public int psm_period;
    public int scr_period;

    public PsMuxerFuncT func;
     //void* param;
    public MpegMuxerT param;
}
