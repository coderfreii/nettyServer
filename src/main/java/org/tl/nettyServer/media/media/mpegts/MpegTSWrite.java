package org.tl.nettyServer.media.media.mpegts;

import org.tl.nettyServer.media.media.mpegts.entity.AVClass;
import org.tl.nettyServer.media.media.mpegts.entity.AVPacket;


public class MpegTSWrite {
    AVClass av_class;
    MpegTSSection pat; /* MPEG-2 PAT table */
    MpegTSSection sdt; /* MPEG-2 SDT table context */
    MpegTSSection nit; /* MPEG-2 NIT table context */
    MpegTSService services;
    AVPacket pkt;
    long sdt_period; /* SDT period in PCR time base */
    long pat_period; /* PAT/PMT period in PCR time base */
    long nit_period; /* NIT period in PCR time base */
    int nb_services;
    long first_pcr;
    int first_dts_checked;
    long next_pcr;
    int mux_rate; ///< set to 1 when VBR
    int pes_payload_size;
    long total_size;

    int transport_stream_id;
    int original_network_id;
    int service_id;
    int service_type;

    int pmt_start_pid;
    int start_pid;
    int m2ts_mode;
    int m2ts_video_pid;
    int m2ts_audio_pid;
    int m2ts_pgssub_pid;
    int m2ts_textsub_pid;

    int pcr_period_ms;
    byte MPEGTS_FLAG_REEMIT_PAT_PMT         = 0x01;
    byte MPEGTS_FLAG_AAC_LATM               = 0x02;
    byte MPEGTS_FLAG_PAT_PMT_AT_FRAMES      = 0x04;
    byte MPEGTS_FLAG_SYSTEM_B               = 0x08;
    byte MPEGTS_FLAG_DISCONT                = 0x10;
    byte MPEGTS_FLAG_NIT                    = 0x20;
    int flags;
    int copyts;
    int tables_version;
    long pat_period_us;
    long sdt_period_us;
    long nit_period_us;
    long last_pat_ts;
    long last_sdt_ts;
    long last_nit_ts;

    byte[] provider_name = new byte[256];

    int omit_video_pes_length;
}
