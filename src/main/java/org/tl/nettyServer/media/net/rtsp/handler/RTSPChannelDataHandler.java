package org.tl.nettyServer.media.net.rtsp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.message.RTSPChannelData;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPacket;
import org.tl.nettyServer.media.session.SessionAccessor;
import org.tl.nettyServer.media.stream.proxy.RTSPPushProxyStream;

import java.net.ProtocolException;
import java.util.List;

public class RTSPChannelDataHandler extends MessageToMessageDecoder<RTSPChannelData> {
    private static Logger log = LoggerFactory.getLogger(RTSPRequestHandler.class);


    public RTSPChannelDataHandler() {

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, RTSPChannelData message, List<Object> out) throws Exception {
        RTSPMinaConnection conn = (RTSPMinaConnection) SessionAccessor.resolveRtspConn(ctx);
        Red5.setConnectionLocal(conn);
        // handle rtsp method
        if (message instanceof RTSPChannelData) { // handle rtsp or rtcp data may be rtsp publish stream
            RTSPChannelData channelData = (RTSPChannelData) message;
            byte channel = channelData.getChannel();
            BufFacade data = channelData.getData();
            if (channel == 0x01 || channel == 0x03) {//rtcp
                //RTCPPacket rtcpPkt = new RTCPPacket();
                //rtcpPkt.decode(data);
                //log.info("rtcp packet {}", rtcpPkt);
            } else {//rtp
                //TODO we need add timescale from sdp parse, but current also not add;
                RTPPacket rtpPkt = new RTPPacket(data);
                rtpPkt.setChannel(channel);
                //log.info("rtp packet channel {}, len {}, ts {}", new Object[]{rtpPkt.getChannel(), rtpPkt.getPayload().length, rtpPkt.getTimestamp().longValue()});
                RTSPPushProxyStream pushStream = (RTSPPushProxyStream) conn.getAttribute("pushStream");
                if (pushStream != null) {
                    pushStream.handleMessage(rtpPkt);
                }
            }
        }
        Red5.setConnectionLocal(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RTSPMinaConnection conn = (RTSPMinaConnection) SessionAccessor.resolveRtspConn(ctx);
        if (conn != null) conn.close();

        if (cause instanceof ProtocolException) {
            log.warn("Exception caught {}", cause.getMessage());
        } else {
            log.error("Exception caught {}", cause.getMessage());
        }
    }
}
