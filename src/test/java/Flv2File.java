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
import org.tl.nettyServer.media.stream.lisener.RecordingListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Flv2File {

    public static void main(String[] args) throws Exception {
        String fileName = "D:\\tl\\webapps\\oflaDemo\\streams\\5.flv";
        String to = "D:\\tl\\webapps\\oflaDemo\\streams\\5_copy.flv";

        IStreamableFileService service = new FLVService();//new MP3Service();
        ITagReader reader = null;
        IStreamableFile streamFile;
        File file = new File(fileName);
        streamFile = service.getStreamableFile(file);
        reader = streamFile.getReader();


        RecordingListener recordingListener = new RecordingListener();


    }
}
