package org.tl.nettyServer.media.net.rtmp.codec;

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.amf.AMF;
import org.tl.nettyServer.media.io.amf.Input;
import org.tl.nettyServer.media.io.amf.Output;
import org.tl.nettyServer.media.io.amf3.AMF3;
import org.tl.nettyServer.media.io.amf3.Input3;
import org.tl.nettyServer.media.io.object.DataTypes;
import org.tl.nettyServer.media.io.object.Deserializer;
import org.tl.nettyServer.media.io.object.IInput;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.event.*;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.net.rtmp.message.SharedObjectTypeMapping;
import org.tl.nettyServer.media.service.call.PendingCall;
import org.tl.nettyServer.media.so.FlexSharedObjectMessage;
import org.tl.nettyServer.media.so.ISharedObjectEvent;
import org.tl.nettyServer.media.so.ISharedObjectMessage;
import org.tl.nettyServer.media.so.SharedObjectMessage;

import java.util.*;

import static org.tl.nettyServer.media.net.rtmp.message.Constants.*;

@Slf4j
public class RtmpPacketToMessageDecoder implements IEventDecoder {
    public IRTMPEvent decode(RTMPConnection conn, Packet packet) {
        return decode(conn, packet.getHeader(), packet.getData());
    }

    public IRTMPEvent decode(RTMPConnection conn, Header header, BufFacade in) {
        IRTMPEvent message;
        byte dataType = header.getDataType();
        switch (dataType) {
            case TYPE_AUDIO_DATA:
                message = decodeAudioData(in);
                message.setSourceType(Constants.SOURCE_TYPE_LIVE);
                break;
            case TYPE_VIDEO_DATA:
                message = decodeVideoData(in);
                message.setSourceType(Constants.SOURCE_TYPE_LIVE);
                break;
            case TYPE_AGGREGATE:
                message = decodeAggregate(in);
                break;
            case TYPE_FLEX_SHARED_OBJECT: // represents an SO in an AMF3 container
                message = decodeFlexSharedObject(in);
                break;
            case TYPE_SHARED_OBJECT:
                message = decodeSharedObject(in);
                break;
            case TYPE_FLEX_MESSAGE:
                message = decodeFlexMessage(in);
                break;
            case TYPE_INVOKE:
                message = decodeAction(conn.getEncoding(), in, header);
                break;
            case TYPE_FLEX_STREAM_SEND:
                if (log.isTraceEnabled()) {
                    log.trace("Decoding flex stream send on stream id: {}", header.getStreamId());
                }
                // skip first byte
                in.skipBytes(1);
                // decode stream data; slice from the current position
                message = decodeStreamData(in.slice(), conn.getEncoding());
                break;
            case TYPE_NOTIFY:
                if (log.isTraceEnabled()) {
                    log.trace("Decoding notify on stream id: {}", header.getStreamId());
                }
                if (header.getStreamId().doubleValue() != 0.0d) {
                    message = decodeStreamData(in, conn.getEncoding());
                } else {
                    message = decodeAction(conn.getEncoding(), in, header);
                }
                break;
            case TYPE_PING:
                message = decodePing(in);
                break;
            case TYPE_BYTES_READ:
                message = decodeBytesRead(in);
                break;
            case TYPE_CHUNK_SIZE:
                message = decodeChunkSize(in);
                break;
            case TYPE_SERVER_BANDWIDTH:
                message = decodeServerBW(in);
                break;
            case TYPE_CLIENT_BANDWIDTH:
                message = decodeClientBW(in);
                break;
            case TYPE_ABORT:
                message = decodeAbort(in);
                break;
            default:
                log.warn("Unknown object type: {}", dataType);
                message = decodeUnknown(dataType, in);
                break;
        }
        // add the header to the message
        message.setHeader(header);
        return message;
    }

    public IRTMPEvent decodeAbort(BufFacade in) {
        return new Abort(in.readInt());
    }

    /**
     * Decodes server bandwidth.
     *
     * @param in BufFacade
     * @return RTMP event
     */
    private IRTMPEvent decodeServerBW(BufFacade in) {
        return new ServerBW(in.readInt());
    }

    /**
     * Decodes client bandwidth.
     *
     * @param in Byte buffer
     * @return RTMP event
     */
    private IRTMPEvent decodeClientBW(BufFacade in) {
        return new ClientBW(in.readInt(), in.readByte());
    }

