package org.tl.nettyServer.media.stream.proxy;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.codec.RtmpProtocolState;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.session.SessionFacade;

public class ProxyClientConnection extends RTMPConnection {

    public ProxyClientConnection() {
        super(Type.PERSISTENT.toString());
        this.state = new RtmpProtocolState();
    }

    @Override
    public void write(Packet out) {

    }

    @Override
    protected void onInactive() {
        this.close();
    }

    @Override
    public void writeRaw(BufFacade out) {
        // TODO Auto-generated method stub

    }

	@Override
	public void setSession(SessionFacade s) {

	}

	@Override
	public SessionFacade getSession() {
		return null;
	}
}
