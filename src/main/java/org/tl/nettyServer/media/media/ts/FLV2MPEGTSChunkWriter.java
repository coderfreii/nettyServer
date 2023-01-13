package org.tl.nettyServer.media.media.ts;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.event.IEvent;
import org.tl.nettyServer.media.net.hls.ts.codec.MpegtsSegmentEncryptor;
import org.tl.nettyServer.media.net.hls.ts.message.MpegTsSegment;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;

import java.io.IOException;

/**
 * FLV TO Mpeg2TS Chunk Writer  直播的
 *
 * @author pengliren
 */
public class FLV2MPEGTSChunkWriter implements IFLV2MPEGTSWriter {

    private static Logger log = LoggerFactory.getLogger(FLV2MPEGTSChunkWriter.class);
    /* io数据 */
    private BufFacade data;
    /* 是否初始化 */
    private boolean init = false;
    /* flv到mpegts的真正的操作类 */
    private FLV2MPEGTSWriter_ flv2tsWriter;
    /* 是否加密 */
    private boolean isEncrypt = false;
    /* 加密类 */
    private MpegtsSegmentEncryptor encryptor;

    public FLV2MPEGTSChunkWriter(BufFacade videoConfig, BufFacade audioConfig, boolean isEncrypt) {

        flv2tsWriter = new FLV2MPEGTSWriter_(this, videoConfig, audioConfig);
        flv2tsWriter.setLastPCRTimeCode(0);

        this.isEncrypt = isEncrypt;
        if (isEncrypt) {
            encryptor = new MpegtsSegmentEncryptor();
        }
    }

    private void initTsHeader() {
        if (init) return;
        flv2tsWriter.addPAT(0);
        init = true;
    }

    /**
     * next ts packet
     *
     * @throws IOException
     */
    @Override
    public void nextBlock(long ts, byte[] block) {
        byte[] encData;
        if (isEncrypt)
            encData = encryptor.encryptChunk(block, 0, block.length);
        else
            encData = block;
        data.writeBytes(encData);
    }

    /**
     * start write chunk ts
     */
    public void startChunkTS(MpegTsSegment segment) {

        this.data = segment.getBuffer();
        if (isEncrypt) {
            encryptor.init(segment.getEncKeyBytes(), segment.getSequence());
        }

        initTsHeader();
        log.debug("ts chunk start!");
    }


    public void startInitializedChunkTS(MpegTsSegment segment) {
        this.data = segment.getBuffer();
        if (isEncrypt) {
            encryptor.init(segment.getEncKeyBytes(), segment.getSequence());
        }
    }

    public void startInitializedChunkTS(BufFacade data) {
        this.data = data;
    }

    /**
     * 每个分片发一次 pat pmt
     *
     * @param data
     */
    public void startChunkTS(BufFacade data) {
        this.data = data;
        initTsHeader();
        log.debug("ts chunk start!");
    }

    /**
     * end write chunk ts
     */
    public void endChunkTS() {
        if (isEncrypt) {
            byte[] encData = encryptor.encryptFinal();
            data.readBytes(encData);
        }
        init = false;
        log.debug("ts chunk end!");
    }

    /**
     * write stream
     *
     * @param event
     * @throws IOException
     */
    public void writeStreamEvent(IEvent event) throws IOException {
        if (event == null) return;

        if (event instanceof VideoData) {
            flv2tsWriter.handleVideo((VideoData) event);
        } else if (event instanceof AudioData) {
            flv2tsWriter.handleAudio((AudioData) event);
        }
    }
}
