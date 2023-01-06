package org.tl.nettyServer.media.net.rtsp.rtp.packetizer;

import gov.nist.javax.sdp.MediaDescriptionImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.media.ts.FLV2MPEGTSWriter;
import org.tl.nettyServer.media.media.ts.IFLV2MPEGTSWriter;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPacket;
import org.tl.nettyServer.media.stream.data.IStreamPacket;

import javax.sdp.SdpFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.tl.nettyServer.media.media.ts.codec.VideNaluInfosResolver.TIME_SCALE;


/**
 * RTP Packetizer MPEGTS
 * @author pengliren
 * 
 */
public class RTPPacketizerMPEGTS extends RTPPacketizerVideoBase implements IRTPPacketizer, IFLV2MPEGTSWriter {

	private static Logger log = LoggerFactory.getLogger(RTPPacketizerMPEGTS.class);
	
	private FLV2MPEGTSWriter flv2tsWriter;
	
	protected long lastPAT = -1L;
	
	protected int mpegtsPacketsPerBlock = 7;
	
	private BufFacade buffer;
	
	private AtomicInteger pcount = new AtomicInteger(0);
	  
	public RTPPacketizerMPEGTS(BufFacade videoConfig, BufFacade audioConfig) {
	
		this.sdpTypeId = RTPTYPE_MP2T;
		this.timeScale = 90000;
		flv2tsWriter = new FLV2MPEGTSWriter(this, videoConfig, audioConfig);
		log.info("flv to mepgts rtp stream!");
	}
		
	@Override
	public void handleStreamPacket(IStreamPacket packet) {

		long cts = System.currentTimeMillis();
		long ts = packet.getTimestamp() * TIME_SCALE;
		if ((lastPAT == -1L) || (cts - lastPAT > 100L)) {
			lastPAT = cts;
			flv2tsWriter.addPAT(ts);
		}
		
		if (packet instanceof VideoData) { // handle video
			flv2tsWriter.handleVideo((VideoData) packet);
		} else if (packet instanceof AudioData) {// handle audio
			flv2tsWriter.handleAudio((AudioData) packet);
		}
	}
	
	/**
	 * callback next ts packet
	 */
	@Override
	public void nextBlock(long ts, byte[] block) {
		
		if(buffer == null) {
			buffer = BufFacade.buffer(1500);
		}
		
		if(pcount.get() >= mpegtsPacketsPerBlock) {
			flushBlock(ts);
			pcount.set(0);
			buffer.readerIndex(0);
		}
		buffer.writeBytes(block);
		pcount.incrementAndGet();
	}
	
	/**
	 * flush ts block
	 * @param ts
	 */
	private void flushBlock(long ts) {
		
		byte[] payload = new byte[buffer.readableBytes()];
		buffer.readBytes(payload);
		
		RTPPacket rtpPacket = new RTPPacket();
		rtpPacket.setPayload(payload);
		rtpPacket.setChannel((byte) 0x00);		
		rtpPacket.setMarker(false);
		rtpPacket.setPadding(false);	
		rtpPacket.setExtensions(false);
		rtpPacket.setTimestamp(ts);
		rtpPacket.setSeqNumber(getNextSequence());
		rtpPacket.setSsrc(getSsrc());
		rtpPacket.setPayloadType(sdpTypeId);
		write(rtpPacket);
	}
	
	@Override
	public MediaDescriptionImpl getDescribeInfo(BufFacade config) throws Exception {

		MediaDescriptionImpl describeInfo;//audio md
		describeInfo = (MediaDescriptionImpl)SdpFactory.getInstance().createMediaDescription("video", 0, 0, "RTP/AVP", new int[]{sdpTypeId});
		describeInfo.setAttribute("rtpmap",
				new StringBuilder()
						.append(sdpTypeId)
						.append(" MP2T/")
						.append(timeScale)
						.toString());
        return describeInfo;
	}
}
