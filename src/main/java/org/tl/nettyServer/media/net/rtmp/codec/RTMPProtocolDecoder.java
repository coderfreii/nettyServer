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

package org.tl.nettyServer.media.net.rtmp.codec;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.RTMPUtils;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.consts.ConstPack;
import org.tl.nettyServer.media.net.rtmp.consts.FormatMessageType;
import org.tl.nettyServer.media.net.rtmp.event.Abort;
import org.tl.nettyServer.media.net.rtmp.event.ChunkSize;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.net.rtmp.message.ChunkHeader;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.net.rtmp.status.Status;
import org.tl.nettyServer.media.net.rtmp.status.StatusCodes;
import org.tl.nettyServer.media.service.stream.StreamCommandService;
import org.tl.nettyServer.media.stream.StreamAction;
import org.tl.nettyServer.media.util.DebugTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RtmpProtocolState protocol decoder.
 */
public class RTMPProtocolDecoder implements Constants {
    private final RtmpPacketToMessageDecoder rtmpPacketToMessageDecoder = new RtmpPacketToMessageDecoder();


    protected static final Logger log = LoggerFactory.getLogger(RTMPProtocolDecoder.class);

    // close when header errors occur
    protected boolean closeOnHeaderError;

    protected static int MAX_PACKET_SIZE = ConstPack.DEFAULT_MAX_PACKET_SIZE; // 3MB

    private BufFacade bufFacadeStore;

    /**
     * Constructs a new RTMPProtocolDecoder.
     */
    public RTMPProtocolDecoder() {
    }

