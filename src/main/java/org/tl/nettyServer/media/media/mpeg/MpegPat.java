package org.tl.nettyServer.media.media.mpeg;


import static org.tl.nettyServer.media.media.mpeg.MpegTsProto.PAT_TID_PAS;
import static org.tl.nettyServer.media.media.mpegts.Mpegts.TS_PACKET_SIZE;

public class MpegPat {
    public static PmtT pat_alloc_pmt(PatT pat)
    {
        PmtT[] ptr;
        int n;

        if (null == pat.pmts)
        {
            assert(0 == pat.pmt_count);
            assert(0 == pat.pmt_capacity);
            pat.pmts = pat.pmt_default;
            pat.pmt_capacity = pat.pmt_default.length;
        }

        if (pat.pmt_count >= pat.pmt_capacity)
        {
            if (pat.pmt_count + 1 > 65535)
            {
                //assert(0);
                return null;
            }

            n = pat.pmt_capacity + pat.pmt_capacity / 4 + 4;
            ptr = pat.pmts == pat.pmt_default ? null : pat.pmts;
            if (ptr == null)
                return null;

            if (pat.pmts == pat.pmt_default)
                ptr = pat.pmt_default;
                //memmove(ptr, pat.pmt_default, sizeof(pat.pmt_default));
            pat.pmts = ptr;
            pat.pmt_capacity = n;
        }
        // new pmt
        //memset(pat.pmts[pat.pmt_count], 0, sizeof(pat.pmts[0]));
        return pat.pmts[pat.pmt_count];
    }
    public static int pat_write(PatT pat, byte[] data,int index)
    {
        // Table 2-30 Program association section(p65)

        int i = 0;
        int len = 0;
        int crc = 0;

        len = pat.pmt_count * 4 + 5 + 4; // 5 bytes remain header and 4 bytes crc32

        // shall not exceed 1021 (0x3FD).
        assert(len <= 1021);
        assert(len <= TS_PACKET_SIZE - 7);

        data[index+0] = PAT_TID_PAS;	// program association table

        // section_syntax_indicator = '1'
        // '0'
        // reserved '11'
        MpegUtil.nbo_w16(data,index + 1, (char) (0xb000 | len));

        // transport_stream_id
        MpegUtil.nbo_w16(data,index + 3, (char)pat.tsid);

        // reserved '11'
        // version_number 'xxxxx'
        // current_next_indicator '1'
        data[index +5] = (byte) (0xC1 | (pat.ver << 1));

        // section_number/last_section_number
        data[index +6] = 0x00;
        data[index +7] = 0x00;

        for(i = 0; i < pat.pmt_count; i++)
        {
            MpegUtil.nbo_w16(data,index + 8 + i * 4 + 0, (char)pat.pmts[i].pn);
            MpegUtil.nbo_w16(data,index + 8 + i * 4 + 2, (char)(0xE000 | pat.pmts[i].pid));
        }

        // crc32
        crc = MpegCrc32.mpeg_crc32(0xffffffff, data, index,len-1);
        //put32(data + section_length - 1, crc);
        data[index +len - 1 + 3] = (byte) ((crc >> 24) & 0xFF);
        data[index +len - 1 + 2] = (byte) ((crc >> 16) & 0xFF);
        data[index +len - 1 + 1] = (byte) ((crc >> 8) & 0xFF);
        data[index +len - 1 + 0] = (byte) (crc & 0xFF);

        return len + 3; // total length
    }

}
