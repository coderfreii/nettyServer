package org.tl.nettyServer.media.net.ws.message;
/**
 * 文本帧
 * @author chao
 * @data 2017-10-27
 */
public class TextMessageFrame extends WebsocketDataFrame {
	private String content="";//内容

	public TextMessageFrame(String content){
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "TextMessageFrame [content=" + content + "]";
	}
}
