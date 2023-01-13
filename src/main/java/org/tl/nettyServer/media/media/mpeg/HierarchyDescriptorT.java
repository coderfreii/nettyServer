package org.tl.nettyServer.media.media.mpeg;

public class HierarchyDescriptorT {
    public int no_view_scalability_flag = 1;
    public int no_temporal_scalability_flag = 1;
    public int no_spatial_scalability_flag = 1;
    public int no_quality_scalability_flag = 1;
    public int hierarchy_type = 4;
    public int tref_present_flag = 1;
    public int reserved1 = 1;
    public int hierarchy_layer_index = 6;
    public int reserved2 = 2;
    public int hierarchy_embedded_layer_index = 6;
    public int reserved3 = 2;
    public int hierarchy_channel = 6;
}
