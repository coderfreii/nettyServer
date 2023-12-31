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

package org.tl.nettyServer.media.net.rtmp.codec;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.ICommand;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.buf.ReleaseUtil;
import org.tl.nettyServer.media.exception.ClientDetailsException;
import org.tl.nettyServer.media.io.amf.Output;
import org.tl.nettyServer.media.io.amf3.Output3;
import org.tl.nettyServer.media.io.object.Serializer;
import org.tl.nettyServer.media.net.rtmp.RTMPUtils;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.*;
import org.tl.nettyServer.media.net.rtmp.status.Status;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.net.rtmp.status.StatusObject;
import org.tl.nettyServer.media.service.call.IPendingServiceCall;
import org.tl.nettyServer.media.service.call.IServiceCall;
import org.tl.nettyServer.media.service.call.ServiceCall;
import org.tl.nettyServer.media.so.ISharedObjectEvent;
import org.tl.nettyServer.media.so.ISharedObjectMessage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RtmpProtocolState protocol encoder encodes RtmpProtocolState messages and packets to byte buffers.
 */
public class RTMPProtocolEncoder implements Constants, IEventEncoder {

    protected Logger log = LoggerFactory.getLogger(RTMPProtocolEncoder.class);

    /**
     * Tolerance (in milliseconds) for late media on streams. A set of levels based on this value will be determined.
     */
    private long baseTolerance = 15000;

    /**
     * Middle tardiness level, between base and this value disposable frames will be dropped. Between this and highest value regular interframes will be dropped.
     */
    private long midTolerance = baseTolerance + (long) (baseTolerance * 0.3);

    /**
     * Highest tardiness level before dropping key frames
     */
    private long highestTolerance = baseTolerance + (long) (baseTolerance * 0.6);

    /**
     * Indicates if we should drop live packets with future timestamp (i.e, when publisher bandwidth is limited) - EXPERIMENTAL
     */
    private boolean dropLiveFuture;

    /**
     * Whether or not to allow dropper determination code.
     */
    private boolean dropEncoded;

    /**
     * Encodes object with given protocol state to byte buffer
     *
     * @param message Object to encode
     * @return BufFacade with encoded data
     * @throws Exception Any decoding exception
     */
    public BufFacade encode(Object message) throws Exception {
        if (message != null) {
            try {
                return encodePacket((Packet) message);
            } catch (Exception e) {
                log.error("Error encoding", e);
            }
        } else if (log.isDebugEnabled()) {
            try {
                String callingMethod = Thread.currentThread().getStackTrace()[4].getMethodName();
                log.debug("Message is null at encode, expecting a Packet from: {}", callingMethod);
            } catch (Throwable t) {
                log.warn("Problem getting current calling method from stacktrace", t);
            }
        }
        return null;
    }

    /**
     * Encode packet.
     *
     * @param packet RtmpProtocolState packet
     * @return Encoded data
     */
    public BufFacade encodePacket(Packet packet) {
        BufFacade out = null;
        Header header = packet.getHeader();
        int channelId = header.getCsId();
        //log.trace("Channel id: {}", channelId);
        IRTMPEvent message = packet.getMessage();
        if (message instanceof ChunkSize) {
            ChunkSize chunkSizeMsg = (ChunkSize) message;
            ((RTMPConnection) Red5.getConnectionLocal()).getState().setWriteChunkSize(chunkSizeMsg.getSize());
        }
        // normally the message is expected not to be dropped
        if (!dropMessage(channelId, message)) {
            //log.trace("Header time: {} message timestamp: {}", header.getTimer(), message.getTimestamp());
            BufFacade data = encodeMessage(header, message);
            if (data != null) {
                RtmpProtocolState rtmpProtocolState = ((RTMPConnection) Red5.getConnectionLocal()).getState();
                // set last write packet
                rtmpProtocolState.setLastWritePacket(channelId, packet);
                // ensure we're at the beginning
                if (data.readerIndex() != 0) {
                    data.rewind();
                }
                // length of the data to be chunked
                int dataLen = data.readableBytes();
                header.setDataSize(dataLen);
                if (log.isTraceEnabled()) {
                    log.trace("Message: {}", data);
                }
                // chunk size for writing
                int chunkSize = rtmpProtocolState.getWriteChunkSize();
                // number of chunks to write
                int numChunks = (int) Math.ceil(dataLen / (float) chunkSize);
                // get last header
                Header lastHeader = rtmpProtocolState.getLastWriteHeader(channelId);
                if (log.isTraceEnabled()) {
                    log.trace("Channel id: {} chunkSize: {}", channelId, chunkSize);
                }
                // attempt to properly guess the size of the buffer we'll need
                int bufSize = dataLen + 18 + (numChunks * 2);
                log.trace("Allocated buffer size: {}", bufSize);
                out = BufFacade.buffer(bufSize);
                do {
                    // encode the header
                    encodeHeader(header, lastHeader, out);
                    // write a chunk
                    byte[] buf = new byte[Math.min(chunkSize, data.readableBytes())];
                    data.readBytes(buf);
                    //log.trace("Buffer: {}", Hex.encodeHexString(buf));
                    out.writeBytes(buf);
                    // move header over to last header
                    lastHeader = header.clone();
                } while (data.readable());
                // collapse the time stamps on the last header after decode is complete
                lastHeader.setTimerBase(lastHeader.getTimer());
                // clear the delta
                lastHeader.setTimerDelta(0);
                // set last write header
                rtmpProtocolState.setLastWriteHeader(channelId, lastHeader);

                //这里release-
                ReleaseUtil.releaseAll(data);
                data = null;
            }
        } else {
            //这里release-
            ReleaseUtil.releaseAll(packet);
        }
        return out;
    }

