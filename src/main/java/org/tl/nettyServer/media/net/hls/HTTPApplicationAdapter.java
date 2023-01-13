package org.tl.nettyServer.media.net.hls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.codec.QueryStringDecoder;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.message.HTTPChunk;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.net.http.service.IHTTPService;

import org.tl.nettyServer.media.net.hls.service.HttpServiceResolver;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.scope.Scope;
import org.tl.nettyServer.media.stream.client.CustomSingleItemSubStream;
import org.tl.nettyServer.media.stream.proxy.HTTPPushProxyStream;

import java.nio.charset.StandardCharsets;

/**
 * HTTP Application Adapter
 * @author pengliren
 *
 */
public class HTTPApplicationAdapter implements IHTTPApplicationAdapter {

	private static Logger log = LoggerFactory.getLogger(HTTPApplicationAdapter.class);
	
	private IScope scope;
	
	private HttpServiceResolver httpServiceResolver;
	
	public HTTPApplicationAdapter() {
		init();
	}
	
	private void init() {
		httpServiceResolver = new HttpServiceResolver();
	}
	
	@Override
	public void onHTTPRequest(HTTPRequest req, HTTPResponse resp) throws Exception {
		String fullPath = req.getPath();
		QueryStringDecoder decoder = new QueryStringDecoder(fullPath);
		String path = decoder.getPath();
		// handle add http service

		IHTTPService ihttpService = httpServiceResolver.find(path);
		if(ihttpService == null) {
			HTTPConnection conn = (HTTPConnection) Red5.getConnectionLocal();
			resp.setStatus(HTTPResponseStatus.NOT_FOUND);
			resp.setContent(BufFacade.wrappedBuffer("未找到服务".getBytes(StandardCharsets.UTF_8)));
			httpServiceResolver.getDefaultHttpService().handleRequest(req,resp);
			return;
		}


		ihttpService.handleRequest(req, resp, scope);
	}


	@Override
	public void onHTTPChunk(HTTPChunk chunk) throws Exception {
		HTTPConnection conn = (HTTPConnection)Red5.getConnectionLocal();
		if(conn.getAttribute("pushStream") != null) {
			HTTPPushProxyStream pushStream = (HTTPPushProxyStream)conn.getAttribute("pushStream");
			if(pushStream != null) pushStream.handleMessage(chunk.getContent());
		}
	}
	
	@Override
	public void onConnectionStart(HTTPConnection conn) {
		
	}

	@Override
	public void onConnectionClose(HTTPConnection conn) {
		
		if(conn.getAttribute("consumer") != null) {
			HTTPConnectionConsumer consumer = (HTTPConnectionConsumer)conn.getAttribute("consumer"); 
			consumer.setClose(true);
		}
		
		if(conn.getAttribute("stream") != null) {
			CustomSingleItemSubStream stream = (CustomSingleItemSubStream)conn.getAttribute("stream");
			stream.close();
		}
		
		if(conn.getAttribute("pushStream") != null) {
			HTTPPushProxyStream pushStream = (HTTPPushProxyStream)conn.getAttribute("pushStream");
			pushStream.stop();
		}
	}

	@Override
	public void setScope(IScope scope) {
		this.scope = scope;
	}

	@Override
	public IScope getScope() {
		return this.scope;
	}

	@Override
	public void addHttpService(String name, IHTTPService httpService) {
		httpServiceResolver.addHttpService(name,httpService);
	}
	
	@Override
	public IHTTPService getHttpService(String name) {
		return httpServiceResolver.getHttpService(name);
	}

	@Override
	public void removeHttpService(String name) {
		httpServiceResolver.removeHttpService(name);
	}
}
