package org.tl.nettyServer.media.net.http.service;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.scope.IScope;


public interface IHTTPService {
    void sendError(HTTPRequest req, HTTPResponse resp, HTTPResponseStatus status);

    void setHeader(HTTPResponse resp);

    void handleRequest(HTTPRequest req, HTTPResponse resp) throws Exception;

    void handleRequest(HTTPRequest req, HTTPResponse resp, IScope scope) throws Exception;

    void commitResponse(HTTPRequest req, HTTPResponse resp, BufFacade data, HTTPResponseStatus status);

    void commitResponse(HTTPRequest req, HTTPResponse resp, BufFacade data);

    void start();

    void stop();
}
