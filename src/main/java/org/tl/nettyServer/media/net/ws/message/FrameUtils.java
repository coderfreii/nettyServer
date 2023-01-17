package org.tl.nettyServer.media.net.ws.message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tl.nettyServer.media.buf.BufFacade;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 处理帧帮助类
 *
 * @author chao
 * @date 2017-10-27
 */
public class FrameUtils {
    private final static Logger log = LogManager
            .getLogger(FrameUtils.class);


    /**
     * 生成帧的二进制数(不分片)
     *
     * @param frame
     * @return
     * @throws Exception
     */
    public static BufFacade genFinalFrameByte(Object frame) throws Exception {
        BufFacade ioBuffer = BufFacade.buffer(1024);
        if (frame instanceof CloseFrame) {
            ioBuffer.writeByte((byte) 0x88);//设置为关闭帧：fin set,Rsv clear,opcode=0x8;
            CloseFrame closeFrame = (CloseFrame) frame;
            ioBuffer.writeByte((byte) 0x02);//掩码FALSE+设置两个长度
            ioBuffer.writeShort(closeFrame.getCode()); //消息码1000
        } else if (frame instanceof PingFrame) {
            ioBuffer.writeByte((byte) 0x89);//设置为ping帧
            ioBuffer.writeByte((byte) 0x00);//设置掩码+长度位0
        } else if (frame instanceof PongFrame) {
            ioBuffer.writeByte((byte) 0x8A);//设置为pong帧
            ioBuffer.writeByte((byte) 0x00);//设置为掩码+长度位0
        } else if (frame instanceof BinMessageFrame) {
            ioBuffer.writeByte((byte) 0x82);
            BinMessageFrame binMessageFrame = (BinMessageFrame) frame;
            int len = binMessageFrame.getContent().length;
            if (len <= 0) {
                throw new Exception("数据不能为空！");
            } else if (len < 126) {
                ioBuffer.writeByte((byte) len);
            } else if (len < 65535) {
                ioBuffer.writeByte((byte) 126);
                ioBuffer.writeBytes(intTo2Bytes(len));
            } else if (len < Integer.MAX_VALUE) {
                ioBuffer.writeByte((byte) 127);
                ioBuffer.writeBytes(intTo8Bytes(len));
            } else {
                throw new Exception("数据太大！");
            }
            ioBuffer.writeBytes(binMessageFrame.getContent());
        } else if (frame instanceof TextMessageFrame) {
            ioBuffer.writeByte((byte) 0x81);//设置为文本帧（不分片）
            TextMessageFrame textMessageFrame = (TextMessageFrame) frame;
            int len = textMessageFrame.getContent().getBytes().length;
            if (len <= 0) {
                throw new Exception("数据不能为空！");
            } else if (len < 126) {
                ioBuffer.writeByte((byte) len);
            } else if (len < 65536) {
                ioBuffer.writeByte((byte) 126);
                byte[] lena = intTo2Bytes(len);
                ioBuffer.writeBytes(intTo2Bytes(len));
            } else if (len < Integer.MAX_VALUE) {
                ioBuffer.writeByte((byte) 127);
                ioBuffer.writeBytes(intTo8Bytes(len));
            } else {
                throw new Exception("数据太大！");
            }
            ioBuffer.writeBytes(textMessageFrame.getContent().getBytes());
        }
        return ioBuffer;
    }

    /**
     * 只取低16位（因为java没有无符号类型）
     *
     * @param value
     * @return
     */
    public static byte[] intTo2Bytes(int value) {
        byte[] src = new byte[2];
        src[0] = (byte) ((value >> 8) & 0xFF);
        src[1] = (byte) (value & 0xFF);
        return src;
    }

    public static int byteArray2ToInt(byte[] b) {
        return b[1] & 0xFF | (b[0] & 0xFF) << 8;
    }

    public static int byteArray2ToShort(byte[] b) {
        return b[1] & 0xFF | (b[0] & 0xFF) << 8;
    }

    public static int byteArray8ToInt(byte[] b) {
        return b[7] & 0xFF | (b[6] & 0xFF) << 8 | (b[5] & 0xFF) << 16 | (b[4] & 0xFF) << 24;
    }

    /**
     * 高32位置0，也就是说最大长度为2的32次方-1(大约512MBytes数据量)
     *
     * @param value
     * @return
     */
    public static byte[] intTo8Bytes(int value) {
        byte[] src = new byte[8];
        src[0] = (byte) (0x00);
        src[1] = (byte) (0x00);
        src[2] = (byte) (0x00);
        src[3] = (byte) (0x00);
        src[4] = (byte) ((value >> 24) & 0xFF);
        src[5] = (byte) ((value >> 16) & 0xFF);
        src[6] = (byte) ((value >> 8) & 0xFF);
        src[7] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 格林尼治时间
     *
     * @return
     */
    static DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);

    static {
        df.setTimeZone(TimeZone.getTimeZone("GMT")); // modify Time Zone.
    }

    public static String getGMT() {
        return (df.format(Calendar.getInstance().getTime()));
    }
}
