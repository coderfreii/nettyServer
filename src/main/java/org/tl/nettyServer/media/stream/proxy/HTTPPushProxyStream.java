package org.tl.nettyServer.media.stream.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.scope.IScope;


/**
 * http 推流
 * @author pengliren
 *
 */
public class HTTPPushProxyStream extends HTTPProxyStream {

	private static Logger log = LoggerFactory.getLogger(HTTPPushProxyStream.class);
	
	public HTTPPushProxyStream(IScope scope, String streamName) {
		super(scope,streamName);
	}

	@Override
	public void start() {
		
		if(getScope() == null) {
			throw new RuntimeException("scope is null!");
		}
		if(start) return;
		synchronized (lock) {
			super.start();
			register();
			start = true;
			connManager.register(publishedName, this);
		}
		
		log.info("http push proxy stream {} is start!", getPublishedName());
	}
}
