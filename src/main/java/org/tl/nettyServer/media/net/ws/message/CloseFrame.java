package org.tl.nettyServer.media.net.ws.message;



/**
 * 关闭帧
 * @author chao
 * @date 2017-10-27
 */
public class CloseFrame extends WebsocketControlFrame {
	short code;
	private String address = "";
	public CloseFrame(){
		this.code = OpcodeEnum.Close_Normal_Closure.getValue();
	}
	public CloseFrame(short code){
		this.code = code;
	}
	public CloseFrame(String address,short code){
		this.address = address;
		this.code = code;
	}
	public short getCode() {
		return code;
	}
	
	@Override
	public String toString() {
		return "CloseFrame [code=" + code + ", address=" + address + "]";
	}
	
}
