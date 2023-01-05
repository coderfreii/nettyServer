package org.tl.nettyServer.media.net.udp;

/**
 * UDP Sender
 * @author pengliren
 *
 */
public interface IUDPSender {

	public void handleSendMessage(byte[] data, int pos, int len);
}
