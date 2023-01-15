package test.java;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.conf.ExtConfiguration;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.flv.FLVUtils;
import org.tl.nettyServer.media.media.flv.IKeyFrameDataAnalyzer;
import org.tl.nettyServer.media.media.flv.impl.FLVService;
import org.tl.nettyServer.media.media.mp4.MP4Service;
import org.tl.nettyServer.media.media.ts.FLV2MPEGTSChunkWriter_;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.service.IStreamableFileService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Flv2MpegtsTest {
    private static Pattern pattern = Pattern.compile("(\\d+)_(\\d+)_(\\d+)\\.ts$");

    public static void main(String[] args) throws Exception {
        String fileName = "D:\\tl\\webapps\\oflaDemo\\streams\\5.mp4";
        String tlp = "D:\\tl\\webapps\\oflaDemo\\hls\\5.mp4\\";

        boolean audioChecked = false;
        boolean videoChecked = false;
        BufFacade videoConfig = null;
        BufFacade audioConfig = null;

        IStreamableFileService service = new MP4Service();//new MP3Service();
        ITagReader reader = null;
        IStreamableFile streamFile;
        File file = new File(fileName);
        // File ts = new File("E:\\demo\\123.ts");

        streamFile = service.getStreamableFile(file);
        reader = streamFile.getReader();
        IKeyFrameDataAnalyzer.KeyFrameMeta keymeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();
        long[] positions = keymeta.positions;
        int[] timestamps = keymeta.timestamps;
        int duration = ExtConfiguration.HLS_SEGMENT_TIME * 1000;
        int nextTime = duration;
        long startPos = positions[1];
        int rest = 0;
        int seqNum = 1;
        double fixDuration = 0;
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] <= positions[1]) {
                continue;
            }

            if (timestamps[i] >= nextTime) {
                fixDuration = timestamps[i] - nextTime;
                fixDuration = (duration + fixDuration) / 1000;
                rest = 0;
                if (i == (positions.length - 1)) {
                    segments.add(String.format("%s_%s_%d.ts", startPos, file.length(), seqNum));
                    seqNum++;
                } else {
                    segments.add(String.format("%s_%s_%d.ts", startPos, positions[i], seqNum));
                    seqNum++;
                }
                startPos = positions[i];
                nextTime = timestamps[i] + duration; // fix next time
            } else {
                rest++;
            }
        }
        // last time < duration
        if (rest > 0) {
            // last time = duration - (nexttime - timestamops(lastone))
            double lastOneDuration = (duration - (nextTime - timestamps[timestamps.length - 1])) / 1000;
            segments.add(String.format("%s_%s_%d.ts", startPos, file.length(), seqNum));
        }
        // System.out.println(segments);
        //重置一下
        reader.close();
        reader = streamFile.getReader();
        ITag tag;
        for (int i = 0; i < 10; i++) {
            if (audioChecked && videoChecked) break;
            tag = reader.readTag();
            if (tag == null) return;
            if (ITag.TYPE_VIDEO == tag.getDataType()) {
                videoChecked = true;
                if (FLVUtils.getVideoCodec(tag.getBody().getByte(0)) == VideoCodec.AVC.getId() && tag.getBody().getByte(1) == 0x00) {
                    //文件首次读取的时候能获取到，其他的再读取缓存也不行
                    videoConfig = tag.getBody();
                }
            } else if (ITag.TYPE_AUDIO == tag.getDataType()) {
                audioChecked = true;
                if ((FLVUtils.getAudioCodec(tag.getBody().getByte(0)) == AudioCodec.AAC.getId() || FLVUtils.getAudioCodec(tag.getBody().getByte(0)) == AudioCodec.MP3.getId()) && tag.getBody().getByte(1) == 0x00) {
                    audioConfig = tag.getBody();
                }
            }
        }


        VideoData videoData;
        AudioData audioData;


        for (String tsIndex : segments) {
            Matcher m = pattern.matcher(tsIndex);
            int start = 0;
            int end = 0;
            if (m.matches()) {
                start = Integer.valueOf(m.group(1)) - 1;
                end = Integer.valueOf(m.group(2)) + 1;
            }
            File ts = new File(tlp + tsIndex);
            FileOutputStream fos = new FileOutputStream(ts);
            FLV2MPEGTSChunkWriter_ writer = new FLV2MPEGTSChunkWriter_(videoConfig, audioConfig, false);
            BufFacade data = BufFacade.buffer(4096);
            writer.startChunkTS(data);

            while (reader.hasMoreTags()) {

                if (end != -1 && reader.getBytesRead() >= end) break;
                tag = reader.readTag();

                if (tag == null) break; // fix tag NPE
                if (tag.getDataType() == 0x09) {
                    videoData = new VideoData(tag.getBody());
                    videoData.setTimestamp(tag.getTimestamp());
                    writer.writeStreamEvent(videoData);
                } else if (tag.getDataType() == 0x08) {
                    audioData = new AudioData(tag.getBody());
                    audioData.setTimestamp(tag.getTimestamp());
                    writer.writeStreamEvent(audioData);
                }
            }
            writer.endChunkTS();
            int dataLen1 = data.readableBytes();
            byte[] data1 = new byte[dataLen1];
            data.readBytes(data1);
            fos.write(data1);
            fos.close();
            System.out.println(start + ":" + end);
        }
        reader.close();
    }
}
