package org.tl.nettyServer.media.media.mpeg;

public class MpegPackHeader {
    // 2.5.3.3 Pack layer of program stream (p78)
// Table 2-38 - Program stream pack
// Table 2-39 - Program stream pack header
    public int  pack_header_read(PsPackHeaderT h,byte[] data, int bytes)
    {
        byte stuffing_length;
        int header_length;

        if (bytes < 14) return 0;
        assert(0x00 == data[0] && 0x00 == data[1] && 0x01 == data[2] && MpegPesProto.PES_SID_START == data[3]);
        if (0 == (0xC0 & data[4]))
        {
            // MPEG-1
            h.mpeg2 = 0;
            assert(0x20 == (0xF0 & data[4]));
            h.system_clock_reference_base = (((data[4] >> 1) & 0x07) << 30) | (data[5] << 22) | ((data[6] >> 1) << 15) | (data[7] << 7) | (data[8] >> 1);
            h.system_clock_reference_extension = 1;
            h.program_mux_rate = ((data[9] >> 1) << 15) | (data[10] << 7) | (data[11] >> 1);
            return 12;
        }
        else
        {
            h.mpeg2 = 1;
            assert((0x44 & data[4]) == 0x44); // '01xxx1xx'
            assert((0x04 & data[6]) == 0x04); // 'xxxxx1xx'
            assert((0x04 & data[8]) == 0x04); // 'xxxxx1xx'
            assert((0x01 & data[9]) == 0x01); // 'xxxxxxx1'
            h.system_clock_reference_base = (((data[4] >> 3) & 0x07) << 30) | ((data[4] & 0x3) << 28) | (data[5] << 20) | (((data[6] >> 3) & 0x1F) << 15) | ((data[6] & 0x3) << 13) | (data[7] << 5) | ((data[8] >> 3) & 0x1F);
            h.system_clock_reference_extension = ((data[8] & 0x3) << 7) | ((data[9] >> 1) & 0x7F);

            assert((0x03 & data[12]) == 0x03); // 'xxxxxx11'
            h.program_mux_rate = (data[10] << 14) | (data[11] << 6) | ((data[12] >> 2) & 0x3F);

            //assert((0xF8 & data[13]) == 0x00); // '00000xxx'
            stuffing_length = (byte) (data[13] & 0x07); // stuffing

            header_length = 14 + stuffing_length;
            if (header_length > bytes)
                return 0;
            return header_length;
        }
    }

    public static int pack_header_write(PsPackHeaderT h, byte[] data,int index)
    {
        // pack_start_code
        MpegUtil.nbo_w32(data,index, 0x000001BA);

        // 33-system_clock_reference_base + 9-system_clock_reference_extension
        // '01xxx1xx xxxxxxxx xxxxx1xx xxxxxxxx xxxxx1xx xxxxxxx1'
        data[index+4] = (byte) (0x44 | (((h.system_clock_reference_base >> 30) & 0x07) << 3) | ((h.system_clock_reference_base >> 28) & 0x03));
        data[index+5] = (byte) ((h.system_clock_reference_base >> 20) & 0xFF);
        data[index+6] = (byte) (0x04 | (((h.system_clock_reference_base >> 15) & 0x1F) << 3) | ((h.system_clock_reference_base >> 13) & 0x03));
        data[index+7] = (byte) ((h.system_clock_reference_base >> 5) & 0xFF);
        data[index+8] = (byte) (0x04 | ((h.system_clock_reference_base & 0x1F) << 3) | ((h.system_clock_reference_extension >> 7) & 0x03));
        data[index+9] = (byte) (0x01 | ((h.system_clock_reference_extension & 0x7F) << 1));

        // program_mux_rate
        // 'xxxxxxxx xxxxxxxx xxxxxx11'
        data[index+10] = (byte) (h.program_mux_rate >> 14);
        data[index+11] = (byte)(h.program_mux_rate >> 6);
        data[index+12] = (byte)(0x03 | ((h.program_mux_rate & 0x3F) << 2));

        // stuffing length
        // '00000xxx'
        data[index+13] = (byte) 0xF8;

        return 14;
    }

}
