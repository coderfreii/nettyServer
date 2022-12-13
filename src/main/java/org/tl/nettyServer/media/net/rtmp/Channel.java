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

package org.tl.nettyServer.media.net.rtmp;

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.consts.ChunkStreamType;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.net.rtmp.event.Invoke;
import org.tl.nettyServer.media.net.rtmp.event.Notify;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.net.rtmp.status.Status;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.Call;
import org.tl.nettyServer.media.service.PendingCall;
import org.tl.nettyServer.media.stream.IClientStream;
import org.tl.nettyServer.media.stream.IRtmpSampleAccess;
import org.tl.nettyServer.media.stream.IStreamData;

/**
 * Identified connection that transfers packets.
 * Chunk stream ID 是用来区分消息信道的 即此项目的channelId
 * 一个tcp连接对应多个channel
 */
@Slf4j
public class Channel {
    private final static String CALL_ON_STATUS = "onStatus";

    /**
     * RTMP connection used to transfer packets.
     */
    private final RTMPConnection connection;

    /**
     * Channel id
     */
    private final int id;

    /**
     * Creates channel from connection and channel id
     *
     * @param conn      Connection
     * @param channelId Channel id
     */
    public Channel(RTMPConnection conn, int channelId) {
        assert (conn != null);
        connection = conn;
        id = channelId;
    }

    /**
     * Closes channel with this id on RTMP connection.
     */
    public void close() {
        log.debug("Closing channel: {}", id);
        connection.closeChannel(id);
    }

    /**
     * Getter for id.
     *
     * @return Channel ID
     */
    public int getId() {
        return id;
    }

    /**
     * Getter for RTMP connection.
     *
     * @return RTMP connection
     */
    protected RTMPConnection getConnection() {
        return connection;
    }

    /**
     * Writes packet from event data to RTMP connection.
     *
     * @param event Event data
     */
    public void write(IRTMPEvent event) {
        if (!connection.isClosed()) {
            final IClientStream stream = connection.getStreamByChannelId(id);
            if (id > ChunkStreamType.RTMP_COMMAND_CHANNEL && stream == null) {
                log.warn("Non-existent stream for channel id: {}, session: {} discarding: {}", id, connection.getSessionId(), event);
            }
            // if the stream is non-existent, the event will go out with stream id == 0
            final Number streamId = (stream == null) ? 0 : stream.getStreamId();
            write(event, streamId);
        } else {
            log.debug("Connection {} is closed, cannot write to channel: {}", connection.getSessionId(), id);
        }
    }

    /**
     * Writes packet from event data to RTMP connection and stream id.
     *
     * @param event    Event data
     * @param streamId Stream id
     */
    private void write(IRTMPEvent event, Number streamId) {
        log.trace("write to stream id: {} channel: {}", streamId, id);
        final Header header = new Header();
        final Packet packet = new Packet(header, event);
        // set the channel id
        header.setCsId(id);
        int ts = event.getTimestamp();
        if (ts != 0) {
            header.setTimer(event.getTimestamp());
        }
        header.setStreamId(streamId);
        header.setDataType(event.getDataType());
        // should use RTMPConnection specific method.. 
        //log.trace("Connection type for write: {}", connection.getClass().getName());
        connection.write(packet);
    }

    /**
     * Discard an event routed to this channel.
     *
     * @param event
     */
    @SuppressWarnings("unused")
    private void discard(IRTMPEvent event) {
        if (event instanceof IStreamData<?>) {
            log.debug("Discarding: {}", ((IStreamData<?>) event).toString());
            BufFacade data = ((IStreamData<?>) event).getData();
            if (data != null) {
                log.trace("Freeing discarded event data");
                data.release();
                data = null;
            }
        }
        event.setHeader(null);
    }

    /**
     * Sends status notification.
     *
     * @param status Status
     */
    public void sendStatus(Status status) {
        if (connection != null) {
            final boolean andReturn = !status.getCode().equals(StatusCodes.NS_DATA_START);
            final Invoke event = new Invoke();
            if (andReturn) {
                final PendingCall call = new PendingCall(null, CALL_ON_STATUS, new Object[]{status});
                if (status.getCode().equals(StatusCodes.NS_PLAY_START)) {
                    IScope scope = connection.getScope();
                    if (scope.getContext().getApplicationContext().containsBean(IRtmpSampleAccess.BEAN_NAME)) {
                        IRtmpSampleAccess sampleAccess = (IRtmpSampleAccess) scope.getContext().getApplicationContext().getBean(IRtmpSampleAccess.BEAN_NAME);
                        boolean videoAccess = sampleAccess.isVideoAllowed(scope);
                        boolean audioAccess = sampleAccess.isAudioAllowed(scope);
                        if (videoAccess || audioAccess) {
                            final Call call2 = new Call(null, "|RtmpSampleAccess", null);
                            Notify notify = new Notify();
                            notify.setCall(call2);
                            notify.setData(BufFacade.wrappedBuffer(new byte[]{0x01, (byte) (audioAccess ? 0x01 : 0x00), 0x01, (byte) (videoAccess ? 0x01 : 0x00)}));
                            write(notify, connection.getStreamIdForChannelId(id));
                        }
                    }
                }
                event.setCall(call);
            } else {
                final Call call = new Call(null, CALL_ON_STATUS, new Object[]{status});
                event.setCall(call);
            }
            // send directly to the corresponding stream as for some status codes, no stream has been created  and thus "getStreamByChannelId" will fail
            write(event, connection.getStreamIdForChannelId(id));
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (connection != null) {
            return "Channel [id=" + id + ", stream id=" + connection.getStreamIdForChannelId(id) + ", session=" + connection.getSessionId() + "]";
        }
        return "Channel [id=" + id + "]";
    }

}
