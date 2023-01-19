package org.tl.nettyServer.media.net.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.buf.ReleaseUtil;
import org.tl.nettyServer.media.messaging.IMessage;
import org.tl.nettyServer.media.messaging.IMessageComponent;
import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.messaging.OOBControlMessage;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.message.DefaultHttpChunk;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.stream.consumer.ICustomPushableConsumer;
import org.tl.nettyServer.media.stream.data.IStreamPacket;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.stream.message.StatusMessage;

@Slf4j
public class HTTPConnectionConsumer implements ICustomPushableConsumer {

    private HTTPConnection conn;

    private boolean closed = false;

    private boolean inited = false;

    private static BufFacade header = BufFacade.buffer(13);

    static {
        // write flv header
        header.writeBytes("FLV".getBytes());
        header.writeBytes(new byte[]{0x01, 0x05});
        header.writeInt(0x09);
        header.writeInt(0x00);
    }

    public HTTPConnectionConsumer(HTTPConnection conn) {
        this.conn = conn;
    }

    @Override
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
                                    OOBControlMessage oobCtrlMsg) {
        if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
            if ("pendingVideoCount".equals(oobCtrlMsg.getServiceName())) {
                long pendings = conn.getPendingMessages();
                if (pendings > 500) {
                    log.info("http pending packet:{}", pendings);
                    oobCtrlMsg.setResult(pendings);
                } else if (pendings > 1000) {
                    log.info("http pending packet > 1000, network is bad");
                    closed = true;
                }
            }
        }
    }


    /**
     * 最终发送消息的方法
     */
    @Override
    public void pushMessage(IPipe pipe, IMessage message) {
        if (!inited) {
            //new byte{}{0x46, 0x4c, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09}
            conn.write(new DefaultHttpChunk(BufFacade.wrappedBuffer(header.array())));
            //这里不能放在future的监听里面
            inited = true;
        }


        if (message instanceof RTMPMessage) {
            IRTMPEvent body = ((RTMPMessage) message).getBody();
            if (body instanceof AudioData || body instanceof VideoData) {
                IStreamPacket packet = (IStreamPacket) body;
                byte[] bytes = encodeMediaAsFlvTagAndPrevTagSize(packet);
                ChannelFuture write = conn.write(new DefaultHttpChunk(BufFacade.wrappedBuffer(bytes)));
                write.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            conn.messageSent();
                        } else {
                            if (!future.channel().isOpen()) {
                                conn.close();
                                return;
                            }
                            log.error("retry");
                            conn.messageSent();
                        }
                    }
                });
            }
        } else if (message instanceof StatusMessage) {
            if (((StatusMessage) message).getBody().getCode().equals(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY)) {
                closed = true;
                conn.close();
            }
        }
    }

    private byte[] encodeMediaAsFlvTagAndPrevTagSize(IStreamPacket msg) {
        int tagType = msg.getDataType();
        byte[] data = msg.getData().array();
        int dataSize = data.length;
        int timestamp = msg.getTimestamp() & 0xffffff;
        int timestampExtended = ((msg.getTimestamp() & 0xff000000) >> 24);

        BufFacade buffer = BufFacade.buffer(1024);
        buffer.writeByte(tagType);    //1
        buffer.writeMedium(dataSize); //3
        buffer.writeMedium(timestamp);  //3
        buffer.writeByte(timestampExtended);// timestampExtended   1
        buffer.writeMedium(0);// streamId   //3
        buffer.writeBytes(data);                 //
        buffer.writeInt(data.length + 11); // previousTagSize //
        byte[] r = new byte[buffer.readableBytes()];
        buffer.readBytes(r);
        buffer.release();
        ReleaseUtil.releaseAll(msg);
        msg = null;
        if (r.length > 1024) {
        }
        return r;
    }

    @Override
    public HTTPConnection getConnection() {
        return conn;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClose(boolean value) {
        this.closed = value;
    }
}
