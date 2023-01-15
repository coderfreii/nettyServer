package org.tl.nettyServer.media.net.http;

import lombok.extern.slf4j.Slf4j;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.messaging.IMessage;
import org.tl.nettyServer.media.messaging.IMessageComponent;
import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.messaging.OOBControlMessage;

import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.stream.consumer.ICustomPushableConsumer;
import org.tl.nettyServer.media.stream.data.IStreamPacket;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.stream.message.StatusMessage;
import org.tl.nettyServer.media.util.IOUtils;

@Slf4j
public class HTTPConnectionConsumer implements ICustomPushableConsumer {
	
	private HTTPConnection conn;
	
	private boolean closed = false;
	
	private boolean inited = false;
	
	private static BufFacade header = BufFacade.buffer(13);
	
	static {
		// write flv header
    	header.writeBytes("FLV".getBytes());
    	header.writeBytes(new byte[] { 0x01, 0x05 });
    	header.writeInt(0x09);
    	header.writeInt(0x00);
	}
    
    public HTTPConnectionConsumer(HTTPConnection conn) {
		this.conn = conn;    		
	}
    
	@Override
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
									OOBControlMessage oobCtrlMsg) {
		if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
			if ("pendingVideoCount".equals(oobCtrlMsg.getServiceName())) {
				long pendings = conn.getPendingMessages();
				if(pendings > 500){
					log.info("http pending packet:{}", pendings);
					oobCtrlMsg.setResult(pendings);
				} else if(pendings > 1000) {
					log.info("http pending packet > 1000, network is bad");
					closed = true;
				}			
			}
		}
	}

	/**
	 * 最终发送消息的方法
	 */
	@Override
	public void pushMessage(IPipe pipe, IMessage message) {
		
		if(!inited) {
			//new byte{}{0x46, 0x4c, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09}
			conn.write(header.asReadOnly());
			inited = true;
		}
		
		if (message instanceof RTMPMessage) {
			if (((RTMPMessage) message).getBody() instanceof IStreamPacket) {
				IStreamPacket packet = (IStreamPacket) (((RTMPMessage) message).getBody());
				if (packet.getData() != null) {
					int bodySize = packet.getData().capacity();
					BufFacade data = BufFacade.buffer(bodySize+16);
					data.writeByte(packet.getDataType());
					IOUtils.writeMediumInt(data, bodySize);
					IOUtils.writeExtendedMediumInt(data,packet.getTimestamp());
					IOUtils.writeMediumInt(data, 0);
					data.writeBytes(packet.getData().duplicate());
					data.writeInt(bodySize + 11);
					conn.write(data);
				}
			}
		} else if(message instanceof StatusMessage) {
			if(((StatusMessage) message).getBody().getCode().equals(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY)) {
				closed = true;
				conn.close();
			}
		}
	}
	
	@Override
	public HTTPConnection getConnection() {
		return conn;
	}

	public boolean isClosed() {
		return closed;
	}
	
	public void setClose(boolean value) {
		this.closed = value;
	}
}
