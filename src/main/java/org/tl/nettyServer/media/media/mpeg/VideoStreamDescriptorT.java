package org.tl.nettyServer.media.media.mpeg;

public class VideoStreamDescriptorT {
    public int multiple_frame_rate_flag = 1;
     // Table 2-47 - Frame rate code
     // 23.976/24.0/25.0/29.97/30.0/50.0/59.94/60.0
    public int frame_rate_code = 4;
    public int MPEG_1_only_flag = 1;
    public int constrained_parameter_flag = 1;
    public int still_picture_flag = 1;
     // MPEG_1_only_flag == 0
    public int profile_and_level_indication = 8;
    public int chroma_format = 2;
    public int frame_rate_extension_flag = 1;
}
