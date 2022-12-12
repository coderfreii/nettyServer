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
 * capacityations under the License.
 */

package org.tl.nettyServer.servers.net.rtmp.codec;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.servers.buf.BufFacade;
import org.tl.nettyServer.servers.net.rtmp.RTMPUtils;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.servers.net.rtmp.event.Abort;
import org.tl.nettyServer.servers.net.rtmp.event.ChunkSize;
import org.tl.nettyServer.servers.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.servers.net.rtmp.message.ChunkHeader;
import org.tl.nettyServer.servers.net.rtmp.message.Constants;
import org.tl.nettyServer.servers.net.rtmp.message.Header;
import org.tl.nettyServer.servers.net.rtmp.message.Packet;
import org.tl.nettyServer.servers.net.rtmp.status.Status;
import org.tl.nettyServer.servers.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.servers.stream.StreamAction;
import org.tl.nettyServer.servers.stream.StreamService;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * RTMP protocol decoder.
 */
public class RTMPProtocolDecoder implements Constants {
    private RtmpMessageDecoder rtmpMessageDecoder = new RtmpMessageDecoder();


    protected static final Logger log = LoggerFactory.getLogger(RTMPProtocolDecoder.class);

    // close when header errors occur
    protected boolean closeOnHeaderError;

    // maximum size for an RTMP packet in Mb
    protected static int MAX_PACKET_SIZE = 3145728; // 3MB

    /**
     * Constructs a new RTMPProtocolDecoder.
     */
    public RTMPProtocolDecoder() {
    }

