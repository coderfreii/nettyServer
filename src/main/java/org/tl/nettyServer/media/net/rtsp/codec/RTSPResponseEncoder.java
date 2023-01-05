package org.tl.nettyServer.media.net.rtsp.codec;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.http.message.HTTPMessage;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;

import java.nio.charset.CharacterCodingException;

/**
 * RTSP Response Encoder
 * @author pengliren
 *
 */
public class RTSPResponseEncoder extends RTSPMessageEncoder {

	public RTSPResponseEncoder() throws CharacterCodingException {
		super();
	}

	@Override
	protected void encodeInitialLine(BufFacade buf, HTTPMessage message) throws Exception {
		
		HTTPResponse response = (HTTPResponse) message;
        buf.writeBytes(response.getProtocolVersion().toString().getBytes("ASCII"));
        buf.writeByte((byte) ' ');
        buf.writeBytes(String.valueOf(response.getStatus().getCode()).getBytes("ASCII"));
        buf.writeByte((byte) ' ');
        buf.writeBytes(String.valueOf(response.getStatus().getReasonPhrase()).getBytes("ASCII"));
        buf.writeByte((byte) '\r');
        buf.writeByte((byte) '\n');
	}

}
