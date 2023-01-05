package org.tl.nettyServer.media.net.rtsp.conn;



import org.tl.nettyServer.media.RtspServer;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.conn.BaseConnection;
import org.tl.nettyServer.media.net.rtsp.RTSPCore;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPlayer;
import org.tl.nettyServer.media.net.udp.UDPPortManager;
import org.tl.nettyServer.media.session.SessionFacade;
import org.tl.nettyServer.media.stream.base.IClientStream;
import org.tl.nettyServer.media.stream.client.CustomSingleItemSubStream;
import org.tl.nettyServer.media.stream.client.IClientBroadcastStream;
import org.tl.nettyServer.media.stream.client.IPlaylistSubscriberStream;
import org.tl.nettyServer.media.stream.client.ISingleItemSubscriberStream;
import org.tl.nettyServer.media.stream.conn.IStreamCapableConnection;
import org.tl.nettyServer.media.stream.proxy.RTSPPushProxyStream;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * RTSP Connection
 * @author pengliren
 *
 */
public class RTSPMinaConnection extends BaseConnection implements IStreamCapableConnection {

	public static final String RTSP_CONNECTION_KEY = "rtsp.conn";
	
	public final static int PLAY_TYPE_UDP = 0;
	
	public final static int PLAY_TYPE_TCP = 1;
	
	private int playType;
	
	private boolean isClosed = false;
	
	private RTPPlayer rtpConnector;
	
	private int[] videoPairPort;
	
	private int[] audioPairPort;

	private SessionFacade rtspSession;
	
	private boolean isMpegts = false;
	
	private boolean isLive = false;
	
	public RTSPMinaConnection(SessionFacade session) {
		this.rtspSession = session;
		this.sessionId = String.valueOf(session.getSessionId());
	}

	public void close() {
		
		if(isClosed == false) {
			isClosed = true;
		} else {
			return;
		}
		
		RTSPConnectionConsumer rtspConsumer = (RTSPConnectionConsumer)getAttribute("rtspConsumer");
		if (rtspConsumer != null) {
			if (rtpConnector != null) {
				rtspConsumer.removeStreamListener(rtpConnector);
				rtpConnector.close();
			}
		}

		UDPPortManager udpPortMgr = UDPPortManager.getInstance();
		if (videoPairPort != null && videoPairPort.length == 2) {
			RtspServer.RTP_VIDEO_ACCEPTOR_CHANNEL.close();
			udpPortMgr.releaseUDPPortPair(videoPairPort[0]);
			RtspServer.RTCP_VIDEO_ACCEPTOR_CHANNEL.close();
			udpPortMgr.releaseUDPPortPair(videoPairPort[1]);
		}

		if (audioPairPort != null && audioPairPort.length == 2) {
			RtspServer.RTP_AUDIO_ACCEPTOR_CHANNEL.close();
			udpPortMgr.releaseUDPPortPair(audioPairPort[0]);
			RtspServer.RTCP_AUDIO_ACCEPTOR_CHANNEL.close();
			udpPortMgr.releaseUDPPortPair(audioPairPort[1]);
		}
		
		if(rtpConnector != null) {
			if(rtpConnector.getVideoRtpPacketizer() != null) {
				InetSocketAddress videoAddress = rtpConnector.getVideoRtpPacketizer().getRtcpAddress();
				if (videoAddress != null) {
					RTSPCore.rtpSocketMaps.remove(String.format("%s:%d", videoAddress.getAddress().getHostAddress(), videoAddress.getPort()));
				}
			}

			if(rtpConnector.getAudioRtpPacketizer() != null) {
				InetSocketAddress audioAddress = rtpConnector.getAudioRtpPacketizer().getRtcpAddress();
				if (audioAddress != null) {
					RTSPCore.rtpSocketMaps.remove(String.format("%s:%d", audioAddress.getAddress().getHostAddress(), audioAddress.getPort()));
				}
			}
		}
		
		// check play stram is null
		CustomSingleItemSubStream pullStream = (CustomSingleItemSubStream)getAttribute("rtspStream");
		
		// check publish stream is null
		RTSPPushProxyStream pushStream = (RTSPPushProxyStream)getAttribute("pushStream");
		
		// we must colse play stream
		if(pullStream != null) {
			pullStream.close();
		}
		
		// we must stop publish stream
		if(pushStream != null) {
			pushStream.stop();
		}			

		//TODO
		// clear rtsp tunnel
//		if(getAttribute("sessioncookie") != null) RTSPTunnel.RTSP_TUNNEL_CONNS.remove(getAttribute("sessioncookie"));
		
//		if(rtspSession != null) rtspSession.closeNow();
	}
	
	public int getPlayType() {
		return playType;
	}

	public void setPlayType(int playType) {
		this.playType = playType;
	}
	
	public RTPPlayer getRtpConnector() {
		return rtpConnector;
	}

	public void setRtpConnector(RTPPlayer rtpConnector) {
		this.rtpConnector = rtpConnector;
	}

	public SessionFacade getRtspSession() {
		return rtspSession;
	}

	public boolean isMpegts() {
		return isMpegts;
	}

	public void setMpegts(boolean isMpegts) {
		this.isMpegts = isMpegts;
	}

	public void setVideoPairPort(int[] videoPairPort) {
		this.videoPairPort = videoPairPort;
	}

	public void setAudioPairPort(int[] audioPairPort) {
		this.audioPairPort = audioPairPort;
	}
	
	public void write(Object out) {
		
		rtspSession.write(out);
	}

	public boolean isLive() {
		return isLive;
	}

	public void setLive(boolean isLive) {
		this.isLive = isLive;
	}

	@Override
	public Encoding getEncoding() {
		return null;
	}

	@Override
	public void ping() {
		
	}

	@Override
	public int getLastPingTime() {

		return 0;
	}

	@Override
	public void setBandwidth(int mbits) {
		
	}

	@Override
	public long getReadBytes() {
		return 0;
	}

	@Override
	public long getWrittenBytes() {
		return 0;
	} 

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId() throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId(Number streamId) throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unreserveStreamId(Number streamId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteStreamById(Number streamId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IClientStream getStreamById(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClientBroadcastStream newBroadcastStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Number, IClientStream> getStreamsMap() {
		// TODO Auto-generated method stub
		return null;
	}	
}
