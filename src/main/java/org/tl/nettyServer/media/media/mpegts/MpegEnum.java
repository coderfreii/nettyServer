package org.tl.nettyServer.media.media.mpegts;

public class MpegEnum {
    // service_type values as defined in ETSI 300 468
    public static byte MPEGTS_SERVICE_TYPE_DIGITAL_TV                   = 0x01;
    public static byte MPEGTS_SERVICE_TYPE_DIGITAL_RADIO                = 0x02;
    public static byte MPEGTS_SERVICE_TYPE_TELETEXT                     = 0x03;
    public static byte MPEGTS_SERVICE_TYPE_ADVANCED_CODEC_DIGITAL_RADIO = 0x0A;
    public static byte MPEGTS_SERVICE_TYPE_MPEG2_DIGITAL_HDTV           = 0x11;
    public static byte MPEGTS_SERVICE_TYPE_ADVANCED_CODEC_DIGITAL_SDTV  = 0x16;
    public static byte MPEGTS_SERVICE_TYPE_ADVANCED_CODEC_DIGITAL_HDTV  = 0x19;
    public static byte MPEGTS_SERVICE_TYPE_HEVC_DIGITAL_HDTV            = 0x1F;
}
