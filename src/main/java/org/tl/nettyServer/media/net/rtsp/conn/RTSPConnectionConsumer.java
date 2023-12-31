package org.tl.nettyServer.media.net.rtsp.conn;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.ReleaseUtil;
import org.tl.nettyServer.media.messaging.IMessage;
import org.tl.nettyServer.media.messaging.IMessageComponent;
import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.messaging.OOBControlMessage;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.stream.consumer.ICustomPushableConsumer;
import org.tl.nettyServer.media.stream.data.IStreamPacket;
import org.tl.nettyServer.media.stream.lisener.IStreamListener;
import org.tl.nettyServer.media.stream.message.Duplicateable;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.stream.message.StatusMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RTSP Conection Consumer
 *
 * @author penglrien
 */
public class RTSPConnectionConsumer implements ICustomPushableConsumer {

    private static Logger log = LoggerFactory.getLogger(RTSPConnectionConsumer.class);

    private boolean closed = false;

    private List<IStreamListener> listeners;

    private RTSPMinaConnection conn;

    public RTSPConnectionConsumer(RTSPMinaConnection conn) {

        this.conn = conn;
        listeners = new CopyOnWriteArrayList<IStreamListener>();
    }

    @Override
    public void pushMessage(IPipe pipe, IMessage message) throws IOException {

        if (message instanceof RTMPMessage) {
            if (((RTMPMessage) message).getBody() instanceof IStreamPacket) {
                IStreamPacket packet = (IStreamPacket) (((RTMPMessage) message).getBody());
                if (packet.getData() != null) {
                    for (IStreamListener listener : listeners) {
                        listener.packetReceived(null, Duplicateable.doDuplicate(packet));
                    }

                    ReleaseUtil.releaseAll(packet);
                    packet = null;
                }
            }
        } else if (message instanceof StatusMessage) {
            if (((StatusMessage) message).getBody().getCode().equals(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY)) {
                closed = true;
                conn.close();
            }
        }

    }

    @Override
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
                                    OOBControlMessage oobCtrlMsg) {

    }

    @Override
    public IConnection getConnection() {

        return conn;
    }

    public boolean isClosed() {
        return closed;
    }

    public void addStreamListener(IStreamListener listener) {

        log.info("add a stream listener");
        this.listeners.add(listener);
    }

    public void removeStreamListener(IStreamListener listener) {

        if (listener != null) {
            log.info("remove a stream listener");
            this.listeners.remove(listener);
        }
    }
}
