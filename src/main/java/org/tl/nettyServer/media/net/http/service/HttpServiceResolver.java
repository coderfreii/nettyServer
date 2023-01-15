package org.tl.nettyServer.media.net.http.service;


import org.tl.nettyServer.media.net.hls.service.HTTPM3U8Service;
import org.tl.nettyServer.media.net.hls.service.HTTPTSService;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.service.BaseHTTPService;
import org.tl.nettyServer.media.net.http.service.IHTTPService;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.util.MatcherUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpServiceResolver {
    private Map<String, IHTTPService> serviceMap = new LinkedHashMap<String, IHTTPService>();

    {
        //addHttpService("/*", new HTTPStaticFileService());
//		addHttpService("/flv/*", new HTTPFlvPlayerService());
//		addHttpService("*/aes", new HTTPAESKeyService());
//		addHttpService("/liveflv/*", new HTTPLiveFlvPublisherService());
        addHttpService("*.m3u8", new HTTPM3U8Service());
        addHttpService("*.ts", new HTTPTSService());
//		addHttpService("*/shutdown", new HTTPShutDownService());
    }

    public void addHttpService(String name, IHTTPService httpService) {
        serviceMap.put(name, httpService);
        httpService.start();
    }


    public IHTTPService getHttpService(String name) {
        IHTTPService service = null;
        for (String key : serviceMap.keySet()) {
            if (key.equals(name)) {
                service = serviceMap.get(key);
                break;
            }
        }

        return service;
    }

    public void removeHttpService(String name) {
        IHTTPService service = serviceMap.remove(name);
        service.stop();
    }


    public IHTTPService find(String path) {
        boolean find = false;

        // handle add http service
        String[] keys = serviceMap.keySet().toArray(new String[0]);
        for (int i = keys.length - 1; i >= 0; i--) {
            if (MatcherUtil.match(keys[i], path)) {
                find = true;
                return serviceMap.get(keys[i]);
            }
        }

        return null;
    }


    public static IHTTPService getDefaultHttpService() {
        return new BaseHTTPService() {
            @Override
            public void setHeader(HTTPResponse resp) {

            }

            @Override
            public void handleRequest(HTTPRequest req, HTTPResponse resp, IScope scope) throws Exception {

            }

            @Override
            public void handleRequest(HTTPRequest req, HTTPResponse resp) throws Exception {
                super.handleRequest(req, resp);
                flush(false, resp, true);
            }
        };
    }
}
