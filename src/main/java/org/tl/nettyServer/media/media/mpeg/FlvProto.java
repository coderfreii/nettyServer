package org.tl.nettyServer.media.media.mpeg;

public interface FlvProto {
    // FLV Tag Type
    public static int FLV_TYPE_AUDIO = 8;
    public static int FLV_TYPE_VIDEO = 9;
    public static int FLV_TYPE_SCRIPT = 18;

    // FLV Audio Type
    public static int FLV_AUDIO_ADPCM = (1 << 4);
    public static int FLV_AUDIO_MP3 = (2 << 4);
    public static int FLV_AUDIO_G711A = (7 << 4); // G711 A-law
    public static int FLV_AUDIO_G711U = (8 << 4); // G711 mu-law
    public static int FLV_AUDIO_AAC = (10 << 4);
    public static int FLV_AUDIO_OPUS = (13 << 4);
    public static int FLV_AUDIO_MP3_8K = (14 << 4);
    public static int FLV_AUDIO_ASC = 0x100; // AudioSpecificConfig(ISO-14496-3)
    public static int FLV_AUDIO_OPUS_HEAD = 0x101; // opus-codec.org

    // FLV Video Type
    public static int FLV_VIDEO_H263 = 2;
    public static int FLV_VIDEO_VP6 = 4;
    public static int FLV_VIDEO_H264 = 7;
    public static int FLV_VIDEO_H265 = 12; // https://github.com/CDN-Union/H265
    public static int FLV_VIDEO_AV1 = 13; // https://aomediacodec.github.io/av1-isobmff
    public static int FLV_VIDEO_AVCC = 0x200; // AVCDecoderConfigurationRecord(ISO-14496-15)
    public static int FLV_VIDEO_HVCC = 0x201; // HEVCDecoderConfigurationRecord(ISO-14496-15)
    public static int FLV_VIDEO_AV1C = 0x202; // AV1CodecConfigurationRecord(av1-isobmff)

    public static int FLV_SCRIPT_METADA = 0x300; // onMetaData






    public static int  STREAM_VIDEO_MPEG4	= 0x10;
    public static int  STREAM_VIDEO_H264	= 0x1b;
    public static int  STREAM_VIDEO_H265   = 0x24;
    public static int  STREAM_VIDEO_SVAC	= 0x80;
    public static int  STREAM_AUDIO_MP3	= 0x04;
    public static int  STREAM_AUDIO_AAC	= 0x0f;
    public static int  STREAM_AUDIO_EAC3	= 0x87;
    public static int  STREAM_AUDIO_G711A	= 0x90;
    public static int  STREAM_AUDIO_G711U	= 0x91;
    public static int  STREAM_AUDIO_G722	= 0x92;
    public static int  STREAM_AUDIO_G723	= 0x93;
    public static int  STREAM_AUDIO_G729	= 0x99;
    public static int  STREAM_AUDIO_SVAC	= 0x9B;
    public static int  STREAM_AUDIO_OPUS   = 0x9C;
}
