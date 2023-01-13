package org.tl.nettyServer.media.media.mpeg;

public class PsSystemHeaderT {
   public int rate_bound;
   public int audio_bound = 6;
   public int fixed_flag = 1;
   public int CSPS_flag = 1;
   public int system_audio_lock_flag = 1;
   public int system_video_lock_flag = 1;
   public int video_bound = 5;
   public int packet_rate_restriction_flag = 1;

    public int stream_count;
    public PsStreamHeaderT[] streams = new PsStreamHeaderT[16];//[16];
}
