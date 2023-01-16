package org.tl.nettyServer.media.media.flv.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagWriter;
import org.tl.nettyServer.media.media.flv.FLVHeader;
import org.tl.nettyServer.media.media.flv.IFLV;
import org.tl.nettyServer.media.media.processor.IPostProcessor;
import org.tl.nettyServer.media.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

/**
 * A Writer is used to write the contents of a FLV file
 */
public class FLVWriter implements ITagWriter {

    private static Logger log = LoggerFactory.getLogger(FLVWriter.class);

    /**
     * Length of the flv header in bytes
     */
    private final static int HEADER_LENGTH = 9;

    /**
     * Length of the flv tag in bytes
     */
    private final static int TAG_HEADER_LENGTH = 11;

    /**
     * Position of the meta data tag in our file.
     */
    private final static int META_POSITION = 13;

    /**
     * FLV object
     */
    private IFLV flv;

    /**
     * Number of bytes written
     */
    private volatile long bytesWritten;

    /**
     * Position in file
     */
    private int offset;

    /**
     * Size of tag containing onMetaData.
     */
    private int fileMetaSize = 0;

    /**
     * Id of the video codec used.
     */
    private volatile int videoCodecId = -1;

    /**
     * Id of the audio codec used.
     */
    private volatile int audioCodecId = -1;

    /**
     * Are we appending to an existing file?
     */
    private boolean append;

    /**
     * Duration of the file.
     */
    private int duration;

    /**
     * need direct access to file to append full duration metadata
     */
    private FileOutputStream file;

    // size of the previous flv tag
    private volatile int previousTagSize = 0;

    private boolean metaWritten;

    private FileChannel channel;

    /**
     * Creates writer implementation with given file and last tag
     * FLV.java uses this constructor so we have access to the file object
     */
    public FLVWriter(File file, boolean append) {
        try {
            log.debug("Writing to: {}", file.getAbsolutePath());
            this.file = new FileOutputStream(file); //rwd
            this.channel = this.file.getChannel();
            this.append = append;
            init();
        } catch (Exception e) {
            log.error("Failed to create FLV writer", e);
        }
    }

    /**
     * Initialize the writer
     */
    private void init() {
        if (!append) {
            try {
                // write the flv file type header
                writeHeader();
                // write onMetaData tag, it will be replaced when the file is closed
                //writeMetadataTag(0, videoCodecId, audioCodecId);
            } catch (IOException e) {
                log.warn("Exception writing header or intermediate meta data", e);
            }
        }
    }

    /**
     * Writes the header bytes
     *
     * @throws IOException Any I/O exception
     */
    public void writeHeader() throws IOException {
        FLVHeader flvHeader = new FLVHeader();
        flvHeader.setFlagAudio(true);
        flvHeader.setFlagVideo(true);
        // create a buffer
        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader (9 bytes) + PreviousTagSize0 (4 bytes)
        flvHeader.write(header);
        // write header to output channel
        channel.write(header);
        bytesWritten = channel.size();
        header.clear();
    }

    /**
     * {@inheritDoc}
     */
    public IStreamableFile getFile() {
        return flv;
    }

    /**
     * Sets the base file.
     *
     * @param file source flv
     */
    public void setFile(File file) {
        try {
            this.file = new FileOutputStream(file);
            this.channel = this.file.getChannel();
        } catch (FileNotFoundException e) {
            log.warn("File could not be set", e);
        }
    }

    /**
     * Setter for FLV object
     *
     * @param flv FLV source
     */
    public void setFLV(IFLV flv) {
        this.flv = flv;
    }

    /**
     * {@inheritDoc}
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Setter for offset
     *
     * @param offset Value to set for offset
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * {@inheritDoc}
     */
    public long getBytesWritten() {
        return bytesWritten;
    }

