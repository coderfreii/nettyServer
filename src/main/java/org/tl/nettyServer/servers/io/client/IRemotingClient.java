package org.tl.nettyServer.servers.io.client;

public interface IRemotingClient {

    Object invokeMethod(String method, Object[] params);

}
