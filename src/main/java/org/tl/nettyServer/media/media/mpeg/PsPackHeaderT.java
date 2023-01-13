package org.tl.nettyServer.media.media.mpeg;

public class PsPackHeaderT {
    public int mpeg2; // 1-mpeg2, other-mpeg1

    public long system_clock_reference_base;
    public int system_clock_reference_extension;

    public int program_mux_rate;
}
