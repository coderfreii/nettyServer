package org.tl.nettyServer.media.media.mpeg;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * // ITU-T H.222.0(06/2012)
 * // Information technology - Generic coding of moving pictures and associated audio information: Systems
 * // 2.6 Program and program element descriptors(p83)

2.6 Program and program element descriptors
2.6.1 Semantic definition of fields in program and program element descriptors
Table 2-45 - Program and program element descriptors
tag		TS	PS	Identification
0		n/a n/a reserved
1		n/a X	forbidden
2		X	X	video_stream_descriptor
3		X	X	audio_stream_descriptor
4		X	X	hierarchy_descriptor
5		X	X	registration_descriptor
6		X	X	data_stream_alignment_descriptor
7		X	X	target_background_grid_descriptor
8		X	X	video_window_descriptor
9		X	X	CA_descriptor
10		X	X	ISO_639_language_descriptor
11		X	X	system_clock_descriptor
12		X	X	multiplex_buffer_utilization_descriptor
13		X	X	copyright_descriptor
14		X		maximum_bitrate_descriptor
15		X	X	private_data_indicator_descriptor
16		X	X	smoothing_buffer_descriptor
17		X		STD_descriptor
18		X	X	IBP_descriptor
19-26	X		Defined in ISO/IEC 13818-6
27		X	X	MPEG-4_video_descriptor
28		X	X	MPEG-4_audio_descriptor
29		X	X	IOD_descriptor
30		X		SL_descriptor
31		X	X	FMC_descriptor
32		X	X	external_ES_ID_descriptor
33		X	X	MuxCode_descriptor
34		X	X	FmxBufferSize_descriptor
35		X		multiplexbuffer_descriptor
36		X	X	content_labeling_descriptor
37		X	X	metadata_pointer_descriptor
38		X	X	metadata_descriptor
39		X	X	metadata_STD_descriptor
40		X	X	AVC video descriptor
41		X	X	IPMP_descriptor (defined in ISO/IEC 13818-11, MPEG-2 IPMP)
42		X	X	AVC timing and HRD descriptor
43		X	X	MPEG-2_AAC_audio_descriptor
44		X	X	FlexMuxTiming_descriptor
45		X	X	MPEG-4_text_descriptor
46		X	X	MPEG-4_audio_extension_descriptor
47		X	X	auxiliary_video_stream_descriptor
48		X	X	SVC extension descriptor
49		X	X	MVC extension descriptor
50		X	n/a J2K video descriptor
51		X	X	MVC operation point descriptor
52		X	X	MPEG2_stereoscopic_video_format_descriptor
53		X	X	Stereoscopic_program_info_descriptor
54		X	X	Stereoscopic_video_info_descriptor
55      X   n/a Transport_profile_descriptor
56      X   n/a HEVC video descriptor
57-63	n/a n/a Rec. ITU-T H.222.0 | ISO/IEC 13818-1 Reserved
64-255	n/a n/a User Private
*/
public class MpegElementDescriptor {
    public int mpeg_elment_descriptor(byte[] data, int bytes)
    {
        byte descriptor_tag = data[0];
        byte descriptor_len = data[1];
        if (descriptor_len + 2 > bytes)
            return bytes;

        switch(descriptor_tag){
            case 2:
                video_stream_descriptor(data, bytes);
                break;

            case 3:
                audio_stream_descriptor(data, bytes);
                break;

            case 4:
                hierarchy_descriptor(data, bytes);
                break;

            case 5:
                registration_descriptor(data, bytes);
                break;

            case 10:
                language_descriptor(data, bytes);
                break;

            case 11:
                system_clock_descriptor(data, bytes);
                break;

            case 27:
                mpeg4_video_descriptor(data, bytes);
                break;

            case 28:
                mpeg4_audio_descriptor(data, bytes);
                break;

            case 37:
                metadata_pointer_descriptor(data, bytes);
                break;

            case 38:
                metadata_descriptor(data, bytes);
                break;

            case 40:
                avc_video_descriptor(data, bytes);
                break;

            case 42:
                avc_timing_hrd_descriptor(data, bytes);
                break;

            case 43:
                mpeg2_aac_descriptor(data, bytes);
                break;

            case 48:
                svc_extension_descriptor(data, bytes);
                break;

            case 49:
                mvc_extension_descriptor(data, bytes);
                break;

            case 0x40:
                clock_extension_descriptor(data, bytes);
                break;

            //default:
            //	assert(0);
        }

        return descriptor_len+2;
    }
    public int video_stream_descriptor(byte[] data, int bytes){
        // 2.6.2 Video stream descriptor(p85)

        byte v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        VideoStreamDescriptorT desc = new VideoStreamDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);
        //数据清空
        //memset(&desc, 0, sizeof(desc));
        v = MpegUtil.mpeg_bits_read8(bits);
        desc.multiple_frame_rate_flag = (v >> 7) & 0x01;
        desc.frame_rate_code = (v >> 3) & 0x0F;
        desc.MPEG_1_only_flag = (v >> 2) & 0x01;
        desc.constrained_parameter_flag = (v >> 1) & 0x01;
        desc.still_picture_flag = v & 0x01;