    /**
     * {@inheritDoc}
     */
    public boolean writeTag(ITag tag) throws IOException {
        /*
         * Tag header = 11 bytes
         * |-|---|----|---|
         *    0 = type
         *  1-3 = data size
         *  4-7 = timestamp
         * 8-10 = stream id (always 0)
         * Tag data = variable bytes
         * Previous tag = 4 bytes (tag header size + tag data size)
         */
        log.debug("writeTag - previous size: {}", previousTagSize);
        log.trace("Tag: {}", tag);
        if (tag.getDataType() == ITag.TYPE_METADATA) {
            metaWritten = true;
        }
        long prevBytesWritten = bytesWritten;
        // skip tags with no data
        int bodySize = tag.getBodySize();
        log.debug("Tag body size: {}", bodySize);
        if (bodySize > 0) {
            // ensure that the channel is still open
            if (channel != null) {
                // get the data type
                byte dataType = tag.getDataType();
                // set a var holding the entire tag size including the previous tag length
                int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
                // create a buffer for this tag
                ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
                // get the current file offset
                long fileOffset = channel.position();
                log.debug("Current file offset: {} expected offset: {}", fileOffset, prevBytesWritten);
                // if we're writing non-meta tags do seeking and tag size update
                if (dataType != ITag.TYPE_METADATA) {
                    if (fileOffset < prevBytesWritten && dataType != ITag.TYPE_METADATA) {
                        log.debug("Seeking to expected offset");
                        // it's necessary to seek to the length of the file
                        // so that we can append new tags
                        channel.position(prevBytesWritten);
                    }
                    // dont reset previous tag size on metadata
                    if (previousTagSize != tag.getPreviousTagSize()) {
                        log.debug("Previous tag size: {} previous per tag: {}", previousTagSize, tag.getPreviousTagSize());
                        tag.setPreviousTagSize(previousTagSize);
                    }
                }
                int timestamp = tag.getTimestamp() + offset;
                // create an array big enough
                byte[] bodyBuf = new byte[bodySize];
                // put the bytes into the array
                tag.getBody().readBytes(bodyBuf);
                // get the audio or video codec identifier
                if (dataType == ITag.TYPE_AUDIO && audioCodecId == -1) {
                    int id = bodyBuf[0] & 0xff; // must be unsigned
                    audioCodecId = (id & ITag.MASK_SOUND_FORMAT) >> 4;
                    log.debug("Audio codec id: {}", audioCodecId);
                } else if (dataType == ITag.TYPE_VIDEO && videoCodecId == -1) {
                    int id = bodyBuf[0] & 0xff; // must be unsigned
                    videoCodecId = id & ITag.MASK_VIDEO_CODEC;
                    log.debug("Video codec id: {}", videoCodecId);
                }
                // Data Type
                tagBuffer.put(dataType); //1
                // Body Size - Length of the message. Number of bytes after StreamID to end of tag
                // (Equal to length of the tag - 11)
                IOUtils.writeMediumInt(tagBuffer, bodySize); //3
                // Timestamp
                IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
                // Stream id
                IOUtils.writeMediumInt(tagBuffer, 0); //3
                // get the body
                tagBuffer.put(bodyBuf);
                // update previous tag size
                previousTagSize = TAG_HEADER_LENGTH + bodySize;
                // we add the tag size
                tagBuffer.putInt(previousTagSize);
                // flip so we can process from the beginning
                tagBuffer.flip();
                if (log.isDebugEnabled()) {
                    //StringBuilder sb = new StringBuilder();
                    //HexDump.dumpHex(sb, tagBuffer.array());
                    //log.debug("\n{}", sb);
                }
                // write the tag
                channel.write(tagBuffer);
                //writeTagData(tagBuffer, false);
                bytesWritten = channel.size();
                log.debug("Bytes written: {} tag size: {}", bytesWritten, 4);
                tagBuffer.clear();
                // update the duration
                duration = Math.max(duration, timestamp);
                log.debug("Writer duration: {}", duration);
                // validate written amount
                if ((bytesWritten - prevBytesWritten) != (previousTagSize + 4)) {
                    log.debug("Not all of the bytes appear to have been written, prev-current: {}", (bytesWritten - prevBytesWritten));
                }
            } else {
                // throw an exception and let them know the cause
                throw new IOException("FLV write channel has been closed and cannot be written to", new ClosedChannelException());
            }
        } else {
            log.debug("Empty tag skipped: {}", tag);
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean writeTag(byte type, BufFacade data) throws IOException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        try {
            log.debug("close");
        } finally {
            try {
                if (file != null) {
                    // run a test on the flv if debugging is on
                    if (log.isDebugEnabled()) {
                        // debugging
                        //testFLV();
                    }
                    // close the file
                    channel.close();
                    file.close();
                }
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean writeStream(byte[] b) {
        // TODO implement writing byte stream
        return false;
    }


    @Override
    public void addPostProcessor(IPostProcessor postProcessor) {

    }

}
