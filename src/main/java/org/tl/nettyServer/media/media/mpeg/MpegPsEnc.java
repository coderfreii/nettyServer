package org.tl.nettyServer.media.media.mpeg;


import static org.tl.nettyServer.media.media.mpeg.MpegTsProto.PSI_STREAM_H264;
import static org.tl.nettyServer.media.media.mpeg.MpegTsProto.PSI_STREAM_H265;

public class MpegPsEnc implements MpegPesProto{

    public static int MAX_PES_HEADER = 1024;	// pack_header + system_header + psm
    public static int MAX_PES_PACKET = 0xFFFF;	// 64k pes data

    public static PsMuxerT ps_muxer_create(PsMuxerFuncT func, MpegMuxerT param){
        PsMuxerT ps ;
       // assert(func);
        ps = new PsMuxerT();
        if(ps == null)
            return null;
        ps.func = func;
       // memcpy(&ps.func, func, sizeof(ps.func));
        ps.param = param;

        ps.system.rate_bound = 26234; //10493600~10mbps(50BPS * 8 = 400bps)
//	ps.system.audio_bound = 1; // [0,32] max active audio streams
//	ps.system.video_bound = 1; // [0,16] max active video streams
        ps.system.fixed_flag = 0; // 1-fixed bitrate, 0-variable bitrate
        ps.system.CSPS_flag = 0; // meets the constraints defined in 2.7.9.
        ps.system.packet_rate_restriction_flag = 0; // dependence CSPS_flag
        ps.system.system_audio_lock_flag = 0; // all audio stream sampling rate is constant
        ps.system.system_video_lock_flag = 0; // all video stream frequency is constant

        //ps.psm.ver = 1;
        //ps.psm.stream_count = 2;
        //ps.psm.streams[0].element_stream_id = PES_SID_VIDEO;
        //ps.psm.streams[0].stream_type = PSI_STREAM_H264;
        //ps.psm.streams[1].element_stream_id = PES_SID_AUDIO;
        //ps.psm.streams[1].stream_type = PSI_STREAM_AAC;

        return ps;
    }
    public static int ps_muxer_destroy(PsMuxerT ps)
    {
        int i;
        for (i = 0; i < ps.psm.stream_count; i++)
        {
            if (ps.psm.streams[i].esinfo != null)
            {
                //free(ps.psm.streams[i].esinfo);
                ps.psm.streams[i].esinfo = null;
            }
        }
        //free(ps);
        return 0;
    }

