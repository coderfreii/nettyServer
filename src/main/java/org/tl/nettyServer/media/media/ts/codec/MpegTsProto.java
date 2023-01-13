package org.tl.nettyServer.media.media.ts.codec;

public interface MpegTsProto {
    //enum EPSI_STREAM_TYPE
   public static int PSI_STREAM_RESERVED			= 0x00; // ITU-T | ISO/IEC Reserved
   public static int PSI_STREAM_MPEG1			= 0x01; // ISO/IEC 11172-2 Video
   public static int PSI_STREAM_MPEG2			= 0x02; // Rec. ITU-T H.262 | ISO/IEC 13818-2 Video or ISO/IEC 11172-2 constrained parameter video stream(see Note 2)
   public static int PSI_STREAM_AUDIO_MPEG1		= 0x03; // ISO/IEC 11172-3 Audio
   public static int PSI_STREAM_MP3				= 0x04; // ISO/IEC 13818-3 Audio
   public static int PSI_STREAM_PRIVATE_SECTION	= 0x05; // Rec. ITU-T H.222.0 | ISO/IEC 13818-1 private_sections
   public static int PSI_STREAM_PRIVATE_DATA		= 0x06; // Rec. ITU-T H.222.0 | ISO/IEC 13818-1 PES packets containing private data
   public static int PSI_STREAM_MHEG				= 0x07; // ISO/IEC 13522 MHEG
   public static int PSI_STREAM_DSMCC			= 0x08; // Rec. ITU-T H.222.0 | ISO/IEC 13818-1 Annex A DSM-CC
   public static int PSI_STREAM_H222_ATM			= 0x09; // Rec. ITU-T H.222.1
   public static int PSI_STREAM_DSMCC_A			= 0x0a; // ISO/IEC 13818-6(Extensions for DSM-CC) type A
   public static int PSI_STREAM_DSMCC_B			= 0x0b; // ISO/IEC 13818-6(Extensions for DSM-CC) type B
   public static int PSI_STREAM_DSMCC_C			= 0x0c; // ISO/IEC 13818-6(Extensions for DSM-CC) type C
   public static int PSI_STREAM_DSMCC_D			= 0x0d; // ISO/IEC 13818-6(Extensions for DSM-CC) type D
   public static int PSI_STREAM_H222_Aux			= 0x0e; // Rec. ITU-T H.222.0 | ISO/IEC 13818-1 auxiliary
   public static int PSI_STREAM_AAC				= 0x0f; // ISO/IEC 13818-7 Audio with ADTS transport syntax
   public static int PSI_STREAM_MPEG4			= 0x10; // ISO/IEC 14496-2 Visual
   public static int PSI_STREAM_MPEG4_AAC_LATM	= 0x11; // ISO/IEC 14496-3 Audio with the LATM transport syntax as defined in ISO/IEC 14496-3
   public static int PSI_STREAM_MPEG4_PES		= 0x12; // ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in PES packets
   public static int PSI_STREAM_MPEG4_SECTIONS	= 0x13; // ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in ISO/IEC 14496_sections
   public static int PSI_STREAM_MPEG2_SDP		= 0x14; // ISO/IEC 13818-6 Synchronized Download Protocol
   public static int PSI_STREAM_PES_META			= 0x15; // Metadata carried in PES packets
   public static int PSI_STREAM_SECTION_META		= 0x16; // Metadata carried in metadata_sections
   public static int PSI_STREAM_DSMCC_DATA		= 0x17; // Metadata carried in ISO/IEC 13818-6 Data Carousel
   public static int PSI_STREAM_DSMCC_OBJECT		= 0x18; // Metadata carried in ISO/IEC 13818-6 Object Carousel
   public static int PSI_STREAM_DSMCC_SDP		= 0x19; // Metadata carried in ISO/IEC 13818-6 Synchronized Download Protocol
   public static int PSI_STREAM_MPEG2_IPMP		= 0x1a; // IPMP stream (defined in ISO/IEC 13818-11; MPEG-2 IPMP)
   public static int PSI_STREAM_H264				= 0x1b; // H.264
   public static int PSI_STREAM_MPEG4_AAC		= 0x1c; // ISO/IEC 14496-3 Audio; without using any additional transport syntax; such as DST; ALS and SLS
   public static int PSI_STREAM_MPEG4_TEXT		= 0x1d; // ISO/IEC 14496-17 Text
   public static int PSI_STREAM_AUX_VIDEO		= 0x1e; // Auxiliary video stream as defined in ISO/IEC 23002-3
   public static int PSI_STREAM_H264_SVC			= 0x1f; // SVC video sub-bitstream of an AVC video stream conforming to one or more profiles defined in Annex G of Rec. ITU-T H.264 | ISO/IEC 14496-10
   public static int PSI_STREAM_H264_MVC			= 0x20; // MVC video sub-bitstream of an AVC video stream conforming to one or more profiles defined in Annex H of Rec. ITU-T H.264 | ISO/IEC 14496-10
   public static int PSI_STREAM_JPEG_2000		= 0x21; // Video stream conforming to one or more profiles as defined in Rec. ITU-T T.800 | ISO/IEC 15444-1
   public static int PSI_STREAM_MPEG2_3D			= 0x22; // Additional view Rec. ITU-T H.262 | ISO/IEC 13818-2 video stream for service-compatible stereoscopic 3D services
   public static int PSI_STREAM_MPEG4_3D			= 0x23; // Additional view Rec. ITU-T H.264 | ISO/IEC 14496-10 video stream conforming to one or more profiles defined in Annex A for service-compatible stereoscopic 3D services
   public static int PSI_STREAM_H265				= 0x24; // Rec. ITU-T H.265 | ISO/IEC 23008-2 video stream or an HEVC temporal video sub-bitstream
   public static int PSI_STREAM_H265_subset		= 0x25; // HEVC temporal video subset of an HEVC video stream conforming to one or more profiles defined in Annex A of Rec. ITU-T H.265 | ISO/IEC 23008-2
   public static int PSI_STREAM_H264_MVCD		= 0x26; // MVCD video sub-bitstream of an AVC video stream conforming to one or more profiles defined in Annex I of Rec. ITU-T H.264 | ISO/IEC 14496-10
   public static int PSI_STREAM_VP8				= 0x9d;
   public static int PSI_STREAM_VP9				= 0x9e;
   public static int PSI_STREAM_AV1				= 0x9f;
   // 0x27-0x7E Rec. ITU-T H.222.0 | ISO/IEC 13818-1 Reserved
   public static int PSI_STREAM_IPMP				= 0x7F; // IPMP stream
    // 0x80-0xFF User Private
   public static int PSI_STREAM_VIDEO_CAVS		= 0x42; // ffmpeg/libavformat/mpegts.h
   public static int PSI_STREAM_AUDIO_AC3		= 0x81; // ffmpeg/libavformat/mpegts.h
   public static int PSI_STREAM_AUDIO_EAC3       = 0x87; // ffmpeg/libavformat/mpegts.h
   public static int PSI_STREAM_AUDIO_DTS		= 0x8a; // ffmpeg/libavformat/mpegts.h
   public static int PSI_STREAM_VIDEO_DIRAC		= 0xd1; // ffmpeg/libavformat/mpegts.h
   public static int PSI_STREAM_VIDEO_VC1		= 0xea; // ffmpeg/libavformat/mpegts.h
   public static int PSI_STREAM_VIDEO_SVAC		= 0x80; // GBT 25724-2010 SVAC(2014)
   public static int PSI_STREAM_AUDIO_SVAC		= 0x9B; // GBT 25724-2010 SVAC(2014)
   public static int PSI_STREAM_AUDIO_G711A		= 0x90;	// GBT 25724-2010 SVAC(2014)
   public static int PSI_STREAM_AUDIO_G711U      = 0x91;
   public static int PSI_STREAM_AUDIO_G722		= 0x92;
   public static int PSI_STREAM_AUDIO_G723		= 0x93;
   public static int PSI_STREAM_AUDIO_G729		= 0x99;
   public static int PSI_STREAM_AUDIO_OPUS		= 0x9c;



