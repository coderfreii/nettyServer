package org.tl.nettyServer.media.net.rtmp.conn;

import org.tl.nettyServer.media.net.rtmp.session.SessionFacade;

public interface SessionConnection {
    void setSession(SessionFacade s);

    SessionFacade getSession();
}
