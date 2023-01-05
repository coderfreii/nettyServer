package org.tl.nettyServer.media.net.udp.mina;


/**
 * The Datagram transport session configuration.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractDatagramSessionConfig extends AbstractIoSessionConfig implements DatagramSessionConfig {
    /** Tells if we should close the session if the port is unreachable. Default to true */
    private boolean closeOnPortUnreachable = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(IoSessionConfig config) {
        super.setAll(config);

        if (!(config instanceof DatagramSessionConfig)) {
            return;
        }

        if (config instanceof AbstractDatagramSessionConfig) {
            // Minimize unnecessary system calls by checking all 'propertyChanged' properties.
            AbstractDatagramSessionConfig cfg = (AbstractDatagramSessionConfig) config;

            if (cfg.isBroadcastChanged()) {
                setBroadcast(cfg.isBroadcast());
            }

            if (cfg.isReceiveBufferSizeChanged()) {
                setReceiveBufferSize(cfg.getReceiveBufferSize());
            }

            if (cfg.isReuseAddressChanged()) {
                setReuseAddress(cfg.isReuseAddress());
            }

            if (cfg.isSendBufferSizeChanged()) {
                setSendBufferSize(cfg.getSendBufferSize());
            }

            if (cfg.isTrafficClassChanged() && getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        } else {
            DatagramSessionConfig cfg = (DatagramSessionConfig) config;
            setBroadcast(cfg.isBroadcast());
            setReceiveBufferSize(cfg.getReceiveBufferSize());
            setReuseAddress(cfg.isReuseAddress());
            setSendBufferSize(cfg.getSendBufferSize());

            if (getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        }
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>broadcast</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isBroadcastChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>receiveBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isReceiveBufferSizeChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>reuseAddress</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isReuseAddressChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>sendBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isSendBufferSizeChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>trafficClass</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isTrafficClassChanged() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCloseOnPortUnreachable() {
        return closeOnPortUnreachable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCloseOnPortUnreachable(boolean closeOnPortUnreachable) {
        this.closeOnPortUnreachable = closeOnPortUnreachable;
    }
}
