package org.tl.nettyServer.media.net.rtsp.rtp.depacketizer;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPacket;

/**
 * RTP DePacketizer Interface
 * @author pengliren
 *
 */
public interface IRTPDePacketizer {

	public BufFacade handleRTPPacket(RTPPacket packet);
	
	public void handleRTCPPacket();
}
