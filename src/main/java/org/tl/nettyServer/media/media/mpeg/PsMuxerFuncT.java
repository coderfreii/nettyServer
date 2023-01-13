package org.tl.nettyServer.media.media.mpeg;

public class PsMuxerFuncT {
    /// alloc new packet
    /// @param[in] param user-defined parameter(by ps_muxer_create)
    /// @param[in] bytes alloc memory size in byte
    /// @return memory pointer
    public byte[] alloc(MpegMuxerT param, int bytes){ return  null;};

    /// free packet
    /// @param[in] param user-defined parameter(by ps_muxer_create)
    /// @param[in] packet PS packet pointer(alloc return pointer)
	void free(MpegMuxerT param, byte[] packet){};

    /// callback on PS packet done
    /// @param[in] param user-defined parameter(by ps_muxer_create)
    /// @param[in] stream stream id, return by ps_muxer_add_stream
    /// @param[in] packet PS packet pointer(alloc return pointer)
    /// @param[in] bytes packet size
    /// @return 0-ok, other-error
	int write(MpegMuxerT param, int stream,byte[] packet, int bytes){return 0;};
}
