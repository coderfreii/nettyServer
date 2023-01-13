package org.tl.nettyServer.media.media.mpegts;


import org.tl.nettyServer.media.media.mpegts.crc.AVCRCId;
import org.tl.nettyServer.media.media.mpegts.crc.Crc;
import org.tl.nettyServer.media.media.mpegts.entity.AVFormatContext;
import org.tl.nettyServer.media.media.mpegts.entity.AVPacket;
import org.tl.nettyServer.media.media.mpegts.entity.AVStream;
import org.tl.nettyServer.media.media.mpegts.util.Bswap;

public class Mpegtsenc implements Mpegts{
    /* mpegts section writer */
    MpegTSSection MpegTSSection;
    MpegTSService MpegTSService;
    MpegTSWrite MpegTSWrite;
    /* a PES packet header is generated every DEFAULT_PES_HEADER_FREQ packets */
    int DEFAULT_PES_HEADER_FREQ = 16;
    int DEFAULT_PES_PAYLOAD_SIZE = ((DEFAULT_PES_HEADER_FREQ - 1) * 184 + 170);
    /* The section length is 12 bits. The first 2 are set to 0, the remaining
     * 10 bits should not exceed 1021. */
    int SECTION_LENGTH = 1020;
    /* NOTE: 4 bytes must be left at the end for the crc32 */
    static void mpegts_write_section(MpegTSSection s, byte[] buf, int len)
    {
         int crc;
         byte[] packet = new byte[TS_PACKET_SIZE];
         int buf_ptr;
         byte[] q;
         boolean first;
         int b, len1, left;

         crc = Bswap.av_bswap32(Crc.av_crc(Crc.av_crc_get_table(AVCRCId.AV_CRC_32_IEEE),
                -1, buf, len - 4));

        buf[len - 4] = (byte) ((crc >> 24) & 0xff);
        buf[len - 3] = (byte) ((crc >> 16) & 0xff);
        buf[len - 2] = (byte) ((crc >>  8) & 0xff);
        buf[len - 1] = (byte) (crc        & 0xff);

        /* send each packet */
        buf_ptr = 0;
        int packetIndex = 0;
        while (len > 0) {
            first = buf_ptr == 0;
            q     = packet;
            q[packetIndex++] = 0x47;
            b     = s.getPid() >> 8;
            if (first)
                b |= 0x40;
            q[packetIndex++] = (byte) b;
            q[packetIndex++]  = (byte) s.getPid();
            s.setCc(s.getCc()+ 1 & 0xf);
            q[packetIndex++]= (byte) (0x10 | s.getCc());
            if (s.getDiscontinuity() != 0) {
                q[-1] |= 0x20;
                q[packetIndex++] = 1;
                q[packetIndex++] = (byte) 0x80;
                s.setDiscontinuity(0);
            }
            if (first)
                q[packetIndex++] = 0; /* 0 offset */
            len1 = TS_PACKET_SIZE - (packetIndex+1);
            if (len1 > len)
                len1 = len;
            System.arraycopy(buf,buf_ptr,q,packetIndex,len1);
           // memcpy(q, buf_ptr, len1);
            packetIndex += len1;
            /* add known padding data */
            left = TS_PACKET_SIZE - (packetIndex+1);
            if (left > 0)
                for(int i=packetIndex+1;i<left;i++){
                    q[i] = (byte) 0xff;
                }
                //memset(q, 0xff, left);
            s.write_packet(s, packet);
            buf_ptr += len1;
            len     -= len1;
        }
    }

    /**
     * 保存val后16位
     * @param q_ptr
     * @param val
     */
    static int put16(int q_ptr, int val){
       return q_ptr | (val & 0xffff);
    }

    static int mpegts_write_section1(MpegTSSection s, byte tid, int id,
                                     byte version, byte sec_num, byte last_sec_num,
                                     byte[] buf, int len)
    {
        byte[] section = new byte[1024];
        int q;
        int tot_len;
        /* reserved_future_use field must be set to 1 for SDT and NIT */
        int flags = (tid == SDT_TID || tid == NIT_TID) ? 0xf000 : 0xb000;

        tot_len = 3 + 5 + len + 4;
        /* check if not too big */
        if (tot_len > 1024)
            return 0;//AVERROR_INVALIDDATA;//'I','N','D','A'

        q    = 0;
        section[q++] = tid;
        put16(section[q], flags | (len + 5 + 4)); /* 5 byte header + 4 byte CRC */
        put16(section[q], id);
        section[q++] = (byte) (0xc1 | (version << 1)); /* current_next_indicator = 1 */
        section[q++] = sec_num;
        section[q++] = last_sec_num;
        System.arraycopy(buf,0,section,q,len);
        //memcpy(q, buf, len);
        mpegts_write_section(s, section, tot_len);
        return 0;
    }

    /*********************************************/
    /* mpegts writer */

    String DEFAULT_PROVIDER_NAME = "FFmpeg";
    String DEFAULT_SERVICE_NAME = "Service";

    /* we retransmit the SI info at this rate */
    int SDT_RETRANS_TIME = 500;
    int PAT_RETRANS_TIME = 100;
    int PCR_RETRANS_TIME = 20;
    int NIT_RETRANS_TIME = 500;
    MpegTSWriteStream MpegTSWriteStream;


    @Override
    public MpegTSContext avpriv_mpegts_parse_open(AVFormatContext s) {
        return null;
    }

    @Override
    public int avpriv_mpegts_parse_packet(MpegTSContext ts, AVPacket pkt, byte[] buf, int len) {
        return 0;
    }

    @Override
    public void avpriv_mpegts_parse_close(MpegTSContext ts) {

    }

    @Override
    public int ff_parse_mpeg2_descriptor(AVFormatContext fc, AVStream st, int stream_type, byte[] pp, byte[] desc_list_end, Mp4Descr mp4_descr, int mp4_descr_count, int pid, MpegTSContext ts) {
        return 0;
    }

    @Override
    public int ff_check_h264_startcode(AVFormatContext s, AVStream st, AVPacket pkt) {
        return 0;
    }
}
