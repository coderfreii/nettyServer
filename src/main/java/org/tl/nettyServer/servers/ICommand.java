package org.tl.nettyServer.servers;

import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.service.IServiceCall;

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
