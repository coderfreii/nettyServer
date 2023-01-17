package org.tl.nettyServer.media.net.ws.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.codec.DecodeState;
import org.tl.nettyServer.media.net.http.codec.HTTPRequestDecoder;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.ws.message.*;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.util.List;

public class WebSocketDecoderHandler extends MessageToMessageDecoder<ByteBuf> {
    private HTTPRequestDecoder hr = new HTTPRequestDecoder();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        HTTPConnection conn = SessionAccessor.resolveHttpConn(ctx);
        if (conn.isWebsocket()) {
            WebSocketFrame webSocketFrame = decodeBuff(BufFacade.wrapperAndCast(msg), conn);
            if (webSocketFrame != null) {
                out.add(webSocketFrame);
            }
        } else {
            try {
                while (msg.readableBytes() > 0) {
                    DecodeState obj = hr.decodeBuffer(BufFacade.wrapperAndCast(msg));
                    if (obj.getState() == DecodeState.ENOUGH) {
                        if (obj.getObject() != null) {
                            out.add(obj.getObject());
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new CodecException(e);
            }
        }
    }


    WebSocketFrame decodeBuff(BufFacade in, HTTPConnection conn) {
        /**
         * 这部分是websocket请求
         */
        //websocket请求
        in.markReaderIndex();
        if (in.readableBytes() > 1) {
            byte b1 = in.readByte();//第一位确定FIN（是否最后一帧数）
            byte b2 = in.readByte();
            //判断是否最后一个帧
            if ((b1 & 0x80) == 0x80) {
                //掩码不为1拒绝
                if ((b2 & 0x80) == 0x80) {
                    //确定长度------start
                    int len = b2 & 0x7f;
                    if (len < 126) {
                        //
                    } else if (len == 126) {
                        if (in.readableBytes() >= 6) {
                            byte[] lenBytes = new byte[2];
                            in.readBytes(lenBytes);
                            len = FrameUtils.byteArray2ToInt(lenBytes);
                        } else {
                            in.resetReaderIndex();
                            return null;
                        }
                    } else if (len == 127) {
                        if (in.readableBytes() >= 10) {
                            byte[] lenBytes = new byte[4];
                            in.readBytes(lenBytes);
                            len = FrameUtils.byteArray8ToInt(lenBytes);
                        } else {
                            in.resetReaderIndex();
                            return null;
                        }
                    } else {
                        in.resetReaderIndex();
                        return null;
                        //非法的
                        //close
                    }
                    //结束长度------start
                    //掩码位
                    byte mask[] = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        mask[i] = in.readByte();
                    }

                    switch (b1 & 0x0f) {
                        case 0x01:
                            //文字类
                            if (in.readableBytes() >= len) {
                                byte[] data = new byte[len];
                                in.readBytes(data);
                                TextMessageFrame textMessageFrame = new TextMessageFrame(
                                        new String(
                                                unMaskData(mask, data)//解码
                                        )
                                );
                                return textMessageFrame;
                            } else {
                                in.resetReaderIndex();
                                return null;
                            }
                        case 0x02:
                            // 字节类
                            if (in.readableBytes() >= len) {
                                byte[] data = new byte[len];
                                in.readBytes(data);
                                BinMessageFrame binMessageFrame = new BinMessageFrame(
                                        unMaskData(mask, data)//解码
                                );
                                return binMessageFrame;
                            } else {
                                in.resetReaderIndex();
                                return null;
                            }
                        case 0x08:
                            // 断开链接
                            short reasonCode = 1000;
                            if (len > 0) {
                                //如果有数据就读取后扔掉（这个一般不可能，但是安全期间做个处理）
                                if (in.readableBytes() >= len) {
                                    byte[] data = new byte[2];//code是16位的数字
                                    reasonCode = in.readByte();//把数据扔掉
                                } else {
                                    in.resetReaderIndex();
                                    return null;
                                }
                            }
                            CloseFrame closeFrame = new CloseFrame(conn.getRemoteAddress(), reasonCode);
                            return closeFrame;
                        case 0x09:
                            // ping
                            break;
                        case 0x0a:
                            // pong
                            if (len > 0) {
                                //如果pong有数据就读取后扔掉（这个一般不可能，但是安全期间做个处理）
                                if (in.readableBytes() >= len) {
                                    byte[] data = new byte[len];
                                    in.readBytes(data);//把数据扔掉
                                } else {
                                    in.resetReaderIndex();
                                    return null;
                                }
                            }
                            PongFrame pongFrame = new PongFrame(conn.getRemoteAddress());
                            return pongFrame;


                        default:
                            // 不支持
                            // 协议错误
                            //
                            break;
                    }
                } else {
                    // 不支持
                    TextMessageFrame textMessageFrame = new TextMessageFrame("数据未掩码处理！");
                    return textMessageFrame;
                    // 协议错误
                    //close
                }

            } else if ((b1 & 0x80) == 0x00) {
                //不支持分片
                TextMessageFrame textMessageFrame = new TextMessageFrame("暂时不支持分片！");
                return textMessageFrame;
                //close
            }
        }

        return null;
    }

    //去掩码
    public byte[] unMaskData(byte mask[], byte[] unMaskData) {
        for (int i = 0; i < unMaskData.length; i++) {
            byte maskedByte = unMaskData[i];
            unMaskData[i] = (byte) (maskedByte ^ mask[i % 4]);
        }
        return unMaskData;
    }
}
