package org.tl.nettyServer.media.media.mpeg;

public class PesT {
   public char pn;        // TS program number(0-ps)
   public char pid;		// PES PID = 13
   public byte sid;		// PES stream_id = 8
   public byte codecid;	// PMT/PSM stream_type = 8
   public byte cc;			// continuity_counter = 4;
   public byte[] esinfo;	// es_info
   public char esinfo_len;// es_info_length = 12

   public int len;		// PES_packet_length = 16;

   public int reserved10 = 2;
   public int PES_scrambling_control = 2;
   public int PES_priority = 1;
   public int data_alignment_indicator = 1;
   public int copyright = 1;
   public int original_or_copy = 1;

   public int PTS_DTS_flags = 2;
   public int ESCR_flag = 1;
   public int ES_rate_flag = 1;
   public int DSM_trick_mode_flag = 1;
   public int additional_copy_info_flag = 1;
   public int PES_CRC_flag = 1;
   public int PES_extension_flag = 1;
   public int PES_header_data_length = 8;

   public long pts;
   public long dts;
   public long ESCR_base;
   public int ESCR_extension;
   public int ES_rate;

    //byte trick_mode;
    //int trick_mode_control = 3;
    //int field_id = 2;
    //int intra_slice_refresh = 1;
    //int frequency_truncation = 2;

    //byte additional_copy_info;
    //int16_t previous_PES_packet_CRC;

    //int PES_private_data_flag = 1;
    //int pack_header_field_flag = 1;
    //int program_packet_sequence_counter_flag = 1;
    //int P_STD_buffer_flag = 1;
    //int reserved_ = 3;
    //int PES_extension_flag_2 = 1;
    //int PES_private_data_flag2 = 1;
    //byte PES_private_data[128/8];

    //int pack_field_length = 8;

    public int have_pes_header; // TS demuxer only
    public int flags; // TS/PS demuxer only
    public PacketT pkt;
}
