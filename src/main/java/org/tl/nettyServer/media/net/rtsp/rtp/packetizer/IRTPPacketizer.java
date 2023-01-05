package org.tl.nettyServer.media.net.rtsp.rtp.packetizer;



import gov.nist.javax.sdp.MediaDescriptionImpl;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPacket;
import org.tl.nettyServer.media.net.udp.IUDPTransportOutgoingConnection;
import org.tl.nettyServer.media.stream.data.IStreamPacket;

import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * RTP Packetizer Interface
 * @author pengliren
 *
 */
public interface IRTPPacketizer {

	public static final int RTPTYPE_RFC3984H264 = 97;
	public static final int RTPTYPE_MPEG4AAC = 96;
	public static final int RTPTYPE_MPEG4LATM = 96;
	public static final int RTPTYPE_RFC2190H263 = 96;
	public static final int RTPTYPE_SPEEX = 97;
	public static final int RTPTYPE_MP3 = 14;
	public static final int RTPTYPE_MP2T = 98;

	public void handleStreamPacket(IStreamPacket packet);
	
	public void initRtcpInfo(InetSocketAddress address) throws SocketException;

	public long getPacketCount();

	public long getByteCount();

	public long getRTCPOctetCount();

	public MediaDescriptionImpl getDescribeInfo(BufFacade config)throws Exception;

	public int getSDPTypeId();

	public void setSDPTypeId(int sdpTypeId);

	public void resetSequence();

	public int getNextSequence();
	
	public long getSsrc();
	
	public int getTimescale();
	
	public void stop();
	
	public InetSocketAddress getRtcpAddress();
	
	public void write(RTPPacket packet);
	
	// tcp
	public void setOutputStream(RTSPMinaConnection conn);
	
	// udp
	public void setOutputStream(IUDPTransportOutgoingConnection conn);
}