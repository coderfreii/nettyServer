package org.tl.nettyServer.media.net.rtmp.consts;

/**
 * 格式消息类型
 * fmt
 *
 * @author TL
 * @date 2022/12/14
 */
public class FormatMessageType {
    public final static byte FULL = 0x00;
    public final static byte RELATIVE_LARGE = 0x01;
    public final static byte RELATIVE_TIMESTAMP_ONLY = 0x02;
    public final static byte RELATIVE_SINGLE_BYTE = 0x03;
}
