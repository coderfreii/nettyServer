package org.tl.nettyServer.media.net.udp.mina;


/**
 * A default implementation of {@link DatagramSessionConfig}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultDatagramSessionConfig extends AbstractDatagramSessionConfig {
    private static final boolean DEFAULT_BROADCAST = false;

    private static final boolean DEFAULT_REUSE_ADDRESS = false;

    /* The SO_RCVBUF parameter. Set to -1 (ie, will default to OS default) */
    private static final int DEFAULT_RECEIVE_BUFFER_SIZE = -1;

    /* The SO_SNDBUF parameter. Set to -1 (ie, will default to OS default) */
    private static final int DEFAULT_SEND_BUFFER_SIZE = -1;

    private static final int DEFAULT_TRAFFIC_CLASS = 0;

    private boolean broadcast = DEFAULT_BROADCAST;

    private boolean reuseAddress = DEFAULT_REUSE_ADDRESS;

    private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;

    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;

    private int trafficClass = DEFAULT_TRAFFIC_CLASS;

    /**
     * Creates a new instance.
     */
    public DefaultDatagramSessionConfig() {
        // Do nothing
    }

    /**
     * @see DatagramSocket#getBroadcast()
     */
    @Override
    public boolean isBroadcast() {
        return broadcast;
    }

    /**
     * @see DatagramSocket#setBroadcast(boolean)
     */
    @Override
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    /**
     * @see DatagramSocket#getReuseAddress()
     */
    @Override
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     */
    @Override
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * @see DatagramSocket#getReceiveBufferSize()
     */
    @Override
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     */
    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * @see DatagramSocket#getSendBufferSize()
     */
    @Override
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * @see DatagramSocket#setSendBufferSize(int)
     */
    @Override
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * @see DatagramSocket#getTrafficClass()
     */
    @Override
    public int getTrafficClass() {
        return trafficClass;
    }

    /**
     * @see DatagramSocket#setTrafficClass(int)
     */
    @Override
    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isBroadcastChanged() {
        return broadcast != DEFAULT_BROADCAST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReceiveBufferSizeChanged() {
        return receiveBufferSize != DEFAULT_RECEIVE_BUFFER_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReuseAddressChanged() {
        return reuseAddress != DEFAULT_REUSE_ADDRESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSendBufferSizeChanged() {
        return sendBufferSize != DEFAULT_SEND_BUFFER_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isTrafficClassChanged() {
        return trafficClass != DEFAULT_TRAFFIC_CLASS;
    }
}