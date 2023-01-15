package org.tl.nettyServer.media.net.http;


import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.message.HTTPChunk;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;

import org.tl.nettyServer.media.net.http.service.IHTTPService;

import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.scope.Scope;

/**
 * HTTP Application Adapter Inteface
 *
 * @author pengliren
 */
public interface IHTTPApplicationAdapter {

    public void onHTTPRequest(HTTPRequest req, HTTPResponse resp) throws Exception;

    public void onHTTPChunk(HTTPChunk chunk) throws Exception;

    public void onConnectionStart(HTTPConnection conn);

    public void onConnectionClose(HTTPConnection conn);

    public void setScope(IScope scope);

    public IScope getScope();

    public void addHttpService(String name, IHTTPService httpService);

    public IHTTPService getHttpService(String name);

    public void removeHttpService(String name);
}
