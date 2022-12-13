package org.tl.nettyServer.media.net.rtmp.consts;

/**
 * 消息头类型
 *
 * @author TL
 * @date 2022/12/14
 */
public class MessageHeaderType {
    public static byte FULL = 0x00;
    public static byte RELATIVE_LARGE = 0x01;
    public static byte RELATIVE_TIMESTAMP_ONLY = 0x02;
    public static byte RELATIVE_SINGLE_BYTE = 0x03;
}
