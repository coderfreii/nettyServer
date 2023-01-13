package org.tl.nettyServer.media.net.hls.ts.service;


import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.codec.*;
import org.tl.nettyServer.media.conf.ExtConfiguration;
import org.tl.nettyServer.media.net.hls.ts.codec.FLV2MPEGTSChunkWriter;
import org.tl.nettyServer.media.net.hls.ts.message.MpegTsSegment;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.stream.base.IBroadcastStream;
import org.tl.nettyServer.media.stream.codec.IStreamCodecInfo;
import org.tl.nettyServer.media.stream.data.IStreamPacket;
import org.tl.nettyServer.media.stream.lisener.IStreamListener;
import org.tl.nettyServer.media.util.HexDump;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FLV TO Mpeg2ts Segment
 *
 * @author pengliren
 * <p>
 * mpeg2ts 的详细介绍 https://blog.csdn.net/Kayson12345/article/details/81266587
 */
@Slf4j
public class MpegTsSegmentService implements IStreamListener {

    // app -> stream -> SegmentFacade
    private static ConcurrentMap<String, ConcurrentHashMap<String, SegmentFacade>> scopeSegMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, SegmentFacade>>();
    /*切片的时长 5秒钟*/
    private long segmentTimeLimit = ExtConfiguration.HLS_SEGMENT_TIME * 1000;
    /*提前 最大的 切片的长度 */
    private int maxSegmentsPerFacade = ExtConfiguration.HLS_SEGMENT_MAX;

    /* 单例模式  */
    private static final class SingletonHolder {
        private static final MpegTsSegmentService INSTANCE = new MpegTsSegmentService();
    }

    protected MpegTsSegmentService() {
    }

