package org.tl.nettyServer.media.media.mpeg;


import static org.tl.nettyServer.media.media.mpeg.MpegTsProto.PSI_STREAM_H264;
import static org.tl.nettyServer.media.media.mpeg.MpegTsProto.PSI_STREAM_H265;
import static org.tl.nettyServer.media.media.mpeg.MpegTypes.PTS_NO_VALUE;
import static org.tl.nettyServer.media.media.mpegts.Mpegts.TS_PACKET_SIZE;

public class MpegTsEnc {

    public static int PCR_DELAY = 0; //(700 * 90) // 700ms
    public static int PAT_PERIOD = (400 * 90); // 500ms
    public static int PAT_CYCLE = 50; // 50fps(audio + video)

    public static int TS_HEADER_LEN = 4; // 1-bytes sync byte + 2-bytes PID + 1-byte CC
    public static int PES_HEADER_LEN = 6; // 3-bytes packet_start_code_prefix + 1-byte stream_id + 2-bytes PES_packet_length

    public static int TS_PAYLOAD_UNIT_START_INDICATOR = 0x40;

    // adaptation flags
    public static int AF_FLAG_PCR = 0x10;
    public static int AF_FLAG_RANDOM_ACCESS_INDICATOR = 0x40; // random_access_indicator

    public static MpegTsEncContextT mpeg_ts_create(MpegTsFuncT func, MpegMuxerT param)
    {
        MpegTsEncContextT tsctx = new MpegTsEncContextT();

        //assert(func);
        //tsctx = (mpeg_ts_enc_context_t *)calloc(1, sizeof(mpeg_ts_enc_context_t));
        if(tsctx == null)
            return null;

        mpeg_ts_reset(tsctx);

        tsctx.pat.tsid = 1;
        tsctx.pat.ver = 0x00;
        tsctx.pat.cc = 0;
        tsctx.pid = 0x100;

        //tsctx.pat.pmt_count = 1; // only one program in ts
        //tsctx.pat.pmts[0].pid = 0x100;
        //tsctx.pat.pmts[0].pn = 1;
        //tsctx.pat.pmts[0].ver = 0x00;
        //tsctx.pat.pmts[0].cc = 0;
        //tsctx.pat.pmts[0].pminfo_len = 0;
        //tsctx.pat.pmts[0].pminfo = NULL;
        //tsctx.pat.pmts[0].PCR_PID = 0x1FFF; // 0x1FFF-don't set PCR

        //tsctx.pat.pmts[0].stream_count = 2; // H.264 + AAC
        //tsctx.pat.pmts[0].streams[0].pid = 0x101;
        //tsctx.pat.pmts[0].streams[0].sid = PES_SID_AUDIO;
        //tsctx.pat.pmts[0].streams[0].codecid = PSI_STREAM_AAC;
        //tsctx.pat.pmts[0].streams[1].pid = 0x102;
        //tsctx.pat.pmts[0].streams[1].sid = PES_SID_VIDEO;
        //tsctx.pat.pmts[0].streams[1].codecid = PSI_STREAM_H264;
        tsctx.func = func;
        //memcpy(&tsctx.func, func, sizeof(tsctx.func));
        tsctx.param = param;
        return tsctx;
    }
    public  static int mpeg_ts_reset(MpegTsEncContextT ts)
    {
        MpegTsEncContextT tsctx = ts;
        //tsctx = (mpeg_ts_enc_context_t*)ts;
        //	tsctx.sdt_period = 0;
        tsctx.pat_period = 0;
        tsctx.pcr_period = 80 * 90; // 100ms maximum
        tsctx.pcr_clock = 0;
        tsctx.pat_cycle = 0;
        return 0;
    }
    public static int mpeg_ts_destroy(MpegTsEncContextT ts)
    {
        int i;
        PmtT pmt;
        MpegTsEncContextT tsctx = ts;

        for(i = 0; i < tsctx.pat.pmt_count; i++)
        {
            pmt = tsctx.pat.pmts[i];
            mpeg_ts_pmt_destroy(pmt);
        }

        if (tsctx.pat.pmts !=null && tsctx.pat.pmts != tsctx.pat.pmt_default)
            tsctx.pat.pmts = null;//free(tsctx.pat.pmts);
        tsctx = null;//free(tsctx);
        return 0;
    }
    public static void mpeg_ts_pmt_destroy(PmtT pmt)
    {
        int i;
        for (i = 0; i < pmt.stream_count; i++)
        {
            if (pmt.streams[i].esinfo != null)
                pmt.streams[i].esinfo= null;//free(pmt.streams[i].esinfo);
        }

        if (pmt.pminfo != null)
            pmt.pminfo = null;//free(pmt.pminfo);
    }
    public static int mpeg_ts_add_stream(MpegTsEncContextT ts, int codecid,byte[] extra_data, int extra_data_size)
    {
        PmtT pmt = null;
        MpegTsEncContextT tsctx = ts;
        if (0 == tsctx.pat.pmt_count)
        {
            // add default program
            if (0 != mpeg_ts_add_program(tsctx, (char) 1, null, 0))
                return -1;
        }
        pmt = tsctx.pat.pmts[0];

        return mpeg_ts_pmt_add_stream(tsctx, pmt, codecid, extra_data, extra_data_size);
    }
    public static int mpeg_ts_add_program(MpegTsEncContextT ts, char pn, byte[] info, int bytes)
    {
        int i;
        PmtT pmt;
        MpegTsEncContextT tsctx;

        if (pn < 1 || bytes < 0 || bytes >= (1 << 12))
            return -1; // EINVAL: pminfo-len 12-bits

        tsctx = ts;
        for (i = 0; i < tsctx.pat.pmt_count; i++)
        {
            pmt = tsctx.pat.pmts[i];
            if (pmt.pn == pn)
                return -1; // EEXIST
        }

        assert(tsctx.pat.pmt_count == i);
        pmt = MpegPat.pat_alloc_pmt(tsctx.pat);
        if (pmt == null)
            return -1; // E2BIG

        pmt.pid = tsctx.pid++;
        pmt.pn = pn;
        pmt.ver = 0x00;
        pmt.cc = 0;
        pmt.PCR_PID = 0x1FFF; // 0x1FFF-don't set PCR

        if (bytes > 0 && info.length>0)
        {
            pmt.pminfo = new byte[bytes];
            if (pmt.pminfo == null)
                return -1; // ENOMEM
            System.arraycopy(info,0,pmt.pminfo,0,bytes);
            //memcpy(pmt.pminfo, info, bytes);
            pmt.pminfo_len = bytes;
        }

        tsctx.pat.pmt_count++;
        mpeg_ts_reset(ts); // update PAT/PMT
        return 0;
    }
    static int mpeg_ts_pmt_add_stream(MpegTsEncContextT ts, PmtT pmt, int codecid, byte[] extra_data, int extra_data_size)
    {
        PesT stream = null;
        if (ts == null || pmt == null || pmt.stream_count >= pmt.streams.length)
        {
            //assert(0);
            return -1;
        }

        stream = pmt.streams[pmt.stream_count];
        stream.codecid = (byte) codecid;
        stream.pid = (char) ts.pid++;
        stream.esinfo_len = 0;
        stream.esinfo = null;

        // stream id
        // Table 2-22 - Stream_id assignments
        if (MpegUtil.mpeg_stream_type_video(codecid) > 0)
        {
            // Rec. ITU-T H.262 | ISO/IEC 13818-2, ISO/IEC 11172-2, ISO/IEC 14496-2
            // or Rec. ITU-T H.264 | ISO/IEC 14496-10 video stream number
            stream.sid = (byte) MpegPesProto.PES_SID_VIDEO;
        }
        else if (MpegUtil.mpeg_stream_type_audio(codecid) > 0)
        {
            // ISO/IEC 13818-3 or ISO/IEC 11172-3 or ISO/IEC 13818-7 or ISO/IEC 14496-3
            // audio stream number
            stream.sid = (byte) MpegPesProto.PES_SID_AUDIO;
        }
        else
        {
            // private_stream_1
            stream.sid = (byte) MpegPesProto.PES_SID_PRIVATE_1;
        }

        if (extra_data_size > 0 && extra_data.length > 0)
        {
            stream.esinfo = new byte[extra_data_size];
            System.arraycopy(extra_data,0,stream.esinfo,0,extra_data_size);
            //memcpy(stream.esinfo, extra_data, extra_data_size);
            stream.esinfo_len = (char) extra_data_size;
        }
        pmt.stream_count++;
        pmt.ver = (pmt.ver + 1) % 32;
        mpeg_ts_reset(ts); // immediate update pat/pmt
        return stream.pid;
    }

