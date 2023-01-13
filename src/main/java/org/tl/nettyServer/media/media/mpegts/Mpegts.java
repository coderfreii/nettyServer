package org.tl.nettyServer.media.media.mpegts;


import org.tl.nettyServer.media.media.mpegts.entity.AVFormatContext;
import org.tl.nettyServer.media.media.mpegts.entity.AVPacket;
import org.tl.nettyServer.media.media.mpegts.entity.AVStream;

public interface Mpegts {
    int TS_FEC_PACKET_SIZE  = 204;
    int TS_DVHS_PACKET_SIZE = 192;
    public static int TS_PACKET_SIZE      = 188;
    int TS_MAX_PACKET_SIZE  = 204;

    int NB_PID_MAX          = 8192;
    int USUAL_SECTION_SIZE  = 1024; /* except EIT which is limited to 4096 */
    int MAX_SECTION_SIZE    = 4096;

    /* pids */
    byte PAT_PID = 0x0000; /* Program Association Table */
    byte CAT_PID = 0x0001; /* Conditional Access Table */
    byte TSDT_PID = 0x0002; /* Transport Stream Description Table */
    byte IPMP_PID = 0x0003;
    /* PID from 0x0004 to 0x000F are reserved */
    byte NIT_PID = 0x0010; /* Network Information Table */
    byte SDT_PID = 0x0011; /* Service Description Table */
    byte BAT_PID = 0x0011; /* Bouquet Association Table */
    byte EIT_PID = 0x0012; /* Event Information Table */
    byte RST_PID = 0x0013; /* Running Status Table */
    byte TDT_PID = 0x0014; /* Time and Date Table */
    byte TOT_PID = 0x0014;
    byte NET_SYNC_PID = 0x0015;
    byte RNT_PID = 0x0016; /* RAR Notification Table */
    /* PID from 0x0017 to 0x001B are reserved for future use */
    /* PID value 0x001C allocated to link-local inband signalling shall not be
     * used on any broadcast signals. It shall only be used between devices in a
     * controlled environment. */
    byte LINK_LOCAL_PID = 0x001C;
    byte MEASUREMENT_PID = 0x001D;
    byte DIT_PID = 0x001E; /* Discontinuity Information Table */
    byte SIT_PID = 0x001F; /* Selection Information Table */
    /* PID from 0x0020 to 0x1FFA may be assigned as needed to PMT, elementary
     * streams and other data tables */
    byte FIRST_OTHER_PID = 0x0020;
    char LAST_OTHER_PID =  0x1FFA;
    /* PID 0x1FFB is used by DigiCipher 2/ATSC MGT metadata */
    /* PID from 0x1FFC to 0x1FFE may be assigned as needed to PMT, elementary
     * streams and other data tables */
    char NULL_PID = 0x1FFF; /* Null packet (used for fixed bandwidth padding) */

    /* m2ts pids */
    char M2TS_PMT_PID = 0x0100;
    char M2TS_PCR_PID = 0x1001;
    char M2TS_VIDEO_PID = 0x1011;
    char M2TS_AUDIO_START_PID = 0x1100;
    char M2TS_PGSSUB_START_PID = 0x1200;
    char M2TS_TEXTSUB_PID = 0x1800;
    char M2TS_SECONDARY_AUDIO_START_PID = 0x1A00;
    char M2TS_SECONDARY_VIDEO_START_PID = 0x1B00;

    /* table ids */
    byte PAT_TID = 0x00; /* Program Association section */
    byte CAT_TID = 0x01; /* Conditional Access section */
    byte PMT_TID = 0x02; /* Program Map section */
    byte TSDT_TID = 0x03; /* Transport Stream Description section */
    /* TID from 0x04 to 0x3F are reserved */
    byte M4OD_TID = 0x05;
    byte NIT_TID = 0x40; /* Network Information section - actual network */
    byte ONIT_TID = 0x41; /* Network Information section - other network */
    byte SDT_TID = 0x42; /* Service Description section - actual TS */
    /* TID from 0x43 to 0x45 are reserved for future use */
    byte OSDT_TID = 0x46; /* Service Descrition section - other TS */
    /* TID from 0x47 to 0x49 are reserved for future use */
    byte BAT_TID = 0x4A; /* Bouquet Association section */
    byte UNT_TID = 0x4B; /* Update Notification Table section */
    byte DFI_TID = 0x4C; /* Downloadable Font Info section */
    /* TID 0x4D is reserved for future use */
    byte EIT_TID = 0x4E; /* Event Information section - actual TS */
    byte OEIT_TID = 0x4F; /* Event Information section - other TS */
    byte EITS_START_TID = 0x50; /* Event Information section schedule - actual TS */
    byte EITS_END_TID = 0x5F; /* Event Information section schedule - actual TS */
    byte OEITS_START_TID = 0x60; /* Event Information section schedule - other TS */
    byte OEITS_END_TID = 0x6F; /* Event Information section schedule - other TS */
    byte TDT_TID = 0x70; /* Time Date section */
    byte RST_TID = 0x71; /* Running Status section */
    byte ST_TID = 0x72; /* Stuffing section */
    byte TOT_TID = 0x73; /* Time Offset section */
    byte AIT_TID = 0x74; /* Application Inforamtion section */
    byte CT_TID = 0x75; /* Container section */
    byte RCT_TID = 0x76; /* Related Content section */
    byte CIT_TID = 0x77; /* Content Identifier section */
    byte MPE_FEC_TID = 0x78; /* MPE-FEC section */
    byte RPNT_TID = 0x79; /* Resolution Provider Notification section */
    byte MPE_IFEC_TID = 0x7A; /* MPE-IFEC section */
    byte PROTMT_TID = 0x7B; /* Protection Message section */
    /* TID from 0x7C to 0x7D are reserved for future use */
    byte DIT_TID = 0x7E; /* Discontinuity Information section */
    byte SIT_TID = 0x7F; /* Selection Information section */
            /* TID from 0x80 to 0xFE are user defined */
            /* TID 0xFF is reserved */

