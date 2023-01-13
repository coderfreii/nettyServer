package org.tl.nettyServer.media.media.mpeg;

public class MpegSystemHeader {
    public static int system_header_write(PsSystemHeaderT h, byte[] data,int index)
    {
        int i, j;

        // system_header_start_code
        MpegUtil.nbo_w32(data,index, 0x000001BB);

        // header length
        //put16(data + 4, 6 + h.stream_count*3);

        // rate_bound
        // 1xxxxxxx xxxxxxxx xxxxxxx1
        data[index+6] = (byte) (0x80 | ((h.rate_bound >> 15) & 0x7F));
        data[index+7] = (byte) ((h.rate_bound >> 7) & 0xFF);
        data[index+8] = (byte) (0x01 | ((h.rate_bound & 0x7F) << 1));

        // 6-audio_bound + 1-fixed_flag + 1-CSPS_flag
        data[index+9] = (byte) (((h.audio_bound & 0x3F) << 2) | ((h.fixed_flag & 0x01) << 1) | (h.CSPS_flag & 0x01));

        // 1-system_audio_lock_flag + 1-system_video_lock_flag + 1-maker + 5-video_bound
        data[index+10] = (byte) (0x20 | ((h.system_audio_lock_flag & 0x01) << 7) | ((h.system_video_lock_flag & 0x01) << 6) | (h.video_bound & 0x1F));

        // 1-packet_rate_restriction_flag + 7-reserved
        data[index+11] = (byte) (0x7F | ((h.packet_rate_restriction_flag & 0x01) << 7));

        i = 12;
        for (j = 0; j < h.stream_count; j++)
        {
            data[index+i++] = (byte) h.streams[j].stream_id;
            if (MpegPesProto.PES_SID_EXTENSION == h.streams[j].stream_id) // '10110111'
            {
                data[index+i++] = (byte) 0xD0; // '11000000'
                data[index+i++] = (byte) (h.streams[j].stream_extid & 0x7F); // '0xxxxxxx'
                data[index+i++] = (byte) 0xB6; // '10110110'
            }

            // '11' + 1-P-STD_buffer_bound_scale + 13-P-STD_buffer_size_bound
            // '11xxxxxx xxxxxxxx'
            data[index+i++] = (byte) (0xC0 | ((h.streams[j].buffer_bound_scale & 0x01) << 5) | ((h.streams[j].buffer_size_bound >> 8) & 0x1F));
            data[index+i++] = (byte) (h.streams[j].buffer_size_bound & 0xFF);
        }

        // header length
        MpegUtil.nbo_w16(data, index+4, (char) (i - 6));
        return i;
    }

}
