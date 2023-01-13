package org.tl.nettyServer.media.media.mpeg;

public class MpegPsm {
    public static int psm_write(PsmT psm, byte[] data,int index)
    {
        // Table 2-41 - Program stream map(p79)

        int i,j;
        char extlen;
        int crc;

        MpegUtil.nbo_w32(data,index, 0x00000100);
        data[index+3] = (byte) MpegPesProto.PES_SID_PSM;

        // program_stream_map_length 16-bits
        //nbo_w16(data+4, 6+4*psm.stream_count+4);

        // current_next_indicator '1'
        // single_extension_stream_flag '1'
        // reserved '0'
        // program_stream_map_version 'xxxxx'
        data[index+6] = (byte) (0xc0 | (psm.ver & 0x1F));

        // reserved '0000000'
        // marker_bit '1'
        data[index+7] = 0x01;

        extlen = 0;
        extlen += MpegElementDescriptor.service_extension_descriptor_write(data,index + 10 + extlen, 32);
        if(MpegPsEnc.MPEG_CLOCK_EXTENSION_DESCRIPTOR) {
            extlen +=  MpegElementDescriptor.clock_extension_descriptor_write(data,index + 10 + extlen, 32, psm.clock);
        }

        // program_stream_info_length 16-bits
        MpegUtil.nbo_w16(data,index + 8, extlen); // program_stream_info_length = 0

        // elementary_stream_map_length 16-bits
        //nbo_w16(data+10+extlen, psm.stream_count*4);

        j = 12 + extlen;
        for(i = 0; i < psm.stream_count; i++)
        {
            assert(MpegPesProto.PES_SID_EXTEND != psm.streams[i].sid);

            // stream_type:8
            data[index+j++] = psm.streams[i].codecid;
            // elementary_stream_id:8
            data[index+j++] = psm.streams[i].sid;
            // elementary_stream_info_length:16
            MpegUtil.nbo_w16(data,index+j, psm.streams[i].esinfo_len);
            // descriptor()
            System.arraycopy(psm.streams[i].esinfo,0,data,index+j+2,psm.streams[i].esinfo_len);
            //memcpy(data+j+2, psm.streams[i].esinfo, psm.streams[i].esinfo_len);

            j += 2 + psm.streams[i].esinfo_len;
        }

        // elementary_stream_map_length 16-bits
        MpegUtil.nbo_w16(data,index + 10 + extlen, (char) (j - 12 - extlen));
        // program_stream_map_length:16
        MpegUtil.nbo_w16(data,index + 4, (char) (j-6+4)); // 4-bytes crc32

        // crc32
        crc = MpegCrc32.mpeg_crc32(0xffffffff, data,index, j);
        data[index+j+3] = (byte) ((crc >> 24) & 0xFF);
        data[index+j+2] = (byte)((crc >> 16) & 0xFF);
        data[index+j+1] = (byte)((crc >> 8) & 0xFF);
        data[index+j+0] = (byte)(crc & 0xFF);

        return index+j+4;
    }


}
