package org.tl.nettyServer.media.media.flv.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.*;
import org.tl.nettyServer.media.io.amf.Input;
import org.tl.nettyServer.media.io.amf.Output;
import org.tl.nettyServer.media.io.object.Deserializer;
import org.tl.nettyServer.media.media.flv.FLVHeader;
import org.tl.nettyServer.media.media.flv.IKeyFrameDataAnalyzer;
import org.tl.nettyServer.media.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Reader is used to read the contents of a FLV file.
 * NOTE: This class is not implemented as threading-safe. The caller
 * should make sure the threading-safety.
 */
public class FLVReader_ implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

    /**
     * Logger
     */
    private static Logger log = LoggerFactory.getLogger(FLVReader.class);

    /**
     * File
     */
    private File file;

    /**
     * File input stream
     */
    private FileInputStream fis;

    /**
     * File channel
     */
    private FileChannel channel;

    private long channelSize;

    /**
     * Keyframe metadata
     */
    private KeyFrameMeta keyframeMeta;

    /**
     * Input byte buffer
     */
    private BufFacade in;

    /** Set to true to generate metadata automatically before the first tag. */
    private boolean generateMetadata;

    /** Position of first video tag. */
    private long firstVideoTag = -1;

    /** Position of first audio tag. */
    private long firstAudioTag = -1;

    /** metadata sent flag */
    private boolean metadataSent = false;

    /** Duration in milliseconds. */
    private long duration;

    /** Mapping between file position and timestamp in ms. */
    private HashMap<Long, Long> posTimeMap;

    /** Buffer type / style to use **/
    private static BufferType bufferType = BufferType.AUTO;

    private static int bufferSize = 4096;

    /** Use load buffer */
    private boolean useLoadBuf;

    /** Cache for keyframe informations. */
    private static IKeyFrameMetaCache keyframeCache = null;// CachingFileKeyFrameMetaCache.getInstance();

    /** The header of this FLV file. */
    private FLVHeader header;

    /** Constructs a new FLVReader. */
    public FLVReader_() {
    }

    /**
     * Creates FLV reader from file input stream.
     */
    public FLVReader_(File f) throws IOException {
        this(f, false);
    }

    /**
     * Creates FLV reader from file input stream, sets up metadata generation flag.
     */
    public FLVReader_(File f, boolean generateMetadata) throws IOException {
        if (null == f) {
            log.warn("Reader was passed a null file");
            log.debug("{}", org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this));
        }
        this.file = f;
        this.fis = new FileInputStream(f);
        this.generateMetadata = generateMetadata;
        channel = fis.getChannel();
        channelSize = channel.size();
        in = null;
        fillBuffer();
        postInitialize();
    }

    /**
     * Creates FLV reader from file channel.
     */
    public FLVReader_(FileChannel channel) throws IOException {
        if (null == channel) {
            log.warn("Reader was passed a null channel");
            log.debug("{}", org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this));
        }
        if (!channel.isOpen()) {
            log.warn("Reader was passed a closed channel");
            return;
        }
        this.channel = channel;
        channelSize = channel.size();
        log.debug("Channel size: {}", channelSize);
        if (channel.position() > 0) {
            log.debug("Channel position: {}", channel.position());
            channel.position(0);
        }
        fillBuffer();
        postInitialize();
    }

    /**
     * Accepts mapped file bytes to construct internal members.
     */
    public FLVReader_(BufFacade buffer, boolean generateMetadata) {
        this.generateMetadata = generateMetadata;
        in = buffer;
        postInitialize();
    }

    public void setKeyFrameCache(IKeyFrameMetaCache keyframeCache) {
        FLVReader_.keyframeCache = keyframeCache;
    }

    /**
     * Get the remaining bytes that could be read from a file or ByteBuffer.
     */
    protected long getRemainingBytes() {
        if (in != null) {
            if (!useLoadBuf) {
                return in.readableBytes();
            }
            try {
                if (channel.isOpen()) {
                    return channelSize - channel.position() + in.readableBytes();
                } else {
                    return in.readableBytes();
                }
            } catch (Exception e) {
                log.error("Error getRemainingBytes", e);
            }
        }
        return 0;
    }

    /**
     * Get the total readable bytes in a file or ByteBuffer.
     */
    public long getTotalBytes() {
        if (!useLoadBuf) {
            return in.capacity();
        }
        try {
            return channelSize;
        } catch (Exception e) {
            log.error("Error getTotalBytes", e);
            return 0;
        }
    }

    /**
     * Get the current position in a file or ByteBuffer.
     */
    private long getCurrentPosition() {
        long pos;
        if (!useLoadBuf) {
            return in.readerIndex();
        }
        try {
            if (in != null) {
                pos = (channel.position() - in.readableBytes());
            } else {
                pos = channel.position();
            }
            return pos;
        } catch (Exception e) {
            log.error("Error getCurrentPosition", e);
            return 0;
        }
    }

    /**
     * Modifies current position.
     */
    protected void setCurrentPosition(long pos) {
        if (pos == Long.MAX_VALUE) {
            pos = file.length();
        }
        if (!useLoadBuf) {
            in.readerIndex((int) pos);
            return;
        }
        try {
            if (pos >= (channel.position() - in.writerIndex()) && pos < channel.position()) {
                in.readerIndex((int) (pos - (channel.position() - in.writerIndex())));
            } else {
                channel.position(pos);
                fillBuffer(bufferSize, true);
            }
        } catch (Exception e) {
            log.error("Error setCurrentPosition", e);
        }

    }

    /**
     * Loads whole buffer from file channel, with no reloading (that is, appending).
     */
    private void fillBuffer() {
        fillBuffer(bufferSize, false);
    }

    /**
     * Loads data from channel to buffer.
     */
    private void fillBuffer(long amount) {
        fillBuffer(amount, false);
    }

    /**
     * Load enough bytes from channel to buffer.
     * After the loading process, the caller can make sure the amount
     * in buffer is of size 'amount' if we haven't reached the end of channel.
     */
    private void fillBuffer(long amount, boolean reload) {
        try {
            if (amount > bufferSize) {
                amount = bufferSize;
            }
            log.debug("Buffering amount: {} buffer size: {}", amount, bufferSize);
            // Read all remaining bytes if the requested amount reach the end
            // of channel.
            if (channelSize - channel.position() < amount) {
                amount = channelSize - channel.position();
            }
            if (in == null) {
                switch (bufferType) {
                    case HEAP:
                        in = BufFacade.buffer(bufferSize);
                        break;
                    case DIRECT:
                        in = BufFacade.directBuffer(bufferSize);
                        break;
                    default:
                        in = BufFacade.buffer(bufferSize);
                }

                in.writeBytes(channel, bufferSize);
                useLoadBuf = true;
            }
            if (!useLoadBuf) {
                return;
            }
            if (reload || in.readableBytes() < amount) {
                if (!reload) {
                    in.discardReadBytes();
                } else {
                    in.clear();
                }
                in.writeBytes(channel, bufferSize);
                if (in.readableBytes() < amount) {
                    throw new IOException("no more bytes");
                }
            }
        } catch (Exception e) {
            log.error("Error fillBuffer", e);
        }
    }

    /**
     * Post-initialization hook, reads keyframe metadata and decodes header (if any).
     */
    protected void postInitialize() {
        if (log.isDebugEnabled()) {
            log.debug("FLVReader_ 1 - Buffer size: {} position: {} remaining: {}", new Object[] { getTotalBytes(), getCurrentPosition(), getRemainingBytes() });
        }
        if (getRemainingBytes() >= 9) {
            decodeHeader();
        }
        if (file != null) {
            keyframeMeta = analyzeKeyFrames();
        }
        long old = getCurrentPosition();
        log.debug("Position: {}", old);
    }

    public boolean hasVideo() {
        KeyFrameMeta meta = analyzeKeyFrames();
        if (meta == null) {
            return false;
        }
        return (!meta.audioOnly && meta.positions.length > 0);
    }

    /**
     * Getter for buffer type (auto, direct or heap).
     */
    public static String getBufferType() {
        switch (bufferType) {
            case AUTO:
                return "auto";
            case DIRECT:
                return "direct";
            case HEAP:
                return "heap";
            default:
                return null;
        }
    }

    /**
     * Setter for buffer type.
     */
    public static void setBufferType(String bufferType) {
        int bufferTypeHash = bufferType.hashCode();
        switch (bufferTypeHash) {
            case 3198444: //heap
                //Get a heap buffer from buffer pool
                FLVReader_.bufferType = BufferType.HEAP;
                break;
            case -1331586071: //direct
                //Get a direct buffer from buffer pool
                FLVReader_.bufferType = BufferType.DIRECT;
                break;
            case 3005871: //auto
                //Let MINA choose
            default:
                FLVReader_.bufferType = BufferType.AUTO;
        }
    }

    /**
     * Getter for buffer size.
     */
    public static int getBufferSize() {
        return bufferSize;
    }

    /**
     * Setter for property 'bufferSize'.
     */
    public static void setBufferSize(int bufferSize) {
        // make sure buffer size is no less than 1024 bytes.
        if (bufferSize < 1024) {
            bufferSize = 1024;
        }
        FLVReader_.bufferSize = bufferSize;
    }

    /**
     * Returns the file buffer.
     */
    public BufFacade getFileData() {
        // TODO as of now, return null will disable cache
        // we need to redesign the cache architecture so that
        // the cache is layered underneath FLVReader_ not above it,
        // thus both tag cache and file cache are feasible.
        return null;
    }

    public void decodeHeader() {
        // flv header is 9 bytes
        fillBuffer(9);
        header = new FLVHeader();
        // skip signature
        in.skipBytes(3);
        // header.setVersion(in.get());
        header.setTypeFlags(in.readByte());
        header.setDataOffset(in.readInt());
        if (log.isDebugEnabled()) {
            log.debug("Header: {}", header.toString());
        }
    }

    public IStreamableFile getFile() {
        // TODO wondering if we need to have a reference
        return null;
    }

    public int getOffset() {
        // XXX what's the difference from getBytesRead
        return 0;
    }

    public long getBytesRead() {
        // XXX should summarize the total bytes read or
        // just the current position?
        return getCurrentPosition();
    }

    public long getDuration() {
        return duration;
    }

    public int getVideoCodecId() {
        KeyFrameMeta meta = analyzeKeyFrames();
        if (meta == null) {
            return -1;
        }
        long old = getCurrentPosition();
        setCurrentPosition(firstVideoTag);
        readTagHeader();
        fillBuffer(1);
        byte frametype = in.readByte();
        setCurrentPosition(old);
        return frametype & MASK_VIDEO_CODEC;
    }

    public int getAudioCodecId() {
        KeyFrameMeta meta = analyzeKeyFrames();
        if (meta == null) {
            return -1;
        }
        long old = getCurrentPosition();
        setCurrentPosition(firstAudioTag);
        readTagHeader();
        fillBuffer(1);
        byte frametype = in.readByte();
        setCurrentPosition(old);
        return frametype & MASK_SOUND_FORMAT;
    }

    public synchronized boolean hasMoreTags() {
        return getRemainingBytes() > 4;
    }

    /**
     * Create tag for metadata event.
     */
    private ITag createFileMeta() {
        // Create tag for onMetaData event
        BufFacade buf = BufFacade.buffer(192);
        Output out = new Output(buf);
        // Duration property
        out.writeString("onMetaData");
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put("duration", duration / 1000.0);
        if (firstVideoTag != -1) {
            long old = getCurrentPosition();
            setCurrentPosition(firstVideoTag);
            readTagHeader();
            fillBuffer(1);
            byte frametype = in.readByte();
            // Video codec id
            props.put("videocodecid", frametype & MASK_VIDEO_CODEC);
            setCurrentPosition(old);
        }
        if (firstAudioTag != -1) {
            long old = getCurrentPosition();
            setCurrentPosition(firstAudioTag);
            readTagHeader();
            fillBuffer(1);
            byte frametype = in.readByte();
            // Audio codec id
            props.put("audiocodecid", (frametype & MASK_SOUND_FORMAT) >> 4);
            setCurrentPosition(old);
        }
        props.put("canSeekToEnd", true);
        out.writeMap(props);

        ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.readableBytes(), null, 0);
        result.setBody(buf);
        //
        out = null;
        return result;
    }

    public synchronized ITag readTag() {
        long oldPos = getCurrentPosition();
        ITag tag = readTagHeader();
        if (tag != null) {
            boolean isMetaData = tag.getDataType() == TYPE_METADATA;
            log.debug("readTag, oldPos: {}, tag header: \n{}", oldPos, tag);
            if (!metadataSent && !isMetaData && generateMetadata) {
                // Generate initial metadata automatically
                setCurrentPosition(oldPos);
                KeyFrameMeta meta = analyzeKeyFrames();
                if (meta != null) {
                    return createFileMeta();
                }
            }
            int bodySize = tag.getBodySize();
            BufFacade body = BufFacade.buffer(bodySize);
            // XXX Paul: this assists in 'properly' handling damaged FLV files
            long newPosition = getCurrentPosition() + bodySize;
            if (newPosition <= getTotalBytes()) {
                while (getCurrentPosition() < newPosition) {
                    fillBuffer(newPosition - getCurrentPosition());
                    if (getCurrentPosition() + in.readableBytes() > newPosition) {
                        in.readBytes(body, body.writableBytes());
                    } else {
                        in.readBytes(body, in.readableBytes());
                    }
                }
                tag.setBody(body);
            }
            if (isMetaData) {
                metadataSent = true;
            }
        } else {
            log.debug("Tag was null");
        }
        return tag;
    }

    @Override
    public void setMetaSent(boolean metaSent) {

    }

    public synchronized void close() {
        log.debug("Reader close");
        if (in != null) {
            in.release();
            in = null;
        }
        if (channel != null) {
            try {
                channel.close();
                fis.close();
            } catch (IOException e) {
                log.error("FLVReader_ :: close ::>\n", e);
            }
        }
    }

    /**
     * Key frames analysis may be used as a utility method so
     * synchronize it.
     */
    public synchronized KeyFrameMeta analyzeKeyFrames() {
        if (keyframeMeta != null) {
            return keyframeMeta;
        }
        // check for cached keyframe informations
        if (keyframeCache != null) {
            keyframeMeta = keyframeCache.loadKeyFrameMeta(file);
            if (keyframeMeta != null) {
                // Keyframe data loaded, create other mappings
                duration = keyframeMeta.duration;
                posTimeMap = new HashMap<Long, Long>();
                for (int i = 0; i < keyframeMeta.positions.length; i++) {
                    posTimeMap.put(keyframeMeta.positions[i], (long) keyframeMeta.timestamps[i]);
                }
                return keyframeMeta;
            }
        }
        // Lists of video positions and timestamps
        List<Long> positionList = new ArrayList<Long>();
        List<Integer> timestampList = new ArrayList<Integer>();
        // Lists of audio positions and timestamps
        List<Long> audioPositionList = new ArrayList<Long>();
        List<Integer> audioTimestampList = new ArrayList<Integer>();
        long origPos = getCurrentPosition();
        // point to the first tag
        setCurrentPosition(9);
        // number of tags read
        int totalValidTags = 0;
        // start off as audio only
        boolean audioOnly = true;
        while (hasMoreTags()) {
            long pos = getCurrentPosition();
            // Read tag header and duration
            ITag tmpTag = this.readTagHeader();
            if (tmpTag != null) {
                totalValidTags++;
            } else {
                break;
            }
            duration = tmpTag.getTimestamp();
            if (tmpTag.getDataType() == IoConstants.TYPE_VIDEO) {
                if (audioOnly) {
                    audioOnly = false;
                    audioPositionList.clear();
                    audioTimestampList.clear();
                }
                if (firstVideoTag == -1) {
                    firstVideoTag = pos;
                }
                // Grab Frame type
                fillBuffer(1);
                byte frametype = in.readByte();
                if (((frametype & MASK_VIDEO_FRAMETYPE) >> 4) == FLAG_FRAMETYPE_KEYFRAME) {
                    positionList.add(pos);
                    timestampList.add(tmpTag.getTimestamp());
                }
            } else if (tmpTag.getDataType() == IoConstants.TYPE_AUDIO) {
                if (firstAudioTag == -1) {
                    firstAudioTag = pos;
                }
                if (audioOnly) {
                    audioPositionList.add(pos);
                    audioTimestampList.add(tmpTag.getTimestamp());
                }
            }
            // XXX Paul: this 'properly' handles damaged FLV files - as far as
            // duration/size is concerned
            long newPosition = pos + tmpTag.getBodySize() + 15;
            // log.debug("---->" + in.remaining() + " limit=" + in.limit() + "
            // new pos=" + newPosition);
            if (newPosition >= getTotalBytes()) {
                log.error("New position exceeds limit");
                if (log.isDebugEnabled()) {
                    log.debug("-----");
                    log.debug("Keyframe analysis");
                    log.debug(" data type=" + tmpTag.getDataType() + " bodysize=" + tmpTag.getBodySize());
                    log.debug(" remaining=" + getRemainingBytes() + " limit=" + getTotalBytes() + " new pos=" + newPosition);
                    log.debug(" pos=" + pos);
                    log.debug("-----");
                }
                //XXX Paul: A runtime exception is probably not needed here
                log.info("New position {} exceeds limit {}", newPosition, getTotalBytes());
                //just break from the loop
                break;
            } else {
                setCurrentPosition(newPosition);
            }
        }
        // restore the pos
        setCurrentPosition(origPos);

        log.debug("Total valid tags found: {}", totalValidTags);

        keyframeMeta = new KeyFrameMeta();
        keyframeMeta.duration = duration;
        posTimeMap = new HashMap<Long, Long>();
        if (audioOnly) {
            // The flv only contains audio tags, use their lists
            // to support pause and seeking
            positionList = audioPositionList;
            timestampList = audioTimestampList;
        }
        keyframeMeta.audioOnly = audioOnly;
        keyframeMeta.positions = new long[positionList.size()];
        keyframeMeta.timestamps = new int[timestampList.size()];
        for (int i = 0; i < keyframeMeta.positions.length; i++) {
            keyframeMeta.positions[i] = positionList.get(i);
            keyframeMeta.timestamps[i] = timestampList.get(i);
            posTimeMap.put((long) positionList.get(i), (long) timestampList.get(i));
        }
        if (keyframeCache != null) {
            keyframeCache.saveKeyFrameMeta(file, keyframeMeta);
        }
        return keyframeMeta;
    }

    /**
     * Put the current position to pos.
     * The caller must ensure the pos is a valid one
     * (eg. not sit in the middle of a frame).
     */
    public void position(long pos) {
        setCurrentPosition(pos);
    }

    /**
     * Read only header part of a tag.
     */
    private ITag readTagHeader() {
        // previous tag size (4 bytes) + flv tag header size (11 bytes)
        fillBuffer(15);
        //		if (log.isDebugEnabled()) {
        //			in.mark();
        //			StringBuilder sb = new StringBuilder();
        //			HexDump.dumpHex(sb, in.array());
        //			log.debug("\n{}", sb);
        //			in.reset();
        //		}
        // previous tag's size
        int previousTagSize = in.readInt();
        // start of the flv tag
        byte dataType = in.readByte();
        // loop counter
        int i = 0;
        while (dataType != 8 && dataType != 9 && dataType != 18) {
            log.debug("Invalid data type detected, reading ahead");
            log.debug("Current position: {} limit: {}", in.readerIndex(), in.readableBytes());
            // only allow 10 loops
            if (i++ > 10) {
                return null;
            }
            // move ahead and see if we get a valid datatype
            dataType = in.readByte();
        }
        int bodySize = IOUtils.readUnsignedMediumInt(in);
        int timestamp = IOUtils.readExtendedMediumInt(in);
        if (log.isDebugEnabled()) {
            int streamId = IOUtils.readUnsignedMediumInt(in);
            log.debug("Data type: {} timestamp: {} stream id: {} body size: {} previous tag size: {}", new Object[] { dataType, timestamp, streamId, bodySize, previousTagSize });
        } else {
            in.skipBytes(3);
        }
        return new Tag(dataType, timestamp, bodySize, null, previousTagSize);
    }

    public static int getDuration(File flvFile) {
        int duration = 0;
        RandomAccessFile flv = null;
        try {
            flv = new RandomAccessFile(flvFile, "r");
            long flvLength = Math.max(flvFile.length(), flv.length());
            log.debug("File length: {}", flvLength);
            if (flvLength > 13) {
                flv.seek(flvLength - 4);
                int lastTagSize = flv.readInt();
                log.debug("Last tag size: {}", lastTagSize);
                if (lastTagSize > 0 && (lastTagSize < flvLength)) {
                    // jump right to where tag timestamp would be
                    flv.seek(flvLength - lastTagSize);
                    // grab timestamp as a regular int
                    duration = flv.readInt();
                    // adjust value to match extended timestamp
                    duration = (duration >>> 8) | ((duration & 0x000000ff) << 24);
                } else {
                    // attempt to read the metadata
                    flv.seek(13);
                    byte tagType = flv.readByte();
                    if (tagType == ITag.TYPE_METADATA) {
                        ByteBuffer buf = ByteBuffer.allocate(3);
                        flv.getChannel().read(buf);
                        int bodySize = IOUtils.readMediumInt(buf);
                        log.debug("Metadata body size: {}", bodySize);
                        flv.skipBytes(4); // timestamp
                        flv.skipBytes(3); // stream id
                        buf.clear();
                        buf = ByteBuffer.allocate(bodySize);
                        flv.getChannel().read(buf);
                        // construct the meta
                        BufFacade ioBuf = BufFacade.wrappedBuffer(buf);
                        Input input = new Input(ioBuf);
                        String metaType = Deserializer.deserialize(input, String.class);
                        log.debug("Metadata type: {}", metaType);
                        Map<String, ?> meta = Deserializer.deserialize(input, Map.class);
                        Object tmp = meta.get("duration");
                        if (tmp != null) {
                            if (tmp instanceof Double) {
                                duration = ((Double) tmp).intValue();
                            } else {
                                duration = Integer.valueOf((String) tmp);
                            }
                        }
                        input = null;
                        meta.clear();
                        meta = null;
                        ioBuf.clear();
                        ioBuf.release();
                        ioBuf = null;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Exception getting file duration", e);
        } finally {
            try {
                if (flv != null) {
                    flv.close();
                }
            } catch (IOException e) {
            }
            flv = null;
        }
        return duration;
    }

    /*@Override
    public ITagReader copy() {
        return null;
    }*/
}
