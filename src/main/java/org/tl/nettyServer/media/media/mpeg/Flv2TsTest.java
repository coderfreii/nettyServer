package org.tl.nettyServer.media.media.mpeg;



import org.tl.nettyServer.media.media.mp4.MP4Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Flv2TsTest implements MpegTsFuncT, FlvProto {



    public static byte[] ts_alloc(MpegMuxerT param, int bytes)
    {
       /* static char s_buffer[188];
        assert(bytes <= sizeof(s_buffer));*/
        return new byte[188];
    }

    @Override
    public byte[] alloc(MpegMuxerT param, int bytes) {
        return ts_alloc(param,bytes);
    }

    public  void free(MpegMuxerT param, byte[] packet){};

    /// callback on PS packet done
    /// @param[in] param user-defined parameter(by ps_muxer_create)
    /// @param[in] stream stream id, return by ps_muxer_add_stream
    /// @param[in] packet PS packet pointer(alloc return pointer)
    /// @param[in] bytes packet size
    /// @return 0-ok, other-error
    public  int write(MpegMuxerT param,byte[] packet, int bytes){
        return 1;// == fwrite(packet, bytes, 1, (FILE*)param) ? 0 : ferror((FILE*)param);
    }
    public static String ftimestamp(int t, String buf)
    {

        return buf.format("%d:%02d:%02d.%03d", t / 3600000, (t / 60000) % 60, (t / 1000) % 60, t % 1000);
    }

    public static void main(String[] args) {
        System.out.println(ftimestamp(136000000,"ssss"));
    }

    static  char flv_type(int type)
    {
        switch (type)
        {
            case FLV_AUDIO_ASC:			return 'a';
            case FLV_AUDIO_AAC:			return 'A';
            case FLV_AUDIO_MP3:			return 'M';
            case FLV_AUDIO_OPUS:		return 'O';
            case FLV_AUDIO_OPUS_HEAD:	return 'o';
            case FLV_VIDEO_H264:		return 'V';
            case FLV_VIDEO_AVCC:		return 'v';
            case FLV_VIDEO_H265:		return 'H';
            case FLV_VIDEO_HVCC:		return 'h';
            default: return '*';
        }
    }
    static  int flv2ts_codec_id(int type)
    {
        switch (type)
        {
            case FLV_AUDIO_ASC:
            case FLV_AUDIO_AAC:			return STREAM_AUDIO_AAC;
            case FLV_AUDIO_MP3:			return STREAM_AUDIO_MP3;
            case FLV_AUDIO_OPUS:
            case FLV_AUDIO_OPUS_HEAD:	return STREAM_AUDIO_OPUS;
            case FLV_VIDEO_H264:
            case FLV_VIDEO_AVCC:		return STREAM_VIDEO_H264;
            case FLV_VIDEO_H265:
            case FLV_VIDEO_HVCC:		return STREAM_VIDEO_H264;
            default: return '*';
        }
    }
    public static Map<Integer, Integer> streams = new HashMap<>();
    static int ts_stream(MpegTsEncContextT ts, int codecid,byte[] data, int bytes)
    {
        if (streams.containsKey(codecid))
            return streams.get(codecid);

        int i = MpegTsEnc.mpeg_ts_add_stream(ts, flv2ts_codec_id(codecid), data, bytes);
        streams.put(codecid, i);
        return i;
    }
    static int onFLV(MpegTsEncContextT ts, int codec,byte[] data, int bytes,int pts,int dts, int flags)
    {
        char[] s_pts = new char[64];
        char[] s_dts= new char[64];
        int v_pts = 0, v_dts = 0;
        int a_pts = 0, a_dts = 0;

       // printf("[%c] pts: %s, dts: %s, ", flv_type(codec), ftimestamp(pts, s_pts), ftimestamp(dts, s_dts));

        if (FLV_AUDIO_AAC == codec || FLV_AUDIO_MP3 == codec || FLV_AUDIO_OPUS == codec)
        {
            //		assert(0 == a_dts || dts >= a_dts);
            pts = ( (a_pts > 0) && pts < a_pts) ? a_pts : pts;
            dts = ( (a_dts > 0) && dts < a_dts) ? a_dts : dts;
            MpegTsEnc.mpeg_ts_write(ts, ts_stream(ts, codec, null, 0), 0, pts * 90, dts * 90, data,0, bytes);

            //printf("diff: %03d/%03d", (int)(pts - a_pts), (int)(dts - a_dts));
            a_pts = pts;
            a_dts = dts;
        }
        else if (FLV_VIDEO_H264 == codec || FLV_VIDEO_H265 == codec)
        {
            assert(0 == v_dts || dts >= v_dts);
            dts = ( (a_dts > 0) && dts < v_dts) ? v_dts : dts;
            MpegTsEnc.mpeg_ts_write(ts, ts_stream(ts, codec, null, 0), (0x01 > 0 & flags > 0) ? 1 : 0, pts * 90, dts * 90, data,0, bytes);

            //printf("diff: %03d/%03d%s", (int)(pts - v_pts), (int)(dts - v_dts), flags ? " [I]" : "");
            v_pts = pts;
            v_dts = dts;
        }
        else if (FLV_AUDIO_OPUS_HEAD == codec)
        {
            ts_stream(ts, FLV_AUDIO_OPUS, data, bytes);
        }
        else
        {
            // nothing to do
        }

        //printf("\n");
        return 0;
    }
    void flv2ts_test(String inputFLV, String outputTS) throws IOException {
        MpegTsFuncT tshandler = this;
        MP4Service service = new MP4Service();

        //FILE* fp = fopen(outputTS, "wb");
       /* MpegTsEncContextT ts = MpegTsEnc.mpeg_ts_create(tshandler, fp);
        //void* reader = flv_reader_create(inputFLV);
        IStreamableFile streamFile = service.getStreamableFile(new File(inputFLV));
        ITagReader reader = streamFile.getReader();*/



    }

}