    /**
     * Determine if this message should be dropped. If the traffic from server to client is congested, then drop LIVE messages to help alleviate congestion.
     * <p>
     * - determine latency between server and client using ping
     * - ping timestamp is unsigned int (4 bytes) and is set from value on sender
     * <p>
     * 1st drop disposable frames - lowest mark 2nd drop interframes - middle 3rd drop key frames - high mark
     *
     * @param channelId the channel ID
     * @param message   the message
     * @return true to drop; false to send
     */
    protected boolean dropMessage(int channelId, IRTMPEvent message) {
        // whether or not to allow dropping functionality
        if (!dropEncoded) {
            log.trace("Not dropping due to flag, source type: {} (0=vod,1=live)", message.getSourceType());
            return false;
        }
        // dont want to drop vod
        boolean isLiveStream = message.getSourceType() == Constants.SOURCE_TYPE_LIVE;
        if (!isLiveStream) {
            log.trace("Not dropping due to vod");
            return false;
        }
        RTMPConnection conn = (RTMPConnection) Red5.getConnectionLocal();
        if (message instanceof Ping) {
            final Ping pingMessage = (Ping) message;
            if (pingMessage.getEventType() == Ping.STREAM_PLAYBUFFER_CLEAR) {
                // client buffer cleared, make sure to reset timestamps for this stream
                final int channel = conn.getChannelIdForStreamId(pingMessage.getValue2());
                log.trace("Ping stream id: {} channel id: {}", pingMessage.getValue2(), channel);
                conn.getState().clearLastTimestampMapping(channel, channel + 1, channel + 2);
            }
            // never drop pings
            return false;
        }
        // whether or not the packet will be dropped
        boolean drop = false;
        // we only drop audio or video data
        boolean isDroppable = message instanceof VideoData || message instanceof AudioData;
        if (isDroppable) {
            if (message.getTimestamp() == 0) {
                // never drop initial packages, also this could be the first packet after
                // MP4 seeking and therefore mess with the timestamp mapping
                return false;
            }
            if (log.isDebugEnabled()) {
                String sourceType = (isLiveStream ? "LIVE" : "VOD");
                log.debug("Connection: {} connType={}", conn.getSessionId(), sourceType);
            }
            RtmpProtocolState rtmpProtocolState = conn.getState();
            long timestamp = (message.getTimestamp() & 0xFFFFFFFFL);
            RtmpProtocolState.LiveTimestampMapping mapping = rtmpProtocolState.getLastTimestampMapping(channelId);
            long now = System.currentTimeMillis();
            if (mapping == null || timestamp < mapping.getLastStreamTime()) {
                log.trace("Resetting clock time ({}) to stream time ({})", now, timestamp);
                // either first time through, or time stamps were reset
                mapping = rtmpProtocolState.new LiveTimestampMapping(now, timestamp);
                rtmpProtocolState.setLastTimestampMapping(channelId, mapping);
            }
            mapping.setLastStreamTime(timestamp);
            // Calculate when this message should have arrived. Take the time when the stream started, add
            // the current message's timestamp and subtract the timestamp of the first message.
            long clockTimeOfMessage = mapping.getClockStartTime() + timestamp - mapping.getStreamStartTime();

            // Determine how late this message is. This will tell us if the incoming path is congested.
            long incomingLatency = clockTimeOfMessage - now;

            if (log.isDebugEnabled()) {
                log.debug("incomingLatency={} clockTimeOfMessage={} getClockStartTime={} timestamp={} getStreamStartTime={} now={}", new Object[]{incomingLatency, clockTimeOfMessage, mapping.getClockStartTime(), timestamp, mapping.getStreamStartTime(), now});
            }
            //TDJ: EXPERIMENTAL dropping for LIVE packets in future (default false)
            if (isLiveStream && dropLiveFuture) {
                incomingLatency = Math.abs(incomingLatency);
                if (log.isDebugEnabled()) {
                    log.debug("incomingLatency={} clockTimeOfMessage={} now={}", new Object[]{incomingLatency, clockTimeOfMessage, now});
                }
            }
            // NOTE: We could decide to drop the message here because it is very late due to incoming traffic congestion.

            // We need to calculate how long will this message reach the client. If the traffic is congested, we decide to drop the message.
            long outgoingLatency = 0L;
            if (conn != null) {
                // Determine the interval when the last ping was sent and when the pong was received. The
                // duration will be the round-trip time (RTT) of the ping-pong message. If it is high then it
                // means that there is congestion in the connection.
                int lastPingPongInterval = conn.getLastPingSentAndLastPongReceivedInterval();
                if (lastPingPongInterval > 0) {
                    // The outgoingLatency is the ping RTT minus the incoming latency.
                    outgoingLatency = lastPingPongInterval - incomingLatency;
                    if (log.isDebugEnabled()) {
                        log.debug("outgoingLatency={} lastPingTime={}", new Object[]{outgoingLatency, lastPingPongInterval});
                    }
                }
            }
            //TODO: how should we differ handling based on live or vod?
            //TODO: if we are VOD do we "pause" the provider when we are consistently late?
            if (log.isTraceEnabled()) {
                log.trace("Packet timestamp: {}; latency: {}; now: {}; message clock time: {}, dropLiveFuture: {}", new Object[]{timestamp, incomingLatency, now, clockTimeOfMessage, dropLiveFuture});
            }
            if (outgoingLatency < baseTolerance) {
                // no traffic congestion in outgoing direction
            } else if (outgoingLatency > highestTolerance) {
                // traffic congestion in outgoing direction
                if (log.isDebugEnabled()) {
                    log.debug("Outgoing direction congested. outgoingLatency={} highestTolerance={}", new Object[]{outgoingLatency, highestTolerance});
                }
                if (isDroppable) {
                    mapping.setKeyFrameNeeded(true);
                }
                drop = true;
            } else {
                if (isDroppable && message instanceof VideoData) {
                    VideoData video = (VideoData) message;
                    if (video.getFrameType() == VideoData.FrameType.KEYFRAME) {
                        // if its a key frame the inter and disposable checks can be skipped
                        if (log.isDebugEnabled()) {
                            log.debug("Resuming stream with key frame; message: {}", message);
                        }
                        mapping.setKeyFrameNeeded(false);
                    } else if (incomingLatency >= baseTolerance && incomingLatency < midTolerance) {
                        // drop disposable frames
                        if (video.getFrameType() == VideoData.FrameType.DISPOSABLE_INTERFRAME) {
                            if (log.isDebugEnabled()) {
                                log.debug("Dropping disposible frame; message: {}", message);
                            }
                            drop = true;
                        }
                    } else if (incomingLatency >= midTolerance && incomingLatency <= highestTolerance) {
                        // drop inter-frames and disposable frames
                        if (log.isDebugEnabled()) {
                            log.debug("Dropping disposible or inter frame; message: {}", message);
                        }
                        drop = true;
                    }
                }
            }
        }
        if (log.isDebugEnabled() && drop) {
            log.debug("Message was dropped");
        }
        return drop;
    }