    /**
     * {@inheritDoc}
     */
    public Unknown decodeUnknown(byte dataType, BufFacade in) {
        if (log.isDebugEnabled()) {
            log.debug("decodeUnknown: {}", dataType);
        }
        return new Unknown(dataType, in);
    }

    /**
     * {@inheritDoc}
     */
    public Aggregate decodeAggregate(BufFacade in) {
        return new Aggregate(in);
    }

    /**
     * {@inheritDoc}
     */
    public ChunkSize decodeChunkSize(BufFacade in) {
        int chunkSize = in.readInt();
        log.debug("Decoded chunk size: {}", chunkSize);
        return new ChunkSize(chunkSize);
    }

    /**
     * {@inheritDoc}
     */
    public ISharedObjectMessage decodeFlexSharedObject(BufFacade in) {
        byte encoding = in.readByte();
        Input input;
        if (encoding == 0) {
            input = new Input(in);
        } else if (encoding == 3) {
            input = new Input3(in);
        } else {
            throw new RuntimeException("Unknown SO encoding: " + encoding);
        }
        String name = input.getString();
        // Read version of SO to modify
        int version = in.readInt();
        // Read persistence informations
        boolean persistent = in.readInt() == 2;
        // Skip unknown bytes
        in.skipBytes(4);
        // create our shared object message
        final SharedObjectMessage so = new FlexSharedObjectMessage(null, name, version, persistent);
        doDecodeSharedObject(so, in, input);
        return so;
    }

    /**
     * {@inheritDoc}
     */
    public ISharedObjectMessage decodeSharedObject(BufFacade in) {
        final Input input = new Input(in);
        String name = input.getString();
        // Read version of SO to modify
        int version = in.readInt();
        // Read persistence informations
        boolean persistent = in.readInt() == 2;
        // Skip unknown bytes
        in.skipBytes(4);
        // create our shared object message
        final SharedObjectMessage so = new SharedObjectMessage(null, name, version, persistent);
        doDecodeSharedObject(so, in, input);
        return so;
    }

    /**
     * Perform the actual decoding of the shared object contents.
     *
     * @param so    Shared object message
     * @param in    input buffer
     * @param input Input object to be processed
     */
    protected void doDecodeSharedObject(SharedObjectMessage so, BufFacade in, IInput input) {
        // Parse request body
        Input3 amf3Input = new Input3(in);
        while (in.readableBytes() > 0) {
            final ISharedObjectEvent.Type type = SharedObjectTypeMapping.toType(in.readByte());
            if (type == null) {
                in.skipBytes(in.readableBytes());
                return;
            }
            String key = null;
            Object value = null;
            final int length = in.readInt();
            if (type == ISharedObjectEvent.Type.CLIENT_STATUS) {
                // Status code
                key = input.getString();
                // Status level
                value = input.getString();
            } else if (type == ISharedObjectEvent.Type.CLIENT_UPDATE_DATA) {
                key = null;
                // Map containing new attribute values
                final Map<String, Object> map = new HashMap<String, Object>();
                final int start = in.readerIndex();
                while (in.readerIndex() - start < length) {
                    String tmp = input.getString();
                    map.put(tmp, Deserializer.deserialize(input, Object.class));
                }
                value = map;
            } else if (type != ISharedObjectEvent.Type.SERVER_SEND_MESSAGE && type != ISharedObjectEvent.Type.CLIENT_SEND_MESSAGE) {
                if (length > 0) {
                    key = input.getString();
                    if (length > key.length() + 2) {
                        // determine if the object is encoded with amf3
                        byte objType = in.readByte();
                        in.setIndex(in.readerIndex() - 1, in.writerIndex());
                        IInput propertyInput;
                        if (objType == AMF.TYPE_AMF3_OBJECT && !(input instanceof Input3)) {
                            // The next parameter is encoded using AMF3
                            propertyInput = amf3Input;
                        } else {
                            // The next parameter is encoded using AMF0
                            propertyInput = input;
                        }
                        value = Deserializer.deserialize(propertyInput, Object.class);
                    }
                }
            } else {
                final int start = in.readerIndex();
                // the "send" event seems to encode the handler name as complete AMF string including the string type byte
                key = Deserializer.deserialize(input, String.class);
                // read parameters
                final List<Object> list = new LinkedList<Object>();
                while (in.readerIndex() - start < length) {
                    byte objType = in.readByte();
                    in.setIndex(in.readerIndex() - 1, in.writerIndex());
                    // determine if the object is encoded with amf3
                    IInput propertyInput;
                    if (objType == AMF.TYPE_AMF3_OBJECT && !(input instanceof Input3)) {
                        // The next parameter is encoded using AMF3
                        propertyInput = amf3Input;
                    } else {
                        // The next parameter is encoded using AMF0
                        propertyInput = input;
                    }
                    Object tmp = Deserializer.deserialize(propertyInput, Object.class);
                    list.add(tmp);
                }
                value = list;
            }
            so.addEvent(type, key, value);
        }
    }

