package org.tl.nettyServer.media.media.mpeg;

public class MpegMuxer implements MpegTsFuncT{


    //返回一个函数指针
    public byte[] on_mpeg_ts_alloc(MpegMuxerT param, int bytes) {
        MpegMuxerT mpeg =  param;
        return mpeg.ts.func.alloc(mpeg.ts.param, bytes);
        //return mpeg.ts.func.alloc(mpeg.ts.param, bytes);
    }

    public void on_mpeg_ts_free(MpegMuxerT param, byte[] packet) {
        MpegMuxerT mpeg = param;
        mpeg.ts.func.free(mpeg.ts.param, packet);
    }

    public int on_mpeg_ts_write(MpegMuxerT param, byte[] packet, int bytes) {
        MpegMuxerT mpeg =  param;
        return mpeg.ts.func.write(mpeg.ts.param, 0, packet, bytes);
    }

    @Override
    public byte[] alloc(MpegMuxerT param, int bytes) {
        return this.on_mpeg_ts_alloc(param, bytes);
    }

    @Override
    public void free(MpegMuxerT param, byte[] packet) {
        this.on_mpeg_ts_free(param, packet);
    }

    @Override
    public int write(MpegMuxerT param, byte[] packet, int bytes) {
        return this.on_mpeg_ts_write(param,packet,bytes);
    }

    MpegMuxerT mpeg_muxer_create(int is_ps,PsMuxerFuncT func, MpegMuxerT param) {
        MpegMuxerT mpeg = new MpegMuxerT() ;

        mpeg.is_ps = is_ps;
        if (is_ps > 0)  {
            mpeg.ps = MpegPsEnc.ps_muxer_create(func, param);
        } else {
            MpegTsFuncT ts_func = this;
            mpeg.ts.func = func;
            mpeg.ts.param = param;
            mpeg.ts.ctx = MpegTsEnc.mpeg_ts_create(ts_func, mpeg);
        }
        return mpeg;
    }

    int mpeg_muxer_destroy(MpegMuxerT muxer) {
        //assert(muxer);
        int ret = -1;
        if (muxer.is_ps > 0) {
            ret = MpegPsEnc.ps_muxer_destroy(muxer.ps);
        } else {
            ret = MpegTsEnc.mpeg_ts_destroy(muxer.ts.ctx);
        }
        //free(muxer);
        return ret;
    }
    int mpeg_muxer_add_stream(MpegMuxerT muxer, int codecid, byte[] extradata, int extradata_size) {
        //assert(muxer);
        if (muxer.is_ps > 0) {
            return MpegPsEnc.ps_muxer_add_stream(muxer.ps, codecid, extradata, extradata_size);
        }
        return MpegTsEnc.mpeg_ts_add_stream(muxer.ts.ctx, codecid, extradata, extradata_size);
    }
    int mpeg_muxer_input(MpegMuxerT muxer, int stream, int flags, long pts, long dts, byte[] data, int bytes) {
        //assert(muxer);
        if (muxer.is_ps > 0) {
            return MpegPsEnc.ps_muxer_input(muxer.ps, stream, flags, pts, dts, data, bytes);
        }
        return MpegTsEnc.mpeg_ts_write(muxer.ts.ctx, stream, flags, pts, dts, data,0, bytes);
    }
    int mpeg_muxer_reset(MpegMuxerT muxer) {
       // assert(muxer);
        if (muxer.is_ps == 0) {
            return -1;
        }
        return MpegTsEnc.mpeg_ts_reset(muxer.ts.ctx);
    }
    
    int mpeg_muxer_add_program(MpegMuxerT muxer, char pn, byte[] info, int bytes) {
        //assert(muxer);
        if (muxer.is_ps == 0) {
            return -1;
        }
        return MpegTsEnc.mpeg_ts_add_program(muxer.ts.ctx, pn, info, bytes);
    }

    int mpeg_muxer_remove_program(MpegMuxerT muxer, char pn) {
        //assert(muxer);
        if (muxer.is_ps==0) {
            return -1;
        }
        return MpegTsEnc.mpeg_ts_remove_program(muxer.ts.ctx, pn);
    }
    int mpeg_muxer_add_program_stream(MpegMuxerT muxer, char pn, int codecid, byte[] extra_data, int extra_data_size) {
        //assert(muxer);
        if (muxer.is_ps == 0) {
            return -1;
        }
        return MpegTsEnc.mpeg_ts_add_program_stream(muxer.ts.ctx, pn, codecid, extra_data, extra_data_size);
    }
}