    /**
     * Determine type of header to use.
     *
     * @param header     RtmpProtocolState message header
     * @param lastHeader Previous header
     * @return Header type to use
     */
    private byte getHeaderType(final Header header, final Header lastHeader) {
        //int lastFullTs = ((RTMPConnection) Red5.getConnectionLocal()).getState().getLastFullTimestampWritten(header.getCsId());
        if (lastHeader == null || header.getStreamId() != lastHeader.getStreamId() || header.getTimer() < lastHeader.getTimer()) {
            // new header mark if header for another stream
            return HEADER_NEW;
        } else if (header.getDataSize() != lastHeader.getDataSize() || header.getDataType() != lastHeader.getDataType()) {
            // same source header if last header data type or size differ
            return HEADER_SAME_SOURCE;
        } else if (header.getTimer() != lastHeader.getTimer()) {
            // timer change marker if there's time gap between header time stamps
            return HEADER_TIMER_CHANGE;
        }
        // continue encoding
        return HEADER_CONTINUE;
    }

    /**
     * Calculate number of bytes necessary to encode the header.
     *
     * @param header     RtmpProtocolState message header
     * @param lastHeader Previous header
     * @return Calculated size
     */
    private int calculateHeaderSize(final Header header, final Header lastHeader) {
        final byte headerType = getHeaderType(header, lastHeader);
        int channelIdAdd = 0;
        int channelId = header.getCsId();
        if (channelId > 320) {
            channelIdAdd = 2;
        } else if (channelId > 63) {
            channelIdAdd = 1;
        }
        return RTMPUtils.getHeaderLength(headerType) + channelIdAdd;
    }

