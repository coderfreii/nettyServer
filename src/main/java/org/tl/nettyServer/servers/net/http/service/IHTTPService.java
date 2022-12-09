package org.tl.nettyServer.servers.net.http.service;

import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.http.request.HTTPRequest;
import org.tl.nettyServer.servers.net.http.response.HTTPResponse;
import org.tl.nettyServer.servers.net.http.response.HTTPResponseStatus;


public interface IHTTPService {
    void sendError(HTTPRequest req, HTTPResponse resp, HTTPResponseStatus status);

    void setHeader(HTTPResponse resp);

    void handleRequest(HTTPRequest req, HTTPResponse resp) throws Exception;

    void commitResponse(HTTPRequest req, HTTPResponse resp, BufFacade data, HTTPResponseStatus status);

    void commitResponse(HTTPRequest req, HTTPResponse resp, BufFacade data);

    void start();

    void stop();
}
