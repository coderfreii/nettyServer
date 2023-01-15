package org.tl.nettyServer.media.net.http.conn;

import io.netty.channel.ChannelFuture;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.IHTTPApplicationAdapter;
import org.tl.nettyServer.media.net.rtmp.conn.BaseConnection;
import org.tl.nettyServer.media.session.NettyHttpSessionFacade;
import org.tl.nettyServer.media.stream.base.IClientStream;
import org.tl.nettyServer.media.stream.client.IClientBroadcastStream;
import org.tl.nettyServer.media.stream.client.IPlaylistSubscriberStream;
import org.tl.nettyServer.media.stream.client.ISingleItemSubscriberStream;
import org.tl.nettyServer.media.stream.conn.IStreamCapableConnection;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP  Connection
 *
 * @author Tl
 */
public class HTTPConnection extends BaseConnection implements IStreamCapableConnection {

    public static final String HTTP_CONNECTION_KEY = "http.conn";

    private boolean isClosed = false;

    private NettyHttpSessionFacade httpSession;

    private AtomicInteger pendings = new AtomicInteger();

    private IHTTPApplicationAdapter applicationAdapter;

    public HTTPConnection() {

    }

    public HTTPConnection(NettyHttpSessionFacade session) {
        httpSession = session;
    }

    public void close() {
        if (isClosed == false) {
            isClosed = true;
        } else {
            return;
        }

        if (applicationAdapter != null) applicationAdapter.onConnectionClose(this);
    }

    public ChannelFuture write(Object out) {
        pendings.incrementAndGet();
        return httpSession.write(out);
    }

    public void messageSent(Object message) {
        if (message instanceof BufFacade) {
            pendings.decrementAndGet();
        }
    }

    @Override
    public Encoding getEncoding() {
        return null;
    }

    @Override
    public void ping() {

    }

    @Override
    public int getLastPingTime() {
        return 0;
    }

    @Override
    public void setBandwidth(int mbits) {

    }

    @Override
    public long getReadBytes() {
        return 0;
    }

    @Override
    public long getWrittenBytes() {
        return 0;
    }

    @Override
    public long getPendingMessages() {

        return pendings.longValue();
    }

    public boolean isClosing() {
        return !httpSession.isConnected();
    }

    public void setHttpSession(NettyHttpSessionFacade httpSession) {
        this.httpSession = httpSession;
    }

    public NettyHttpSessionFacade getHttpSession() {
        return httpSession;
    }

    public IHTTPApplicationAdapter getApplicationAdapter() {
        return applicationAdapter;
    }

    public void setApplicationAdapter(IHTTPApplicationAdapter applicationAdapter) {
        this.applicationAdapter = applicationAdapter;
    }

    @Override
    public String getProtocol() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number reserveStreamId() throws IndexOutOfBoundsException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number reserveStreamId(Number streamId) throws IndexOutOfBoundsException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void unreserveStreamId(Number streamId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteStreamById(Number streamId) {
        // TODO Auto-generated method stub

    }

    @Override
    public IClientStream getStreamById(Number streamId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IClientBroadcastStream newBroadcastStream(Number streamId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Number, IClientStream> getStreamsMap() {
        // TODO Auto-generated method stub
        return null;
    }
}
