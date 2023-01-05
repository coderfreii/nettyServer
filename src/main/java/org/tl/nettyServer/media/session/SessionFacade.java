package org.tl.nettyServer.media.session;

import io.netty.channel.ChannelFuture;

import javax.crypto.Cipher;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public interface SessionFacade<T, S> {
    static String sessionKey = "sessionId";

    void setContext(T t);

    String getSessionId();

    void setConnection(S connection);

    public S getConnection();

    boolean isConnected();


    SocketAddress getRemoteAddress();

    SocketAddress getLocalAddress();


    ChannelFuture write(Object o);

    void setCipherIn(Cipher cipherIn);

    void setCipherOut(Cipher cipherOut);

    Cipher getCipherIn();

    Cipher getCipherOut();


    void closeOnFlush();
}