    /**
     * Encode RtmpProtocolState header.
     *
     * @param header     RtmpProtocolState message header
     * @param lastHeader Previous header
     * @return Encoded header data
     */
    public BufFacade encodeHeader(Header header, Header lastHeader) {
        final BufFacade result = BufFacade.buffer(calculateHeaderSize(header, lastHeader));
        encodeHeader(header, lastHeader, result);
        return result;
    }

    /**
     * Encode RtmpProtocolState header into given BufFacade.
     *
     * @param header     RtmpProtocolState message header
     * @param lastHeader Previous header
     * @param buf        Buffer for writing encoded header into
     */
    public void encodeHeader(Header header, Header lastHeader, BufFacade buf) {
        byte headerType = getHeaderType(header, lastHeader);
        RTMPUtils.encodeHeaderByte(buf, headerType, header.getCsId());
        if (log.isTraceEnabled()) {
//            log.trace("{} lastHeader: {}", HeaderType.HeaderTypeValues.values[headerType], lastHeader);
        }
        /*
         Timestamps in RtmpProtocolState are given as an integer number of milliseconds relative to an unspecified epoch. Typically, each stream will start
         with a timestamp of 0, but this is not required, as long as the two endpoints agree on the epoch. Note that this means that any
         synchronization across multiple streams (especially from separate hosts) requires some additional mechanism outside of RtmpProtocolState.
         Because timestamps are 32 bits long, they roll over every 49 days, 17 hours, 2 minutes and 47.296 seconds. Because streams are allowed to
         run continuously, potentially for years on end, an RtmpProtocolState application SHOULD use serial number arithmetic [RFC1982] when processing
         timestamps, and SHOULD be capable of handling wraparound. For example, an application assumes that all adjacent timestamps are
         within 2^31 - 1 milliseconds of each other, so 10000 comes after 4000000000, and 3000000000 comes before 4000000000.
         Timestamp deltas are also specified as an unsigned integer number of milliseconds, relative to the previous timestamp. Timestamp deltas
         may be either 24 or 32 bits long.
         */
        int timeBase = 0, timeDelta = 0;
        int headerSize = header.getDataSize();
        // encode the message header section
        switch (headerType) {
            case HEADER_NEW: // type 0 - 11 bytes
                timeBase = header.getTimerBase();
                // absolute time - unsigned 24-bit (3 bytes) (chop at max 24bit time) 
                RTMPUtils.writeMediumInt(buf, Math.min(timeBase, MEDIUM_INT_MAX));
                // header size 24-bit (3 bytes)
                RTMPUtils.writeMediumInt(buf, headerSize);
                // 1 byte
                buf.writeByte(header.getDataType());
                // little endian 4 bytes
                RTMPUtils.writeReverseInt(buf, header.getStreamId().intValue());
                header.setTimerDelta(timeDelta);
                // write the extended timestamp if we are indicated to do so
                if (timeBase >= MEDIUM_INT_MAX) {
                    buf.writeInt(timeBase);
                    header.setExtended(true);
                }
                RTMPConnection conn = (RTMPConnection) Red5.getConnectionLocal();
                if (conn != null) {
                    conn.getState().setLastFullTimestampWritten(header.getCsId(), timeBase);
                }
                break;
            case HEADER_SAME_SOURCE: // type 1 - 7 bytes
                // delta type
                timeDelta = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
                header.setTimerDelta(timeDelta);
                // write the time delta 24-bit 3 bytes
                RTMPUtils.writeMediumInt(buf, Math.min(timeDelta, MEDIUM_INT_MAX));
                // write header size
                RTMPUtils.writeMediumInt(buf, headerSize);
                buf.writeByte(header.getDataType());
                // write the extended timestamp if we are indicated to do so
                if (timeDelta >= MEDIUM_INT_MAX) {
                    buf.writeInt(timeDelta);
                    header.setExtended(true);
                }
                // time base is from last header minus delta
                timeBase = header.getTimerBase() - timeDelta;
                header.setTimerBase(timeBase);
                break;
            case HEADER_TIMER_CHANGE: // type 2 - 3 bytes
                // delta type
                timeDelta = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
                header.setTimerDelta(timeDelta);
                // write the time delta 24-bit 3 bytes
                RTMPUtils.writeMediumInt(buf, Math.min(timeDelta, MEDIUM_INT_MAX));
                // write the extended timestamp if we are indicated to do so
                if (timeDelta >= MEDIUM_INT_MAX) {
                    buf.writeInt(timeDelta);
                    header.setExtended(true);
                }
                // time base is from last header minus delta
                timeBase = header.getTimerBase() - timeDelta;
                header.setTimerBase(timeBase);
                break;
            case HEADER_CONTINUE: // type 3 - 0 bytes
                // time base from the most recent header
                timeBase = header.getTimerBase() - timeDelta;
                // write the extended timestamp if we are indicated to do so
                if (lastHeader.isExtended()) {
                    buf.writeInt(timeBase);
                }
                break;
            default:
                break;
        }
//        log.trace("Encoded chunk {} {}", HeaderType.HeaderTypeValues.values[headerType], header);
    }

