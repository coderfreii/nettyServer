package org.tl.nettyServer.media.stream.consumer;


import org.tl.nettyServer.media.net.rtmp.conn.IConnection;

public interface ICustomPushableConsumer extends IPushableConsumer {

	IConnection getConnection();
}
