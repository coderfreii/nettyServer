/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 *
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tl.nettyServer.media.net.rtmp.handler.packet;

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.ICommand;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.client.IContext;
import org.tl.nettyServer.media.client.IServer;
import org.tl.nettyServer.media.client.ScopeContextBean;
import org.tl.nettyServer.media.conf.ConfigServer;
import org.tl.nettyServer.media.exception.ClientRejectedException;
import org.tl.nettyServer.media.exception.ScopeNotFoundException;
import org.tl.nettyServer.media.exception.ScopeShuttingDownException;
import org.tl.nettyServer.media.messaging.IConsumer;
import org.tl.nettyServer.media.messaging.OOBControlMessage;
import org.tl.nettyServer.media.net.rtmp.Channel;
import org.tl.nettyServer.media.net.rtmp.DeferredResult;
import org.tl.nettyServer.media.net.rtmp.codec.RTMP;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.consts.Action;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.net.rtmp.status.Status;
import org.tl.nettyServer.media.net.rtmp.status.StatusObject;
import org.tl.nettyServer.media.net.rtmp.status.StatusObjectService;
import org.tl.nettyServer.media.scope.IBroadcastScope;
import org.tl.nettyServer.media.scope.IGlobalScope;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.scope.IScopeHandler;
import org.tl.nettyServer.media.service.call.ServiceCall;
import org.tl.nettyServer.media.service.call.IPendingServiceCall;
import org.tl.nettyServer.media.service.call.IServiceCall;
import org.tl.nettyServer.media.service.stream.IStreamService;
import org.tl.nettyServer.media.service.stream.StreamService;
import org.tl.nettyServer.media.so.*;
import org.tl.nettyServer.media.stream.*;
import org.tl.nettyServer.media.stream.client.IClientBroadcastStream;
import org.tl.nettyServer.media.stream.base.IClientStream;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * rtmp事件处理程序。
 */
@Slf4j
public class RtmpPacketHandler extends BaseRtmpPacketHandler {
    protected StatusObjectService statusObjectService = ConfigServer.configServer.statusObjectService();

    protected IServer server = ConfigServer.configServer.config();

    private boolean unvalidatedConnectionAllowed;

    private boolean dispatchStreamActions;

    private boolean globalScopeConnectionAllowed = false;

    @Override
    protected void onChunkSize(RTMPConnection conn, Channel channel, Header source, ChunkSize chunkSize) {
        int requestedChunkSize = chunkSize.getSize();
        log.debug("Chunk size: {}", requestedChunkSize);
        // set chunk size on the connection
        RTMP state = conn.getState();
        // set only the read chunk size since it came from the client
        state.setReadChunkSize(requestedChunkSize);
        //state.setWriteChunkSize(requestedChunkSize);
        // set on each of the streams
        for (IClientStream stream : conn.getStreams()) {
            if (stream instanceof IClientBroadcastStream) {
                IClientBroadcastStream bs = (IClientBroadcastStream) stream;
                IBroadcastScope scope = bs.getScope().getBroadcastScope(bs.getPublishedName());
                if (scope == null) {
                    continue;
                }
                OOBControlMessage setChunkSize = new OOBControlMessage();
                setChunkSize.setTarget("ClientBroadcastStream");
                setChunkSize.setServiceName("chunkSize");
                if (setChunkSize.getServiceParamMap() == null) {
                    setChunkSize.setServiceParamMap(new HashMap<String, Object>());
                }
                setChunkSize.getServiceParamMap().put("chunkSize", requestedChunkSize);
                scope.sendOOBControlMessage((IConsumer) null, setChunkSize);
                log.debug("Sending chunksize {} to {}", chunkSize, bs.getProvider());
            }
        }
    }

    /**
     * Remoting call invocation handler.
     */
    protected void invokeCall(RTMPConnection conn, IServiceCall call) {
        final IScope scope = conn.getScope();
        if (scope != null) {
            if (scope.hasHandler()) {
                final IScopeHandler handler = scope.getHandler();
                log.debug("Scope: {} handler: {}", scope, handler);
                if (!handler.serviceCall(conn, call)) {
                    // XXX: What to do here? Return an error?
                    log.warn("Scope: {} handler failed on service call", scope.getName(), new Exception("Service call failed"));
                    return;
                }
            }
            final IContext context = scope.getContext();
            log.debug("Context: {}", context);
            context.getServiceInvoker().invoke(call, scope);
        } else {
            log.warn("Scope was null for invoke: {} connection state: {}", call.getServiceMethodName(), conn.getStateCode());
        }
    }

