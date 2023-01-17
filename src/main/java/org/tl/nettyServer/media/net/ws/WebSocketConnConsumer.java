package org.tl.nettyServer.media.net.ws;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.messaging.IMessage;
import org.tl.nettyServer.media.messaging.IMessageComponent;
import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.messaging.OOBControlMessage;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.net.ws.message.BinMessageFrame;
import org.tl.nettyServer.media.stream.consumer.ICustomPushableConsumer;
import org.tl.nettyServer.media.stream.data.IStreamPacket;
import org.tl.nettyServer.media.stream.message.RTMPMessage;

import java.io.IOException;

@Slf4j
public class WebSocketConnConsumer implements ICustomPushableConsumer {
    private HTTPConnection connection;

    private static BufFacade header = BufFacade.buffer(13);

    private boolean inited = false;

    static {
        // write flv header
        header.writeBytes("FLV".getBytes());
        header.writeBytes(new byte[]{0x01, 0x05});
        header.writeInt(0x09);
        header.writeInt(0x00);
    }

    public WebSocketConnConsumer(HTTPConnection connection) {
        this.connection = connection;
    }

    @Override
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {

    }

    @Override
    public HTTPConnection getConnection() {
        return this.connection;
    }

    @Override
    public void pushMessage(IPipe pipe, IMessage message) throws IOException {
        if (!inited) {
            //new byte{}{0x46, 0x4c, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09}
            connection.write(new BinMessageFrame(header.array()));
            //尝试发送metadata
            inited = true;
        }
        if (message instanceof RTMPMessage) {
            RTMPMessage rm = (RTMPMessage) message;
            IRTMPEvent body = rm.getBody();
            if (body instanceof IStreamPacket) {
                IStreamPacket ip = (IStreamPacket) body;
                BinMessageFrame binMessageFrame = encodeMediaAsFlvTagAndPrevTagSize(ip);
                ChannelFuture write = connection.write(binMessageFrame);
                write.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            connection.messageSent();
                        } else {
                            log.error("retry");
                            connection.messageSent();
                        }
                    }
                });
            }
        }
    }


    private BinMessageFrame encodeMediaAsFlvTagAndPrevTagSize(IStreamPacket msg) {
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
        if (r.length > 1024) {
        }
        return new BinMessageFrame(r);
    }
}
