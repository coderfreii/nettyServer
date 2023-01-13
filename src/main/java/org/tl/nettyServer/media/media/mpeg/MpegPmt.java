package org.tl.nettyServer.media.media.mpeg;

import java.nio.charset.StandardCharsets;

import static org.tl.nettyServer.media.media.mpeg.MpegTsProto.PSI_STREAM_AUDIO_OPUS;
import static org.tl.nettyServer.media.media.mpeg.MpegTsProto.PSI_STREAM_PRIVATE_DATA;
import static org.tl.nettyServer.media.media.mpegts.Mpegts.TS_PACKET_SIZE;


public class MpegPmt {
    public static int pmt_write(PmtT pmt, byte[] data,int index)
    {
        // 2.4.4.8 Program map table (p68)
        // Table 2-33

        int i = 0;
        int crc = 0;
        //ptrdiff_t len = 0;
        int len = 0;
        int p = 0;

        data[index+0] = MpegTsProto.PAT_TID_PMS;	// program map table

        // skip section_length

        // program_number
        MpegUtil.nbo_w16(data,index + 3, (char) pmt.pn);

        // reserved '11'
        // version_number 'xxxxx'
        // current_next_indicator '1'
        data[index +5] = (byte) (0xC1 | (pmt.ver << 1));

        // section_number/last_section_number
        data[index +6] = 0x00;
        data[index +7] = 0x00;

        // reserved '111'
        // PCR_PID 13-bits 0x1FFF
        MpegUtil.nbo_w16(data,index + 8, (char)(0xE000 | pmt.PCR_PID));

        // reserved '1111'
        // program_info_length 12-bits, the first two bits of which shall be '00'.
        assert(pmt.pminfo_len < 0x400);
        MpegUtil.nbo_w16(data,index + 10, (char) (0xF000 | pmt.pminfo_len));
        if(pmt.pminfo_len > 0 && pmt.pminfo_len < 0x400)
        {
            // fill program info
            //assert(pmt.pminfo);
            MpegUtil.memcpy(data,index + 12, pmt.pminfo, pmt.pminfo_len);
        }

        // streams
        p = index + 12 + pmt.pminfo_len;
        for(i = 0; i < pmt.stream_count && p - index < 1021 - 4 - 5 - pmt.streams[i].esinfo_len; i++)
        {
            // stream_type
		p = (PSI_STREAM_AUDIO_OPUS == pmt.streams[i].codecid) ? PSI_STREAM_PRIVATE_DATA : pmt.streams[i].codecid;

            // reserved '111'
            // elementary_PID 13-bits
            MpegUtil.nbo_w16(data,p + 1, (char) (0xE000 | pmt.streams[i].pid));

            len = 0;
            // fill elementary stream info
            //if(PSI_STREAM_AUDIO_OPUS == pmt.streams[i].codecid || (pmt.streams[i].esinfo_len > 0 && pmt.streams[i].esinfo))
            {
                //assert(pmt.streams[i].esinfo);
                //memcpy(p, pmt.streams[i].esinfo, pmt.streams[i].esinfo_len);
                //p += pmt.streams[i].esinfo_len;
                len = pmt_write_descriptor(pmt.streams[i],data, p + 5, 1021 - (int)(p + 5 - index));
            }

            // reserved '1111'
            // ES_info_lengt 12-bits
            MpegUtil.nbo_w16(data,p + 3, (char) (0xF000 | len));
            p += 5 + len;
        }

        // section_length
        len = p + 4 - (index + 3); // 4 bytes crc32
        assert(len <= 1021); // shall not exceed 1021 (0x3FD).
        assert(len <= TS_PACKET_SIZE - 7);
        // section_syntax_indicator '1'
        // '0'
        // reserved '11'
        MpegUtil.nbo_w16(data,index + 1, (char) (0xb000 | len));

        // crc32
        crc = MpegCrc32.mpeg_crc32(0xffffffff, data,index, p-index);
        //put32(p, crc);
        data[p+index +3] = (byte) ((crc >> 24) & 0xFF);
        data[p+index +2] = (byte) ((crc >> 16) & 0xFF);
        data[p+index +1] = (byte) ((crc >> 8) & 0xFF);
        data[p+index +0] = (byte) (crc & 0xFF);

        return (p - index) + 4; // total length
    }

    public static int pmt_write_descriptor(PesT stream,byte[] data,int index, int bytes)
    {
        int p;

        p = index;
        if (PSI_STREAM_AUDIO_OPUS == stream.codecid && bytes > 2 + 4 /*fourcc*/ + 4 /*DVI OPUS*/ )
        {
            data[p++] = 0x05; // 2.6.8 Registration descriptor(p94)
            data[p++] = 4;
            MpegUtil.memcpy(data,p, "Opus".getBytes(StandardCharsets.UTF_8), 4);
            p += 4;

            data[p++] = 0x7f; // DVB-Service Information: 6.1 Descriptor identification and location (p38)
            data[p++] = 2;
            data[p++] = (byte) MpegTsOpus.OPUS_EXTENSION_DESCRIPTOR_TAG;
            data[p++] = stream.esinfo_len > 8 ? stream.esinfo[9] : 2 /*0xFF*/ ; // default 2-channels
        }

        return p - index;
    }

}