    /**
     * Remoting call invocation handler.指定 service
     */
    private boolean invokeCall(RTMPConnection conn, IServiceCall call, Object service) {
        final IScope scope = conn.getScope();
        final IContext context = scope.getContext();
        if (log.isTraceEnabled()) {
            log.trace("Scope: {} context: {} service: {}", scope, context, service);
        }
        return context.getServiceInvoker().invoke(call, service);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected void onCommand(RTMPConnection conn, Channel channel, Header source, ICommand command) {
        log.debug("onCommand {}", command);
        // incoming transaction id (response to 'connect' must be == 1)
        final int transId = command.getTransactionId();
        if (log.isTraceEnabled()) {
            log.trace("transId: {}", transId);
        }
        // get the call
        final IServiceCall call = command.getCall();
        if (log.isTraceEnabled()) {
            log.trace("call: {}", call);
        }

        // get the method name
        final String action = call.getServiceMethodName();
        //TODO
        // If it's a callback for server remote call then pass it over to callbacks handler and return
        if (Action.RESULT.equals(action) || Action.ERROR.equals(action)) {
            handlePendingCallResult(conn, (Invoke) command);
            return;
        }

        // 此处的“connected”表示存在与连接相关联的作用域（post-“connect”）
        boolean connected = conn.isConnected();
        boolean disconnectOnReturn = false;
        if (connected) {
            invokeCall(command, conn, channel, source);
        } else if (StreamAction.CONNECT.equals(StreamAction.getEnum(action))) {
            doConnect(command, conn);
        } else {
            // not connected and attempting to send an invoked
            log.warn("Not connected, closing connection");
            conn.close();
        }
        postProcessCommand(command, source, conn, channel, disconnectOnReturn);
    }

    void postProcessCommand(ICommand command, Header source, RTMPConnection conn, Channel channel, boolean disconnectOnReturn) {
        IServiceCall call = command.getCall();
        final int transId = command.getTransactionId();
        if (!(command instanceof Invoke)) {
            log.debug("Command type: {}", command.getClass().getName());
            return;
        }

        log.debug("Command type Invoke");
        if ((source.getStreamId().intValue() != 0) && (call.getStatus() == ServiceCall.STATUS_SUCCESS_VOID || call.getStatus() == ServiceCall.STATUS_SUCCESS_NULL)) {
            // This fixes a bug in the FP on Intel Macs.
            log.debug("Method does not have return value, do not reply");
            return;
        }
        boolean sendResult = true;
        if (call instanceof IPendingServiceCall) {
            IPendingServiceCall psc = (IPendingServiceCall) call;
            Object result = psc.getResult();
            if (result instanceof DeferredResult) {
                //Remember the deferred result to be sent later
                DeferredResult dr = (DeferredResult) result;
                dr.setServiceCall(psc);
                dr.setChannel(channel);
                dr.setTransactionId(transId);
                conn.registerDeferredResult(dr);
                sendResult = false;
            }
        }
        if (sendResult) {
            // The client expects a result for the method call
            Invoke reply = new Invoke();
            reply.setCall(call);
            reply.setTransactionId(transId);
            channel.write(reply);
            if (disconnectOnReturn) {
                log.debug("Close connection due to connect handling exception: {}", conn.getSessionId());
                conn.getSession().closeOnFlush(); //must wait until flush to close as we just wrote asynchronously to the stream
            }
        }

    }


    boolean doConnect(ICommand command, RTMPConnection conn) {
        boolean disconnectOnReturn = false;
        final IServiceCall call = command.getCall();
        log.debug("connect - transaction id: {}", command.getTransactionId());
        // Get parameters passed from client to NetConnection#connection
        final Map<String, Object> params = normalizeParams(command);
        String host = getHostname((String) params.get("tcUrl"));
        String path = (String) params.get("app");
        // connection setup
        conn.setup(host, path, params);
        try {
            disconnectOnReturn = doConnect0(command, conn, host, path, disconnectOnReturn);
        } catch (RuntimeException e) {
            call.setStatus(ServiceCall.STATUS_GENERAL_EXCEPTION);
            if (call instanceof IPendingServiceCall) {
                IPendingServiceCall pc = (IPendingServiceCall) call;
                pc.setResult(getStatus(NC_CONNECT_FAILED));
            }
            log.error("Error connecting {}", e);
            disconnectOnReturn = true;
        }
        // Evaluate request for AMF3 encoding
        if (new Double(3d).equals(params.get("objectEncoding"))) {
            conn.getState().setEncoding(IConnection.Encoding.AMF3);
            if (call instanceof IPendingServiceCall) {
                Object pcResult = ((IPendingServiceCall) call).getResult();
                Map<String, Object> result;
                if (pcResult instanceof Map) {
                    result = (Map<String, Object>) pcResult;
                    result.put("objectEncoding", 3);
                } else if (pcResult instanceof StatusObject) {
                    result = new HashMap<>();
                    StatusObject status = (StatusObject) pcResult;
                    result.put("code", status.getCode());
                    result.put("description", status.getDescription());
                    result.put("application", status.getApplication());
                    result.put("level", status.getLevel());
                    result.put("objectEncoding", 3);
                    ((IPendingServiceCall) call).setResult(result);
                }
            }
        }
        return disconnectOnReturn;
    }


    void invokeCall(ICommand command, RTMPConnection conn, Channel channel, Header source) {
        IServiceCall call = command.getCall();
        final String action = call.getServiceMethodName();

        // If this is not a service call then handle connection...
        if (call.getServiceName() != null) {
            // handle service calls
            invokeCall(conn, call);
            return;
        }


        StreamAction streamAction = StreamAction.getEnum(action);
        if (log.isDebugEnabled()) {
            log.debug("Stream action: {}", streamAction.toString());
        }
        // TODO change this to an application scope parameter and / or change to the listener pattern
        if (dispatchStreamActions) {
            // pass the stream action event to the handler
            try {
                conn.getScope().getHandler().handleEvent(new StreamActionEvent(streamAction));
            } catch (Exception ex) {
                log.warn("Exception passing stream action: {} to the scope handler", streamAction, ex);
            }
        }
        //if the "stream" action is not predefined a custom type will be returned
        switch (streamAction) {
            case DISCONNECT:
                conn.close();
                break;
            case CREATE_STREAM:
            case INIT_STREAM:
            case CLOSE_STREAM:
            case RELEASE_STREAM:
            case DELETE_STREAM:
            case PUBLISH:
            case PLAY:
            case PLAY2:
            case SEEK:
            case PAUSE:
            case PAUSE_RAW:
            case RECEIVE_VIDEO:
            case RECEIVE_AUDIO:
                IStreamService streamService = (IStreamService) ScopeUtils.getScopeService(conn.getScope(), IStreamService.class, StreamService.class);
                try {
                    log.debug("Invoking {} from {} with service: {}", new Object[]{call, conn.getSessionId(), streamService});
                    if (invokeCall(conn, call, streamService)) {
                        log.debug("Stream service invoke {} success", action);
                    } else {
                        Status status = getStatus(NS_INVALID_ARGUMENT).asStatus();
                        status.setDescription(String.format("Failed to %s (stream id: %d)", action, source.getStreamId()));
                        channel.sendStatus(status);
                    }
                } catch (Throwable err) {
                    log.error("Error while invoking {} on stream service. {}", action, err);
                    Status status = getStatus(NS_FAILED).asStatus();
                    status.setDescription(String.format("Error while invoking %s (stream id: %d)", action, source.getStreamId()));
                    status.setDetails(err.getMessage());
                    channel.sendStatus(status);
                }
                break;
            default:
                log.debug("Defaulting to invoke for: {}", action);
                invokeCall(conn, call);
        }
    }


    Map<String, Object> normalizeParams(ICommand command) {
        final Map<String, Object> params = command.getConnectionParams();
        // app name as path, but without query string if there is one
        String path = (String) params.get("app");
        if (path.indexOf("?") != -1) {
            int idx = path.indexOf("?");
            params.put("queryString", path.substring(idx));
            path = path.substring(0, idx);
        }
        params.put("path", path);

        return params;
    }

    boolean doConnect0(ICommand command, RTMPConnection conn, String host, String path, boolean disconnectOnReturn) {
        IServiceCall call = command.getCall();

        // Lookup server scope when connected using host and application name
        IGlobalScope global = server.lookupGlobal(host, path);
        log.trace("Global lookup result: {}", global);
        if (global != null) {
            final IContext context = global.getContext();
            IScope scope = null;
            try {
                // TODO optimize this to use Scope instead of Context
                scope = context.resolveScope(global, path);
                if (scope != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Connecting to: {}", scope.getName());
                        log.debug("Conn {}, scope {}, call {} args {}", new Object[]{conn, scope, call, call.getArguments()});
                    }
                    // if scope connection is allowed
                    if (scope.isConnectionAllowed(conn)) {
                        // connections connect result
                        boolean connectSuccess;
                        try {
                            if (call.getArguments() != null) {
                                connectSuccess = conn.connect(scope, call.getArguments());
                            } else {
                                connectSuccess = conn.connect(scope);
                            }
                            if (connectSuccess) {
                                log.debug("Connected - {}", conn.getClient());
                                call.setStatus(ServiceCall.STATUS_SUCCESS_RESULT);
                                if (call instanceof IPendingServiceCall) {
                                    IPendingServiceCall pc = (IPendingServiceCall) call;
                                    //send fmsver and capabilities
                                    StatusObject result = getStatus(NC_CONNECT_SUCCESS);
                                    result.setAdditional("fmsVer", Red5.getFMSVersion());
                                    result.setAdditional("capabilities", Red5.getCapabilities());
                                    result.setAdditional("mode", Integer.valueOf(1));
                                    result.setAdditional("data", Red5.getDataVersion());
                                    pc.setResult(result);
                                }
                                // Measure initial round-trip time after connecting
                                conn.ping(new Ping(Ping.STREAM_BEGIN, 0, -1));
                            } else {
                                log.debug("Connect failed");
                                call.setStatus(ServiceCall.STATUS_ACCESS_DENIED);
                                if (call instanceof IPendingServiceCall) {
                                    IPendingServiceCall pc = (IPendingServiceCall) call;
                                    pc.setResult(getStatus(NC_CONNECT_REJECTED));
                                }
                                disconnectOnReturn = true;
                            }
                        } catch (ClientRejectedException rejected) {
                            log.debug("Connect rejected");
                            call.setStatus(ServiceCall.STATUS_ACCESS_DENIED);
                            if (call instanceof IPendingServiceCall) {
                                IPendingServiceCall pc = (IPendingServiceCall) call;
                                StatusObject status = getStatus(NC_CONNECT_REJECTED);
                                Object reason = rejected.getReason();
                                if (reason != null) {
                                    status.setApplication(reason);
                                    //should we set description?
                                    status.setDescription(reason.toString());
                                }
                                pc.setResult(status);
                            }
                            disconnectOnReturn = true;
                        }
                    } else {
                        // connection to specified scope is not allowed
                        log.debug("Connect to specified scope is not allowed");
                        call.setStatus(ServiceCall.STATUS_ACCESS_DENIED);
                        if (call instanceof IPendingServiceCall) {
                            IPendingServiceCall pc = (IPendingServiceCall) call;
                            StatusObject status = getStatus(NC_CONNECT_REJECTED);
                            status.setDescription(String.format("Connection to '%s' denied.", path));
                            pc.setResult(status);
                        }
                        disconnectOnReturn = true;
                    }
                }
            } catch (ScopeNotFoundException err) {
                log.warn("Scope not found", err);
                call.setStatus(ServiceCall.STATUS_SERVICE_NOT_FOUND);
                if (call instanceof IPendingServiceCall) {
                    StatusObject status = getStatus(NC_CONNECT_REJECTED);
                    status.setDescription(String.format("No scope '%s' on this server.", path));
                    ((IPendingServiceCall) call).setResult(status);
                }
                log.info("Scope {} not found on {}", path, host);
                disconnectOnReturn = true;
            } catch (ScopeShuttingDownException err) {
                log.warn("Scope shutting down", err);
                call.setStatus(ServiceCall.STATUS_APP_SHUTTING_DOWN);
                if (call instanceof IPendingServiceCall) {
                    StatusObject status = getStatus(NC_CONNECT_APPSHUTDOWN);
                    status.setDescription(String.format("Application at '%s' is currently shutting down.", path));
                    ((IPendingServiceCall) call).setResult(status);
                }
                log.info("Application at {} currently shutting down on {}", path, host);
                disconnectOnReturn = true;
            }
        } else {
            log.warn("Scope {} not found", path);
            call.setStatus(ServiceCall.STATUS_SERVICE_NOT_FOUND);
            if (call instanceof IPendingServiceCall) {
                StatusObject status = getStatus(NC_CONNECT_INVALID_APPLICATION);
                status.setDescription(String.format("No scope '%s' on this server.", path));
                ((IPendingServiceCall) call).setResult(status);
            }
            log.info("No application scope found for {} on host {}", path, host);
            disconnectOnReturn = true;
        }

        return disconnectOnReturn;
    }

