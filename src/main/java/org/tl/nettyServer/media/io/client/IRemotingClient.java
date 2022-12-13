package org.tl.nettyServer.media.io.client;

public interface IRemotingClient {

    Object invokeMethod(String method, Object[] params);

}
