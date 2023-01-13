package org.tl.nettyServer.media.media.mpegts;

public interface MpegTSService {
    MpegTSSection pmt = null; /* MPEG-2 PMT table context */
    int sid = 0;           /* service ID */
    byte[] name = new byte[256];
    byte[] provider_name = new byte[256];
    int pcr_pid = 0;
    //AVProgram *program;
}