    /**
     * Decode all available objects in in.
     *
     * @param conn RtmpProtocolState connection
     * @param in   BufFacade of data to be decoded
     * @return a list of decoded objects, may be empty if nothing could be decoded
     */
    public List<Object> decodeBuffer(RTMPConnection conn, BufFacade in) {
        //buffer当前所在的操作位置
        final int position = in.readerIndex();
        if (log.isTraceEnabled()) {
            log.trace("decodeBuffer: {}", Hex.encodeHexString(Arrays.copyOfRange(in.array(), position, in.capacity())));
        }
        // decoded results
        List<Object> result = null;
        if (conn != null) {
            log.trace("Decoding for connection - session id: {}", conn.getSessionId());
            try {
                // instance list to hold results
                result = new ArrayList<>();
                // get the local decode state
                RTMPDecodeState state = conn.getDecoderState();
                if (log.isTraceEnabled()) {
                    log.trace("RtmpProtocolState decode state {}", state);
                }
                if (!conn.getSessionId().equals(state.getSessionId())) {
                    log.warn("Session decode overlap: {} != {}", conn.getSessionId(), state.getSessionId());
                }
                // buffer剩余
                int remaining;
                // buffer可操作空间要大于0
                while ((remaining = in.readableBytes()) > 0) {
                    // 通过decoderBufferAmount判断是否可以进行解码
                    if (state.canStartDecoding(remaining)) {
                        log.trace("Can start decoding");
                        state.startDecoding();
                    } else {
                        log.trace("Cannot start decoding");
                        break;
                    }
                    final Object decodedObject = decode(conn, state, in);
                    if (state.hasDecodedObject()) {
                        log.trace("Has decoded object");
                        if (decodedObject != null) {
                            result.add(decodedObject);
                        }
                    } else if (state.canContinueDecoding()) {
                        log.trace("Can continue decoding");
                        continue;
                    } else {
                        log.trace("Cannot continue decoding");
                        break;
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to decodeBuffer: pos {}, capacity {}, chunk size {}, in {}", position, in.capacity(), conn.getState().getReadChunkSize(), Hex.encodeHexString(Arrays.copyOfRange(in.array(), position, in.capacity())));
                // catch any non-handshake exception in the decoding; close the connection
                log.warn("Closing connection because decoding failed: {}", conn, ex);
                // clear the in to eliminate memory leaks when we can't parse protocol
                in.clear();
                // close connection because we can't parse data from it
                conn.close();
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace("decodeBuffer - post decode input in position: {} remaining: {}", in.readerIndex(), in.readableBytes());
                }
            }
        } else {
            log.error("Decoding in failed, no current connection!?");
        }
        return result;
    }

    /**
     * Decodes the buffer data.
     *
     * @param conn  RtmpProtocolState connection
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
                case RtmpProtocolState.STATE_CONNECTED:
                    return decodePacket(conn, state, in);
                case RtmpProtocolState.STATE_ERROR:
                case RtmpProtocolState.STATE_DISCONNECTING:
                case RtmpProtocolState.STATE_DISCONNECTED:
                    // throw away any remaining input data:
                    in.clear();
                    return null;
                default:
                    throw new IllegalStateException("Invalid RtmpProtocolState state: " + connectionState);
            }
        } catch (ProtocolException pe) {
            // raise to caller unmodified
            throw pe;
        } catch (RuntimeException e) {
            throw new ProtocolException("Error during decoding", e);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace("Decoding finished for {}", conn.getSessionId());
            }
        }
    }

    private DebugTool debugTool = new DebugTool();

    /**
     * Decodes an BufFacade into a Packet.
     *
     * @param conn  Connection
     * @param state RtmpProtocolState protocol state
     * @param in    BufFacade
     * @return Packet
     */
    public Packet decodePacket(RTMPConnection conn, RTMPDecodeState state, BufFacade in) {
        int position = in.readerIndex();
        in.markReaderIndex();
        // get RtmpProtocolState state holder
        RtmpProtocolState rtmpProtocolState = conn.getState();

        // read the chunk header (variable from 1-3 bytes)
        final ChunkHeader chunkHeader = ChunkHeader.read(in);
        // represents "packet" header length via "format" only 1 byte in the chunk header is needed here
        int messageHeaderLength = RTMPUtils.getHeaderLength(chunkHeader.getFormat());
        if (in.readableBytes() < messageHeaderLength) {
            state.bufferDecoding(messageHeaderLength - in.readableBytes());
            in.resetReaderIndex();
            return null;
        }

        final Header latestHeader = decodeHeader(chunkHeader, state, in, rtmpProtocolState);
        if (latestHeader == null || latestHeader.isEmpty()) {
            // get the channel id
            final int channelId = chunkHeader.getCsId();
            if (log.isTraceEnabled()) {
                log.trace("Header was null or empty - chh: {}", chunkHeader);
            }
            // clear / slice the input and close the channel
            in.clear();
            // send a NetStream.Failed message
            StreamCommandService.sendNetStreamStatus(conn, StatusCodes.NS_FAILED, "Bad data on channel: " + channelId, "no-name", Status.ERROR, conn.getStreamIdForChannelId(channelId));
            // close the channel on which the issue occurred, until we find a way to exclude the current data
            //TODO
            conn.closeChannel(channelId);
            return null;
        }
        // ensure that we don't exceed maximum packet size
        int size = latestHeader.getDataSize();
        if (size > MAX_PACKET_SIZE) {
            // Reject packets that are too big, to protect against OOM when decoding has failed in some way
            log.warn("Packet size exceeded. size={}, max={}, connId={}", latestHeader.getDataSize(), MAX_PACKET_SIZE, conn.getSessionId());
            // send a NetStream.Failed message
            StreamCommandService.sendNetStreamStatus(conn, StatusCodes.NS_FAILED, "Data exceeded maximum allowed by " + (size - MAX_PACKET_SIZE) + " bytes", "no-name", Status.ERROR, conn.getStreamIdForChannelId(latestHeader.getCsId()));
            throw new ProtocolException(String.format("Packet size exceeded. size: %s", latestHeader.getDataSize()));
        }

        Packet packet = resolveAPacketToComplete(rtmpProtocolState, latestHeader);
        BufFacade data = packet.getData();
        if (log.isTraceEnabled()) {
            log.trace("Source buffer position: {}, capacity: {}, packet-buf.position {}, packet size: {}", in.readerIndex(), in.capacity(), data.readerIndex(), latestHeader.getDataSize());
        }
        // read chunk
        // check in buf size
        // get the size of our chunks
        int readChunkSize = rtmpProtocolState.getReadChunkSize();
        int length = Math.min(data.writableBytes(), readChunkSize);
        if (in.readableBytes() < length) {
            log.debug("Chunk too small, buffering ({},{})", in.readableBytes(), length);
            // how much more data we need to continue?
            state.bufferDecoding(length + in.readerIndex() - position - 1);
            // we need to move back position so header will be available during another decode round
            in.resetReaderIndex();
            return null;
        }
        // store the header based on its channel id  //这之前 in.readerIndex 可能会重置
        rtmpProtocolState.setLastReadHeader(chunkHeader.getCsId(), latestHeader);

        // get the chunk from our input
        byte[] chunk = Arrays.copyOfRange(in.array(), in.readerIndex(), in.readerIndex() + length);
        if (log.isTraceEnabled()) {
            log.trace("Read chunkSize: {}, length: {}, chunk: {}", readChunkSize, length, Hex.encodeHexString(chunk));
        }
        // move the position
        in.skipBytes(length);
        // put the chunk into the packet
        data.writeBytes(chunk);

        // 检查是否写完（没写完的 message 要大于 chunk size）
        if (data.writeable()) {
            if (log.isTraceEnabled()) {
                log.trace("Packet is incomplete ({},{})", data.readableBytes(), data.capacity());
            }
            packet.setUnCompletedToContinue(true);
            return null;
        }

        decodeMessage(conn, packet);
        postProcessMessage(rtmpProtocolState, packet);
        return packet;
    }

    Packet resolveAPacketToComplete(RtmpProtocolState rtmpProtocolState, Header header) {
        // check to see if this is a new packet or continue decoding an existing one
        Packet packet = rtmpProtocolState.getIncompleteReadPacket(header.getCsId());
        if (packet == null) {
            // create a new packet
            packet = new Packet(header.clone());
            // store the packet based on its channel id
            rtmpProtocolState.setLastReadPacket(header.getCsId(), packet);
        } else {
            if (packet.isUnCompletedToContinue()) {
                packet.updateHeader(header.clone());
                packet.setUnCompletedToContinue(false);
                log.trace("not completed 's next round");
            } else {
                log.trace("'Chunk too small' 's next round");
            }
        }
        return packet;
    }

    void postProcessMessage(RtmpProtocolState rtmpProtocolState, Packet packet) {
        IRTMPEvent message = packet.getMessage();
        if (message instanceof ChunkSize) { //设置读到的
            ChunkSize chunkSizeMsg = (ChunkSize) message;
            rtmpProtocolState.setReadChunkSize(chunkSizeMsg.getSize());
        } else if (message instanceof Abort) {
            log.debug("Abort packet detected");
            // client is aborting a message, reset the packet because the next chunk will start a new packet
            Abort abort = (Abort) message;
            rtmpProtocolState.setLastReadPacket(abort.getChannelId(), null);
        }
    }

    void decodeMessage(RTMPConnection connection, Packet packet) {
        // decode the packet data into a message
        RtmpProtocolState rtmpProtocolState = connection.getState();
        Header header = packet.getHeader();
        try {
            // timebase + time delta
            final int timestamp = header.getTimer();
            // store the last ts in thread local for debugging
            //lastTimestamp.set(header.getTimerBase());
            final IRTMPEvent message = rtmpPacketToMessageDecoder.decode(connection, packet);
            // flash will send an earlier time stamp when resetting a video stream with a new key frame. To avoid dropping it, we give it the
            // minimal increment since the last message. To avoid relative time stamps being mis-computed, we don't reset the header we stored.
            message.setTimestamp(timestamp);
            if (log.isTraceEnabled()) {
                log.trace("Decoded message: {}", message);
            }
            packet.setMessage(message);
            debugTool.addPacket(packet);
            if (log.isTraceEnabled()) {
                log.trace("Latest read header after decode: {}", packet.getHeader());
            }
        } finally {
            //读完清除
            rtmpProtocolState.setLastReadPacket(header.getCsId(), null);
        }
    }

    /**
     * Decodes packet header.
     *
     * @param chh               chunk header
     * @param state             RtmpProtocolState decode state
     * @param in                Input BufFacade
     * @param rtmpProtocolState RtmpProtocolState object to get last header
     * @return Decoded header
     */
    public Header decodeHeader(ChunkHeader chh, RTMPDecodeState state, BufFacade in, RtmpProtocolState rtmpProtocolState) {
        if (log.isTraceEnabled()) {
            log.trace("decodeHeader - chh: {} input: {}", chh, Hex.encodeHexString(Arrays.copyOfRange(in.array(), in.readerIndex(), in.capacity())));
            log.trace("decodeHeader - chh: {}", chh);
        }
        final int csId = chh.getCsId();
        // identifies the header type of the four types
        final byte format = chh.getFormat();
        Header lastHeader = rtmpProtocolState.getLastReadHeader(csId);
        if (log.isTraceEnabled()) {
            log.trace("{} lastHeader: {}", Header.HeaderType.values()[format], lastHeader);
        }
        // got a non-new header for a channel which has no last-read header
        if (format != FormatMessageType.FULL && lastHeader == null) {
            String detail = String.format("Last header null: %s, csId %s", Header.HeaderType.values()[format], csId);
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
        header.setCsId(csId);
        switch (format) {
            case FormatMessageType.FULL: // type 0
                // an absolute time value
                timeBase = RTMPUtils.readUnsignedMediumInt(in);
                header.setDataSize(RTMPUtils.readUnsignedMediumInt(in));
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
            case FormatMessageType.RELATIVE_LARGE: // type 1
                // time base from last header
                timeBase = lastHeader.getTimerBase();
                // a delta time value
                timeDelta = RTMPUtils.readUnsignedMediumInt(in);
                header.setDataSize(RTMPUtils.readUnsignedMediumInt(in));
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
            case FormatMessageType.RELATIVE_TIMESTAMP_ONLY: // type 2
                // time base from last header
                timeBase = lastHeader.getTimerBase();
                // a delta time value
                timeDelta = RTMPUtils.readUnsignedMediumInt(in);
                header.setDataSize(lastHeader.getDataSize());
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
            case FormatMessageType.RELATIVE_SINGLE_BYTE: // type 3
                // time base from last header
                timeBase = lastHeader.getTimerBase();
                timeDelta = lastHeader.getTimerDelta();
                header.setDataSize(lastHeader.getDataSize());
                header.setDataType(lastHeader.getDataType());
                header.setStreamId(lastHeader.getStreamId());
                // read the extended timestamp if we have the indication that it exists
                // This field is present in Type 3 chunks when the most recent Type 0, 1, or 2 chunk for the same chunk stream ID
                // indicated the presence of an extended timestamp field
                if (lastHeader.isExtended()) {   //和文档有出入
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
        header.collapseTimeStamps();
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
