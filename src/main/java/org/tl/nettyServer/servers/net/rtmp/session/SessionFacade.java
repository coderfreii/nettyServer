package org.tl.nettyServer.servers.net.rtmp.session;

import org.tl.nettyServer.servers.net.rtmp.conn.IConnection;

import javax.crypto.Cipher;

public interface SessionFacade<T> {
    static String sessionKey = "sessionId";

    void setSession(T t);

    void setConnection(IConnection connection);

    void setCipherIn(Cipher cipherIn);

    void setCipherOut(Cipher cipherOut);

    Cipher getCipherIn();

    Cipher getCipherOut();
}