    public static int mpeg_ts_write(MpegTsEncContextT ts, int pid, int flags, long pts, long dts, byte[] data,int index, int bytes)
    {
        int r = 0;
        int i, n;
        PmtT pmt = null;
        PesT stream = null;
        MpegTsEncContextT tsctx = ts;
        stream = mpeg_ts_find(tsctx, pid,pmt);
        if (null == stream)
            return -2; // not found

        stream.pts = pts;
        stream.dts = dts;
        stream.data_alignment_indicator = (flags & MpegTsProto.MPEG_FLAG_IDR_FRAME) > 0 ? 1 : 0; // idr frame
        tsctx.h264_h265_with_aud = (flags & MpegTsProto.MPEG_FLAG_H264_H265_WITH_AUD) >0 ? 1 : 0;

        // set PCR_PID
        //assert(1 == tsctx.pat.pmt_count);
        if (0x1FFF == pmt.PCR_PID || (MpegPesProto.PES_SID_VIDEO == (stream.sid & MpegPesProto.PES_SID_VIDEO) && pmt.PCR_PID != stream.pid))
        {
            pmt.PCR_PID = stream.pid;
            tsctx.pat_period = 0;
            tsctx.pat_cycle = 0;
        }

        if (pmt.PCR_PID == stream.pid)
            ++tsctx.pcr_clock;

        if(0 == ++tsctx.pat_cycle % PAT_CYCLE || 0 == tsctx.pat_period || tsctx.pat_period + PAT_PERIOD <= dts)
        {
            tsctx.pat_cycle = 0;
            tsctx.pat_period = dts;

            if (0 == tsctx.sdt_period)
            {
                // SDT
                tsctx.sdt_period = dts;
                n = MpegSdt.sdt_write(tsctx.pat, tsctx.payload,0);
                r = mpeg_ts_write_section_header(ts, MpegTsProto.TS_PID_SDT,tsctx.pat /*fixme*/ , tsctx.payload, n);
                if (0 != r) return r;
            }

            // PAT(program_association_section)
            n = MpegPat.pat_write(tsctx.pat, tsctx.payload,0);
            r = mpeg_ts_write_section_header(ts, MpegTsProto.TS_PID_PAT, tsctx.pat, tsctx.payload, n); // PID = 0x00 program association table
            if (0 != r) return r;

            // PMT(Transport stream program map section)
            for(i = 0; i < tsctx.pat.pmt_count; i++)
            {
                n = MpegPmt.pmt_write(tsctx.pat.pmts[i], tsctx.payload,0);
                r = mpeg_ts_write_section_header(ts, tsctx.pat.pmts[i].pid, tsctx.pat.pmts[i], tsctx.payload, n);
                if (0 != r) return r;
            }
        }

        return ts_write_pes(tsctx, pmt, stream, data,index, bytes);
    }
    static PesT mpeg_ts_find(MpegTsEncContextT ts, int pid,PmtT pmt)
    {
        int i, j;
        PesT stream;

        for (i = 0; i < ts.pat.pmt_count; i++)
        {
        pmt = ts.pat.pmts[i];
            for (j = 0; j < pmt.stream_count; j++)
            {
                stream = pmt.streams[j];
                if (pid == (int)stream.pid)
                    return stream;
            }
        }

        return null;
    }
    static int mpeg_ts_write_section_header(MpegTsEncContextT ts, int pid,Object patt,byte[] payload, int len)
    {
        int r;
        byte[] data = null;
        data = ts.func.alloc(ts.param, TS_PACKET_SIZE);
        if(data == null) return -12;

        assert(len < TS_PACKET_SIZE - 5); // TS-header + pointer

        // TS Header

        // sync_byte
        data[0] = 0x47;
        // transport_error_indicator = 0
        // payload_unit_start_indicator = 1
        // transport_priority = 0
        data[1] = (byte) (0x40 | ((pid >> 8) & 0x1F));
        data[2] = (byte) (pid & 0xFF);
        // transport_scrambling_control = 0x00
        // adaptation_field_control = 0x01-No adaptation_field, payload only, 0x03-adaptation and payload
        if(patt instanceof PatT){
            PatT pat = (PatT) patt;
            data[3] = (byte) (0x10 | (pat.cc & 0x0F));
            pat.cc = (pat.cc + 1) % 16; // update continuity_counter
        }
        if(patt instanceof PmtT){
            PmtT pat = (PmtT) patt;
            data[3] = (byte) (0x10 | (pat.cc & 0x0F));
            pat.cc = (pat.cc + 1) % 16; // update continuity_counter
        }

//	// Adaptation
//	if(len < TS_PACKET_SIZE - 5)
//	{
//		data[3] |= 0x20; // with adaptation
//		data[4] = TS_PACKET_SIZE - len - 5 - 1; // 4B-Header + 1B-pointer + 1B-self
//		if(data[4] > 0)
//		{
//			// adaptation
//			data[5] = 0; // no flag
//			memset(data+6, 0xFF, data[4]-1);
//		}
//	}

        // pointer (payload_unit_start_indicator==1)
        //data[TS_PACKET_SIZE-len-1] = 0x00;
        data[4] = 0x00;

        // TS Payload
        //memmove(data + TS_PACKET_SIZE - len, payload, len);
        MpegUtil.memmove(data, + 5, payload, len);
        MpegUtil.memset(data,5+len, (byte) 0xff, TS_PACKET_SIZE-len-5);

        r = ts.func.write(ts.param, data, TS_PACKET_SIZE);
        ts.func.free(ts.param, data);
        return r;
    }
    static int ts_write_pes(MpegTsEncContextT tsctx, PmtT pmt, PesT stream, byte[] payload,int index, int bytes)
    {
        // 2.4.3.6 PES packet
        // Table 2-21

        int r = 0;
        int len = 0;
        int start = 1; // first packet
        int p ;
        byte[] data = null;
        int header;
        int payloadIndex =0;

        while(0 == r && bytes > 0)
        {
            data = tsctx.func.alloc(tsctx.param, TS_PACKET_SIZE);
            if(data == null) return -12;

            // TS Header
            data[0] = 0x47;	// sync_byte
            data[1] = (byte) (0x00 | ((stream.pid >>8) & 0x1F));
            data[2] = (byte) (stream.pid & 0xFF);
            data[3] = (byte) (0x10 | (stream.cc & 0x0F)); // no adaptation, payload only
            data[4] = 0; // clear adaptation length
            data[5] = 0; // clear adaptation flags

            stream.cc = (byte) ((stream.cc + 1) % 16);

            // 2.7.2 Frequency of coding the program clock reference
            // http://www.bretl.com/mpeghtml/SCR.HTM
            // the maximum between PCRs is 100ms.
            if(start >0 && stream.pid == pmt.PCR_PID)
            {
                data[3] |= 0x20; // +AF
                data[5] |= AF_FLAG_PCR; // +PCR_flag
            }

            // random_access_indicator
            if(start > 0 && stream.data_alignment_indicator > 0 && PTS_NO_VALUE != stream.pts)
            {
                //In the PCR_PID the random_access_indicator may only be set to '1'
                //in a transport stream packet containing the PCR fields.
                data[3] |= 0x20; // +AF
                data[5] |= AF_FLAG_RANDOM_ACCESS_INDICATOR; // +random_access_indicator
            }

            if((data[3] & 0x20) > 0)
            {
                data[4] = 1; // 1-byte flag

                if((data[5] & AF_FLAG_PCR) > 0) // PCR_flag
                {
                    long pcr = 0;
                    pcr = (PTS_NO_VALUE==stream.dts) ? stream.pts : stream.dts;
                    MpegUtil.pcr_write(data,  6, (pcr - PCR_DELAY) * 300); // TODO: delay???
                    data[4] += 6; // 6-PCR
                }

                header = 0 + TS_HEADER_LEN + 1 + data[4]; // 4-TS + 1-AF-Len + AF-Payload
            }
            else
            {
                header = 0 + TS_HEADER_LEN;
            }

            p = header;

            // PES header
            if(start > 0)
            {
                data[1] |= TS_PAYLOAD_UNIT_START_INDICATOR; // payload_unit_start_indicator

                p += MpegPes.pes_write_header(stream, data,header, TS_PACKET_SIZE - (header/* - data*/));

                if(PSI_STREAM_H264 == stream.codecid && tsctx.h264_h265_with_aud != 0)
                {
                    // 2.14 Carriage of Rec. ITU-T H.264 | ISO/IEC 14496-10 video
                    // Each AVC access unit shall contain an access unit delimiter NAL Unit
                    MpegUtil.nbo_w32(data,p, 0x00000001);
                    data[p+4] = 0x09; // AUD
                    data[p+5] = (byte) 0xF0; // any slice type (0xe) + rbsp stop one bit
                    p += 6;
                }
                else if (PSI_STREAM_H265 == stream.codecid && tsctx.h264_h265_with_aud != 0)
                {
                    // 2.17 Carriage of HEVC
                    // Each HEVC access unit shall contain an access unit delimiter NAL unit.
                    MpegUtil.nbo_w32(data,p, 0x00000001);
                    data[p+4] = 0x46; // 35-AUD_NUT
                    data[p+5] = 0x01;
                    data[p+6] = 0x50; // B&P&I (0x2) + rbsp stop one bit
                    p += 7;
                }

                // PES_packet_length = PES-Header + Payload-Size
                // A value of 0 indicates that the PES packet length is neither specified nor bounded
                // and is allowed only in PES packets whose payload consists of bytes from a
                // video elementary stream contained in transport stream packets
                if((p - header - PES_HEADER_LEN) + bytes > 0xFFFF)
                    MpegUtil.nbo_w16(data,header + 4, (char) 0); // 2.4.3.7 PES packet => PES_packet_length
                else
                    MpegUtil.nbo_w16(data,header + 4, (char) ((p - header - PES_HEADER_LEN) + bytes));
            }

            len = p ;//- data; // TS + PES header length
            if(len + bytes < TS_PACKET_SIZE)
            {
                // stuffing_len = TS_PACKET_SIZE - (len + bytes)

                // move pes header
                if(p - header > 0)
                {
                    //assert(start);
                    MpegUtil.memmove(data , (TS_PACKET_SIZE - bytes - (p - header)), data,header, p - header);
                }

                // adaptation
                if((data[3] & 0x20) > 0) // has AF?
                {
                    assert(0 != data[5] && data[4] > 0);
                    MpegUtil.memset(data , TS_HEADER_LEN + 1 + data[4], (byte) 0xFF, TS_PACKET_SIZE - (len + bytes));
                    data[4] += (byte) (TS_PACKET_SIZE - (len + bytes));
                }
                else
                {
                    assert(len == (p - header) + TS_HEADER_LEN);
                    data[3] |= 0x20; // +AF
                    data[4] = (byte) (TS_PACKET_SIZE - (len + bytes) - 1/*AF length*/);
                    if (data[4] > 0) data[5] = 0; // no flag
                    if (data[4] > 1) MpegUtil.memset(data, 6, (byte) 0xFF, TS_PACKET_SIZE - (len + bytes) - 2);
                }
                len = bytes;

                p = /*data +*/ 5 + data[4] + (p - header);
            }
            else
            {
                len = TS_PACKET_SIZE - len;
            }

            // payload
            MpegUtil.memcpy(data,p, payload,index+payloadIndex, len);

            payloadIndex += len;
            bytes -= len;
            start = 0;
            // send with TS-header
            r = tsctx.func.write(tsctx.param, data, TS_PACKET_SIZE);
            tsctx.func.free(tsctx.param, data);
        }

        return r;
    }
    public static int mpeg_ts_add_program_stream(MpegTsEncContextT ts, char pn, int codecid, byte[] extra_data, int extra_data_size)
    {
        int i;
        PmtT pmt = null;
        MpegTsEncContextT tsctx=ts;
        for (i = 0; i < tsctx.pat.pmt_count; i++)
        {
            pmt = tsctx.pat.pmts[i];
            if (pmt.pn == pn)
                return mpeg_ts_pmt_add_stream(tsctx, pmt, codecid, extra_data, extra_data_size);
        }

        return -1; // ENOTFOUND: program not found
    }
    public static int mpeg_ts_remove_program(MpegTsEncContextT ts, char pn)
    {
        int i;
        PmtT pmt = null;
        MpegTsEncContextT tsctx = ts;
        for (i = 0; i < tsctx.pat.pmt_count; i++)
        {
            pmt = tsctx.pat.pmts[i];
            if (pmt.pn != pn)
                continue;

            mpeg_ts_pmt_destroy(pmt);

            if (i + 1 < tsctx.pat.pmt_count)
                tsctx.pat.pmts[i] = tsctx.pat.pmts[i + 1];
                //MpegUtil.memmove(tsctx.pat.pmts[i],0, tsctx.pat.pmts[i + 1], (tsctx.pat.pmt_count - i - 1) /** tsctx.pat.pmts[0]*/);
            tsctx.pat.pmt_count--;
            mpeg_ts_reset(ts); // update PAT/PMT
            return 0;
        }

        return -1; // ENOTFOUND
    }

}