    public static int ps_muxer_add_stream(PsMuxerT ps, int codecid,byte[] extradata, int bytes)
    {
        PsmT psm;
        PesT pes;
        assert(bytes < 512);
        if (ps == null || ps.psm.stream_count >= ps.psm.streams.length )
        {
            //assert(0);
            return -1;
        }

        psm = ps.psm;
        pes = psm.streams[psm.stream_count];

        if (MpegUtil.mpeg_stream_type_video(codecid) > 0)
        {
            pes.sid = (byte) (PES_SID_VIDEO + ps.system.video_bound);

            assert(ps.system.video_bound + 1 < 16);
            ++ps.system.video_bound; // [0,16] max active video streams
            ps.system.streams[ps.system.stream_count].buffer_bound_scale = 1;
            /* FIXME -- VCD uses 46, SVCD uses 230, ffmpeg has 230 with a note that it is small */
            ps.system.streams[ps.system.stream_count].buffer_size_bound = 400 /* 8191-13 bits max value */;
        }
        else if (MpegUtil.mpeg_stream_type_audio(codecid) > 0)
        {
            pes.sid = (byte)(PES_SID_AUDIO + ps.system.audio_bound);

            assert(ps.system.audio_bound + 1 < 32);
            ++ps.system.audio_bound; // [0,32] max active audio streams
            ps.system.streams[ps.system.stream_count].buffer_bound_scale = 0;
            /* This value HAS to be used for VCD (see VCD standard, p. IV-7).
             * Right now it is also used for everything else. */
            ps.system.streams[ps.system.stream_count].buffer_size_bound = 32 /* 4 * 1024 / 128 */;
        }
        else
        {
            //assert(0);
            return -1;
        }

        if (bytes > 0)
        {
            pes.esinfo = new byte[bytes];//(uint8_t*)malloc(bytes);
            if (pes.esinfo == null)
                return -1;
            System.arraycopy(extradata,0,pes.esinfo,0,bytes);
            //memcpy(pes.esinfo, extradata, bytes);
            pes.esinfo_len = (char) bytes;
        }

        assert(psm.stream_count == ps.system.stream_count);
        ps.system.streams[ps.system.stream_count].stream_id = pes.sid;
        ++ps.system.stream_count;

        pes.codecid = (byte) codecid;
        ++psm.stream_count;
        ++psm.ver;

        ps.psm_period = 0; // immediate update psm
        return pes.sid;
    }
    public static boolean MPEG_FIX_VLC_3_X_PS_SYSTEM_HEADER = true;
    public static boolean MPEG_CLOCK_EXTENSION_DESCRIPTOR = false;
    public static int ps_muxer_input(PsMuxerT ps, int streamid, int flags, long pts, long dts,byte[] data, int bytes)
    {
        int r, first;
        int i, n, sz;
        byte[] packet;
        PesT stream;
        byte[] payload;
        int payloadIndex = 0;

        i = 0;
        first = 1;
        payload = data;

        stream = ps_stream_find(ps, streamid);
        if (null == stream) return -1; // not found
        stream.data_alignment_indicator = (flags & MpegTsProto.MPEG_FLAG_IDR_FRAME) > 0 ? 1 : 0; // idr frame
        stream.pts = pts;
        stream.dts = dts;

        // Add PSM for IDR frame
        ps.psm_period = ((flags & MpegTsProto.MPEG_FLAG_IDR_FRAME) > 0 && MpegUtil.mpeg_stream_type_video(stream.codecid)> 0) ? 0 : ps.psm_period;
        ps.h264_h265_with_aud = (flags & MpegTsProto.MPEG_FLAG_H264_H265_WITH_AUD)>0 ? 1 : 0;

        // TODO:
        // 1. update packet header program_mux_rate
        // 2. update system header rate_bound

        // alloc once (include Multi-PES packet)
        sz = bytes + MAX_PES_HEADER + (bytes/MAX_PES_PACKET+1) * 64; // 64 = 0x000001 + stream_id + PES_packet_length + other
        packet = ps.func.alloc(ps.param, sz);
        if(packet == null) return -12;

        // write pack_header(p74)
        // 2.7.1 Frequency of coding the system clock reference
        // http://www.bretl.com/mpeghtml/SCR.HTM
        //the maximum allowed interval between SCRs is 700ms
        //ps.pack.system_clock_reference_base = (dts-3600) % (((int64_t)1)<<33);
        ps.pack.system_clock_reference_base = dts >= 3600 ? (dts - 3600) : 0;
        ps.pack.system_clock_reference_extension = 0;
        ps.pack.program_mux_rate = 6106;
        i += MpegPackHeader.pack_header_write(ps.pack,packet,i); //packet + i

        if(MPEG_FIX_VLC_3_X_PS_SYSTEM_HEADER) {
            // https://github.com/videolan/vlc/blob/3.0.x/modules/demux/mpeg/ps.h#L488
            // fix ps_pkt_parse_system . ps_track_fill with default mp1/2 audio codec(without psm)

            // write system_header(p76)
            if (0 == (ps.psm_period % 30))
                i += MpegSystemHeader.system_header_write(ps.system, packet, i);
        }

        // write program_stream_map(p79)
        if (0 == (ps.psm_period % 30)) {
            if (MPEG_CLOCK_EXTENSION_DESCRIPTOR){
                ps.psm.clock = System.currentTimeMillis();// * 1000; // todo: gettimeofday
            }
            i += MpegPsm.psm_write(ps.psm, packet, i);
        }
        // check packet size
        assert(i < MAX_PES_HEADER);

        // write data
        while(bytes > 0)
        {
            int p;
            int pes = /*packet +*/ i;

            p = pes + MpegPes.pes_write_header(stream,packet, pes, sz - i);
            assert(p - pes < 64);

            if(first >0)
            {
                if (PSI_STREAM_H264 == stream.codecid && ps.h264_h265_with_aud != 0)
                {
                    // 2.14 Carriage of Rec. ITU-T H.264 | ISO/IEC 14496-10 video
                    // Each AVC access unit shall contain an access unit delimiter NAL Unit
                    MpegUtil.nbo_w32(packet,p, 0x00000001);
                    packet[p+4] = 0x09; // AUD
                    packet[p+5] = (byte) 0xE0; // any slice type (0xe) + rbsp stop one bit
                    p += 6;
                }
                else if (PSI_STREAM_H265 == stream.codecid && ps.h264_h265_with_aud != 0)
                {
                    // 2.17 Carriage of HEVC
                    // Each HEVC access unit shall contain an access unit delimiter NAL unit.
                    MpegUtil.nbo_w32(packet,p, 0x00000001);
                    packet[p+4] = 0x46; // 35-AUD_NUT
                    packet[p+5] = 01;
                    packet[p+6] = 0x50; // B&P&I (0x2) + rbsp stop one bit
                    p += 7;
                }
            }

            // PES_packet_length = PES-Header + Payload-Size
            // A value of 0 indicates that the PES packet length is neither specified nor bounded
            // and is allowed only in PES packets whose payload consists of bytes from a
            // video elementary stream contained in transport stream packets
            if((p - pes - 6) + bytes > MAX_PES_PACKET)
            {
                MpegUtil.nbo_w16(packet,pes + 4, (char) MAX_PES_PACKET);
                n = MAX_PES_PACKET - (p - pes - 6);
            }
            else
            {
                MpegUtil.nbo_w16(packet,pes + 4, (char) ((p - pes - 6) + bytes));
                n = bytes;
            }

            MpegUtil.memcpy(packet,p, payload,payloadIndex, n);
            payloadIndex += n;
            bytes -= n;

            // notify packet already
            i += n + (p - pes);

//		i = 0; // clear value, the next pes packet don't need pack_header
            first = 0; // clear first packet flag
            pts = dts = 0; // only first packet write PTS/DTS
        }

        assert(i < sz);
        r = ps.func.write(ps.param, stream.sid, packet, i);
        ps.func.free(ps.param, packet);

        ++ps.psm_period;
        return r;
    }
    public static PesT ps_stream_find(PsMuxerT ps, int streamid)
    {
        int i;
        for (i = 0; i < ps.psm.stream_count; i++)
        {
            if (streamid == ps.psm.streams[i].sid)
                return ps.psm.streams[i];
        }
        return null;
    }
}
