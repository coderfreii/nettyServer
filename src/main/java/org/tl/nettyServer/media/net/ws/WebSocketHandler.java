
package org.tl.nettyServer.media.net.ws;


import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.ws.message.BinMessageFrame;
import org.tl.nettyServer.media.net.ws.message.TextMessageFrame;

/**
 * websocketHandler interface
 *
 * @author chao
 * @date 2017-10-27
 */
public interface WebSocketHandler {
    public void onOpen(HTTPConnection session);

    public void onClose(HTTPConnection session);

    public void onMessage(HTTPConnection session, TextMessageFrame textMessageFrame);

    public void onMessage(HTTPConnection session, BinMessageFrame binMessageFrame);

    public void onError(HTTPConnection session);

    public void onIdle(HTTPConnection session);

    public void onPing(HTTPConnection session);

    public void onPong(HTTPConnection session);
}