    byte STREAM_TYPE_VIDEO_MPEG1 = 0x01;
    byte STREAM_TYPE_VIDEO_MPEG2 = 0x02;
    byte STREAM_TYPE_AUDIO_MPEG1 = 0x03;
    byte STREAM_TYPE_AUDIO_MPEG2 = 0x04;
    byte STREAM_TYPE_PRIVATE_SECTION = 0x05;
    byte STREAM_TYPE_PRIVATE_DATA = 0x06;
    byte STREAM_TYPE_AUDIO_AAC = 0x0f;
    byte STREAM_TYPE_AUDIO_AAC_LATM = 0x11;
    byte STREAM_TYPE_VIDEO_MPEG4 = 0x10;
    byte STREAM_TYPE_METADATA = 0x15;
    byte STREAM_TYPE_VIDEO_H264 = 0x1b;
    byte STREAM_TYPE_VIDEO_HEVC = 0x24;
    byte STREAM_TYPE_VIDEO_CAVS = 0x42;
    char STREAM_TYPE_VIDEO_AVS2 = 0xd2;
    char STREAM_TYPE_VIDEO_AVS3 = 0xd4;
    char STREAM_TYPE_VIDEO_VC1 = 0xea;
    char STREAM_TYPE_VIDEO_DIRAC = 0xd1;


    char STREAM_TYPE_AUDIO_AC3 = 0x81;
    char STREAM_TYPE_AUDIO_DTS = 0x82;
    char STREAM_TYPE_AUDIO_TRUEHD = 0x83;
    char STREAM_TYPE_AUDIO_EAC3 = 0x87;

    /* ISO/IEC 13818-1 Table 2-22 */
    char STREAM_ID_PROGRAM_STREAM_MAP = 0xbc;
    char STREAM_ID_PRIVATE_STREAM_1 = 0xbd;
    char STREAM_ID_PADDING_STREAM = 0xbe;
    char STREAM_ID_PRIVATE_STREAM_2 = 0xbf;
    char STREAM_ID_AUDIO_STREAM_0 = 0xc0;
    char STREAM_ID_VIDEO_STREAM_0 = 0xe0;
    char STREAM_ID_ECM_STREAM = 0xf0;
    char STREAM_ID_EMM_STREAM = 0xf1;
    char STREAM_ID_DSMCC_STREAM = 0xf2;
    char STREAM_ID_TYPE_E_STREAM = 0xf8;
    char STREAM_ID_METADATA_STREAM = 0xfc;
    char STREAM_ID_EXTENDED_STREAM_ID = 0xfd;
    char STREAM_ID_PROGRAM_STREAM_DIRECTORY = 0xff;

    /* ISO/IEC 13818-1 Table 2-45 */
    byte VIDEO_STREAM_DESCRIPTOR = 0x02;
    byte REGISTRATION_DESCRIPTOR = 0x05;
    byte ISO_639_LANGUAGE_DESCRIPTOR = 0x0a;
    byte IOD_DESCRIPTOR = 0x1d;
    byte SL_DESCRIPTOR = 0x1e;
    byte FMC_DESCRIPTOR = 0x1f;
    byte METADATA_DESCRIPTOR = 0x26;
    byte METADATA_STD_DESCRIPTOR = 0x27;

    MpegTSContext MpegTSContext = null;
    MpegTSContext avpriv_mpegts_parse_open(AVFormatContext s);
    int avpriv_mpegts_parse_packet(MpegTSContext ts, AVPacket pkt,
                               byte[] buf, int len);

    void avpriv_mpegts_parse_close(MpegTSContext ts);

    SLConfigDescr SLConfigDescr = null;
    Mp4Descr Mp4Descr = null;
    DVBAC3Descriptor DVBAC3Descriptor = null;

    /**
     * Parse an MPEG-2 descriptor
     * @param[in] fc                    Format context (used for logging only)
     * @param st                        Stream
     * @param stream_type               STREAM_TYPE_xxx
     * @param pp                        Descriptor buffer pointer
     * @param desc_list_end             End of buffer
     * @return <0 to stop processing
     */
    int ff_parse_mpeg2_descriptor(AVFormatContext fc, AVStream st, int stream_type,
                                  byte[] pp, byte[] desc_list_end,
                                  Mp4Descr mp4_descr, int mp4_descr_count, int pid,
                                  MpegTSContext ts);
    /**
     * Check presence of H264 startcode
     * @return <0 to stop processing
     */
    int ff_check_h264_startcode(AVFormatContext s,AVStream st,AVPacket pkt);
}
