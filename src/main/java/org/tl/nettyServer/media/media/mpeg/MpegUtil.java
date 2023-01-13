package org.tl.nettyServer.media.media.mpeg;


public class MpegUtil implements MpegTsProto{
    static void mpeg_bits_init(MpegBitsT bits,byte[] data, int bytes){
        bits.ptr = data;
        bits.len = bytes;
        bits.off = 0;
    }
    static  byte mpeg_bits_read8(MpegBitsT bits){
        return ++bits.off <= bits.len ? bits.ptr[bits.off - 1] : 0;
    }
    static int mpeg_bits_error(MpegBitsT bits){
        return bits.off > bits.len ? 1 : 0;
    }
    static int mpeg_bits_read32(MpegBitsT bits)
    {
        int v;
        v = mpeg_bits_read16(bits) << 16;
        v |= mpeg_bits_read16(bits);
        return v;
    }
    static  char mpeg_bits_read16(MpegBitsT bits)
    {
        char v;
        v = (char) (mpeg_bits_read8(bits) << 8);
        v |= mpeg_bits_read8(bits);
        return v;
    }
    public static void mpeg_bits_skip(MpegBitsT bits, int n)
    {
        bits.off += n;
    }
    public static int mpeg_stream_type_video(int codecid)
    {
        switch (codecid)
        {
            case PSI_STREAM_H264:
            case PSI_STREAM_H265:
            case PSI_STREAM_MPEG1:
            case PSI_STREAM_MPEG2:
            case PSI_STREAM_MPEG4:
            case PSI_STREAM_VIDEO_VC1:
            case PSI_STREAM_VIDEO_SVAC:
            case PSI_STREAM_VIDEO_DIRAC:
            case PSI_STREAM_VIDEO_CAVS:
            case PSI_STREAM_VP8:
            case PSI_STREAM_VP9:
            case PSI_STREAM_AV1:
                return 1;
            default:
                return 0;
        }
    }
    public static int mpeg_stream_type_audio(int codecid)
    {
        switch (codecid)
        {
            case PSI_STREAM_AAC:
            case PSI_STREAM_MPEG4_AAC:
            case PSI_STREAM_MPEG4_AAC_LATM:
            case PSI_STREAM_AUDIO_MPEG1:
            case PSI_STREAM_MP3:
            case PSI_STREAM_AUDIO_AC3:
            case PSI_STREAM_AUDIO_DTS:
            case PSI_STREAM_AUDIO_EAC3:
            case PSI_STREAM_AUDIO_SVAC:
            case PSI_STREAM_AUDIO_G711A:
            case PSI_STREAM_AUDIO_G711U:
            case PSI_STREAM_AUDIO_G722:
            case PSI_STREAM_AUDIO_G723:
            case PSI_STREAM_AUDIO_G729:
            case PSI_STREAM_AUDIO_OPUS:
                return 1;
            default:
                return 0;
        }
    }

    public static char Swap16(char A) {
      return (char) ((((A) & 0xff00) >>> 8) | ((A & 0x00ff) << 8));
    }

    public static int Swap32(int A) {
      return (((A & 0xff000000) >>> 24) |
        ((A & 0x00ff0000) >>>  8) |
        ((A & 0x0000ff00) <<  8) |
        ((A & 0x000000ff) << 24));
    }


    //将一个无符号长整形数从网络字节顺序转换为主机字节顺序
    public static int ntohl(int nl)
    {
        return  Swap32(nl);
    }


    public static  void nbo_w32(byte[] ptr,int index, int val)
    {
        ptr[index] = (byte)((val >>> 24) & 0xFF);
        ptr[index+1] = (byte)((val >>> 16) & 0xFF);
        ptr[index+2] = (byte)((val >>> 8) & 0xFF);
        ptr[index+3] = (byte)(val & 0xFF);

    }
    public static char ntohs(byte[]pData,int index){
        byte high= pData[index];
        byte lower=pData[index+1];
        return (char) (lower<<8+high);
    }

    public static  int byteToInt(byte[] ptr,int index)
    {
        int value = 0;
        // 由高位到低位
        /*for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (ptr[index+i] & 0xFF) << shift;// 往高位游
        }*/
        for (int i = 3; i >= 0; i--) {
            int shift = (4 - 1 - i) * 8;
            value += (ptr[index+i] & 0xFF) << shift;// 往高位游
        }
        return value;

    }
    public static  char byteToChar(byte[] ptr,int index)
    {
        char value = 0;
        int size = 2;
        // 由高位到低位
        for (int i = 0; i < size; i++) {
            int shift = (size - 1 - i) * 8;
            value += (ptr[index+i] & 0x000000FF) << shift;// 往高位游
        }
        return value;

    }
    public static  long byteToLong(byte[] ptr,int index)
    {
        long value = 0;
        int size = 8;
        // 由高位到低位
        for (int i = 0; i < size; i++) {
            int shift = (size - 1 - i) * 8;
            value += (ptr[index+i] & 0x000000FF) << shift;// 往高位游
        }
        return value;

    }
    public static void nbo_w16(byte[]  ptr,int index, char val)
    {
        ptr[index] = (byte) ((val >>> 8) & 0xFF);
        ptr[index+1] = (byte)(val & 0xFF);
    }
    public static void memcpy(byte[] des,int start,byte[] src,int size)
    {
        System.arraycopy(src,0,des,start,size);
    }
    public static void memcpy(byte[] des,int start,int[] src,int size)
    {
        System.arraycopy(src,0,des,start,size);
    }
    public static void memcpy(byte[] des,int start,byte[] src,int srcStart,int size)
    {
        System.arraycopy(src,srcStart,des,start,size);
    }
    public static void memmove(byte[]  des,int start,byte[] src,int size){
        System.arraycopy(src,0,des,start,size);
    }
    public static void memmove(byte[]  des,int start,byte[] src,int srcStart,int size){
        System.arraycopy(src,srcStart,des,start,size);
    }
    public static void memset(byte[]  des,int start,byte val,int size){
        for(int i=0;i<size;i++)
            des[start+i] = val;
    }
    public static void pcr_write(byte[] ptr,int index, long pcr)
    {
        long pcr_base = pcr / 300;
        long pcr_ext = pcr % 300;

        ptr[index+0] = (byte) ((pcr_base >>> 25) & 0xFF);
        ptr[index+1] = (byte) ((pcr_base >>> 17) & 0xFF);
        ptr[index+2] = (byte) ((pcr_base >>> 9) & 0xFF);
        ptr[index+3] = (byte) ((pcr_base >>> 1) & 0xFF);
        ptr[index+4] = (byte) (((pcr_base & 0x01) << 7) | 0x7E | ((pcr_ext>>>8) & 0x01));
        ptr[index+5] = (byte) (pcr_ext & 0xFF);
    }

    public static int strncmp(String s1,String s2,int size){
        String s11 =s1.substring(0, size);
        String s22 =s2.substring(0, size);

        if(s11.compareTo(s22) == 0){
            return 0;
        }else if(s11.compareTo(s11) >0){
            return 1;
        }
        return -1;
    }
}
