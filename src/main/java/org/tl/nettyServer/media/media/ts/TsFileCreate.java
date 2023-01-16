package org.tl.nettyServer.media.media.ts;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.event.IEvent;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.cms.CmsTs;
import org.tl.nettyServer.media.media.flv.FLVUtils;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.service.IStreamableFileService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ts文件异步生成
 */
public class TsFileCreate implements Runnable {

    private static Pattern pattern = Pattern.compile("(\\d+)_(\\d+)_(\\d+)\\.ts$");
    private File createFile;
    private IStreamableFileService service;
    private File file;
    private Lock writeLock;

    public TsFileCreate(IStreamableFileService service, File file, Lock writeLock, File createFile) {
        this.service = service;
        this.file = file;
        this.writeLock = writeLock;
        this.createFile = createFile;
    }

    public File doCreateFile() {
        //枷锁 外面传进来 双重判断枷锁 判断页面是否存在
        if (createFile.exists()) {
            return createFile;
        }
        writeLock.lock();
        if (createFile.exists()) {
            writeLock.unlock();
            return createFile;
        }
        try {
            File parentFile = createFile.getParentFile();

            if (!parentFile.exists()) {
                boolean mkdir = parentFile.mkdir();
            }
            createFile.createNewFile();
            createFile(createFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
        return createFile;
    }

    @Override
    public void run() {
        //枷锁 外面传进来 双重判断枷锁 判断页面是否存在
        if (createFile.exists()) {
            return;
        }
        writeLock.lock();
        if (createFile.exists()) {
            writeLock.unlock();
            return;
        }
        try {
            createFile(createFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }

    }

    private void createFile(File createTs) {


        IStreamableFile streamFile;
        ITagReader reader = null;
        ITag tag;
        try {
            streamFile = service.getStreamableFile(file);
            reader = streamFile.getReader();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Matcher m = pattern.matcher(createFile.getName());
        int start = 0;
        int end = 0;
        if (m.matches()) {
            start = Integer.valueOf(m.group(1));
            end = Integer.valueOf(m.group(2));
        }
        try (FileOutputStream fos = new FileOutputStream(createTs)) {
            fos.write(packageTs(reader, start, end));
            //fos.write(newPackageTs(reader, start,end));
            //ffmpeg(file, createFile, start, end);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
    }

    private byte[] newPackageTs(ITagReader reader, int start, int end) {
        CmsTs ts = new CmsTs();
        reader.position(start);
        BufFacade result = BufFacade.buffer(1024 * 1024);
        while (reader.hasMoreTags()) {
            if (end != -1 && reader.getBytesRead() >= end) break;
            ITag s = reader.readTag();
            if (s == null) continue; // fix tag NPE
            if (s.getDataType() == 0x09) {

            } else if (s.getDataType() == 0x08) {

            } /*else if (s.getDataType() == 0x12) {

                }*/ else {
                continue;
            }


            BufFacade dataBuff = s.getBody().asReadOnly();
            dataBuff.markReaderIndex();
            int dataLen = dataBuff.readableBytes();
            byte[] data = new byte[dataLen];
            dataBuff.readBytes(data);
            dataBuff.resetReaderIndex();
            //byte[] outBuf = new byte[(data.length / 184 + 2) * TS_CHUNK_SIZE + TS_CHUNK_SIZE * 2];;
            int[] outlen = new int[1];
            String[] outType = new String[1];
            long[] outPts = {0};
            byte[] tagHeader = new byte[11];

            byte[] mData = new byte[dataLen + 11];
            //类型
            tagHeader[0] = s.getDataType();
            //数据大小
            tagHeader[1] = (byte) ((s.getBodySize() >>> 16) & 0xFF);
            tagHeader[2] = (byte) ((s.getBodySize() >>> 8 & 0xFF));
            tagHeader[3] = (byte) ((s.getBodySize() & 0xFF));
            //时间戳
            long timestamp = s.getTimestamp();
            tagHeader[4] = (byte) ((timestamp >>> 16 & 0xFF));
            tagHeader[5] = (byte) ((timestamp >>> 8 & 0xFF));
            tagHeader[6] = (byte) ((timestamp & 0xFF));
            tagHeader[7] = (byte) ((timestamp >>> 24 & 0xFF)); //扩展字段
            //流id
            tagHeader[8] = 0;
            tagHeader[9] = 0;
            tagHeader[10] = 0;
            System.arraycopy(tagHeader, 0, mData, 0, 11);
            System.arraycopy(data, 0, mData, 11, dataLen);
            BufFacade outBuf = BufFacade.buffer(4096);
            ts.tag2Ts(true, mData, mData.length, outBuf, outlen, outType, outPts);
            int dataLen1 = outBuf.readableBytes();
            byte[] data1 = new byte[dataLen1];
            outBuf.readBytes(data1);
            result.writeBytes(data1);
            CmsTs.lastTime = timestamp * 90;
        }
        BufFacade dataBuff = result.asReadOnly();
        int dataLen = dataBuff.readableBytes();
        byte[] dataBytes = new byte[dataLen];
        dataBuff.readBytes(dataBytes);
        dataBuff.clear();
        // System.out.println(data.position()+":"+data.limit()+":"+data.capacity()+":"+data.array().length+":"+dataBytes.length);
        return dataBytes;

    }


    private byte[] packageTs(ITagReader reader, int start, int end) throws IOException {
        boolean audioChecked = false;
        boolean videoChecked = false;
        BufFacade videoConfig = null;
        BufFacade audioConfig = null;
        VideoData videoData;
        AudioData audioData;
        ITag tag;
        for (int i = 0; i < 10; i++) {
            if (audioChecked && videoChecked) break;
            tag = reader.readTag();
            if (tag == null) return null;
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
        FLV2MPEGTSChunkWriter writer = new FLV2MPEGTSChunkWriter(videoConfig, audioConfig, false);
        BufFacade data = BufFacade.buffer(1024 * 1024);
        writer.startChunkTS(data);
        reader.position(start);
        List<IEvent> tags = new ArrayList<>();
        while (reader.hasMoreTags()) {
            if (end != -1 && reader.getBytesRead() >= end) break;
            tag = reader.readTag();
            if (tag == null) break; // fix tag NPE
            if (tag.getDataType() == 0x09) {
                videoData = new VideoData(tag.getBody());
                videoData.setTimestamp(tag.getTimestamp());
                tags.add(videoData);
                //writer.writeStreamEvent(videoData);
            } else if (tag.getDataType() == 0x08) {
                audioData = new AudioData(tag.getBody());
                audioData.setTimestamp(tag.getTimestamp());
                tags.add(audioData);
                //writer.writeStreamEvent(audioData);
            }
        }
        //消除获取数据的数据
        tags.stream().forEach(d -> {
            try {
                writer.writeStreamEvent(d);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        writer.endChunkTS();

        BufFacade dataBuff = data;
        int dataLen = dataBuff.readableBytes();
        byte[] dataBytes = new byte[dataLen];
        dataBuff.readBytes(dataBytes);
        dataBuff.clear();
        // System.out.println(data.position()+":"+data.limit()+":"+data.capacity()+":"+data.array().length+":"+dataBytes.length);
        return dataBytes;
    }
}
