package org.tl.nettyServer.media.net.rtsp.codec;


import org.tl.nettyServer.media.net.http.message.HTTPMessage;
import org.tl.nettyServer.media.net.http.response.DefaultHttpResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.net.rtsp.message.RTSPVersions;

/**
 * RTSP Response Decoder
 *
 * @author pengliren
 */
public class RTSPResponseDecoder extends RTSPMessageDecoder {
    @Override
    protected HTTPMessage createMessage(String[] initialLine) throws Exception {

        return new DefaultHttpResponse(RTSPVersions.valueOf(initialLine[0]),
                new HTTPResponseStatus(Integer.valueOf(initialLine[1]), initialLine[2]));
    }
}
