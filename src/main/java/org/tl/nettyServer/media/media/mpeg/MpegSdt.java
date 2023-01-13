package org.tl.nettyServer.media.media.mpeg;

import java.nio.charset.StandardCharsets;

import static org.tl.nettyServer.media.media.mpegts.Mpegts.TS_PACKET_SIZE;


public class MpegSdt {

    public static String SERVICE_ENCODER = "encoder";
    public static int sdt_write(PatT pat, byte[] data,int index)
    {
        int i, j;
        int len, s1, n1, v1;
        int crc = 0;

        n1 = SERVICE_ENCODER.length();
        v1 = MpegElementDescriptor.SERVICE_NAME.length();
        s1 = 3 /*tag*/ + 1 + n1 + 1 + v1;
        len = 3 /*nid*/ + s1 + 5 /*service head*/ + 5 + 4; // 5 bytes remain header and 4 bytes crc32

        // shall not exceed 1021 (0x3FD).
        assert(len <= 1021);
        assert(len <= TS_PACKET_SIZE - 7);

        data[index+0] = MpegTsProto.PAT_TID_SDT;	// service_description_section

        // section_syntax_indicator = '1'
        // '0'
        // reserved '11'
        MpegUtil.nbo_w16(data, index+ 1, (char) (0xf000 | len));

        // transport_stream_id
        MpegUtil.nbo_w16(data,index + 3, (char)pat.tsid);

        // reserved '11'
        // version_number 'xxxxx'
        // current_next_indicator '1'
        data[index+5] = (byte) (0xC1 | (pat.ver << 1));

        // section_number/last_section_number
        data[index+6] = 0x00;
        data[index+7] = 0x00;

        // original_network_id
        MpegUtil.nbo_w16(data,index + 8, (char) pat.tsid);
        data[index+10] = (byte) 0xFF; // reserved

        j = 11;
        // only one
        for (i = 0; i < 1; i++)
        {
            MpegUtil.nbo_w16(data,index + j, (char)pat.tsid);
            data[index+j + 2] = (byte) (0xfc | 0x00); // no EIT

            assert(n1 < 255 && v1 < 255 && len < 255);
            MpegUtil.nbo_w16(data,index + j + 3, (char)(0x8000 | s1));

            data[index+j + 5] = 0x48; // tag id
            data[index+j + 6] = (byte) (3 + n1 + v1); // tag len
            data[index+j + 7] = 1; // service type
            data[index+j + 8] = (byte) n1;
            MpegUtil.memcpy(data,index + j + 9, MpegElementDescriptor.SERVICE_NAME.getBytes(StandardCharsets.UTF_8),n1);
            data[index+j + 9 + n1] = (byte) v1;
            MpegUtil.memcpy(data,index + j + 10 + n1,MpegElementDescriptor.SERVICE_NAME.getBytes(StandardCharsets.UTF_8),v1 );
            j += 10 + v1 + n1;
        }

        // crc32
        crc = MpegCrc32.mpeg_crc32(0xffffffff, data,index,j);
        data[index+j + 3] = (byte) ((crc >> 24) & 0xFF);
        data[index+j + 2] = (byte)((crc >> 16) & 0xFF);
        data[index+j + 1] = (byte)((crc >> 8) & 0xFF);
        data[index+j + 0] = (byte)(crc & 0xFF);
        return j + 4;
    }

}
