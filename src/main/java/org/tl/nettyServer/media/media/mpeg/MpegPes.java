package org.tl.nettyServer.media.media.mpeg;


import static org.tl.nettyServer.media.media.mpeg.MpegPesProto.PES_SID_VIDEO;
import static org.tl.nettyServer.media.media.mpeg.MpegTypes.PTS_NO_VALUE;

public class MpegPes {
    /// @return 0-error, pes header length
    public static int pes_write_header(PesT pes, byte[] data,int index,int bytes)
    {
        byte len = 0;
        byte flags = 0x00;
        int p = 0;

        if (bytes < 9) return 0; // error

        // packet_start_code_prefix 0x000001
        data[index+0] = 0x00;
        data[index+1] = 0x00;
        data[index+2] = 0x01;
        data[index+3] = pes.sid;

        // skip PES_packet_length
        //data[4] = 0x00;
        //data[5] = 0x00;

        // '10'
        // PES_scrambling_control '00'
        // PES_priority '0'
        // data_alignment_indicator '1'
        // copyright '0'
        // original_or_copy '0'
        data[index+6] = (byte) 0x80;
        if(pes.data_alignment_indicator > 0)
            data[index+6] |= 0x04;
        //if (IDR | subtitle | raw data)
        //data[6] |= 0x04;

        // PTS_DTS_flag 'xx'
        // ESCR_flag '0'
        // ES_rate_flag '0'
        // DSM_trick_mode_flag '0'
        // additional_copy_info_flag '0'
        // PES_CRC_flag '0'
        // PES_extension_flag '0'
        if(PTS_NO_VALUE != pes.pts)
        {
            flags |= 0x80;  // pts
            len += 5;
        }
        assert(PTS_NO_VALUE == pes.dts || pes.pts == pes.dts || PES_SID_VIDEO == data[3]); // audio PTS==DTS
        if(PTS_NO_VALUE != pes.dts /*&& PES_SID_VIDEO==(PES_SID_VIDEO&data[3])*/ && pes.dts != pes.pts)
        {
            flags |= 0x40;  // dts
            len += 5;
        }
        data[7] = flags;

        // PES_header_data_length : 8
        data[8] = len;

        if (len + 9 > bytes)
            return 0; // error
        p = index + 9;

        if( (flags & 0x80) >0)
        {
		data[p++] = (byte) (((flags >> 2) & 0x30)/* 0011/0010 */ | (((pes.pts >> 30) & 0x07) << 1) /* PTS 30-32 */ | 0x01) /* marker_bit */;
            data[p++] = (byte) ((pes.pts >> 22) & 0xFF); /* PTS 22-29 */
            data[p++] = (byte) (((pes.pts >> 14) & 0xFE) /* PTS 15-21 */ | 0x01) /* marker_bit */;
            data[p++] = (byte) ((pes.pts >> 7) & 0xFF); /* PTS 7-14 */
            data[p++] = (byte) (((pes.pts << 1) & 0xFE) /* PTS 0-6 */ | 0x01) /* marker_bit */;
        }

        if((flags & 0x40) > 0)
        {
            data[p++] = (byte) (0x10 /* 0001 */ | (((pes.dts >> 30) & 0x07) << 1) /* DTS 30-32 */ | 0x01) /* marker_bit */;
            data[p++] = (byte) ((pes.dts >> 22) & 0xFF); /* DTS 22-29 */
            data[p++] = (byte) (((pes.dts >> 14) & 0xFE) /* DTS 15-21 */ | 0x01) /* marker_bit */;
            data[p++] = (byte) ((pes.dts >> 7) & 0xFF); /* DTS 7-14 */
            data[p++] = (byte) (((pes.dts << 1) & 0xFE) /* DTS 0-6 */ | 0x01) /* marker_bit */;
        }

        return p - index;
    }

}
