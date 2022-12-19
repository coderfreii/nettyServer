package org.tl.nettyServer.media;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.service.call.IServiceCall;

import java.util.Map;

/**
 * Represents a "command" sent to or received from an end-point.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ICommand {

    int getTransactionId();

    IServiceCall getCall();

    Map<String, Object> getConnectionParams();

    BufFacade getData();

}
