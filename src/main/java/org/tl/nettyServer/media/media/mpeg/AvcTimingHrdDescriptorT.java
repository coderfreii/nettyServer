package org.tl.nettyServer.media.media.mpeg;

public class AvcTimingHrdDescriptorT {
    public int hrd_management_valid_flag = 1;
    public int picture_and_timing_info_present = 1;
    public int _90kHZ_flag = 1;
    public int fixed_frame_rate_flag = 1;
    public int temporal_poc_flag = 1;
    public int picture_to_display_conversion_flag = 1;
    public int N;
    public int K;
    public int num_unit_in_tick;
}
