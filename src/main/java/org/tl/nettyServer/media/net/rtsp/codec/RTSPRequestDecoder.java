package org.tl.nettyServer.media.net.rtsp.codec;

import org.tl.nettyServer.media.net.http.message.HTTPMessage;
import org.tl.nettyServer.media.net.http.request.DefaultHttpRequest;
import org.tl.nettyServer.media.net.rtsp.message.RTSPMethods;
import org.tl.nettyServer.media.net.rtsp.message.RTSPVersions;

/**
 * RTSP Request Decoder
 *
 * @author pengliren
 */
public class RTSPRequestDecoder extends RTSPMessageDecoder {

    @Override
    protected HTTPMessage createMessage(String[] initialLine) throws Exception {

        return new DefaultHttpRequest(RTSPVersions.valueOf(initialLine[2]),
                RTSPMethods.valueOf(initialLine[0]), initialLine[1]);
    }


}
