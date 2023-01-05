package org.tl.nettyServer.media.stream.proxy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.client.ScopeContextBean;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.scope.IBroadcastScope;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.IPendingServiceCallback;
import org.tl.nettyServer.media.service.provider.IProviderService;
import org.tl.nettyServer.media.stream.timeshift.RecordableBroadcastStream;

import java.util.UUID;

/**
 * 远程拉流到本地服务器
 * 如需要实现其他协议的拉流，都应继承该类
 * @author pengliren
 *
 */
public abstract class BaseRTMPProxyStream extends RecordableBroadcastStream implements IPendingServiceCallback, IProxyStream {

	private static Logger log = LoggerFactory.getLogger(BaseRTMPProxyStream.class);
	
	protected volatile boolean start = false;
	
	protected final ProxyStreamManager connManager;
	
	protected long lastReceiveTime = -1;
	
	protected Object lock = new Object();
	
	public BaseRTMPProxyStream() {
		
		connManager = ProxyStreamManager.getInstance();
		
		ProxyClientConnection conn = new ProxyClientConnection(); 
		setConnection(conn);
		Red5.setConnectionLocal(conn);
	}
	
	@Override
	public boolean isClosed() {
		if (lastReceiveTime > 0 && (System.currentTimeMillis() - lastReceiveTime) > 5000) {
    		log.info("proxy not receive data over 5 secs!");    		
    		stop();    		
    		return true;
    	}
    	return super.closed.get();
	}
	
	@Override
	public void register(){
		
		IScope scope = this.getScope();
		IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);
		IBroadcastScope bsScope = (IBroadcastScope) providerService.getLiveProviderInput(scope, this.getPublishedName(), true);
		this.setName(UUID.randomUUID().toString());
		providerService.registerBroadcastStream(scope, this.getPublishedName(), this);
		//bsScope.setAttribute(IBroadcastScope.STREAM_ATTRIBUTE, this);
	}
	
	@Override
	public void setScope(IScope scope) {
		
		super.setScope(scope);
		IConnection conn = getConnection();
		conn.connect(scope);
	}
}