        if(0 == desc.MPEG_1_only_flag)
        {
            desc.profile_and_level_indication = MpegUtil.mpeg_bits_read8(bits);
            v = MpegUtil.mpeg_bits_read8(bits);
            desc.chroma_format = (v >> 6) & 0x03;
            desc.frame_rate_code = (v >> 5) & 0x01;
            assert((0x1F & v) == 0x00); // 'xxx00000'
        }

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int  audio_stream_descriptor(byte[] data, int bytes)
    {
        // 2.6.4 Audio stream descriptor(p86)

        byte v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        AudioStreamDescriptorT desc = new AudioStreamDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        v = MpegUtil.mpeg_bits_read8(bits);
        //memset(&desc, 0, sizeof(desc));
        desc.free_format_flag = (v >> 7) & 0x01;
        desc.ID = (v >> 6) & 0x01;
        desc.layer = (v >> 4) & 0x03;
        desc.variable_rate_audio_indicator = (v >> 3) & 0x01;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }
    public int hierarchy_descriptor(byte[] data, int bytes)
    {
        // 2.6.6 Hierarchy descriptor(p86)

        byte v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        HierarchyDescriptorT desc = new HierarchyDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        v = MpegUtil.mpeg_bits_read8(bits);
       // memset(&desc, 0, sizeof(desc));
        desc.no_view_scalability_flag = (v >> 7) & 0x01;
        desc.no_temporal_scalability_flag = (v >> 6) & 0x01;
        desc.no_spatial_scalability_flag = (v >> 5) & 0x01;
        desc.no_quality_scalability_flag = (v >> 4) & 0x01;
        desc.hierarchy_type = v & 0x0F;
        desc.hierarchy_layer_index = MpegUtil.mpeg_bits_read8(bits) & 0x3F;
        v = MpegUtil.mpeg_bits_read8(bits);
        desc.tref_present_flag = (v >> 7) & 0x01;
        desc.hierarchy_embedded_layer_index = v & 0x3F;
        desc.hierarchy_channel = MpegUtil.mpeg_bits_read8(bits) & 0x3F;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int registration_descriptor(byte[] data, int bytes)
    {
        // 2.6.8 Registration descriptor(p94)
        int fourcc;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        fourcc = MpegUtil.mpeg_bits_read32(bits);
        /*
        这只是一种防止编译器编译时报警告的用法。有些变量如果未曾使用，在编译时是会报错，从而有些导致编译不过，所以才会出现这种用法。而此语句在代码中没有具体意义，只是告诉编译器该变量已经使用了。
         */
        //(void)fourcc;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len + 2;
    }
    public int language_descriptor(byte[] data, int bytes)
    {
        // 2.6.18 ISO 639 language descriptor(p92)
        int i;
        //	byte descriptor_tag = data[0];
        int descriptor_len = data[1];
        assert(descriptor_len+2 <= bytes);

        for(i = 2; i + 4 <= descriptor_len + 2; i += 4)
        {
            LanguageDescriptorT desc = new LanguageDescriptorT();
            //memset(&desc, 0, sizeof(desc));
            desc.code = (data[i] << 16) | (data[i+1] << 8) | data[i+2];
            desc.audio = data[i+3];
        }

        return descriptor_len+2;
    }

    public int system_clock_descriptor(byte[] data, int bytes)
    {
        // 2.6.20 System clock descriptor(p92)

        byte v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        SystemClockDescriptorT desc = new SystemClockDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        v = MpegUtil.mpeg_bits_read8(bits);
       // memset(&desc, 0, sizeof(desc));
        desc.external_clock_reference_indicator = (v >> 7) & 0x01;
        desc.clock_accuracy_integer = v & 0x3F;
        desc.clock_accuracy_exponent = (MpegUtil.mpeg_bits_read8(bits) >> 5) & 0x07;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int mpeg4_video_descriptor(byte[] data, int bytes)
    {
        // 2.6.36 MPEG-4 video descriptor(p96)

        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        Mpeg4VideoDescriptorT desc = new Mpeg4VideoDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        //memset(&desc, 0, sizeof(desc));
        desc.visual_profile_and_level = MpegUtil.mpeg_bits_read8(bits);

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int mpeg4_audio_descriptor(byte[] data, int bytes)
    {
        // 2.6.38 MPEG-4 audio descriptor(p97)

        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        Mpeg4AudioDescriptorT desc = new Mpeg4AudioDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        //memset(&desc, 0, sizeof(desc));
        desc.profile_and_level = MpegUtil.mpeg_bits_read8(bits);

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int metadata_pointer_descriptor(byte[] data, int bytes)
    {
        // 2.6.58 Metadata pointer descriptor(p112)

        byte flags;
        MpegBitsT bits = new MpegBitsT();
        MetadataPointerDescriptorT desc = new MetadataPointerDescriptorT();
        int descriptor_len;

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        desc.metadata_application_format_identifier = MpegUtil.mpeg_bits_read16(bits);
        if (0xFFFF == desc.metadata_application_format_identifier)
            desc.metadata_application_format_identifier = MpegUtil.mpeg_bits_read32(bits);

        desc.metadata_format_identifier = MpegUtil.mpeg_bits_read8(bits);
        if (0xFF == desc.metadata_format_identifier)
            desc.metadata_format_identifier = MpegUtil.mpeg_bits_read32(bits);

        desc.metadata_service_id = MpegUtil.mpeg_bits_read8(bits);
        flags = MpegUtil.mpeg_bits_read8(bits);
        desc.MPEG_carriage_flags = (byte) ((flags >> 5) & 0x03);

        if ( (flags & 0x80) > 0) // metadata_locator_record_flag
        {
            desc.metadata_locator_record_length = MpegUtil.mpeg_bits_read8(bits);
            MpegUtil.mpeg_bits_skip(bits, desc.metadata_locator_record_length); // metadata_locator_record_byte
        }

        if (desc.MPEG_carriage_flags <= 2)
            desc.program_number = MpegUtil.mpeg_bits_read16(bits);

        if (1 == desc.MPEG_carriage_flags)
        {
            desc.transport_stream_location = MpegUtil.mpeg_bits_read16(bits);
            desc.transport_stream_id = MpegUtil.mpeg_bits_read16(bits);
        }

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len + 2;
    }

    public int metadata_descriptor(byte[] data, int bytes)
    {
        // 2.6.60 Metadata descriptor(p115)

        byte flags;
        MpegBitsT bits = new MpegBitsT();
        MetadataDescriptorT desc = new MetadataDescriptorT();
        int descriptor_len;

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        desc.metadata_application_format_identifier = MpegUtil.mpeg_bits_read16(bits);
        if (0xFFFF == desc.metadata_application_format_identifier)
            desc.metadata_application_format_identifier = MpegUtil.mpeg_bits_read32(bits);

        desc.metadata_format_identifier = MpegUtil.mpeg_bits_read8(bits);
        if (0xFF == desc.metadata_format_identifier)
            desc.metadata_format_identifier = MpegUtil.mpeg_bits_read32(bits);

        desc.metadata_service_id = MpegUtil.mpeg_bits_read8(bits);
        flags = MpegUtil.mpeg_bits_read8(bits);
        desc.decoder_config_flags = (byte) ((flags >> 5) & 0x07);
        if ( (flags & 0x10) > 0) // DSM-CC_flag
        {
            desc.service_identification_length = MpegUtil.mpeg_bits_read8(bits);
            MpegUtil.mpeg_bits_skip(bits, desc.service_identification_length); // service_identification_record_byte
        }

        if (0x01 == desc.decoder_config_flags)
        {
            desc.decoder_config_length = MpegUtil.mpeg_bits_read8(bits);
            MpegUtil.mpeg_bits_skip(bits, desc.decoder_config_length); // decoder_config_byte
        }
        else if (0x03 == desc.decoder_config_flags)
        {
            desc.dec_config_identification_record_length = MpegUtil.mpeg_bits_read8(bits);
            MpegUtil.mpeg_bits_skip(bits, desc.dec_config_identification_record_length); // dec_config_identification_record_byte
        }
        else if (0x04 == desc.decoder_config_flags)
        {
            desc.decoder_config_metadata_service_id = MpegUtil.mpeg_bits_read8(bits);
        }

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len + 2;
    }

    public int avc_video_descriptor(byte[] data, int bytes)
    {
        // 2.6.64 AVC video descriptor(p110)

        byte v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        AvcVideoDescriptorT desc = new AvcVideoDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        //memset(&desc, 0, sizeof(desc));
        desc.profile_idc = MpegUtil.mpeg_bits_read8(bits);
        v = MpegUtil.mpeg_bits_read8(bits);
        desc.constraint_set0_flag = (v >> 7) & 0x01;
        desc.constraint_set1_flag = (v >> 6) & 0x01;
        desc.constraint_set2_flag = (v >> 5) & 0x01;
        desc.constraint_set3_flag = (v >> 4) & 0x01;
        desc.constraint_set4_flag = (v >> 3) & 0x01;
        desc.constraint_set5_flag = (v >> 2) & 0x01;
        desc.AVC_compatible_flags = v & 0x03;
        desc.level_idc = MpegUtil.mpeg_bits_read8(bits);
        v = MpegUtil.mpeg_bits_read8(bits);
        desc.AVC_still_present = (v >> 7) & 0x01;
        desc.AVC_24_hour_picture_flag = (v >> 6) & 0x01;
        desc.frame_packing_SEI_not_present_flag = (v >> 5) & 0x01;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int avc_timing_hrd_descriptor(byte[] data, int bytes)
    {
        // 2.6.66 AVC timing and HRD descriptor(p112)

        byte v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        AvcTimingHrdDescriptorT desc = new AvcTimingHrdDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        //memset(&desc, 0, sizeof(desc));
        v = MpegUtil.mpeg_bits_read8(bits);
        desc.hrd_management_valid_flag = (v >> 7) & 0x01;
        desc.picture_and_timing_info_present = (v >> 0) & 0x01;
        if(desc.picture_and_timing_info_present >0)
        {
            v = MpegUtil.mpeg_bits_read8(bits);
            desc._90kHZ_flag = (v >> 7) & 0x01;
            if(0 == desc._90kHZ_flag)
            {
                desc.N = MpegUtil.mpeg_bits_read32(bits);
                desc.K = MpegUtil.mpeg_bits_read32(bits);
            }
            desc.num_unit_in_tick = MpegUtil.mpeg_bits_read32(bits);
        }

        v = MpegUtil.mpeg_bits_read8(bits);
        desc.fixed_frame_rate_flag = (v >> 7) & 0x01;
        desc.temporal_poc_flag = (v >> 6) & 0x01;
        desc.picture_to_display_conversion_flag = (v >> 5) & 0x01;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int mpeg2_aac_descriptor(byte[] data, int bytes)
    {
        // 2.6.68 MPEG-2 AAC audio descriptor(p113)

        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        Mpeg2AacDescriptorT desc = new Mpeg2AacDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        //memset(&desc, 0, sizeof(desc));
        desc.profile = MpegUtil.mpeg_bits_read8(bits);
        desc.channel_configuration = MpegUtil.mpeg_bits_read8(bits);
        desc.additional_information = MpegUtil.mpeg_bits_read8(bits);

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int svc_extension_descriptor(byte[] data, int bytes)
    {
        // 2.6.76 SVC extension descriptor(p116)

        byte v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        SvcExtensionDescriptorT desc = new SvcExtensionDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        //memset(&desc, 0, sizeof(desc));
        desc.width = MpegUtil.mpeg_bits_read16(bits);
        desc.height = MpegUtil.mpeg_bits_read16(bits);
        desc.frame_rate = MpegUtil.mpeg_bits_read16(bits);
        desc.average_bitrate = MpegUtil.mpeg_bits_read16(bits);
        desc.maximum_bitrate = MpegUtil.mpeg_bits_read16(bits);
        desc.dependency_id = (MpegUtil.mpeg_bits_read8(bits) >> 5) & 0x07;
        v = MpegUtil.mpeg_bits_read8(bits);
        desc.quality_id_start = (v >> 4) & 0x0F;
        desc.quality_id_end = (v >> 0) & 0x0F;
        v = MpegUtil.mpeg_bits_read8(bits);
        desc.temporal_id_start = (v >> 5) & 0x07;
        desc.temporal_id_end = (v >> 2) & 0x07;
        desc.no_sei_nal_unit_present = (v >> 1) & 0x01;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len+2;
    }

    public int mvc_extension_descriptor(byte[] data, int bytes)
    {
        // 2.6.78 MVC extension descriptor(p117)

        int v;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();
        MvcDxtensionDescriptorT desc = new MvcDxtensionDescriptorT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        //memset(&desc, 0, sizeof(desc));
        desc.average_bit_rate = MpegUtil.mpeg_bits_read16(bits);
        desc.maximum_bitrate = MpegUtil.mpeg_bits_read16(bits);
        v = MpegUtil.mpeg_bits_read32(bits);
        desc.view_order_index_min = (v >> 18) & 0x3FF;
        desc.view_order_index_max = (v >> 8) & 0x3FF;
        desc.temporal_id_start = (v >> 5) & 0x07;
        desc.temporal_id_end = (v >> 2) & 0x07;
        desc.no_sei_nal_unit_present = (v >> 1) & 0x01;
        desc.no_prefix_nal_unit_present = (v >> 0) & 0x01;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len + 2;
    }
    public static String  SERVICE_NAME	="ireader/media-server";
    public static byte  SERVICE_ID	=	0x71;
    public static int service_extension_descriptor_write(byte[] data,int index, int bytes)
    {
        byte n;
        n = (byte)SERVICE_NAME.length();
        if (bytes < 2 + n)
            return 0;
        data[index+0] = SERVICE_ID;
        data[index+1] = (byte) (2 + n);
        System.arraycopy(SERVICE_NAME.getBytes(StandardCharsets.UTF_8),0,data,index+2,n);
        //memcpy(data + 2, SERVICE_NAME, n);
        return 2 + n;
    }

    public int clock_extension_descriptor(byte[] data, int bytes)
    {
        int v;
        Tm t = new Tm();
        long clock;
        int descriptor_len;
        MpegBitsT bits = new MpegBitsT();

        MpegUtil.mpeg_bits_init(bits, data, bytes);
        MpegUtil.mpeg_bits_read8(bits); // descriptor_tag
        descriptor_len = MpegUtil.mpeg_bits_read8(bits);
        assert(descriptor_len + 2 <= bytes);

        v = MpegUtil.mpeg_bits_read32(bits); // skip 4-bytes leading
        //memset(&t, 0, sizeof(t));
        t.tm_year = MpegUtil.mpeg_bits_read8(bits) + 2000 - 1900;
        v = MpegUtil.mpeg_bits_read32(bits);
        t.tm_mon = ((v >> 28) & 0x0F) - 1;
        t.tm_mday = (v >> 23) & 0x1F;
        t.tm_hour = (v >> 18) & 0x1F;
        t.tm_min = (v >> 12) & 0x3F;
        t.tm_sec = (v >> 6) & 0x3F;
        //desc.microsecond = v & 0x3F;
        clock = mktime(t) * 1000;

        assert(0 == MpegUtil.mpeg_bits_error(bits));
        return descriptor_len + 2;
    }
    public long mktime(Tm mt){
        LocalDateTime time = LocalDateTime.of(mt.tm_year, mt.tm_mon, mt.tm_mday, mt.tm_hour, mt.tm_min, mt.tm_sec, 0);
        return time.getSecond();
    }
    public static Tm localtime(long seconds){
        Tm t = new Tm();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(seconds), ZoneId.systemDefault());
        t.tm_year = localDateTime.getYear();
        t.tm_mon = localDateTime.getMonthValue();
        t.tm_mday= localDateTime.getDayOfMonth();
        t.tm_hour = localDateTime.getHour();
        t.tm_min = localDateTime.getMinute();
        t.tm_sec = localDateTime.getSecond();
        return t;
    }
    public static int clock_extension_descriptor_write(byte[] data,int index, int bytes, long clock)
    {
        Tm t = new Tm();
        long seconds;
        if (bytes < 16)
            return 0;

        seconds = (clock / 1000);
        t = localtime(seconds);

        data[index+0] = 0x40;
        data[index+1] = 0x0E;
        data[index+2] = 0x48;
        data[index+3] = 0x4B;
        data[index+4] = 0x01;
        data[index+5] = 0x00;
        data[index+6] = (byte)(t.tm_year + 1900 - 2000); // base 2000
        data[index+7] = (byte) ((byte)((t.tm_mon + 1) << 4) | ((t.tm_mday >> 1) & 0x0F));
        data[index+8] = (byte) ((byte)((t.tm_mday & 0x01) << 7) | ((t.tm_hour & 0x1F) << 2) | ((t.tm_min >> 4) & 0x03));
        data[index+9] = (byte) ((byte)((t.tm_min & 0x0F) << 4) | ((t.tm_sec >> 2) & 0x0F));
        data[index+10] = (byte)((t.tm_sec & 0x03) << 6);
        data[index+11] = 0x00;
        data[index+12] = 0x00;
        data[index+13] = (byte) 0xFF;
        data[index+14] = (byte) 0xFF;
        data[index+15] = (byte) 0xFF;
        return 16;
    }

}
