package org.tl.nettyServer.media.net.rtsp.rtp.packetizer;

/**
 * RTP Packetizer Audio Base
 * @author pengliren
 *
 */
public abstract class RTPPacketizerAudioBase extends RTPPacketizerBase {

	public RTPPacketizerAudioBase() {
		
		baseType = "aud";
	}
}
