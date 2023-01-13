package org.tl.nettyServer.media.media.mpeg;

/**
 * ps_muxer_func_t
 */
public class MpegMuxerFuncT {
    /// alloc new packet
    /// @param[in] param user-defined parameter(by ps_muxer_create)
    /// @param[in] bytes alloc memory size in byte
    /// @return memory pointer
    //void* (*alloc)(void* param, size_t bytes);
    void alloc(int param, int bytes){};

    /// free packet
    /// @param[in] param user-defined parameter(by ps_muxer_create)
    /// @param[in] packet PS packet pointer(alloc return pointer)
	//void (*free)(void* param, void* packet);
	void free(int param, int packet){};

    /// callback on PS packet done
    /// @param[in] param user-defined parameter(by ps_muxer_create)
    /// @param[in] stream stream id, return by ps_muxer_add_stream
    /// @param[in] packet PS packet pointer(alloc return pointer)
    /// @param[in] bytes packet size
    /// @return 0-ok, other-error
	//int (*write)(void* param, int stream, void* packet, size_t bytes);
	int write(int param, int stream, int packet, int bytes){ return 0;}
}
