package org.tl.nettyServer.media.media.mpeg;

public interface MpegPesProto {
    public static int PES_SID_SUB			= 0x20; // ffmpeg/libavformat/mpeg.h
    public static int PES_SID_AC3			= 0x80; // ffmpeg/libavformat/mpeg.h
    public static int PES_SID_DTS			= 0x88; // ffmpeg/libavformat/mpeg.h
    public static int PES_SID_LPCM		= 0xA0; // ffmpeg/libavformat/mpeg.h
    public static int PES_SID_EXTENSION	= 0xB7; // PS system_header extension(p81)
    public static int PES_SID_END			= 0xB9; // MPEG_program_end_code
    public static int PES_SID_START		= 0xBA; // Pack start code
    public static int PES_SID_SYS			= 0xBB; // System header start code
    public static int PES_SID_PSM			= 0xBC; // program_stream_map
    public static int PES_SID_PRIVATE_1	= 0xBD; // private_stream_1
    public static int PES_SID_PADDING		= 0xBE; // padding_stream
    public static int PES_SID_PRIVATE_2	= 0xBF; // private_stream_2
    public static int PES_SID_AUDIO		= 0xC0; // ISO/IEC 13818-3/11172-3/13818-7/14496-3 audio stream '110x xxxx'
    public static int PES_SID_VIDEO		= 0xE0; // H.262 | H.264 | H.265 | ISO/IEC 13818-2/11172-2/14496-2/14496-10 video stream '1110 xxxx'
    public static int PES_SID_ECM			= 0xF0; // ECM_stream
    public static int PES_SID_EMM			= 0xF1; // EMM_stream
    public static int PES_SID_DSMCC		= 0xF2; // H.222.0 | ISO/IEC 13818-1/13818-6_DSMCC_stream
    public static int PES_SID_13522		= 0xF3; // ISO/IEC_13522_stream
    public static int PES_SID_H222_A		= 0xF4; // Rec. ITU-T H.222.1 type A
    public static int PES_SID_H222_B		= 0xF5; // Rec. ITU-T H.222.1 type B
    public static int PES_SID_H222_C		= 0xF6; // Rec. ITU-T H.222.1 type C
    public static int PES_SID_H222_D		= 0xF7; // Rec. ITU-T H.222.1 type D
    public static int PES_SID_H222_E		= 0xF8; // Rec. ITU-T H.222.1 type E
    public static int PES_SID_ANCILLARY	= 0xF9; // ancillary_stream
    public static int PES_SID_MPEG4_SL	= 0xFA; // ISO/IEC 14496-1_SL_packetized_stream
    public static int PES_SID_MPEG4_Flex	= 0xFB; // ISO/IEC 14496-1_FlexMux_stream
    public static int PES_SID_META		= 0xFC; // metadata stream
    public static int PES_SID_EXTEND		= 0xFD;	// extended_stream_id
    public static int PES_SID_RESERVED	= 0xFE;	// reserved data stream
    public static int PES_SID_PSD			= 0xFF; // program_stream_directory
}
