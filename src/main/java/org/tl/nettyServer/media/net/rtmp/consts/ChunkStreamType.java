package org.tl.nettyServer.media.net.rtmp.consts;

/**
 * 块流类型  csId
 *
 * 官方建议不同平台可能会有不同实现
 * https://pengrl.com/lal/#/RTMPID?id=chunk-stream-id
 * @author TL
 * @date 2022/12/14
 */

public class ChunkStreamType {
    public static byte RTMP_CONTROL_CHANNEL = 0x02;
    public static byte RTMP_COMMAND_CHANNEL = 0x03;
    public static byte RTMP_STREAM_CHANNEL = 0x05;
    public static byte RTMP_VIDEO_CHANNEL = 0x06;
    public static byte RTMP_AUDIO_CHANNEL = 0x07;
}