    /**
     * Decode the 'action' for a supplied an Invoke.
     *
     * @param encoding AMF encoding
     * @param in       buffer
     * @param header   data header
     * @return notify
     */
    private Invoke decodeAction(IConnection.Encoding encoding, BufFacade in, Header header) {
        // for response, the action string and invokeId is always encoded as AMF0 we use the first byte to decide which encoding to use
        in.markReaderIndex();
        byte tmp = in.readByte();
        in.resetReaderIndex();
        IInput input;
        if (encoding == IConnection.Encoding.AMF3 && tmp == AMF.TYPE_AMF3_OBJECT) {
            input = new Input3(in);
            ((Input3) input).enforceAMF3();
        } else {
            input = new Input(in);
        }
        // get the action
        String action = Deserializer.deserialize(input, String.class);
        if (action == null) {
            throw new RuntimeException("Action was null");
        }
        if (log.isTraceEnabled()) {
            log.trace("Action: {}", action);
        }
        // instance the invoke
        Invoke invoke = new Invoke();
        // set the transaction id
        invoke.setTransactionId(readTransactionId(input));
        // reset and decode parameters
        input.reset();
        // get / set the parameters if there any
        Object[] params = in.readable() ? handleParameters(in, invoke, input) : new Object[0];
        // determine service information
        final int dotIndex = action.lastIndexOf('.');
        String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
        // pull off the prefixes since java doesn't allow this on a method name
        if (serviceName != null && (serviceName.startsWith("@") || serviceName.startsWith("|"))) {
            serviceName = serviceName.substring(1);
        }
        String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());
        // pull off the prefixes since java doesnt allow this on a method name
        if (serviceMethod.startsWith("@") || serviceMethod.startsWith("|")) {
            serviceMethod = serviceMethod.substring(1);
        }
        // create the pending call for invoke
        PendingCall call = new PendingCall(serviceName, serviceMethod, params);
        invoke.setCall(call);
        return invoke;
    }


    private Object[] handleParameters(BufFacade in, Notify notify, IInput input) {
        Object[] params = new Object[]{};
        List<Object> paramList = new ArrayList<>();
        final Object obj = Deserializer.deserialize(input, Object.class);
        if (obj instanceof Map) {
            // Before the actual parameters we sometimes (connect) get a map of parameters, this is usually null, but if set should be
            // passed to the connection object.
            @SuppressWarnings("unchecked") final Map<String, Object> connParams = (Map<String, Object>) obj;
            notify.setConnectionParams(connParams);
        } else if (obj != null) {
            paramList.add(obj);
        }
        while (in.readableBytes() > 0) {
            paramList.add(Deserializer.deserialize(input, Object.class));
        }
        params = paramList.toArray();
        if (log.isDebugEnabled()) {
            log.debug("Num params: {}", paramList.size());
            for (int i = 0; i < params.length; i++) {
                log.debug(" > {}: {}", i, params[i]);
            }
        }
        return params;
    }


    private int readTransactionId(IInput input) {
        Number transactionId = Deserializer.<Number>deserialize(input, Number.class);
        return transactionId == null ? 0 : transactionId.intValue();
    }

    /**
     * Decodes ping event.
     *
     * @param in BufFacade
     * @return Ping event
     */
    public Ping decodePing(BufFacade in) {
        Ping ping = null;
        if (log.isTraceEnabled()) {
            // gets the raw data as hex without changing the data or pointer
//            String hexDump = in.getHexDump();
//            log.trace("Ping dump: {}", hexDump);
        }
        // control type
        short type = in.readShort();
        switch (type) {
            case Ping.CLIENT_BUFFER:
                ping = new SetBuffer(in.readInt(), in.readInt());
                break;
            case Ping.PING_SWF_VERIFY:
                // only contains the type (2 bytes)
                ping = new Ping(type);
                break;
            case Ping.PONG_SWF_VERIFY:
                byte[] bytes = new byte[42];
                in.readBytes(bytes);
                ping = new SWFResponse(bytes);
                break;
            default:
                //STREAM_BEGIN, STREAM_PLAYBUFFER_CLEAR, STREAM_DRY, RECORDED_STREAM
                //PING_CLIENT, PONG_SERVER
                //BUFFER_EMPTY, BUFFER_FULL
                ping = new Ping(type, in.readInt());
                break;
        }
        return ping;
    }

    /**
     * {@inheritDoc}
     */
    public BytesRead decodeBytesRead(BufFacade in) {
        return new BytesRead(in.readInt());
    }

    /**
     * {@inheritDoc}
     */
    public AudioData decodeAudioData(BufFacade in) {
        return new AudioData(in.asReadOnly());
    }

    /**
     * {@inheritDoc}
     */
    public VideoData decodeVideoData(BufFacade in) {
        return new VideoData(in.asReadOnly());
    }

    /**
     * Decodes stream data, to include onMetaData, onCuePoint, and onFI.
     *
     * @param in input buffer
     * @return Notify
     */
    @SuppressWarnings("unchecked")
    public Notify decodeStreamData(BufFacade in, IConnection.Encoding encoding) {
        if (log.isDebugEnabled()) {
            log.debug("decodeStreamData");
        }
        // our result is a notify
        Notify ret = null;
        // check the encoding, if its AMF3 check to see if first byte is set to AMF0
        log.trace("Encoding: {}", encoding);
        // set mark
        in.markReaderIndex();
        // create input using AMF0 to start with
        Input input = new Input(in);
        if (encoding == IConnection.Encoding.AMF3) {
            log.trace("Client indicates its using AMF3");
        }
        //get the first datatype
        byte dataType = input.readDataType();
        log.debug("Data type: {}", dataType);
        if (dataType == DataTypes.CORE_STRING) {
            String action = input.readString();
            if ("@setDataFrame".equals(action)) {
                // get the second datatype
                byte dataType2 = input.readDataType();
                log.debug("Dataframe method type: {}", dataType2);
                String onCueOrOnMeta = input.readString();
                // get the params datatype
                byte object = input.readDataType();
                if (object == DataTypes.CORE_SWITCH) {
                    log.trace("Switching decoding to AMF3");
                    input = new Input3(in);
                    ((Input3) input).enforceAMF3();
                    // re-read data type after switching decode
                    object = input.readDataType();
                }
                log.debug("Dataframe params type: {}", object);
                Map<Object, Object> params = Collections.EMPTY_MAP;
                if (object == DataTypes.CORE_MAP) {
                    // the params are sent as a Mixed-Array. Required to support the RTMP publish provided by ffmpeg
                    params = (Map<Object, Object>) input.readMap();
                } else if (object == DataTypes.CORE_ARRAY) {
                    params = (Map<Object, Object>) input.readArray(Object[].class);
                } else if (object == DataTypes.CORE_STRING) {
                    // decode the string and drop-in as first map entry since we dont know how its encoded
                    String str = input.readString();
                    log.debug("String params: {}", str);
                    params = new HashMap<>();
                    params.put("0", str);
                    //} else if (object == DataTypes.CORE_OBJECT) {
                    //    params = (Map<Object, Object>) input.readObject();
                } else {
                    try {
                        // read the params as a standard object
                        params = (Map<Object, Object>) input.readObject();
                    } catch (Exception e) {
                        log.warn("Dataframe decode error", e);
                        params = Collections.EMPTY_MAP;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Dataframe: {} params: {}", onCueOrOnMeta, params.toString());
                }
                BufFacade buf = BufFacade.buffer(64);
                Output out = new Output(buf);
                out.writeString(onCueOrOnMeta);
                out.writeMap(params);
                // instance a notify with action
                ret = new Notify(buf, onCueOrOnMeta);
            } else {
                byte object = input.readDataType();
                if (object == DataTypes.CORE_SWITCH) {
                    log.trace("Switching decoding to AMF3");
                    input = new Input3(in);
                    ((Input3) input).enforceAMF3();
                    // re-read data type after switching decode
                    object = input.readDataType();
                }
                // onFI
                // the onFI request contains 2 items relative to the publishing client application
                // sd = system date (12-07-2011) st = system time (09:11:33.387)
                log.info("Stream send: {}", action);
                Map<Object, Object> params = Collections.EMPTY_MAP;
                log.debug("Params type: {}", object);
                if (object == DataTypes.CORE_MAP) {
                    params = (Map<Object, Object>) input.readMap();
                    if (log.isDebugEnabled()) {
                        log.debug("Map params: {}", params.toString());
                    }
                } else if (object == DataTypes.CORE_ARRAY) {
                    params = (Map<Object, Object>) input.readArray(Object[].class);
                    if (log.isDebugEnabled()) {
                        log.debug("Array params: {}", params);
                    }
                } else if (object == DataTypes.CORE_STRING) {
                    String str = input.readString();
                    if (log.isDebugEnabled()) {
                        log.debug("String params: {}", str);
                    }
                    params = new HashMap<>();
                    params.put("0", str);
                } else if (object == DataTypes.CORE_OBJECT) {
                    params = (Map<Object, Object>) input.readObject();
                    if (log.isDebugEnabled()) {
                        log.debug("Object params: {}", params);
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("Stream send did not provide a parameter map");
                }
                // need to debug this further
                /*
                BufFacade buf = BufFacade.allocate(64);
                buf.setAutoExpand(true);
                Output out = null;
                if (encoding == Encoding.AMF3) {
                    out = new org.red5.io.amf3.Output(buf);
                } else {
                    out = new Output(buf);
                }
                out.writeString(action);
                out.writeMap(params);
                buf.flip();
                // instance a notify with action
                ret = new Notify(buf, action);
                */
                // go back to the beginning
                in.resetReaderIndex();
                // instance a notify with action
                ret = new Notify(in.asReadOnly(), action);
            }
        } else {
            // go back to the beginning
            in.resetReaderIndex();
            // instance a notify
            ret = new Notify(in.asReadOnly());
        }
        return ret;
    }

    /**
     * Decodes FlexMessage event.
     *
     * @param in BufFacade
     * @return FlexMessage event
     */
    public FlexMessage decodeFlexMessage(BufFacade in) {
        if (log.isDebugEnabled()) {
            log.debug("decodeFlexMessage");
        }
        // TODO: Unknown byte, probably encoding as with Flex SOs?
        byte flexByte = in.readByte();
        log.trace("Flex byte: {}", flexByte);
        // Encoding of message params can be mixed - some params may be in AMF0, others in AMF3,
        // but according to AMF3 spec, we should collect AMF3 references for the whole message body (through all params)
        Input3.RefStorage refStorage = new Input3.RefStorage();
        Input input = new Input(in);
        String action = Deserializer.deserialize(input, String.class);
        FlexMessage msg = new FlexMessage();
        msg.setTransactionId(readTransactionId(input));
        Object[] params = new Object[]{};
        if (in.readable()) {
            ArrayList<Object> paramList = new ArrayList<>();
            final Object obj = Deserializer.deserialize(input, Object.class);
            if (obj != null) {
                paramList.add(obj);
            }
            while (in.readable()) {
                // Check for AMF3 encoding of parameters
                byte objectEncodingType = in.readByte();
                log.debug("Object encoding: {}", objectEncodingType);
                in.setIndex(in.readerIndex() - 1, in.writerIndex());
                switch (objectEncodingType) {
                    case AMF.TYPE_AMF3_OBJECT:
                    case AMF3.TYPE_VECTOR_NUMBER:
                    case AMF3.TYPE_VECTOR_OBJECT:
                        // The next parameter is encoded using AMF3
                        input = new Input3(in, refStorage);
                        // Vectors with number and object have to have AMF3 forced
                        ((Input3) input).enforceAMF3();
                        break;
                    case AMF3.TYPE_VECTOR_INT:
                    case AMF3.TYPE_VECTOR_UINT:
                        // The next parameter is encoded using AMF3
                        input = new Input3(in, refStorage);
                        break;
                    default:
                        // The next parameter is encoded using AMF0
                        input = new Input(in);
                }
                paramList.add(Deserializer.deserialize(input, Object.class));
            }
            params = paramList.toArray();
            if (log.isTraceEnabled()) {
                log.trace("Parameter count: {}", paramList.size());
                for (int i = 0; i < params.length; i++) {
                    log.trace(" > {}: {}", i, params[i]);
                }
            }
        }
        final int dotIndex = action.lastIndexOf('.');
        String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
        String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());
        log.debug("Service name: {} method: {}", serviceName, serviceMethod);
        PendingCall call = new PendingCall(serviceName, serviceMethod, params);
        msg.setCall(call);
        return msg;
    }
}