    public StatusObject getStatus(String code) {
        return statusObjectService.getStatusObject(code);
    }

    @Override
    protected void onPing(RTMPConnection conn, Channel channel, Header source, Ping ping) {
        switch (ping.getEventType()) {
            case Ping.CLIENT_BUFFER:
                SetBuffer setBuffer = (SetBuffer) ping;
                // get the stream id
                int streamId = setBuffer.getStreamId();
                // get requested buffer size in milliseconds
                int buffer = setBuffer.getBufferLength();
                log.debug("Client sent a buffer size: {} ms for stream id: {}", buffer, streamId);
                IClientStream stream = null;
                if (streamId != 0) {
                    // The client wants to set the buffer time
                    stream = conn.getStreamById(streamId);
                    if (stream != null) {
                        stream.setClientBufferDuration(buffer);
                        log.trace("Stream type: {}", stream.getClass().getName());
                    }
                }
                //catch-all to make sure buffer size is set
                if (stream == null) {
                    // Remember buffer time until stream is created
                    conn.rememberStreamBufferDuration(streamId, buffer);
                    log.debug("Remembering client buffer on stream: {}", buffer);
                }
                break;
            case Ping.PONG_SERVER:
                // This is the response to an IConnection.ping request
                conn.pingReceived(ping);
                break;
            default:
                log.warn("Unhandled ping: {}", ping);
        }
    }

