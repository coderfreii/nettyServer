package org.tl.nettyServer.media.media.mpegts;


import org.tl.nettyServer.media.media.mpegts.entity.AVFormatContext;

public class MpegTSWriteStream {
    int pid; /* stream associated pid */
    int cc;
    int discontinuity;
    int payload_size;
    int first_timestamp_checked; ///< first pts/dts check needed
    int prev_payload_key;
    long payload_pts;
    long payload_dts;
    int payload_flags;
    byte payload;
    AVFormatContext amux;
    int data_st_warning;

    long pcr_period; /* PCR period in PCR time base */
    long last_pcr;

    /* For Opus */
    int opus_queued_samples;
    int opus_pending_trim_start;

    DVBAC3Descriptor dvb_ac3_desc;
}
