package org.tl.nettyServer.servers.net.rtmp.conn;

import org.apache.commons.lang3.RandomStringUtils;
import org.tl.nettyServer.servers.net.rtmp.AttributeStore;
import org.tl.nettyServer.servers.net.rtmp.session.SessionFacade;


public abstract class BaseConnection extends AttributeStore implements IConnection {
    private String sessionId;
    private SessionFacade sessionFacade;


    public String getSessionId() {
        return this.sessionId;
    }

    BaseConnection() {
        this.sessionId = RandomStringUtils.randomAlphanumeric(13).toUpperCase();
    }

    @Override
    public void setSession(SessionFacade sessionFacade) {
        this.sessionFacade = sessionFacade;
    }

    @Override
    public SessionFacade getSession() {
        return this.sessionFacade;
    }
}