    /**
     * Create and send SO message stating that a SO could not be created.
     */
    private void sendSOCreationFailed(RTMPConnection conn, SharedObjectMessage message) {
        log.debug("sendSOCreationFailed - message: {} conn: {}", message, conn);
        // reset the object so we can re-use it
        message.reset();
        // add the error event
        message.addEvent(new SharedObjectEvent(ISharedObjectEvent.Type.CLIENT_STATUS, "error", SO_CREATION_FAILED));
        if (conn.isChannelUsed(3)) {
            // XXX Paul: I dont like this direct write stuff, need to move to event-based
            conn.createChannelIfAbsent(3).write(message);
        } else {
            log.warn("Channel is not in-use and cannot handle SO event: {}", message, new Exception("SO event handling failure"));
            // XXX Paul: I dont like this direct write stuff, need to move to event-based
            conn.createChannelIfAbsent(3).write(message);
        }
    }

    @Override
    protected void onSharedObject(RTMPConnection conn, Channel channel, Header source, SharedObjectMessage message) {
        if (log.isDebugEnabled()) {
            log.debug("onSharedObject - conn: {} channel: {} so message: {}", new Object[]{conn.getSessionId(), channel.getId(), message});
        }
        final IScope scope = conn.getScope();
        if (scope != null) {
            // so name
            String name = message.getName();
            // whether or not the incoming so is persistent
            boolean persistent = message.isPersistent();
            // shared object service
            ISharedObjectService sharedObjectService = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, SharedObjectService.class, false);
            if (!sharedObjectService.hasSharedObject(scope, name)) {
                log.debug("Shared object service doesnt have requested object, creation will be attempted");
                ISharedObjectSecurityService security = (ISharedObjectSecurityService) ScopeUtils.getScopeService(scope, ISharedObjectSecurityService.class);
                if (security != null) {
                    // Check handlers to see if creation is allowed
                    for (ISharedObjectSecurity handler : security.getSharedObjectSecurity()) {
                        if (!handler.isCreationAllowed(scope, name, persistent)) {
                            log.debug("Shared object create failed, creation is not allowed");
                            sendSOCreationFailed(conn, message);
                            return;
                        }
                    }
                }
                if (!sharedObjectService.createSharedObject(scope, name, persistent)) {
                    log.debug("Shared object create failed");
                    sendSOCreationFailed(conn, message);
                    return;
                }
            }
            ISharedObject so = sharedObjectService.getSharedObject(scope, name);
            if (so != null) {
                if (so.isPersistent() == persistent) {
                    log.debug("Dispatch persistent shared object");
                    so.dispatchEvent(message);
                } else {
                    log.warn("Shared object persistence mismatch - current: {} incoming: {}", so.isPersistent(), persistent);
                    // reset the object so we can re-use it
                    message.reset();
                    // add the error event
                    message.addEvent(new SharedObjectEvent(ISharedObjectEvent.Type.CLIENT_STATUS, "error", SO_PERSISTENCE_MISMATCH));
                    conn.createChannelIfAbsent(3).write(message);
                }
            } else {
                log.warn("Shared object lookup returned null for {} in {}", name, scope.getName());
                // reset the object so we can re-use it
                message.reset();
                // add the error event
                message.addEvent(new SharedObjectEvent(ISharedObjectEvent.Type.CLIENT_STATUS, "error", NC_CALL_FAILED));
                conn.createChannelIfAbsent(3).write(message);
            }
        } else {
            // The scope already has been deleted
            log.debug("Shared object scope was not found");
            sendSOCreationFailed(conn, message);
        }
    }

    protected void onBWDone() {
        log.debug("onBWDone");
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected void onInvoke(RTMPConnection conn, Channel channel, Header source, Notify invoke, RTMP rtmp) {
        log.debug("Invoke: {}", invoke);
        // Get call
        final IServiceCall call = invoke.getCall();

        if (call == null) return;
        // method name
        final String action = call.getServiceMethodName();

        // If it's a callback for server remote call then pass it over to
        // callbacks handler and return
        if ("_result".equals(action) || "_error".equals(action)) {
            handlePendingCallResult(conn, invoke);
            return;
        }

        boolean disconnectOnReturn = false;

        // If this is not a service call then handle connection...
        if (call.getServiceName() == null) {
            log.debug("call: {}", call);
            if (!conn.isConnected() && StreamAction.CONNECT.equals(action)) {
                // Handle connection
                log.debug("connect");
                // Get parameters passed from client to
                // NetConnection#connection
                final Map<String, Object> params = invoke.getConnectionParams();
                // Get hostname
                String host = getHostname((String) params.get("tcUrl"));
                // Check up port
                if (host.endsWith(":1935")) {
                    // Remove default port from connection string
                    host = host.substring(0, host.length() - 5);
                }
                // App name as path, but without query string if there is
                // one
                String path = (String) params.get("app");
                if (path.indexOf("?") != -1) {
                    int idx = path.indexOf("?");
                    params.put("queryString", path.substring(idx));
                    path = path.substring(0, idx);
                }
                params.put("path", path);

                final String sessionId = null;
                conn.setup(host, path, sessionId, params);
                try {
                    // Lookup server scope when connected
                    // Use host and application name
                    IGlobalScope global = server.lookupGlobal(host, path);
                    log.trace("Global lookup result: {}", global);
                    if (global != null) {
                        final IContext context = global.getContext();
                        IScope scope = null;
                        try {
                            scope = context.resolveScope(global, path);
                            //if global scope connection is not allowed, reject
                            if (scope.getDepth() < 1 && !globalScopeConnectionAllowed) {
                                call.setStatus(ServiceCall.STATUS_ACCESS_DENIED);
                                if (call instanceof IPendingServiceCall) {
                                    IPendingServiceCall pc = (IPendingServiceCall) call;
                                    StatusObject status = getStatus(NC_CONNECT_REJECTED);
                                    status.setDescription("Global scope connection disallowed on this server.");
                                    pc.setResult(status);
                                }
                                disconnectOnReturn = true;
                            }
                        } catch (ScopeNotFoundException err) {
                            call.setStatus(ServiceCall.STATUS_SERVICE_NOT_FOUND);
                            if (call instanceof IPendingServiceCall) {
                                StatusObject status = getStatus(NC_CONNECT_REJECTED);
                                status.setDescription(String.format("No scope '%s' on this server.", path));
                                ((IPendingServiceCall) call).setResult(status);
                            }
                            log.info("Scope {} not found on {}", path, host);
                            disconnectOnReturn = true;
                        } catch (ScopeShuttingDownException err) {
                            call.setStatus(ServiceCall.STATUS_APP_SHUTTING_DOWN);
                            if (call instanceof IPendingServiceCall) {
                                StatusObject status = getStatus(NC_CONNECT_APPSHUTDOWN);
                                status.setDescription(String.format("Application at '%s' is currently shutting down.", path));
                                ((IPendingServiceCall) call).setResult(status);
                            }
                            log.info("Application at {} currently shutting down on {}", path, host);
                            disconnectOnReturn = true;
                        }
                        if (scope != null) {
                            log.info("Connecting to: {}", scope);
                            boolean okayToConnect;
                            try {
                                log.debug("Conn {}, scope {}, call {}", new Object[]{conn, scope, call});
                                log.debug("ServiceCall args {}", call.getArguments());
                                if (call.getArguments() != null) {
                                    okayToConnect = conn.connect(scope, call.getArguments());
                                } else {
                                    okayToConnect = conn.connect(scope);
                                }
                                if (okayToConnect) {
                                    log.debug("Connected - Client: {}", conn.getClient());
                                    call.setStatus(ServiceCall.STATUS_SUCCESS_RESULT);
                                    if (call instanceof IPendingServiceCall) {
                                        IPendingServiceCall pc = (IPendingServiceCall) call;
                                        //send fmsver and capabilities
                                        StatusObject result = getStatus(NC_CONNECT_SUCCESS);
                                        result.setAdditional("fmsVer", Red5.getFMSVersion());
                                        result.setAdditional("capabilities", Integer.valueOf(31));
                                        result.setAdditional("mode", Integer.valueOf(1));
                                        result.setAdditional("data", Red5.getDataVersion());
                                        pc.setResult(result);
                                    }
                                    // Measure initial roundtrip time after connecting
                                    conn.ping(new Ping(Ping.STREAM_BEGIN, 0, -1));
                                    conn.startRoundTripMeasurement();
                                } else {
                                    log.debug("Connect failed");
                                    call.setStatus(ServiceCall.STATUS_ACCESS_DENIED);
                                    if (call instanceof IPendingServiceCall) {
                                        IPendingServiceCall pc = (IPendingServiceCall) call;
                                        pc.setResult(getStatus(NC_CONNECT_REJECTED));
                                    }
                                    disconnectOnReturn = true;
                                }
                            } catch (ClientRejectedException rejected) {
                                log.debug("Connect rejected");
                                call.setStatus(ServiceCall.STATUS_ACCESS_DENIED);
                                if (call instanceof IPendingServiceCall) {
                                    IPendingServiceCall pc = (IPendingServiceCall) call;
                                    StatusObject status = getStatus(NC_CONNECT_REJECTED);
                                    Object reason = rejected.getReason();
                                    if (reason != null) {
                                        status.setApplication(reason);
                                        //should we set description?
                                        status.setDescription(reason.toString());
                                    }
                                    pc.setResult(status);
                                }
                                disconnectOnReturn = true;
                            }
                        }
                    } else {
                        call.setStatus(ServiceCall.STATUS_SERVICE_NOT_FOUND);
                        if (call instanceof IPendingServiceCall) {
                            StatusObject status = getStatus(NC_CONNECT_INVALID_APPLICATION);
                            status.setDescription(String.format("No scope '%s' on this server.", path));
                            ((IPendingServiceCall) call).setResult(status);
                        }
                        log.info("No application scope found for {} on host {}", path, host);
                        disconnectOnReturn = true;
                    }
                } catch (RuntimeException e) {
                    call.setStatus(ServiceCall.STATUS_GENERAL_EXCEPTION);
                    if (call instanceof IPendingServiceCall) {
                        IPendingServiceCall pc = (IPendingServiceCall) call;
                        pc.setResult(getStatus(NC_CONNECT_FAILED));
                    }
                    log.error("Error connecting {}", e);
                    disconnectOnReturn = true;
                }

                // Evaluate request for AMF3 encoding
                if (Integer.valueOf(3).equals(params.get("objectEncoding")) && call instanceof IPendingServiceCall) {
                    Object pcResult = ((IPendingServiceCall) call).getResult();
                    Map<String, Object> result;
                    if (pcResult instanceof Map) {
                        result = (Map<String, Object>) pcResult;
                        result.put("objectEncoding", 3);
                    } else if (pcResult instanceof StatusObject) {
                        result = new HashMap<String, Object>();
                        StatusObject status = (StatusObject) pcResult;
                        result.put("code", status.getCode());
                        result.put("description", status.getDescription());
                        result.put("application", status.getApplication());
                        result.put("level", status.getLevel());
                        result.put("objectEncoding", 3);
                        ((IPendingServiceCall) call).setResult(result);
                    }

                    rtmp.setEncoding(IConnection.Encoding.AMF3);
                }
            } else {
                //log.debug("Enum value of: {}", StreamAction.getEnum(action));
                StreamAction streamAction = StreamAction.getEnum(action);
                //if the "stream" action is not predefined a custom type will be returned
                switch (streamAction) {
                    case DISCONNECT:
                        conn.close();
                        break;
                    case CREATE_STREAM:
                    case INIT_STREAM:
                    case CLOSE_STREAM:
                    case RELEASE_STREAM:
                    case DELETE_STREAM:
                    case PUBLISH:
                    case PLAY:
                    case PLAY2:
                    case SEEK:
                    case PAUSE:
                    case PAUSE_RAW:
                    case RECEIVE_VIDEO:
                    case RECEIVE_AUDIO:
                        IStreamService streamService = (IStreamService) conn.getScope().getContext().getBean(ScopeContextBean.STREAMSERVICE_BEAN);
                        //ScopeUtils.getScopeService(conn.getScope(), IStreamService.class, StreamService.class);
                        Status status = null;
                        try {
                            log.debug("Invoking {} from {} with service: {}", new Object[]{call, conn, streamService});
                            if (!invokeCall(conn, call, streamService)) {
                                status = getStatus(NS_INVALID_ARGUMENT).asStatus();
                                status.setDescription(String.format("Failed to %s (stream id: %d)", action, source.getStreamId()));
                            }
                        } catch (Throwable err) {
                            log.error("Error while invoking {} on stream service. {}", action, err);
                            status = getStatus(NS_FAILED).asStatus();
                            status.setDescription(String.format("Error while invoking %s (stream id: %d)", action, source.getStreamId()));
                            status.setDetails(err.getMessage());
                        }
                        if (status != null) {
                            channel.sendStatus(status);
                        }
                        break;
                    default:
                        invokeCall(conn, call);
                }
            }
        } else if (conn.isConnected()) {
            // Service calls, must be connected.
            invokeCall(conn, call);
        } else {
            // Warn user attempts to call service without being connected
            log.warn("Not connected, closing connection");
            conn.close();
        }

        if (invoke instanceof Invoke) {
            if ((source.getStreamId() != null) && (call.getStatus() == ServiceCall.STATUS_SUCCESS_VOID || call.getStatus() == ServiceCall.STATUS_SUCCESS_NULL)) {
                // This fixes a bug in the FP on Intel Macs.
                log.debug("Method does not have return value, do not reply");
                return;
            }

            boolean sendResult = true;
            if (call instanceof IPendingServiceCall) {
                IPendingServiceCall psc = (IPendingServiceCall) call;
                Object result = psc.getResult();
                if (result instanceof DeferredResult) {
                    // Remember the deferred result to be sent later
                    DeferredResult dr = (DeferredResult) result;
                    dr.setServiceCall(psc);
                    dr.setChannel(channel);
                    dr.setInvokeId(invoke.getInvokeId());
                    conn.registerDeferredResult(dr);
                    sendResult = false;
                }
            }

            if (sendResult) {
                // The client expects a result for the method call.
                Invoke reply = new Invoke();
                reply.setCall(call);
                reply.setInvokeId(invoke.getInvokeId());
                channel.write(reply);
                if (disconnectOnReturn) {
                    conn.close();
                }
            }
        }
    }

    public boolean isGlobalScopeConnectionAllowed() {
        return globalScopeConnectionAllowed;
    }

    public void setGlobalScopeConnectionAllowed(boolean globalScopeConnectionAllowed) {
        this.globalScopeConnectionAllowed = globalScopeConnectionAllowed;
    }

    public void setServer(IServer server) {
        this.server = server;
    }

    public void setStatusObjectService(StatusObjectService statusObjectService) {
        this.statusObjectService = statusObjectService;
    }

    public boolean isUnvalidatedConnectionAllowed() {
        return unvalidatedConnectionAllowed;
    }

    public void setUnvalidatedConnectionAllowed(boolean unvalidatedConnectionAllowed) {
        this.unvalidatedConnectionAllowed = unvalidatedConnectionAllowed;
    }

    public boolean isDispatchStreamActions() {
        return dispatchStreamActions;
    }

    public void setDispatchStreamActions(boolean dispatchStreamActions) {
        this.dispatchStreamActions = dispatchStreamActions;
    }
}
