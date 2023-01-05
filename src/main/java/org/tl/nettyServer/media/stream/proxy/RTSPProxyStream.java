package org.tl.nettyServer.media.stream.proxy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.event.IEvent;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPacket;
import org.tl.nettyServer.media.service.call.IPendingServiceCall;

/**
 * rtsp proxy stream
 * @author pengliren
 * 
 */
public abstract class RTSPProxyStream extends BaseRTMPProxyStream {

	private static Logger log = LoggerFactory.getLogger(RTSPProxyStream.class);

	private volatile BufFacade currentNalu;

	private BufFacade avcConfig;

	private BufFacade aacConfig;
	
	private int videoTimescale;
	
	private int audioTimescale;
	
	private volatile boolean hasVideo = false;
	
	private volatile boolean hasAudio = false;

	public RTSPProxyStream(String streamName) {

		super();
		setPublishedName(streamName);
	}

	@Override
	public void resultReceived(IPendingServiceCall call) {

		log.info("rtsp proxy handle call result:{}", call);
	}

	public void handleMessage(RTPPacket packet) throws Exception {
		
		byte channel = packet.getChannel();
		switch (channel) {
		case 0x00:// video
			if(hasVideo) encodeVideoData(packet, videoTimescale);
			break;
		case 0x02:// audio
			if(hasAudio) encodeAudioData(packet, audioTimescale);
			break;
		default:// unkown
			break;
		}
	}

	private void encodeVideoData(RTPPacket packet, int timeScale) throws Exception {
		BufFacade data = BufFacade.wrappedBuffer(packet.getPayload());
		byte naluHeader = data.readByte();
		int naluType = naluHeader & 0x1F;
		if ((naluType >=0) && (naluType <= 23)) { // Single NALU Packet 
			data.rewind();
			BufFacade videoData = BufFacade.buffer(2 + 3 + 4 + data.readableBytes());
			if (naluType == 5) {
				videoData.writeByte((byte) 0x17); // keyframe
			}
			else videoData.writeByte((byte) 0x27);// nonkeyframe
			videoData.writeByte((byte) 0x01);
			videoData.writeByte((byte) 0);
			videoData.writeByte((byte) 0);
			videoData.writeByte((byte) 0);
			videoData.writeInt(data.readableBytes());
			videoData.writeBytes(data);
			VideoData vData = new VideoData();
			vData.setData(videoData);
			vData.setTimestamp(Math.round((float) packet.getTimestamp() / (timeScale / 1000)));
			dispatchEvent(vData);
		} else if (naluType == 28) {// NALU_TYPE_FUA
			byte fuHeader = data.readByte();
			int fuNaluType = fuHeader & 0x1F;
			//System.err.println("nalue type : "+fuNaluType);
			int fuHeaderS = (fuHeader & 0xFF) >> 7;
			// first NAL
			if (fuHeaderS == 1) {
				//System.err.println("start");
				currentNalu = BufFacade.buffer(1500);
				byte nalu = (byte) (((data.getByte(0) & 0xE0) | (data.getByte(1) & 0x1F)) & 0xFF);
				currentNalu.writeByte(nalu); // put 1byte nalu header
				currentNalu.writeBytes(data); // put nalu data
				return;
			} else {
				// middle NAL
				if(currentNalu == null) return;
				currentNalu.writeBytes(data);
				
				if ((((fuHeader & 0xFF) >> 6) & 0x01) == 1) { // FU Finish
					BufFacade videoData = BufFacade.buffer(2 + 3 + 4 + currentNalu.readableBytes());
					if (fuNaluType == 5) {
						videoData.writeByte((byte) 0x17); // keyframe
					}
					else videoData.writeByte((byte) 0x27); // nonkeyframe
					videoData.writeByte((byte) 0x01);
					videoData.writeByte((byte) 0);
					videoData.writeByte((byte) 0);
					videoData.writeByte((byte) 0);
					videoData.writeInt(currentNalu.readableBytes());
					videoData.writeBytes(currentNalu);
					VideoData vData = new VideoData();
					vData.setData(videoData);
					vData.setTimestamp(Math.round((float) packet.getTimestamp() / (timeScale / 1000)));
					dispatchEvent(vData);
					currentNalu = null;
				}
			}
		} else if (naluType == 24) {// NALU_TYPE_STAPA			
			log.debug("rtsp proxy stream unsupported packet type: STAP-A");
		} else if(naluType == 25) { // STAP-B 
			log.info("rtsp proxy stream unsupported packet type: STAP-B");
		} else if(naluType == 26) {// MTAP-16
			log.info("rtsp proxy stream unsupported packet type: MTAP-16");
		} else if(naluType == 27) {// MTAP-24
			log.info("rtsp proxy stream unsupported packet type: MTAP-24");
		} else if(naluType == 29) {// FU-B
			log.info("rtsp proxy stream unsupported packet type: FU-B");
		} else {// unknow
			log.info("rtsp proxy stream unsupported packet type: unknow");
		}
	}

	private void encodeAudioData(RTPPacket packet, int timeScale) {

		BufFacade data = BufFacade.wrappedBuffer(packet.getPayload());
		
		if(data.readableBytes() <= 4) return;
		
		// auheader start code 0x00, 0x10, ausize
		data.skipBytes(2); // skip start code 2byte
		int au13 = ((data.readByte() << 5) & 0x1FE0) & 0xFFFF;
		int au3 = ((data.readByte() >> 3) & 0x1F) & 0xFF;
		int ausize = au13 | au3;
		log.debug("aac au size : "+ ausize +" packe len : "+data.readableBytes());
		
		byte[] aacPload = new byte[data.readableBytes()];
		data.readBytes(aacPload);
		BufFacade aacData = BufFacade.buffer(2 + aacPload.length);
		aacData.writeByte((byte)0xaf);
		aacData.writeByte((byte)0x01);
		aacData.writeBytes(aacPload);
		
		AudioData aData = new AudioData();
		aData.setData(aacData);
		aData.setTimestamp(Math.round((float) packet.getTimestamp() / (timeScale / 1000)));
		dispatchEvent(aData);
	}
	
	@Override
	public void dispatchEvent(IEvent event) {
		super.dispatchEvent(event);
	}

	public void setAVCConfig(BufFacade avcConfig) {
		hasVideo = true;
		this.avcConfig = avcConfig;
		VideoData vData = new VideoData();
		vData.setData(avcConfig);
		vData.setTimestamp(0);
		dispatchEvent(vData);		
	}

	public void setAACConfig(BufFacade aacConfig) {
		hasAudio = true;
		this.aacConfig = aacConfig;
		AudioData aData = new AudioData();
		aData.setData(aacConfig);
		aData.setTimestamp(0);
		dispatchEvent(aData);		
	}

	public int getVideoTimescale() {
		return videoTimescale;
	}

	public void setVideoTimescale(int videoTimescale) {
		this.videoTimescale = videoTimescale;
	}

	public int getAudioTimescale() {
		return audioTimescale;
	}

	public void setAudioTimescale(int audioTimescale) {
		this.audioTimescale = audioTimescale;
	}

	public BufFacade getAVCConfig() {
		return avcConfig;
	}

	public BufFacade getAACConfig() {
		return aacConfig;
	}
}
