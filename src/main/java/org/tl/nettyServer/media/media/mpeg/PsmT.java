package org.tl.nettyServer.media.media.mpeg;

public class PsmT {
    int ver = 5;	// version_number : 5;

    PesT[] streams = new PesT[16];//[16];
    int stream_count;

    long clock; // ms
}
