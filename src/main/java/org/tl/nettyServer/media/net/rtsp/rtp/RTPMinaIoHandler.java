package org.tl.nettyServer.media.net.rtsp.rtp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTP Mina Io Handler
 * @author pengliren
 *
 */
public class RTPMinaIoHandler   {

	private static Logger log = LoggerFactory.getLogger(RTPMinaIoHandler.class);
	
	public RTPMinaIoHandler() {

	}
	
//	@Override
//	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
//
//		log.info("rtp exception {}", cause.getMessage());
//		session.close(true);
//	}
//
//	@Override
//	public void messageReceived(IoSession session, Object message) throws Exception {
//
//	}
//
//	@Override
//	public void sessionClosed(IoSession session) throws Exception {
//
//		log.info("rtp session close");
//	}
//
//	@Override
//	public void sessionCreated(IoSession session) throws Exception {
//
//		log.info("rtp session create");
//	}

}
