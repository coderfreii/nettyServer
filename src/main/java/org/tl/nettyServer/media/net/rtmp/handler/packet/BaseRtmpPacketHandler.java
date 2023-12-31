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
import org.tl.nettyServer.media.event.IEventDispatcher;
import org.tl.nettyServer.media.net.rtmp.Channel;
import org.tl.nettyServer.media.net.rtmp.codec.RtmpProtocolState;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.scheduling.ISchedulingService;
import org.tl.nettyServer.media.scheduling.QuartzSchedulingService;
import org.tl.nettyServer.media.service.IPendingServiceCallback;
import org.tl.nettyServer.media.service.call.IPendingServiceCall;
import org.tl.nettyServer.media.service.call.IServiceCall;
import org.tl.nettyServer.media.so.SharedObjectMessage;
import org.tl.nettyServer.media.stream.StreamAction;
import org.tl.nettyServer.media.stream.base.IClientStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 所有rtmp处理程序的基类.
 *
 * @author The Red5 Project
 */
@Slf4j
public abstract class BaseRtmpPacketHandler implements IRtmpPacketHandler, Constants, StatusCodes {

    public void connectionOpened(RTMPConnection conn) {
        if (log.isTraceEnabled()) {
            log.trace("connectionOpened - conn: {} state: {}", conn, conn.getState());
        }
        conn.open();
    }

    @Override
    public void messageReceived(RTMPConnection conn, Packet packet) throws Exception {
        if (conn == null) {
            return;
        }
        log.trace("messageReceived connection: {}", conn.getSessionId());
        IRTMPEvent message = null;
        try {
            message = packet.getMessage();
            final Header header = packet.getHeader();
            final Number streamId = header.getStreamId();
            final Channel channel = conn.createChannelIfAbsent(header.getCsId());
            final IClientStream stream = conn.getStreamById(streamId);
            if (log.isTraceEnabled()) {
                log.trace("Message received - header: {}", header);
            }
            conn.setStreamId(streamId);
            //增加接收的消息数
            conn.messageReceived();
            // 消息来源
            message.setSource(conn);
            // 基于数据类型的处理
            final byte headerDataType = header.getDataType();
            if (log.isTraceEnabled()) {
                log.trace("Header / message data type: {}", headerDataType);
            }
            switch (headerDataType) {
                case TYPE_AGGREGATE:
                    log.debug("Aggregate type data - header timer: {} size: {}", header.getTimer(), header.getDataSize());
                case TYPE_AUDIO_DATA:
                case TYPE_VIDEO_DATA:
                    // 将事件标记为来自实时源
                    message.setSourceType(Constants.SOURCE_TYPE_LIVE);
                    //注意：如果用“NetStream.Publish.BadName”响应“publish”，
                    //客户端在停止前发送一些流数据包。我们得无视他们
                    if (stream != null) {
                        ((IEventDispatcher) stream).dispatchEvent(message);
                    }
                    return;
                case TYPE_FLEX_SHARED_OBJECT:
                case TYPE_SHARED_OBJECT:
                    onSharedObject(conn, channel, header, (SharedObjectMessage) message);
                    break;
                case TYPE_INVOKE:  //AFM0
                    log.trace("AFM0");
                case TYPE_FLEX_MESSAGE: //AFM3
                    onCommand(conn, channel, header, (Invoke) message);
                    IPendingServiceCall call = ((Invoke) message).getCall();
                    if (message.getHeader().getStreamId().intValue() != 0 && call.getServiceName() == null && StreamAction.PUBLISH.equals(call.getServiceMethodName())) {
                        if (stream != null) {
                            // Only dispatch if stream really was created
                            ((IEventDispatcher) stream).dispatchEvent(message);
                        }
                    }
                    break;
                case TYPE_NOTIFY:
                    // 类似于调用，但不返回任何内容，且调用/事务ID为0
                case TYPE_FLEX_STREAM_SEND:
                    if (((Notify) message).getData() != null && stream != null) {
                        // Stream metadata
                        ((IEventDispatcher) stream).dispatchEvent(message);
                    } else {
                        onCommand(conn, channel, header, (Notify) message);
                    }
                    break;
                case TYPE_PING:
                    onPing(conn, channel, header, (Ping) message);
                    break;
                case TYPE_BYTES_READ:
                    onStreamBytesRead(conn, channel, header, (BytesRead) message);
                    break;
                case TYPE_CHUNK_SIZE:
                    onChunkSize(conn, channel, header, (ChunkSize) message);
                    break;
                case Constants.TYPE_CLIENT_BANDWIDTH: // onBWDone / peer bw
                    log.debug("Client bandwidth: {}", message);
                    onClientBandwidth(conn, channel, (ClientBW) message);
                    break;
                case Constants.TYPE_SERVER_BANDWIDTH: // window ack size
                    log.debug("Server bandwidth: {}", message);
                    onServerBandwidth(conn, channel, (ServerBW) message);
                    break;
                default:
                    log.debug("Unknown type: {}", header.getDataType());
            }
            if (message instanceof Unknown) {
                log.info("Message type unknown: {}", message);
            }
            log.debug("headerDataType {}", headerDataType);
        } catch (Throwable t) {
            log.error("Exception", t);
        }
    }

