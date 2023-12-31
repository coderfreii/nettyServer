//package org.tl.nettyServer.media.net.udp;
//
//import org.apache.mina.core.buffer.BufFacade;
//import org.red5.io.ts.FLV2MPEGTSWriter;
//import org.red5.io.ts.IFLV2MPEGTSWriter;
//import org.red5.server.api.stream.IStreamPacket;
//import org.red5.server.net.rtmp.event.AudioData;
//import org.red5.server.net.rtmp.event.VideoData;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.red5.io.ts.TransportStreamUtils.TIME_SCALE;
//
///**
// * UDP Mpegts
// * @author pengliren
// *
// */
//public class UDPPacketizerMPEGTS implements IUDPPacketizer, IFLV2MPEGTSWriter {
//
//	private static Logger log = LoggerFactory.getLogger(UDPPacketizerMPEGTS.class);
//
//	private FLV2MPEGTSWriter flv2tsWriter;
//
//	private IUDPTransportOutgoingConnection conn;
//
//	protected long lastPAT = -1L;
//
//	protected int mpegtsPacketsPerBlock = 7;
//
//	private BufFacade buffer;
//
//	private AtomicInteger pcount = new AtomicInteger(0);
//
//	private AtomicInteger frameCounter = new AtomicInteger();
//
//	private BufFacade videoConfig;
//
//	private BufFacade audioConfig;
//
//	private boolean init = false;
//
//	public UDPPacketizerMPEGTS(IUDPTransportOutgoingConnection conn) {
//
//		this.conn = conn;
//	}
//
//	@Override
//	public void nextBlock(long ts, byte[] block) {
//		if(buffer == null) {
//			buffer = BufFacade.allocate(1500).setAutoExpand(true);
//		}
//
//		if(pcount.get() >= mpegtsPacketsPerBlock) {
//			buffer.flip();
//			flushBlock();
//			pcount.set(0);
//			buffer.position(0);
//		}
//		buffer.put(block);
//		pcount.incrementAndGet();
//	}
//
//	private void flushBlock() {
//
//		byte[] payload = new byte[buffer.remaining()];
//		buffer.get(payload);
//		conn.sendMessage(payload, 0, payload.length);
//	}
//
//	@Override
//	public void handleStreamPacket(IStreamPacket packet) {
//
//		if (!init) {
//			flv2tsWriter = new FLV2MPEGTSWriter(this, videoConfig, audioConfig);
//			init = true;
//			log.info("flv to mepgts udp stream!");
//		}
//
//		long cts = System.currentTimeMillis();
//		long ts = packet.getTimestamp() * TIME_SCALE;
//		if ((lastPAT == -1L) || (cts - lastPAT > 100L)) {
//			lastPAT = cts;
//			flv2tsWriter.addPAT(ts);
//		}
//
//		if (packet instanceof VideoData) { // handle video
//			flv2tsWriter.handleVideo((VideoData) packet);
//		} else if (packet instanceof AudioData) {// handle audio
//			flv2tsWriter.handleAudio((AudioData) packet);
//		}
//	}
//
//	@Override
//	public void stop() {
//
//		if(conn != null) {
//			conn.close();
//		}
//	}
//
//	@Override
//	public IUDPTransportOutgoingConnection getConnection() {
//		return conn;
//	}
//
//	@Override
//	public BufFacade getAudioConfig() {
//
//		return audioConfig;
//	}
//
//	@Override
//	public void setAudioConfig(BufFacade config) {
//
//		audioConfig = config;
//	}
//
//	@Override
//	public void setVideoConfig(BufFacade config) {
//
//		videoConfig = config;
//	}
//
//	@Override
//	public BufFacade getVideoConfig() {
//
//		return videoConfig;
//	}
//
//	@Override
//	public AtomicInteger getFrameCount() {
//
//		return frameCounter;
//	}
//
//	@Override
//	public boolean isInit() {
//
//		return init;
//	}
//}