    public static MpegTsSegmentService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 根据应用名和流名称获取切片个数
     *
     * @param scopeName
     * @param streamName
     * @return int
     */
    public int getSegmentCount(String scopeName, String streamName) {
        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scopeName);
        if (segments.containsKey(streamName)) {
            SegmentFacade facade = segments.get(streamName);
            return facade.getSegmentCount();
        } else {
            return 0;
        }
    }

    /**
     * 根据应用名和流名称获取切片
     *
     * @param scopeName
     * @param streamName
     * @return MpegTsSegment
     */
    public MpegTsSegment getSegment(String scopeName, String streamName) {
        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scopeName);
        if (segments.containsKey(streamName)) {
            SegmentFacade facade = segments.get(streamName);
            return facade.getSegment();
        } else {
            return null;
        }
    }

    /**
     * 根据应用名、流名称和索引id获取切片
     *
     * @param scopeName
     * @param streamName
     * @return MpegTsSegment
     */
    public MpegTsSegment getSegment(String scopeName, String streamName, int index) {
        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scopeName);
        if (segments.containsKey(streamName)) {
            SegmentFacade facade = segments.get(streamName);
            return facade.getSegment(index);
        } else {
            return null;
        }
    }

    /**
     * 根据应用名和流名称获取全部切片
     *
     * @param scopeName
     * @param streamName
     * @return List<MpegTsSegment>
     */
    public List<MpegTsSegment> getSegmentList(String scopeName, String streamName) {
        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scopeName);
        if (segments.containsKey(streamName)) {
            SegmentFacade facade = segments.get(streamName);
            return facade.segments;
        } else {
            return null;
        }
    }

    /**
     * 应用是否有效
     *
     * @param scope      作用域
     * @param streamName 流名称
     * @return boolean
     */
    public boolean isAvailable(IScope scope, String streamName) {

        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scope.getName());

        if (segments == null) {
            segments = new ConcurrentHashMap<String, SegmentFacade>();
            scopeSegMap.put(scope.getName(), segments);
        }
        return segments.containsKey(streamName);
    }

    /**
     * 根据应用和流来更新对应的切片
     *
     * @param stream     流
     * @param scope      作用域
     * @param streamName 作用于名称
     * @param event      命令消息体
     */
    public void update(IBroadcastStream stream, IScope scope, String streamName, IRTMPEvent event) {
        String app = scope.getName();
        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(app);
        if (segments == null) {
            segments = new ConcurrentHashMap<String, SegmentFacade>();
            scopeSegMap.put(app, segments);
        }

        SegmentFacade facade = segments.get(streamName);
        if (facade == null) { //http live stream aes 128是否加密在这里处理
            facade = new SegmentFacade(streamName, ExtConfiguration.HLS_ENCRYPT);
            segments.put(streamName, facade);
        }

        try {
            facade.writeEvent(stream, event);
        } catch (IOException e) {
            log.info("write ts exception {}", e.getMessage());
        }
    }

    /**
     * 获取切片密匙key
     *
     * @param scopeName
     * @param streamName
     * @return String
     */
    public String getSegmentEnckey(String scopeName, String streamName) {
        String encKey = null;
        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scopeName);
        if (segments != null) {
            SegmentFacade facade = segments.get(streamName);
            if (facade != null) encKey = facade.encKey;
        }
        return encKey;
    }

    /**
     * 切片是否加密
     *
     * @param scopeName
     * @param streamName
     * @return boolean
     */
    public boolean getSegmentIsEncrypt(String scopeName, String streamName) {
        boolean isEncrypt = false;
        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scopeName);
        if (segments != null) {
            SegmentFacade facade = segments.get(streamName);
            if (facade != null) isEncrypt = facade.isEncrypt;
        }
        return isEncrypt;
    }

    /**
     * 删除切片
     *
     * @param scopeName
     * @param streamName
     */
    public void removeSegment(String scopeName, String streamName) {

        ConcurrentHashMap<String, SegmentFacade> segments = scopeSegMap.get(scopeName);
        if (segments != null) {
            SegmentFacade facade = segments.remove(streamName);
            if (facade != null) facade.close();
        }
    }

    /**
     * 获取流消息
     */
    @Override
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
        if (packet instanceof VideoData || packet instanceof AudioData)
            this.update(stream, stream.getScope(), stream.getPublishedName(), (IRTMPEvent) packet);
    }

    /**
     * streamName对应的切片实体类
     *
     * @author Administrator
     */
    private class SegmentFacade {
        /*全部切片*/
        List<MpegTsSegment> segments = new ArrayList<MpegTsSegment>();

        /* 临时切片，当前流要写入的切片 */
        // segment currently being written to
        MpegTsSegment segment;
        /* 切片索引 */
        // segment index counter
        AtomicInteger counter = new AtomicInteger();
        /* 视频和音频数 */
        // video and audio packet count
        AtomicInteger frameCounter = new AtomicInteger();
        /* 是否加密  */
        boolean isEncrypt = false;
        /* 密匙key */
        String encKey;

        /* flv转mpegts块写入类 */
        FLV2MPEGTSChunkWriter writer;

        String streamName;

        //current segment startTimestamp  video keyframe
        long currentSegmentStartTimestamp = -1L;

        //last eventTimestamp
        long lastEventTimestamp = 0;

        BufFacade videoConfig;
        BufFacade audioConfig;

        SegmentFacade(String streamName, boolean isEncrypt) {
            this.isEncrypt = isEncrypt;
            this.streamName = streamName;
            if (isEncrypt) {
                this.encKey = generateKey();
                log.info("http live stream publish, name : {}, is encrypt, enc key : {}", streamName, encKey);
            } else {
                log.info("http live stream publish : {}", streamName);
            }
        }

        public int getSegmentCount() {
            return segments.size();
        }

        public MpegTsSegment getSegment() {
            return segment;
        }

        public MpegTsSegment getSegment(int index) {

            for (MpegTsSegment segment : segments) {
                if (segment.getSequence() == index)
                    return segment;
            }
            return null;
        }

        public void close() {
            writer = null;
            segments.clear();
            segment = null;
            videoConfig = null;
            audioConfig = null;
            log.info("http live stream unPublish, name : {}", streamName);
        }

        private boolean isVideoKeyframe(IRTMPEvent event) {
            return event instanceof VideoData && ((VideoData) event).getFrameType() == VideoData.FrameType.KEYFRAME;
        }

        private boolean isVideoFrame(IRTMPEvent event) {
            return event instanceof VideoData;
        }

        public void writeEvent(IBroadcastStream stream, IRTMPEvent event) throws IOException {
            // fix wait video and audio config
            if (frameCounter.get() <= 20) {
                frameCounter.incrementAndGet();
                return;
            }

            if (currentSegmentStartTimestamp == -1L)
                currentSegmentStartTimestamp = event.getTimestamp();

            boolean newSegment = false;

            if (event.getTimestamp() > lastEventTimestamp) {
                lastEventTimestamp = event.getTimestamp();
            }

            if (segment == null) {
                if (isVideoKeyframe(event)) {
                    segment = new MpegTsSegment(streamName, counter.incrementAndGet());
                    segment.setEncKey(encKey);
                    currentSegmentStartTimestamp = event.getTimestamp();
                    segment.setSegmentStartTimestamp(currentSegmentStartTimestamp);
                    // flag that we created a new segment
                    newSegment = true;
                } else {
                    // first segment waiting video keyframe
                    return;
                }
            } else {
                long currentSegmentTs = event.getTimestamp() - currentSegmentStartTimestamp;
                if ((currentSegmentTs >= segmentTimeLimit) && isVideoKeyframe(event)) {
                    writer.endChunkTS(); //结束上一个
                    // close active segment
                    segment.close();
                    segments.add(segment);


                    currentSegmentStartTimestamp = event.getTimestamp();
                    segment.setSegmentStartTimestamp(currentSegmentStartTimestamp);
                    // create a segment
                    segment = new MpegTsSegment(streamName, counter.incrementAndGet());
                    segment.setEncKey(encKey);
                    newSegment = true;
                }
            }

            if (newSegment) {
                //移除旧的
                if (segments.size() > maxSegmentsPerFacade) {
                    // get current segments index minux max
                    int rmNum = segments.size() - maxSegmentsPerFacade;
                    MpegTsSegment seg;
                    if (rmNum > 0) {
                        for (int i = 0; i < rmNum; i++) {
                            seg = segments.remove(0);
                            seg.dispose();
                        }
                    }
                }

                // 音视频config信息
                if (videoConfig == null || audioConfig == null) {
                    this.resolveConfig(stream);
                }

                if (writer == null) {
                    writer = new FLV2MPEGTSChunkWriter(videoConfig, audioConfig, isEncrypt);
                }

                writer.startChunkTS(segment);
            }

            writer.writeStreamEvent(event);
        }

        private long generateSegmentKey(long timestamp) {
            return timestamp + 500;
        }

        private void resolveConfig(IBroadcastStream stream) {
            IStreamCodecInfo codecInfo = stream.getCodecInfo();
            IVideoStreamCodec videoCodecInfo = null;

            if (codecInfo != null && codecInfo.hasVideo()) {
                videoCodecInfo = codecInfo.getVideoCodec();
            }

            if (videoCodecInfo != null && videoCodecInfo.getDecoderConfiguration() != null) {
                this.videoConfig = videoCodecInfo.getDecoderConfiguration().asReadOnly();
            }

            IAudioStreamCodec audioCodecInfo = null;
            if (codecInfo != null && codecInfo.hasAudio()) {
                audioCodecInfo = codecInfo.getAudioCodec();
            }
            if (audioCodecInfo != null && audioCodecInfo.getDecoderConfiguration() != null) {
                this.audioConfig = audioCodecInfo.getDecoderConfiguration().asReadOnly();
            }
        }
    }


    public static String generateKey() {

        KeyGenerator keyGenerator;
        String encKey = null;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey skey = keyGenerator.generateKey();
            byte[] raw = skey.getEncoded();
            encKey = HexDump.encodeHexString(raw);
        } catch (NoSuchAlgorithmException e) {
            log.error("generate hls key fail : {}", e.toString());
        }

        return encKey;
    }

    public long getSegmentTimeLimit() {
        return segmentTimeLimit;
    }

    public void setSegmentTimeLimit(long segmentTimeLimit) {
        this.segmentTimeLimit = segmentTimeLimit;
    }

    public int getMaxSegmentsPerFacade() {
        return maxSegmentsPerFacade;
    }

    public void setMaxSegmentsPerFacade(int maxSegmentsPerFacade) {
        this.maxSegmentsPerFacade = maxSegmentsPerFacade;
    }
}
