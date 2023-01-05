package org.tl.nettyServer.media.net.udp;

import java.net.SocketAddress;

/**
 * UDP Transport Session
 * @author pengliren
 *
 */
public interface IUDPTransportSession {

	public void write(byte[] data, int pos, int len, SocketAddress address);
}
