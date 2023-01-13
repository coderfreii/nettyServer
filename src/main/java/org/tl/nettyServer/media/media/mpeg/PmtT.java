package org.tl.nettyServer.media.media.mpeg;

public class PmtT {
    public int pid;		// PID : 13 [0x0010, 0x1FFE]
    public int pn;		// program_number: 16 [1, 0xFFFF]
    public int ver;		// version_number : 5
    public int cc;		// continuity_counter : 4
    public int PCR_PID;	// 13-bits
    public int pminfo_len;// program_info_length : 12
    public byte[] pminfo;	// program_info;

    public char[] provider = new char[64];
    public char[] name= new char[64];

    public int stream_count;
    public PesT[] streams = new PesT[4];//[4];
}