    /**
     * Encode message.
     *
     * @param header  RtmpProtocolState message header
     * @param message RtmpProtocolState message (event)
     * @return Encoded message data
     */
    public BufFacade encodeMessage(Header header, IRTMPEvent message) {
        IServiceCall call = null;
        switch (header.getDataType()) {
            case TYPE_CHUNK_SIZE:
                return encodeChunkSize((ChunkSize) message);
            case TYPE_INVOKE:
                log.trace("Invoke {}", message);
                call = ((Invoke) message).getCall();
                if (call != null) {
                    log.debug("{}", call.toString());
                    Object[] args = call.getArguments();
                    if (args != null && args.length > 0) {
                        Object a0 = args[0];
                        if (a0 instanceof Status) {
                            Status status = (Status) a0;
                            //code: NetStream.Seek.Notify
                            if (StatusCodes.NS_SEEK_NOTIFY.equals(status.getCode())) {
                                //desc: Seeking 25000 (stream ID: 1).
                                int seekTime = Integer.valueOf(status.getDescription().split(" ")[1]);
                                log.trace("Seek to time: {}", seekTime);
                                // TODO make sure this works on stream ids > 1
                                //audio and video channels
                                int[] channels = new int[]{5, 6};
                                //if its a seek notification, reset the "mapping" for audio (5) and video (6)
                                RtmpProtocolState rtmpProtocolState = ((RTMPConnection) Red5.getConnectionLocal()).getState();
                                for (int channelId : channels) {
                                    RtmpProtocolState.LiveTimestampMapping mapping = rtmpProtocolState.getLastTimestampMapping(channelId);
                                    if (mapping != null) {
                                        long timestamp = mapping.getClockStartTime() + (seekTime & 0xFFFFFFFFL);
                                        log.trace("Setting last stream time to: {}", timestamp);
                                        mapping.setLastStreamTime(timestamp);
                                    } else {
                                        log.trace("No ts mapping for channel id: {}", channelId);
                                    }
                                }
                            }
                        }
                    }
                }
                return encodeInvoke((Invoke) message);
            case TYPE_NOTIFY:
                log.trace("Notify {}", message);
                call = ((Notify) message).getCall();
                if (call == null) {
                    return encodeStreamMetadata((Notify) message);
                } else {
                    return encodeNotify((Notify) message);
                }
            case TYPE_PING:
                if (message instanceof SetBuffer) {
                    return encodePing((SetBuffer) message);
                } else if (message instanceof SWFResponse) {
                    return encodePing((SWFResponse) message);
                } else {
                    return encodePing((Ping) message);
                }
            case TYPE_BYTES_READ:
                return encodeBytesRead((BytesRead) message);
            case TYPE_AGGREGATE:
                log.trace("Encode aggregate message");
                return encodeAggregate((Aggregate) message);
            case TYPE_AUDIO_DATA:
                log.trace("Encode audio message");
                return encodeAudioData((AudioData) message);
            case TYPE_VIDEO_DATA:
                log.trace("Encode video message");
                return encodeVideoData((VideoData) message);
            case TYPE_FLEX_SHARED_OBJECT:
                return encodeFlexSharedObject((ISharedObjectMessage) message);
            case TYPE_SHARED_OBJECT:
                return encodeSharedObject((ISharedObjectMessage) message);
            case TYPE_SERVER_BANDWIDTH:
                return encodeServerBW((ServerBW) message);
            case TYPE_CLIENT_BANDWIDTH:
                return encodeClientBW((ClientBW) message);
            case TYPE_FLEX_MESSAGE:
                return encodeFlexMessage((FlexMessage) message);
            case TYPE_FLEX_STREAM_SEND:
                return encodeFlexStreamSend((FlexStreamSend) message);
            default:
                log.warn("Unknown object type: {}", header.getDataType());
        }
        return null;
    }

