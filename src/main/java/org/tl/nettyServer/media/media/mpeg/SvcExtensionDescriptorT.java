package org.tl.nettyServer.media.media.mpeg;

public class SvcExtensionDescriptorT {
    public char width;
    public char height;
    public char frame_rate;
    public char average_bitrate;
    public char maximum_bitrate;
    public int quality_id_start = 4;
    public int quality_id_end = 4;
    public int temporal_id_start = 3;
    public int temporal_id_end = 3;
    public int dependency_id = 3;
    public int no_sei_nal_unit_present = 1;
}