    /**
     * Decode all available objects in buffer.
     *
     * @param conn   RTMP connection
     * @param buffer BufFacade of data to be decoded
     * @return a list of decoded objects, may be empty if nothing could be decoded
     */
    public List<Object> decodeBuffer(RTMPConnection conn, BufFacade buffer) {
        //buffer当前所在的操作位置
        final int position = buffer.readerIndex();
        if (log.isTraceEnabled()) {
            log.trace("decodeBuffer: {}", Hex.encodeHexString(Arrays.copyOfRange(buffer.array(), position, buffer.capacity())));
        }
        // decoded results
        List<Object> result = null;
        if (conn != null) {
            //log.trace("Decoding for connection - session id: {}", conn.getSessionId());
            try {
                // instance list to hold results
                result = new LinkedList<>();
                // get the local decode state
                RTMPDecodeState state = conn.getDecoderState();
                if (log.isTraceEnabled()) {
                    log.trace("RTMP decode state {}", state);
                }
                if (!conn.getSessionId().equals(state.getSessionId())) {
                    log.warn("Session decode overlap: {} != {}", conn.getSessionId(), state.getSessionId());
                }
                // buffer剩余
                int remaining;
                // buffer可操作空间要大于0
                while ((remaining = buffer.readableBytes()) > 0) {
                    // 通过decoderBufferAmount判断是否可以进行解码
                    if (state.canStartDecoding(remaining)) {
                        //log.trace("Can start decoding");
                        state.startDecoding();
                    } else {
                        log.trace("Cannot start decoding");
                        break;
                    }
                    final Object decodedObject = decode(conn, state, buffer);
                    if (state.hasDecodedObject()) {
                        //log.trace("Has decoded object");
                        if (decodedObject != null) {
                            result.add(decodedObject);
                        }
                    } else if (state.canContinueDecoding()) {
                        //log.trace("Can continue decoding");
                        continue;
                    } else {
                        log.trace("Cannot continue decoding");
                        break;
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to decodeBuffer: pos {}, capacity {}, chunk size {}, buffer {}", position, buffer.capacity(), conn.getState().getReadChunkSize(), Hex.encodeHexString(Arrays.copyOfRange(buffer.array(), position, buffer.capacity())));
                // catch any non-handshake exception in the decoding; close the connection
                log.warn("Closing connection because decoding failed: {}", conn, ex);
                // clear the buffer to eliminate memory leaks when we can't parse protocol
                buffer.clear();
                // close connection because we can't parse data from it
                conn.close();
            } finally {
                //if (log.isTraceEnabled()) {
                //log.trace("decodeBuffer - post decode input buffer position: {} remaining: {}", buffer.position(), buffer.remaining());
                //}
                buffer.slice();
            }
        } else {
            log.error("Decoding buffer failed, no current connection!?");
        }
        return result;
    }

    /**
     * Decodes the buffer data.
     *
     * @param conn  RTMP connection
     * @param state Stores state for the protocol, ProtocolState is just a marker interface
     * @param in    BufFacade of data to be decoded
     * @return one of three possible values:
     *
     * <pre>
     * 1. null : the object could not be decoded, or some data was skipped, just continue
     * 2. ProtocolState : the decoder was unable to decode the whole object, refer to the protocol state
     * 3. Object : something was decoded, continue
     * </pre>
     * @throws ProtocolException on error
     */
    public Object decode(RTMPConnection conn, RTMPDecodeState state, BufFacade in) throws ProtocolException {
        if (log.isTraceEnabled()) {
            log.trace("Decoding for {}", conn.getSessionId());
        }
        try {
            final byte connectionState = conn.getState().getState();
            switch (connectionState) {
                case RTMP.STATE_CONNECTED:
                    return decodePacket(conn, state, in);
                case RTMP.STATE_ERROR:
                case RTMP.STATE_DISCONNECTING:
                case RTMP.STATE_DISCONNECTED:
                    // throw away any remaining input data:
                    in.clear();
                    return null;
                default:
                    throw new IllegalStateException("Invalid RTMP state: " + connectionState);
            }
        } catch (ProtocolException pe) {
            // raise to caller unmodified
            throw pe;
        } catch (RuntimeException e) {
            throw new ProtocolException("Error during decoding", e);
        } finally {
            //if (log.isTraceEnabled()) {
            //log.trace("Decoding finished for {}", conn.getSessionId());
            //}
        }
    }

    /**
     * Decodes an BufFacade into a Packet.
     *
     * @param conn  Connection
     * @param state RTMP protocol state
     * @param in    BufFacade
     * @return Packet
     */
    public Packet decodePacket(RTMPConnection conn, RTMPDecodeState state, BufFacade in) {
        int position = in.readerIndex();
        in.markReaderIndex();
        // get RTMP state holder
        RTMP rtmp = conn.getState();
        // read the chunk header (variable from 1-3 bytes)
        //读取ChunkHeader数据
        final ChunkHeader chunkHeader = ChunkHeader.read(in);
        // represents "packet" header length via "format" only 1 byte in the chunk header is needed here
        //根据header的format获取header的长度

        int messageHeaderLength = RTMPUtils.getHeaderLength(chunkHeader.getFormat());
        if (in.readableBytes() < messageHeaderLength) {
            state.bufferDecoding(messageHeaderLength - in.readableBytes());
            in.resetReaderIndex();
            return null;
        }
        final Header header = decodeHeader(chunkHeader, state, in, rtmp);
        // get the channel id
        final int channelId = header != null ? header.getChannelId() : chunkHeader.getChannelId();
        if (header == null || header.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("Header was null or empty - chh: {}", chunkHeader);
            }
            // clear / slice the input and close the channel
            in.clear();
            in.slice();
            // send a NetStream.Failed message
            StreamService.sendNetStreamStatus(conn, StatusCodes.NS_FAILED, "Bad data on channel: " + channelId, "no-name", Status.ERROR, conn.getStreamIdForChannelId(channelId));
            // close the channel on which the issue occurred, until we find a way to exclude the current data
//            conn.closeChannel(channelId);
            return null;
        }
        // store the header based on its channel id
        rtmp.setLastReadHeader(channelId, header);
        // ensure that we dont exceed maximum packet size
        int size = header.getSize();
        if (size > MAX_PACKET_SIZE) {
            // Reject packets that are too big, to protect against OOM when decoding has failed in some way
            log.warn("Packet size exceeded. size={}, max={}, connId={}", header.getSize(), MAX_PACKET_SIZE, conn.getSessionId());
            // send a NetStream.Failed message
            StreamService.sendNetStreamStatus(conn, StatusCodes.NS_FAILED, "Data exceeded maximum allowed by " + (size - MAX_PACKET_SIZE) + " bytes", "no-name", Status.ERROR, conn.getStreamIdForChannelId(channelId));
            throw new ProtocolException(String.format("Packet size exceeded. size: %s", header.getSize()));
        }
        // get the size of our chunks
        int readChunkSize = rtmp.getReadChunkSize();
        // check to see if this is a new packet or continue decoding an existing one
        Packet packet = rtmp.getLastReadPacket(channelId);
        if (packet == null) {
            // create a new packet
            packet = new Packet(header.clone());
            // store the packet based on its channel id
            rtmp.setLastReadPacket(channelId, packet);
        }
        // get the packet data
        BufFacade data = packet.getData();
        //if (log.isTraceEnabled()) {
        //log.trace("Source buffer position: {}, capacity: {}, packet-buf.position {}, packet size: {}", new Object[] { in.position(), in.capacity(), buf.position(), header.getSize() });
        //}
        // read chunk
        int length = Math.min(data.readableBytes(), readChunkSize);
        if (in.readableBytes() < length) {
            //log.debug("Chunk too small, buffering ({},{})", in.remaining(), length);
            // how much more data we need to continue?
            state.bufferDecoding(in.readerIndex() - position + length);
            // we need to move back position so header will be available during another decode round
            in.resetReaderIndex();
            return null;
        }
        // get the chunk from our input
        byte[] chunk = Arrays.copyOfRange(in.array(), in.readerIndex(), in.readerIndex() + length);
        //if (log.isTraceEnabled()) {
        //log.trace("Read chunkSize: {}, length: {}, chunk: {}", readChunkSize, length, Hex.encodeHexString(chunk));
        //}
        // move the position
        in.skipBytes(length);
        // put the chunk into the packet
        data.writeBytes(chunk);
        if (data.writeable()) {
            //if (log.isTraceEnabled()) {
            //log.trace("Packet is incomplete ({},{})", buf.remaining(), buf.capacity());
            //}
            return null;
        }
        // flip so we can read / decode the packet data into a message
        try {
            // timebase + timedelta
            final int timestamp = header.getTimer();
            // store the last ts in thread local for debugging
            //lastTimestamp.set(header.getTimerBase());
            final IRTMPEvent message = rtmpMessageDecoder.decode(conn, packet.getHeader(), data);
            // flash will send an earlier time stamp when resetting a video stream with a new key frame. To avoid dropping it, we give it the 
            // minimal increment since the last message. To avoid relative time stamps being mis-computed, we don't reset the header we stored.
            message.setTimestamp(timestamp);
            if (log.isTraceEnabled()) {
                log.trace("Decoded message: {}", message);
            }
            packet.setMessage(message);
            if (message instanceof ChunkSize) {
                ChunkSize chunkSizeMsg = (ChunkSize) message;
                rtmp.setReadChunkSize(chunkSizeMsg.getSize());
            } else if (message instanceof Abort) {
                log.debug("Abort packet detected");
                // client is aborting a message, reset the packet because the next chunk will start a new packet
                Abort abort = (Abort) message;
                rtmp.setLastReadPacket(abort.getChannelId(), null);
                packet = null;
            }
            // collapse the time stamps on the last header after decode is complete
            Header lastHeader = rtmp.getLastReadHeader(channelId);
            lastHeader.setTimerBase(timestamp);
            // clear the delta
            //lastHeader.setTimerDelta(0);
            if (log.isTraceEnabled()) {
                log.trace("Last read header after decode: {}", lastHeader);
            }
        } finally {
            rtmp.setLastReadPacket(channelId, null);
        }
        return packet;
    }

    /**
     * Decodes packet header.
     *
     * @param chh   chunk header
     * @param state RTMP decode state
     * @param in    Input BufFacade
     * @param rtmp  RTMP object to get last header
     * @return Decoded header
     */
    public Header decodeHeader(ChunkHeader chh, RTMPDecodeState state, BufFacade in, RTMP rtmp) {
        if (log.isTraceEnabled()) {
            log.trace("decodeHeader - chh: {} input: {}", chh, Hex.encodeHexString(Arrays.copyOfRange(in.array(), in.readerIndex(), in.capacity())));
            log.trace("decodeHeader - chh: {}", chh);
        }
        final int channelId = chh.getChannelId();
        // identifies the header type of the four types
        final byte format = chh.getFormat();
        Header lastHeader = rtmp.getLastReadHeader(channelId);
        if (log.isTraceEnabled()) {
            log.trace("{} lastHeader: {}", Header.HeaderType.values()[format], lastHeader);
        }
        // got a non-new header for a channel which has no last-read header
        if (format != HEADER_NEW && lastHeader == null) {
            String detail = String.format("Last header null: %s, channelId %s", Header.HeaderType.values()[format], channelId);
            log.debug("{}", detail);
            // if the op prefers to exit or kill the connection, we should allow based on configuration param
            if (closeOnHeaderError) {
                // this will trigger an error status, which in turn will disconnect the "offending" flash player
                // preventing a memory leak and bringing the whole server to its knees
                throw new ProtocolException(detail);
            } else {
                // we need to skip the current channel data and continue until a new header is sent
                return null;
            }
        }
        int headerLength = RTMPUtils.getHeaderLength(format);
        if (in.readableBytes() < headerLength) {
            state.bufferDecoding(headerLength);
            return null;
        }

        int timeBase = 0, timeDelta = 0;
        Header header = new Header();
        header.setChannelId(channelId);
        switch (format) {
            case HEADER_NEW: // type 0
                // an absolute time value
                timeBase = RTMPUtils.readUnsignedMediumInt(in);
                header.setSize(RTMPUtils.readUnsignedMediumInt(in));
                header.setDataType(in.readByte());
                header.setStreamId(RTMPUtils.readReverseInt(in));
                // read the extended timestamp if we have the indication that it exists
                if (timeBase >= MEDIUM_INT_MAX) {
                    long ext = in.readUnsignedInt();
                    timeBase = (int) (ext ^ (ext >>> 32));
                    if (log.isTraceEnabled()) {
                        log.trace("Extended time read: {}", timeBase);
                    }
                    header.setExtended(true);
                }
                header.setTimerBase(timeBase);
                header.setTimerDelta(timeDelta);
                break;
            case HEADER_SAME_SOURCE: // type 1
                // time base from last header
                timeBase = lastHeader.getTimerBase();
                // a delta time value
                timeDelta = RTMPUtils.readUnsignedMediumInt(in);
                header.setSize(RTMPUtils.readUnsignedMediumInt(in));
                header.setDataType(in.readByte());
                header.setStreamId(lastHeader.getStreamId());
                // read the extended timestamp if we have the indication that it exists
                if (timeDelta >= MEDIUM_INT_MAX) {
                    long ext = in.readUnsignedInt();
                    timeDelta = (int) (ext ^ (ext >>> 32));
                    header.setExtended(true);
                }
                header.setTimerBase(timeBase);
                header.setTimerDelta(timeDelta);
                break;
            case HEADER_TIMER_CHANGE: // type 2
                // time base from last header
                timeBase = lastHeader.getTimerBase();
                // a delta time value
                timeDelta = RTMPUtils.readUnsignedMediumInt(in);
                header.setSize(lastHeader.getSize());
                header.setDataType(lastHeader.getDataType());
                header.setStreamId(lastHeader.getStreamId());
                // read the extended timestamp if we have the indication that it exists
                if (timeDelta >= MEDIUM_INT_MAX) {
                    long ext = in.readUnsignedInt();
                    timeDelta = (int) (ext ^ (ext >>> 32));
                    header.setExtended(true);
                }
                header.setTimerBase(timeBase);
                header.setTimerDelta(timeDelta);
                break;
            case HEADER_CONTINUE: // type 3
                // time base from last header
                timeBase = lastHeader.getTimerBase();
                timeDelta = lastHeader.getTimerDelta();
                header.setSize(lastHeader.getSize());
                header.setDataType(lastHeader.getDataType());
                header.setStreamId(lastHeader.getStreamId());
                // read the extended timestamp if we have the indication that it exists
                // This field is present in Type 3 chunks when the most recent Type 0, 1, or 2 chunk for the same chunk stream ID
                // indicated the presence of an extended timestamp field
                if (lastHeader.isExtended()) {
                    long ext = in.readUnsignedInt();
                    int timeExt = (int) (ext ^ (ext >>> 32));
                    if (log.isTraceEnabled()) {
                        log.trace("Extended time read: {} {}", ext, timeExt);
                    }
                    timeBase = timeExt;
                    header.setExtended(true);
                }
                header.setTimerBase(timeBase);
                header.setTimerDelta(timeDelta);
                break;
            default:
                throw new ProtocolException(String.format("Unexpected header: %s", format));
        }
        log.trace("Decoded chunk {} {}", Header.HeaderType.values()[format], header);
        return header;
    }


    /**
     * Sets whether or not a header error on any channel should result in a closed connection.
     *
     * @param closeOnHeaderError true to close on header decode errors
     */
    public void setCloseOnHeaderError(boolean closeOnHeaderError) {
        this.closeOnHeaderError = closeOnHeaderError;
    }

    /**
     * Checks if the passed action is a reserved stream method.
     *
     * @param action Action to check
     * @return true if passed action is a reserved stream method, false otherwise
     */
    @SuppressWarnings("unused")
    private boolean isStreamCommand(String action) {
        switch (StreamAction.getEnum(action)) {
            case CREATE_STREAM:
            case DELETE_STREAM:
            case RELEASE_STREAM:
            case PUBLISH:
            case PLAY:
            case PLAY2:
            case SEEK:
            case PAUSE:
            case PAUSE_RAW:
            case CLOSE_STREAM:
            case RECEIVE_VIDEO:
            case RECEIVE_AUDIO:
                return true;
            default:
                log.debug("Stream action {} is not a recognized command", action);
                return false;
        }
    }

    /**
     * Set the maximum allowed packet size. Default is 3 Mb.
     *
     * @param maxPacketSize maximum allowed size for a packet
     */
    public static void setMaxPacketSize(int maxPacketSize) {
        MAX_PACKET_SIZE = maxPacketSize;
        if (log.isDebugEnabled()) {
            log.debug("Max packet size: {}", MAX_PACKET_SIZE);
        }
    }

}
