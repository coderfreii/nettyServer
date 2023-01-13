package org.tl.nettyServer.media.net.http.conn;

import java.util.Collection;

public interface IHTTPConnManager {

	HTTPConnection getConnection(long clientId);

	void addConnection(HTTPConnection conn, long clientId);

	HTTPConnection removeConnection(long clientId);

	Collection<HTTPConnection> removeConnections();
	
	int getConnectionCount();

	HTTPConnection CreateConnection();
}
