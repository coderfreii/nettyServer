package org.tl.nettyServer.media.net.udp;



import org.tl.nettyServer.media.buf.BufFacade;

import java.net.SocketAddress;

/**
 * UDP Message Handler
 * @author pengliren
 *
 */
public interface IUDPMessageHandler {

	public void handleMessage(SocketAddress address, BufFacade buffer);

	public void sessionOpened(IUDPTransportSession session);

	public void sessionClosed(IUDPTransportSession session);
}
