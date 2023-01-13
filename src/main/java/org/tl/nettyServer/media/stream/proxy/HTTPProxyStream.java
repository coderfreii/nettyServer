package org.tl.nettyServer.media.stream.proxy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.IoConstants;
import org.tl.nettyServer.media.io.Tag;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.Notify;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.call.IPendingServiceCall;
import org.tl.nettyServer.media.util.IOUtils;
import org.tl.nettyServer.media.util.SystemTimer;

/**
 * HTTP FLV代理流
 * @author pengliren
 *
 */
public abstract class HTTPProxyStream extends BaseRTMPProxyStream implements IoConstants {

	private static Logger log = LoggerFactory.getLogger(HTTPProxyStream.class);

	private BufFacade lastBuffer;
	
	private volatile boolean skipFlvHeader = false;
	
	public HTTPProxyStream(IScope scope, String streamName) {
	
		super();
		setScope(scope);
		setPublishedName(streamName);	
	}
		
	@Override
	public void stop() {
		
		synchronized (lock) {
			super.stop();
			start = false;
			getConnection().close();
			connManager.unregister(publishedName);
		}
	}

	@Override
	public void resultReceived(IPendingServiceCall call) {
		
		log.info("http proxy handle call result:{}",call);
	}
	
	private synchronized void dispatchEvent(ITag tag) {
		
		byte dataType = tag.getDataType();
		if(dataType == TYPE_METADATA) {
			
			Notify notify = new Notify();
			notify.setTimestamp(tag.getTimestamp());
			notify.setData(tag.getBody());	
			notify.setSource(getConnection());
			dispatchEvent(notify);
		} else if(dataType == TYPE_AUDIO) {
			
			AudioData aData = new AudioData();
			aData.setData(tag.getBody());
			aData.setTimestamp(tag.getTimestamp());
			aData.setSource(getConnection());
			dispatchEvent(aData);	
		} else if(dataType == TYPE_VIDEO) {
			
			VideoData vData = new VideoData();
			vData.setData(tag.getBody());
			vData.setTimestamp(tag.getTimestamp());
			vData.setSource(getConnection());
			dispatchEvent(vData);			
		}
	}
	
	public void handleMessage(BufFacade in) throws Exception {
		
		lastReceiveTime = SystemTimer.currentTimeMillis();
		if(in == null || in.readableBytes() == 0) return;
		if(lastBuffer == null) {
			lastBuffer = in;
		} else {			
			lastBuffer.writeBytes(in);
		}
		
		if(!skipFlvHeader) {
			if(lastBuffer.readableBytes() > 9) {
				byte[] skipByte = new byte[9];
				lastBuffer.readBytes(skipByte);
				skipFlvHeader = true;
			} else {
				resetLastBuffer();
				return;
			}
		}
		
		byte[] data = null;
		while(lastBuffer.readableBytes() > 15) {
			lastBuffer.markReaderIndex();
			ITag tag = readTagHeader(lastBuffer);
			if(tag != null && lastBuffer.readableBytes() > tag.getBodySize()) {
				data = new byte[tag.getBodySize()];				
				lastBuffer.readBytes(data);
				tag.setBody(BufFacade.buffer(tag.getBodySize()).writeBytes(data, 0, tag.getBodySize()));
				BufFacade temp = lastBuffer.slice();
				lastBuffer = BufFacade.buffer(4096);
				lastBuffer.writeBytes(temp);
				dispatchEvent(tag);
				temp = null;
			} else {
				lastBuffer.resetReaderIndex();
				break;
			}
		}
		
		resetLastBuffer();
	}
	
	private void resetLastBuffer() {
		if(lastBuffer.readableBytes() > 0) {
			BufFacade temp = BufFacade.buffer(lastBuffer.readableBytes());
			temp.writeBytes(lastBuffer);
			lastBuffer = temp;
		} else {
			lastBuffer = null;
		}
	}
	
	private ITag readTagHeader(BufFacade in) {

		// previous tag size (4 bytes) + flv tag header size (11 bytes)
		//		if (log.isDebugEnabled()) {
		//			in.mark();
		//			StringBuilder sb = new StringBuilder();
		//			HexDump.dumpHex(sb, in.array());
		//			log.debug("\n{}", sb);
		//			in.reset();
		//		}		
		// previous tag's size
		int previousTagSize = in.readInt();
		// start of the flv tag
		byte dataType = in.readByte();
		// loop counter
		int i = 0;
		while (dataType != 8 && dataType != 9 && dataType != 18) {
			log.info("Invalid data type detected, reading ahead");
			// only allow 10 loops
			if (i++ > 10) {
				return null;
			}
			// move ahead and see if we get a valid datatype		
			dataType = in.readByte();
		}
		int bodySize = IOUtils.readUnsignedMediumInt(in);
		int timestamp = IOUtils.readExtendedMediumInt(in);
		in.skipBytes(3);
		return new Tag(dataType, timestamp, bodySize, null, previousTagSize);
	}
	
}
