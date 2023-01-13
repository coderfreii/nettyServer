package org.tl.nettyServer.media.media.mpeg;

public class PatT {
    public int tsid;	// transport_stream_id : 16;
    public int ver;	// version_number : 5;
    public int cc;	//continuity_counter : 4;

    public int pmt_count;
    public int pmt_capacity;
    public PmtT[] pmt_default = new PmtT[1];//[1];
    public PmtT[] pmts;
}