    /**
     * Encode server-side bandwidth event.
     *
     * @param serverBW Server-side bandwidth event
     * @return Encoded event data
     */
    private BufFacade encodeServerBW(ServerBW serverBW) {
        final BufFacade out = BufFacade.buffer(4);
        out.writeInt(serverBW.getBandwidth());
        return out;
    }

    /**
     * Encode client-side bandwidth event.
     *
     * @param clientBW Client-side bandwidth event
     * @return Encoded event data
     */
    private BufFacade encodeClientBW(ClientBW clientBW) {
        final BufFacade out = BufFacade.buffer(5);
        out.writeInt(clientBW.getBandwidth());
        out.writeByte(clientBW.getLimitType());
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeChunkSize(ChunkSize chunkSize) {
        final BufFacade out = BufFacade.buffer(4);
        out.writeInt(chunkSize.getSize());
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeFlexSharedObject(ISharedObjectMessage so) {
        final BufFacade out = BufFacade.buffer(128);
        out.writeByte((byte) 0x00); // unknown (not AMF version)
        doEncodeSharedObject(so, out);
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeSharedObject(ISharedObjectMessage so) {
        final BufFacade out = BufFacade.buffer(128);
        doEncodeSharedObject(so, out);
        return out;
    }

    /**
     * Perform the actual encoding of the shared object contents.
     *
     * @param so  shared object
     * @param out output buffer
     */
    private void doEncodeSharedObject(ISharedObjectMessage so, BufFacade out) {
        final IConnection.Encoding encoding = Red5.getConnectionLocal().getEncoding();
        final Output output = new Output(out);
        final Output amf3output = new Output3(out);
        output.putString(so.getName());
        // SO version
        out.writeInt(so.getVersion());
        // Encoding (this always seems to be 2 for persistent shared objects)
        out.writeInt(so.isPersistent() ? 2 : 0);
        // unknown field
        out.writeInt(0);
        int mark, len;
        for (final ISharedObjectEvent event : so.getEvents()) {
            final ISharedObjectEvent.Type eventType = event.getType();
            byte type = SharedObjectTypeMapping.toByte(eventType);
            switch (eventType) {
                case SERVER_CONNECT:
                case CLIENT_INITIAL_DATA:
                case CLIENT_CLEAR_DATA:
                    out.writeByte(type);
                    out.writeInt(0);
                    break;
                case SERVER_DELETE_ATTRIBUTE:
                case CLIENT_DELETE_DATA:
                case CLIENT_UPDATE_ATTRIBUTE:
                    out.writeByte(type);
                    mark = out.readerIndex();
                    out.skipBytes(4); // we will be back
                    output.putString(event.getKey());
                    len = out.readerIndex() - mark - 4;
                    out.setInt(mark, len);
                    break;
                case SERVER_SET_ATTRIBUTE:
                case CLIENT_UPDATE_DATA:
                    if (event.getKey() == null) {
                        // Update multiple attributes in one request
                        Map<?, ?> initialData = (Map<?, ?>) event.getValue();
                        for (Object o : initialData.keySet()) {
                            out.writeByte(type);
                            mark = out.readerIndex();
                            out.skipBytes(4); // we will be back
                            String key = (String) o;
                            output.putString(key);
                            if (encoding == IConnection.Encoding.AMF3) {
                                Serializer.serialize(amf3output, initialData.get(key));
                            } else {
                                Serializer.serialize(output, initialData.get(key));
                            }
                            len = out.readerIndex() - mark - 4;
                            out.setInt(mark, len);
                        }
                    } else {
                        out.writeByte(type);
                        mark = out.readerIndex();
                        out.skipBytes(4); // we will be back
                        output.putString(event.getKey());
                        if (encoding == IConnection.Encoding.AMF3) {
                            Serializer.serialize(amf3output, event.getValue());
                        } else {
                            Serializer.serialize(output, event.getValue());
                        }
                        len = out.readerIndex() - mark - 4;
                        out.setInt(mark, len);
                    }
                    break;
                case CLIENT_SEND_MESSAGE:
                case SERVER_SEND_MESSAGE:
                    // Send method name and value
                    out.writeByte(type);
                    mark = out.readerIndex();
                    out.skipBytes(4);
                    // Serialize name of the handler to call...
                    Serializer.serialize(output, event.getKey());
                    try {
                        List<?> arguments = (List<?>) event.getValue();
                        if (arguments != null) {
                            // ...and the arguments
                            for (Object arg : arguments) {
                                if (encoding == IConnection.Encoding.AMF3) {
                                    Serializer.serialize(amf3output, arg);
                                } else {
                                    Serializer.serialize(output, arg);
                                }
                            }
                        } else {
                            // serialize a null as the arguments
                            if (encoding == IConnection.Encoding.AMF3) {
                                Serializer.serialize(amf3output, null);
                            } else {
                                Serializer.serialize(output, null);
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Exception encoding args for event: {}", event, ex);
                    }
                    len = out.readerIndex() - mark - 4;
                    //log.debug(len);
                    out.setInt(mark, len);
                    //log.info(out.getHexDump());
                    break;
                case CLIENT_STATUS:
                    out.writeByte(type);
                    final String status = event.getKey();
                    final String message = (String) event.getValue();
                    out.writeInt(message.length() + status.length() + 4);
                    output.putString(message);
                    output.putString(status);
                    break;
                default:
                    log.warn("Unknown event: {}", eventType);
                    // XXX: need to make this work in server or client mode
                    out.writeByte(type);
                    mark = out.readerIndex();
                    out.skipBytes(4); // we will be back
                    output.putString(event.getKey());
                    if (encoding == IConnection.Encoding.AMF3) {
                        Serializer.serialize(amf3output, event.getValue());
                    } else {
                        Serializer.serialize(output, event.getValue());
                    }
                    len = out.readerIndex() - mark - 4;
                    out.setInt(mark, len);
                    break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeNotify(Notify notify) {
        return encodeCommand(notify);
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeInvoke(Invoke invoke) {
        return encodeCommand(invoke);
    }

    /**
     * Encode notification event.
     *
     * @param invoke Notification event
     * @return Encoded event data
     */
    protected BufFacade encodeCommand(Notify invoke) {
        BufFacade out = BufFacade.buffer(1024);
        encodeCommand(out, invoke);
        return out;
    }

    /**
     * Encode command event and fill given byte buffer.
     *
     * @param out     Buffer to fill
     * @param command Command event
     */
    protected void encodeCommand(BufFacade out, ICommand command) {
        // TODO: tidy up here
        Output output = new Output(out);
        final IServiceCall call = command.getCall();
        final boolean isPending = (call.getStatus() == ServiceCall.STATUS_PENDING);
        log.debug("ServiceCall: {} pending: {}", call, isPending);
        if (!isPending) {
            log.debug("ServiceCall has been executed, send result");
            Serializer.serialize(output, call.isSuccess() ? "_result" : "_error");
        } else {
            log.debug("This is a pending call, send request");
            final String action = (call.getServiceName() == null) ? call.getServiceMethodName() : call.getServiceName() + '.' + call.getServiceMethodName();
            Serializer.serialize(output, action); // seems right
        }
        if (command instanceof Invoke) {
            Serializer.serialize(output, Integer.valueOf(command.getTransactionId()));
            Serializer.serialize(output, command.getConnectionParams());
        }
        if (call.getServiceName() == null && "connect".equals(call.getServiceMethodName())) {
            // response to initial connect, always use AMF0
            output = new Output(out);
        } else {
            if (Red5.getConnectionLocal().getEncoding() == IConnection.Encoding.AMF3) {
                output = new Output3(out);
            } else {
                output = new Output(out);
            }
        }
        if (!isPending && (command instanceof Invoke)) {
            IPendingServiceCall pendingCall = (IPendingServiceCall) call;
            if (!call.isSuccess() && (call.getException() != null || pendingCall.getResult() == null)) {
                log.debug("ServiceCall was not successful");
                StatusObject status = generateErrorResult(StatusCodes.NC_CALL_FAILED, call.getException());
                pendingCall.setResult(status);
            }
            Object res = pendingCall.getResult();
            log.debug("Writing result: {}", res);
            Serializer.serialize(output, res);
        } else {
            log.debug("Writing params");
            final Object[] args = call.getArguments();
            if (args != null) {
                for (Object element : args) {
                    if (element instanceof ByteBuffer) {
                        // a byte buffer indicates that serialization is already complete, send raw
                        final ByteBuffer buf = (ByteBuffer) element;
                        buf.mark();
                        try {
                            out.writeBytes(buf);
                        } finally {
                            buf.reset();
                        }
                    } else {
                        // standard serialize
                        Serializer.serialize(output, element);
                    }
                }
            }
        }
        if (command.getData() != null) {
            out.writeBytes(command.getData());
        }
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodePing(Ping ping) {
        int len;
        short type = ping.getEventType();
        switch (type) {
            case Ping.CLIENT_BUFFER:
                len = 10;
                break;
            case Ping.PONG_SWF_VERIFY:
                len = 44;
                break;
            default:
                len = 6;
        }
        final BufFacade out = BufFacade.buffer(len);
        out.writeShort(type);
        switch (type) {
            case Ping.STREAM_BEGIN:
            case Ping.STREAM_PLAYBUFFER_CLEAR:
            case Ping.STREAM_DRY:
            case Ping.RECORDED_STREAM:
            case Ping.PING_CLIENT:
            case Ping.PONG_SERVER:
            case Ping.BUFFER_EMPTY:
            case Ping.BUFFER_FULL:
                out.writeInt(ping.getValue2().intValue());
                break;
            case Ping.CLIENT_BUFFER:
                if (ping instanceof SetBuffer) {
                    SetBuffer setBuffer = (SetBuffer) ping;
                    out.writeInt(setBuffer.getStreamId());
                    out.writeInt(setBuffer.getBufferLength());
                } else {
                    out.writeInt(ping.getValue2().intValue());
                    out.writeInt(ping.getValue3());
                }
                break;
            case Ping.PING_SWF_VERIFY:
                break;
            case Ping.PONG_SWF_VERIFY:
                out.writeBytes(((SWFResponse) ping).getBytes());
                break;
        }
        // this may not be needed anymore
        if (ping.getValue4() != Ping.UNDEFINED) {
            out.writeInt(ping.getValue4());
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeBytesRead(BytesRead bytesRead) {
        final BufFacade out = BufFacade.buffer(4);
        out.writeInt(bytesRead.getBytesRead());
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeAggregate(Aggregate aggregate) {
        final BufFacade result = aggregate.getData();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeAudioData(AudioData audioData) {
        final BufFacade result = audioData.getData();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeVideoData(VideoData videoData) {
        final BufFacade result = videoData.getData();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public BufFacade encodeUnknown(Unknown unknown) {
        final BufFacade result = unknown.getData();
        return result;
    }

    public BufFacade encodeStreamMetadata(Notify metaData) {
        final BufFacade result = metaData.getData();
        return result;
    }

    /**
     * Generate error object to return for given exception.
     *
     * @param code  call
     * @param error error
     * @return status object
     */
    protected StatusObject generateErrorResult(String code, Throwable error) {
        // Construct error object to return
        String message = "";
        while (error != null && error.getCause() != null) {
            error = error.getCause();
        }
        if (error != null && error.getMessage() != null) {
            message = error.getMessage();
        }
        StatusObject status = new StatusObject(code, "error", message);
        if (error instanceof ClientDetailsException) {
            // Return exception details to client
            status.setApplication(((ClientDetailsException) error).getParameters());
            if (((ClientDetailsException) error).includeStacktrace()) {
                List<String> stack = new ArrayList<String>();
                for (StackTraceElement element : error.getStackTrace()) {
                    stack.add(element.toString());
                }
                status.setAdditional("stacktrace", stack);
            }
        } else if (error != null) {
            status.setApplication(error.getClass().getCanonicalName());
            List<String> stack = new ArrayList<String>();
            for (StackTraceElement element : error.getStackTrace()) {
                stack.add(element.toString());
            }
            status.setAdditional("stacktrace", stack);
        }
        return status;
    }

    /**
     * Encodes Flex message event.
     *
     * @param msg Flex message event
     * @return Encoded data
     */
    public BufFacade encodeFlexMessage(FlexMessage msg) {
        BufFacade out = BufFacade.buffer(1024);
        // Unknown byte, always 0?
        out.writeByte((byte) 0);
        encodeCommand(out, msg);
        return out;
    }

    public BufFacade encodeFlexStreamSend(FlexStreamSend msg) {
        final BufFacade result = msg.getData();
        return result;
    }

    private void updateTolerance() {
        midTolerance = baseTolerance + (long) (baseTolerance * 0.3);
        highestTolerance = baseTolerance + (long) (baseTolerance * 0.6);
    }

    public void setBaseTolerance(long baseTolerance) {
        this.baseTolerance = baseTolerance;
        // update high and low tolerance
        updateTolerance();
    }

    /**
     * Setter for dropLiveFuture
     *
     * @param dropLiveFuture drop live data with future times
     */
    public void setDropLiveFuture(boolean dropLiveFuture) {
        this.dropLiveFuture = dropLiveFuture;
    }

    public void setDropEncoded(boolean dropEncoded) {
        this.dropEncoded = dropEncoded;
    }

    public long getBaseTolerance() {
        return baseTolerance;
    }

}
