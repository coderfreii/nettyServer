package org.tl.nettyServer.media.net.rtsp.rtp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.conf.ExtConfiguration;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.rtp.packetizer.IRTPPacketizer;
import org.tl.nettyServer.media.net.udp.IUDPTransportOutgoingConnection;
import org.tl.nettyServer.media.net.udp.UnicastOutgoing;
import org.tl.nettyServer.media.stream.base.IBroadcastStream;
import org.tl.nettyServer.media.stream.data.IStreamPacket;
import org.tl.nettyServer.media.stream.lisener.IStreamListener;

import java.net.InetSocketAddress;

/**
 * rtp live and vod play
 * tcp and udp support
 * @author pengliren
 *
 */
public class RTPPlayer implements IStreamListener {

	private static Logger log = LoggerFactory.getLogger(RTPPlayer.class);

	private boolean isUdp;
	
	private IRTPPacketizer videoRtpPacketizer;
	
	private IRTPPacketizer audioRtpPacketizer;
	
	private static UnicastOutgoing unicastout = new UnicastOutgoing();
	
	static {
		unicastout.init(ExtConfiguration.UNICAST_EXECUTOR_THREADS);
	}
	
	private InetSocketAddress videoRtpAddress;
	
	private InetSocketAddress audioRtpAddress;
	
	
	public RTPPlayer(RTSPMinaConnection conn, IRTPPacketizer videoRtpPacketizer, IRTPPacketizer audioRtpPacketizer) {

		this.videoRtpPacketizer = videoRtpPacketizer;
		this.audioRtpPacketizer = audioRtpPacketizer;
		if(this.videoRtpPacketizer != null) this.videoRtpPacketizer.setOutputStream(conn);
		if(this.audioRtpPacketizer != null) this.audioRtpPacketizer.setOutputStream(conn);
		this.isUdp = false;
	}
	
	public void close() {		
		// close rtcp socket
		if(this.videoRtpPacketizer != null) videoRtpPacketizer.stop();
		if(this.audioRtpPacketizer != null) audioRtpPacketizer.stop();
	}

	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
		if (packet.getDataType() == Constants.TYPE_VIDEO_DATA && videoRtpPacketizer != null) {
			videoRtpPacketizer.handleStreamPacket(packet);
		} else if (packet.getDataType() == Constants.TYPE_AUDIO_DATA && audioRtpPacketizer != null) {
			audioRtpPacketizer.handleStreamPacket(packet);	
		}
	}

	public boolean isUdp() {
		return isUdp;
	}

	public IRTPPacketizer getVideoRtpPacketizer() {
		return videoRtpPacketizer;
	}

	public IRTPPacketizer getAudioRtpPacketizer() {
		return audioRtpPacketizer;
	}

	public InetSocketAddress getVideoRtpAddress() {
		return videoRtpAddress;
	}

	public InetSocketAddress getAudioRtpAddress() {
		return audioRtpAddress;
	}

	public void setVideoRtpAddress(InetSocketAddress videoRtpAddress) {
		this.isUdp = true;
		IUDPTransportOutgoingConnection conn = unicastout.connect(null, videoRtpAddress);
		this.videoRtpAddress = videoRtpAddress;
		if(conn == null) {
			log.error("connect video rtp address : {}", videoRtpAddress);
			return;
		}
		this.videoRtpPacketizer.setOutputStream(conn);
	}

	public void setAudioRtpAddress(InetSocketAddress audioRtpAddress) {
		this.isUdp = true;
		IUDPTransportOutgoingConnection conn = unicastout.connect(null, audioRtpAddress);
		this.audioRtpAddress = audioRtpAddress;
		if(conn == null) {
			log.error("connect audio rtp address : {}", audioRtpAddress);
			return;
		}
		this.audioRtpPacketizer.setOutputStream(conn);
	}
}
