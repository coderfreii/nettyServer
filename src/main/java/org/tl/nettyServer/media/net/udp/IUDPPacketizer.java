package org.tl.nettyServer.media.net.udp;



import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.stream.data.IStreamPacket;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * UDPPacketizer Inteface
 * @author pengliren
 *
 */
public interface IUDPPacketizer {

	public void handleStreamPacket(IStreamPacket packet);
	
	public void stop();
	
	public IUDPTransportOutgoingConnection getConnection();
	
	public void setVideoConfig(BufFacade config);
	
	public void setAudioConfig(BufFacade config);
	
	public BufFacade getVideoConfig();
	
	public BufFacade getAudioConfig();
	
	public AtomicInteger getFrameCount();
	
	public boolean isInit();
}