    public void messageSent(RTMPConnection conn, Packet packet) {
        log.trace("Message sent");
        // increase number of sent messages
        conn.messageSent(packet);
    }

    public void connectionClosed(RTMPConnection conn) {
        log.debug("connectionClosed: {}", conn.getSessionId());
        if (conn.getStateCode() != RtmpProtocolState.STATE_DISCONNECTED) {
            // inform any callbacks for pending calls that the connection is closed
            conn.sendPendingServiceCallsCloseError();
            // close the connection
            if (conn.getStateCode() != RtmpProtocolState.STATE_DISCONNECTING) {
                conn.close();
            }
            // set as disconnected
            conn.setStateCode(RtmpProtocolState.STATE_DISCONNECTED);
        }
        //TODO: 移除连接
        log.trace("connectionClosed: {}", conn);
    }

    /**
     * Return hostname for URL.
     */
    protected String getHostname(String url) {
        if (log.isDebugEnabled()) {
            log.debug("getHostname - url: {}", url);
        }
        String[] parts = url.split("/");
        if (parts.length == 2) {
            return "";
        } else {
            String host = parts[2];
            // strip out default port in case the client added the port explicitly
            if (host.endsWith(":1935")) {
                // remove default port from connection string
                return host.substring(0, host.length() - 5);
            }
            return host;
        }
    }

    /**
     * 挂起调用结果的处理程序。将结果分派给所有挂起的调用处理程序。
     */
    protected void handlePendingCallResult(RTMPConnection conn, Notify invoke) {
        final IServiceCall call = invoke.getCall();
        final IPendingServiceCall pendingCall = conn.retrievePendingCall(invoke.getTransactionId());
        if (pendingCall == null) {
            return;
        }
        // 客户机对先前发出的呼叫发送了响应.
        Object[] args = call.getArguments();
        if (args != null && args.length > 0) {
            pendingCall.setResult(args[0]);
        }
        Set<IPendingServiceCallback> callbacks = pendingCall.getCallbacks();
        if (callbacks.isEmpty()) {
            return;
        }
        HashSet<IPendingServiceCallback> tmp = new HashSet<>();
        tmp.addAll(callbacks);
        for (IPendingServiceCallback callback : tmp) {
            try {
                callback.resultReceived(pendingCall);
            } catch (Exception e) {
                log.error("Error while executing callback {}", callback, e);
            }
        }
    }

    /**
     * Chunk size change event handler. Abstract, to be implemented in subclasses.
     */
    protected abstract void onChunkSize(RTMPConnection conn, Channel channel, Header source, ChunkSize chunkSize);

    /**
     * Invocation event handler.
     */
    protected abstract void onInvoke(RTMPConnection conn, Channel channel, Header source, Notify invoke, RtmpProtocolState rtmpProtocolState);

    /**
     * Command event handler, which current consists of an Invoke or Notify type object.
     */
    protected abstract void onCommand(RTMPConnection conn, Channel channel, Header source, ICommand command);

    /**
     * Ping event handler.
     */
    protected abstract void onPing(RTMPConnection conn, Channel channel, Header source, Ping ping);

    /**
     * Server bandwidth / Window ACK size event handler.
     */
    protected void onServerBandwidth(RTMPConnection conn, Channel channel, ServerBW message) {
    }

    protected void onClientBandwidth(RTMPConnection conn, Channel channel, ClientBW message) {
    }

    protected void onStreamBytesRead(RTMPConnection conn, Channel channel, Header source, BytesRead streamBytesRead) {
        conn.receivedBytesRead(streamBytesRead.getBytesRead());
    }

    protected abstract void onSharedObject(RTMPConnection conn, Channel channel, Header source, SharedObjectMessage message);

    public void connectionOpened(RTMPConnection conn, RtmpProtocolState state) {
        log.trace("connectionOpened - conn: {} state: {}", conn, state);
        if (state.getMode() == RtmpProtocolState.MODE_SERVER /*&& appCtx != null*/) {
            ISchedulingService service = QuartzSchedulingService.getInstance();//(ISchedulingService) appCtx.getBean(ISchedulingService.BEAN_NAME);
//            conn.startWaitForHandshake(service);
        }
    }

    public void connectionClosed(RTMPConnection conn, RtmpProtocolState state) {
        log.debug("connectionClosed: {}", conn.getSessionId());
        if (conn.getStateCode() != RtmpProtocolState.STATE_DISCONNECTED) {
            // inform any callbacks for pending calls that the connection is closed
            conn.sendPendingServiceCallsCloseError();
            // close the connection
            if (conn.getStateCode() != RtmpProtocolState.STATE_DISCONNECTING) {
                conn.close();
            }
            // set as disconnected
            conn.setStateCode(RtmpProtocolState.STATE_DISCONNECTED);
        }
        log.trace("connectionClosed: {}", conn);
    }
}
