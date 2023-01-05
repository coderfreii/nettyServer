package org.tl.nettyServer.media.net.rtsp.rtp;


import org.tl.nettyServer.media.buf.BufFacade;

/**
 * Base interface for RTP packets
 * 
 * @author pengliren
 */
public interface IRTPPacket {

	public byte getChannel();

	public void setChannel(byte channel);
	
	public BufFacade toByteBuffer();
}
