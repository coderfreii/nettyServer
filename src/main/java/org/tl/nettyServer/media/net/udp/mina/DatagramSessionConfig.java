package org.tl.nettyServer.media.net.udp.mina;

import java.net.DatagramSocket;
/**
 * An {@link IoSessionConfig} for datagram transport type.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface DatagramSessionConfig extends IoSessionConfig {
    /**
     * @see DatagramSocket#getBroadcast()
     *
     * @return <tt>true</tt> if SO_BROADCAST is enabled.
     */
    boolean isBroadcast();

    /**
     * @see DatagramSocket#setBroadcast(boolean)
     *
     * @param broadcast Tells if SO_BROACAST is enabled or not
     */
    void setBroadcast(boolean broadcast);

    /**
     * @see DatagramSocket#getReuseAddress()
     *
     * @return <tt>true</tt> if SO_REUSEADDR is enabled.
     */
    boolean isReuseAddress();

    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     *
     * @param reuseAddress Tells if SO_REUSEADDR is enabled or disabled
     */
    void setReuseAddress(boolean reuseAddress);

    /**
     * @see DatagramSocket#getReceiveBufferSize()
     *
     * @return the size of the receive buffer
     */
    int getReceiveBufferSize();

    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     *
     * @param receiveBufferSize The size of the receive buffer
     */
    void setReceiveBufferSize(int receiveBufferSize);

    /**
     * @see DatagramSocket#getSendBufferSize()
     *
     * @return the size of the send buffer
     */
    int getSendBufferSize();

    /**
     * @see DatagramSocket#setSendBufferSize(int)
     *
     * @param sendBufferSize The size of the send buffer
     */
    void setSendBufferSize(int sendBufferSize);

    /**
     * @see DatagramSocket#getTrafficClass()
     *
     * @return the traffic class
     */
    int getTrafficClass();

    /**
     * @see DatagramSocket#setTrafficClass(int)
     *
     * @param trafficClass The traffic class to set, one of IPTOS_LOWCOST (0x02)
     * IPTOS_RELIABILITY (0x04), IPTOS_THROUGHPUT (0x08) or IPTOS_LOWDELAY (0x10)
     */
    void setTrafficClass(int trafficClass);

    /**
     * If method returns true, it means session should be closed when a
     * {@link PortUnreachableException} occurs.
     *
     * @return Tells if we should close if the port is unreachable
     */
    boolean isCloseOnPortUnreachable();

    /**
     * Sets if the session should be closed if an {@link PortUnreachableException}
     * occurs.
     *
     * @param closeOnPortUnreachable <tt>true</tt> if we should close if the port is unreachable
     */
    void setCloseOnPortUnreachable(boolean closeOnPortUnreachable);
}