   public static int MPEG_FLAG_IDR_FRAME				= 0x0001;
   public static int MPEG_FLAG_PACKET_LOST			= 0x1000; // packet(s) lost before the packet(this packet is ok; but previous packet has missed or corrupted)
   public static int MPEG_FLAG_PACKET_CORRUPT		= 0x2000; // this packet miss same data(packet lost)
   public static int MPEG_FLAG_H264_H265_WITH_AUD	= 0x8000;


   public static byte PAT_TID_PAS				= 0x00; // program_association_section
   public static byte PAT_TID_CAS				= 0x01; // conditional_access_section(CA_section)
   public static byte PAT_TID_PMS				= 0x02; // TS_program_map_section
   public static byte PAT_TID_SDS				= 0x03; // TS_description_section
   public static byte PAT_TID_MPEG4_scene		= 0x04; // ISO_IEC_14496_scene_description_section
   public static byte PAT_TID_MPEG4_object	= 0x05; // ISO_IEC_14496_object_descriptor_section
   public static byte PAT_TID_META			= 0x06; // Metadata_section
   public static byte PAT_TID_IPMP			= 0x07; // IPMP_Control_Information_section(defined in ISO/IEC 13818-11)
   public static byte PAT_TID_H222			= 0x08; // Rec. ITU-T H.222.0 | ISO/IEC 13818-1 reserved
   public static byte PAT_TID_USER			= 0x40;	// User private
   public static byte PAT_TID_SDT             = 0x42; // service_description_section
   public static int PAT_TID_Forbidden		= 0xFF;

   // Table 2-3 - PID table(p36)
   //enum ETS_PID
  // {
     public static byte  TS_PID_PAT	= 0x00; // program association table
     public static byte  TS_PID_CAT	= 0x01; // conditional access table
     public static byte  TS_PID_TSDT	= 0x02; // transport stream description table
     public static byte  TS_PID_IPMP	= 0x03; // IPMP control information table
      // 0x0004-0x000F Reserved
       // 0x0010-0x1FFE May be assigned as network_PID; Program_map_PID; elementary_PID; or for other purposes
     public static byte  TS_PID_SDT  = 0x11; // https://en.wikipedia.org/wiki/Service_Description_Table / https://en.wikipedia.org/wiki/MPEG_transport_stream
     public static byte  TS_PID_USER	= 0x0042;
     public static int  TS_PID_NULL	= 0x1FFF; // Null packet
  // };




}
