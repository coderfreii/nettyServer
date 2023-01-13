package org.tl.nettyServer.media.media.cms;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.conf.ExtConfiguration;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.flv.IKeyFrameDataAnalyzer;
import org.tl.nettyServer.media.media.mp4.MP4Service;
import org.tl.nettyServer.media.media.mpeg.MpegUtil;
import org.tl.nettyServer.media.service.IStreamableFileService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.tl.nettyServer.media.media.cms.CMux.*;
import static org.tl.nettyServer.media.media.cms.CmsTsChunk.TS_CHUNK_SIZE;
import static org.tl.nettyServer.media.media.cms.CmsTsChunk.cmsMin;


public class CmsTs {

    boolean minfoTag;   //收到scriptTag


    private static Pattern pattern = Pattern.compile("(\\d+)_(\\d+)_(\\d+)\\.ts$");

    public static void main(String[] args) throws IOException {
        IStreamableFileService service = new MP4Service();//new MP3Service();
        ITagReader reader = null;
        IStreamableFile streamFile;
        File file = new File("D:\\demo_file\\3.mp4");
        streamFile = service.getStreamableFile(file);
        reader = streamFile.getReader();
        IKeyFrameDataAnalyzer.KeyFrameMeta keymeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();

        long[] positions = keymeta.positions;
        int[] timestamps = keymeta.timestamps;
        System.out.println(Arrays.toString(timestamps));
        int duration = ExtConfiguration.HLS_SEGMENT_TIME * 1000;
        int nextTime = duration;
        long startPos = 0;//positions[0];
        int rest = 0;
        int seqNum = 1;
        double fixDuration = 0;
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < positions.length; i++) {
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


        CmsTs ts = new CmsTs();
        int counter = 1;
        int fileSize = 0;


        for (String tsIndex : segments) {
        //String tsIndex = segments.get(0);
            Matcher m = pattern.matcher(tsIndex);
            int start = 0;
            int end = 0;
            if (m.matches()) {
                start = Integer.valueOf(m.group(1)) - 1;
                end = Integer.valueOf(m.group(2)) + 1;
            }
            File ts_out_path = new File("D:\\demo_file\\ts\\" + tsIndex);
            FileWriter ts_out = new FileWriter(ts_out_path);
            reader.position(start);
            boolean isFisrt = true;
            while (reader.hasMoreTags()) {
                if (end != -1 && reader.getBytesRead() >= end) break;
                ITag s = reader.readTag();
                int uiTimestamp = s.getTimestamp();
                int mtimeStamp = 0; //读到的上一帧的时间戳，有时时间戳重置了会用到

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

                ts.tag2Ts(isFisrt,mData, mData.length, outBuf, outlen, outType, outPts);

                int dataLen1 = outBuf.readableBytes();
                byte[] data1 = new byte[dataLen1];
                outBuf.readBytes(data1);
                //System.out.println(Arrays.toString(data1));
                //Files.write(data1,ts_out_path);
                ts_out.append(new String(data1), 0, dataLen1);
                fileSize += dataLen1;
                lastTime =  timestamp * 90;
                //System.out.println(fileSize/1024/1024);
                //System.out.println(Arrays.toString(outBuf));
            }
        }
    }
    public static long lastTime = 0;
    public static long pcrBegin = 0;
    public static long pcrEnd = 0;
    public static long getPCRTimecode() {
        long ts = -1L;
        if (lastTime >= 0L)
            ts = lastTime;
        pcrEnd = System.nanoTime();
        System.out.println(pcrEnd - pcrBegin);
        ts += (pcrEnd - pcrBegin) / 1000;
        if (ts < 0L)
            ts = 0L;
        lastTime = ts;
        return ts;
    }
    public static long getSystemPCRTimecode() {
        long temp = System.currentTimeMillis() / 1000;

        long _PCR_base = ((27000000 * temp) / 300) & 0x1ffffffffl;//2^33 //1<<33
        long _PCR_ext = ((27000000 * temp) / 1) % 300l;

        long _PCR = _PCR_base * 300 + _PCR_ext;

        return _PCR;
    }


    //设置PAT的包负载信息
    static void setPAT(char pmtPid, byte[] PATpack, int[] PATlen) {

        MpegUtil.memset(PATpack, 0, (byte) 0, TS_CHUNK_SIZE);
        PATlen[0] = 0;
        byte section_len = 13;
        byte tableId = 0x00;                       //固定为0x00 ，标志是该表是PAT
        byte sectionLength1 = (byte) ((section_len >>> 8) & 0x0f); //表示这个字节后面有用的字节数，包括CRC32
        byte reserved1 = 0x03;                   // 保留位
        byte zero = 0;                                   //0
        byte sectionSyntaxIndicator = 1;        //段语法标志位，固定为1

        byte sectionLength2 = (byte) (section_len & 0xff); //(section_length1 & 0x0F) << 8 | section_length2;

        byte transportStream_id1 = 0x00;
        byte transportStream_id2 = 0x01; //该传输流的ID，区别于一个网络中其它多路复用的流

        byte currentNextIndicator = 0x01; //发送的PAT是当前有效还是下一个PAT有效
        byte versionNumber = 0x00;        //范围0-31，表示PAT的版本号
        byte reserved2 = 0x03;            // 保留位

        byte sectionNumber = 0x00;     //分段的号码。第一段为00，以后每个分段加1，最多可能有256个分段
        byte lastSectionNumber = 0x00; //最后一个分段的号码

        byte prograNumber1 = 0x00; //节目号
        byte programNumber2 = 0x01;

        byte programMapPID1 = (byte) ((pmtPid >>> 8) & 0x1f); //节目映射表的PID 节目号为0时对应的PID为network_PID
        byte reserved3 = 0x07;                       // 保留位

        byte programMapPID2 = (byte) (pmtPid & 0xff); // program_map_PID1 << 8 | program_map_PID2

        PATpack[0] = tableId;
        PATpack[1] = (byte) (sectionSyntaxIndicator << 7 | zero << 6 | reserved1 << 4 | sectionLength1);
        PATpack[2] = sectionLength2;
        PATpack[3] = transportStream_id1;
        PATpack[4] = transportStream_id2;
        PATpack[5] = (byte) (reserved2 << 6 | versionNumber << 1 | currentNextIndicator);
        PATpack[6] = sectionNumber;
        PATpack[7] = lastSectionNumber;
        PATpack[8] = prograNumber1;
        PATpack[9] = programNumber2;
        PATpack[10] = (byte) (reserved3 << 5 | programMapPID1);
        PATpack[11] = programMapPID2;

        int crc32 = AuxCrc32.chksum_crc32(PATpack, 0, 12);
        PATpack[12] = (byte) (crc32 >>> 24);
        PATpack[13] = (byte) (crc32 >>> 16);
        PATpack[14] = (byte) (crc32 >>> 8);
        PATpack[15] = (byte) (crc32);
        PATlen[0] = 16;
        return;
	/*
		//PAT:
		tableId                byte	//8
		SectionSyntaxIndicator byte	//1 通常设为“1”
		"0"                    byte //1
		Reserved               byte //2
		SectionLength          byte	//12
		transportStreamId      byte	//16
		Reserved               byte //2
		VersionNumber          byte	//5
		CurrentNextIndicator   byte	//1
		SectionNumber          byte	//8
		lastSectionNumber      byte	//8

		for(i=0;i<N;i++){
			programNumber          	//16
			Reserved                // 3
			if(programNumber == 0){
				networkPID         	//13
			}
			else{
				programMapPID     	//13
			}
		}
		CRCW32             			//32

	*/

    }

    //设置PMT的包负载信息
    public static void setPMT(char aPid, char vPid, char pcrPid, byte AstreamType, byte[] PMTpack, int[] PMTlen) {

        //MpegUtil.memset(outBuf, 0, (byte) 0, TS_CHUNK_SIZE);

        boolean VFlag = true;
        boolean AFlag = true;
        //PMT:
        PMTlen[0] = 0;
        int sectionLen = 16 - 3;

        byte tableId = 0x02;                              //0固定为0x02, 表示PMT表
        byte sectionLength1 = (byte) ((sectionLen >>> 8) & 0x0f); //1首先两位bit置为00，它指示段的byte数，由该域开始，包含CRC。
        byte reserved1 = 0x03;                            //1 0x03
        byte zero = 0x00;                                 //1 0x01
        byte sectionSyntaxIndicator = 0x01;               //1 固定为0x01

        byte sectionLength2 = (byte) (sectionLen & 0xff); //2 (section_length1 & 0x0F) << 8 | section_length2;

        byte programNumber1 = 0x00; //3 指出该节目对应于可应用的Program map PID
        byte programNumber2 = 0x01;

        byte currentNextIndicator = 0x01; //5 当该位置1时，当前传送的Program map section可用；
        byte versionNumber = 0x00;        //5 指出TS流中Program map section的版本号
        byte reserved2 = 0x03;            //5 0x03

        byte sectionNumber = 0x00; //6 固定为0x00

        byte lastSectionNumber = 0x00; //7 固定为0x00

        byte pcrPID1 = (byte) ((PCRpid >>> 8) & 0x1f); //8 指明TS包的PID值，该TS包含有PCR域，
        byte reserved3 = 0x07;                 //8 0x07

        byte pcrPID2 = (byte) (PCRpid & 0xff); //9 PCR_PID1 << 8 | PCR_PID2
        //该PCR值对应于由节目号指定的对应节目。
        //如果对于私有数据流的节目定义与PCR无关，这个域的值将为0x1FFF。
        byte programInfoLength1 = 0x00; //10 前两位bit为00。该域指出跟随其后对节目信息的描述的byte
        byte reserved4 = 0x0f;          //10 预留为0x0F

        byte programInfoLength2 = 0x00; //11 program_info_length1 <<8 | program_info_length2

        int Pos = 12;
        if (VFlag) {
            //视频信息
            byte videoStreamType = 0x1b; //17指示特定PID的节目元素包的类型。该处PID由elementary PID指定

            byte videoElementaryPID1 = (byte) ((vPid >>> 8) & 0x1f); //18该域指示TS包的PID值。这些TS包含有相关的节目元素
            byte videoReserved1 = 0x07;

            byte videoElementaryPID2 = (byte) (vPid & 0xff); //19 m_elementary_PID1 <<8 | m_elementary_PID2

            byte videoESnfoLength1 = 0x00; //20前两位bit为00。该域指示跟随其后的描述相关节目元素的byte数
            byte videoReserved2 = 0x0f;

            byte videoESnfoLength2 = 0x00; //21 m_ES_nfo_length1 <<8 | m_ES_nfo_length2*/

            PMTpack[Pos] = videoStreamType;
            PMTpack[Pos + 1] = (byte) (videoReserved1 << 5 | videoElementaryPID1);
            PMTpack[Pos + 2] = videoElementaryPID2;
            PMTpack[Pos + 3] = (byte) (videoReserved2 << 4 | videoESnfoLength1);
            PMTpack[Pos + 4] = videoESnfoLength2;
            Pos += 5;
            sectionLen += 5;
        }
        if (AFlag) {
            //音频信息
            byte audioStreamType = AstreamType; //12指示特定PID的节目元素包的类型。该处PID由elementary PID指定

            byte audioElementaryPID1 = (byte) ((aPid >>> 8) & 0x1f); //13该域指示TS包的PID值。这些TS包含有相关的节目元素
            byte audioReserved1 = 0x07;

            byte audioElementaryPID2 = (byte) (aPid & 0xff); //14 m_elementary_PID1 <<8 | m_elementary_PID2

            byte audioESnfoLength1 = 0x00; //15前两位bit为00。该域指示跟随其后的描述相关节目元素的byte数
            byte audioReserved2 = 0x0f;

            byte audioESnfoLength2 = 0x00; //16 m_ES_nfo_length1 <<8 | m_ES_nfo_length2

            PMTpack[Pos] = audioStreamType;
            PMTpack[Pos + 1] = (byte) (audioReserved1 << 5 | audioElementaryPID1);
            PMTpack[Pos + 2] = audioElementaryPID2;
            PMTpack[Pos + 3] = (byte) (audioReserved2 << 4 | audioESnfoLength1);
            PMTpack[Pos + 4] = audioESnfoLength2;
            Pos += 5;
            sectionLen += 5;
            //audio_descriptor;  //丢弃
        }

        sectionLength2 = (byte) (sectionLen & 0xff);
        sectionLength1 = (byte) ((sectionLen >>> 8) & 0x0f);

        PMTpack[0] = tableId;
        PMTpack[1] = (byte) (sectionSyntaxIndicator << 7 | zero << 6 | reserved1 << 4 | sectionLength1);
        PMTpack[2] = sectionLength2;
        PMTpack[3] = programNumber1;
        PMTpack[4] = programNumber2;
        PMTpack[5] = (byte) (reserved2 << 6 | versionNumber << 1 | currentNextIndicator);
        PMTpack[6] = sectionNumber;
        PMTpack[7] = lastSectionNumber;
        PMTpack[8] = (byte) (reserved3 << 5 | pcrPID1);
        PMTpack[9] = pcrPID2;
        PMTpack[10] = (byte) (reserved4 << 4 | programInfoLength1);
        PMTpack[11] = programInfoLength2;

        //int crc32 = TransportStreamUtils.doCRC32(-1, PMTpack,  5, 18 - 1);

        int crc32 = AuxCrc32.chksum_crc32(PMTpack, 0, Pos);
        PMTpack[Pos] = (byte) (crc32 >>> 24);
        PMTpack[Pos + 1] = (byte) (crc32 >>> 16);
        PMTpack[Pos + 2] = (byte) (crc32 >>> 8);
        PMTpack[Pos + 3] = (byte) (crc32);

        PMTlen[0] = sectionLen + 3;

        return;
	/*
		table_id                //8
		Section_syntax_indicator//1 通常设为“1”
		"0"                     //1
		Reserved                //2
		Section_length          //12
		program_number          //16
		Reserved                //2
		Version_number          //5
		Current_next_indicator  //1
		Section_number          //8
		last_section_number     //8
		reserved                //3
		PCR_PID                 //13
		reserved                //4
		program_info_length     //12 头两位为"00"
		for(i=0;i<N;i++){
			descriptor()
		}
		for(i=0;i<N1;i++){
			stream_type         //8
			reserved            //3
			elementary_PID      //13
			reserved            //4
		   ES_info_length       //12 头两位为"00"
			for(j=0;j<N2;j++){
				descriptor();
			}
		}
		CRC_32                  //32 rpchof
	*/
    }

    public static void setPcr(long pcr1, byte[] outBuf, int[] outLen) {
        long pcr = getPCRTimecode();
        long pcrExt = pcr >>> 33;
        outBuf[0] = (byte) (pcr >>> 25 & 0xff);
        outBuf[1] = (byte) (pcr >>> 17 & 0xff);
        outBuf[2] = (byte) (pcr >>> 9 & 0xff);
        outBuf[3] = (byte) (pcr >>> 1 & 0xff);

        byte PCRbase2 = (byte) (pcr & 0x01);
        byte PCRExt1 = (byte) (pcrExt >>> 8 & 0x01);
        byte PCRExt2 = (byte) (pcrExt & 0xff);

        outBuf[4] = (byte) ((PCRbase2 << 7 | 0 | PCRExt1) & 0xff);
        outBuf[5] = PCRExt2;

       /* outBuf[0] = (byte) (pcr >>> 25);
        outBuf[1] = (byte) (pcr >>> 17);
        outBuf[2] = (byte) (pcr >>> 9);
        outBuf[3] = (byte) (pcr >>> 1);
        outBuf[4] = (byte) (pcr << 7 | 0x7e);
        outBuf[5] =  0;*/

        /*long pcr = getPCRTimecode();
        pcr <<= 7;
        byte[] pcrData = BufferUtil.longToByteArray(pcr);
        outBuf[4] = (byte) ((pcrData[7] & 0x80) + 126);
        outBuf[3] = (byte) (pcrData[6] & 0xFF);
        outBuf[2] = (byte) (pcrData[5] & 0xFF);
        outBuf[1] = (byte) (pcrData[4] & 0xFF);
        outBuf[0] = (byte) (pcrData[3] & 0xFF);*/

       /* outBuf[5] = (byte) (pcr >>> 40);
        outBuf[4] = (byte) (pcr >>> 32);
        outBuf[3] = (byte) (pcr >>> 24);
        outBuf[2] = (byte) (pcr >>> 16);
        outBuf[1] = (byte) (pcr >>> 8);
        outBuf[0] = (byte) (pcr & 0xFF);*/
        //outBuf = mPCR;
        outLen[0] = outBuf.length;
        return;
    }

    //送入完整的Tag并且解出数据再打包成TS
    public int tag2Ts(boolean isFisrt,byte[] inBuf, int inLen, BufFacade TsStream, int[] StreamLen, String[] framType, long[] outPts) {
        pcrBegin = System.nanoTime();
        byte[] PESbuf = new byte[inLen + 1024];
        int[] PESLen = {1};

        BufFacade TSbuf = null;
        int[] TSlen = {-1};
        //TsStream = new byte[(inLen / 184 + 2) * TS_CHUNK_SIZE + TS_CHUNK_SIZE * 2];

        outPts[0] = 0;
        framType[0] = "A";

        //curTag.head
        mcurTag.head.tagType = inBuf[0];
        mcurTag.head.dataSize = (inBuf[1] << 16 | inBuf[2]  << 8 | inBuf[3]);
        mcurTag.head.timeStamp = (inBuf[4] << 16 | inBuf[5] << 8 | (inBuf[6] & 0xFF) | inBuf[7] << 24) ; // |2|3|4|1|

        if (mcurTag.head.timeStamp > 47721858) { //0xFFFFFFFF ÷ 90
            mcurTag.head.timeStamp = mcurTag.head.timeStamp - 47721858;
        }

        mcurTag.head.streamId = (inBuf[8] << 16 | inBuf[9] << 8 | inBuf[10]);
        if ((inBuf[0] & 0xFF) == 0x09 && (inBuf[11] & 0x0f) != 7) {
            System.out.println("ERROR[TagToTs] Not H264 code!!!!");
        }
        // tagHeader 11bytes,剩下的开始是数据
        if ((mcurTag.head.tagType & 0xFF) == 0x08) {
            //音频Tag
            mcurTag.flag = 'a';
            mcurTag.audio = dealATag(inBuf, 11, 1);
            mcurTag.head.deviation = 0;
            framType[0] = "A";
            PESbuf = new byte[inLen + 1024];
            packPES(inBuf, 11, inLen - 11, framType[0], mcurTag.head.timeStamp, PESbuf, PESLen);
            if (PESLen[0] > 0) {
                int max = (PESLen[0] / 184 + 1) * TS_CHUNK_SIZE + TS_CHUNK_SIZE * 2;
                TSbuf = BufFacade.buffer(max);//new byte[max] ;//(sizeof(byte)*(max));

                packTS(PESbuf, PESLen,
                        framType[0], (byte) 0x01,
                        (byte) 0x01, mamcc,
                        Apid, mcurTag.head.timeStamp,
                        TSbuf, TSlen);
                //MpegUtil.memcpy(TsStream ,StreamLen[0], TSbuf, TSlen[0]);
                TsStream.writeBytes(TSbuf);
                StreamLen[0] += TSlen[0];
            } else {
                System.out.println("ts error packPES file %s line %d" + "音频Tag");
            }
        } else if ((mcurTag.head.tagType & 0xFF) == 0x09) { //视频Tag
            //视频 Tag Data
            mcurTag.flag = 'V';
            //开始的第一个字节包含视频数据的参数信息
            mcurTag.video = dealVTag(inBuf, 11);
            /**
             * 从第二个字节开始为视频流数据。结构如下
             * */
            //视频的格式（CodecID）是 AVC（H.264）的话，
            // VideoTagHeader 会多出 4 个字节的信息，AVCPacketType = inBuf[12] 和
            // CompositionTime
            mcurTag.head.deviation = (inBuf[13]) << 16 | (inBuf[14]) << 8 | (inBuf[15]);//偏移量cts
            framType[0] = "P";
            if (mcurTag.flag == 'V' && (mcurTag.video.framType & 0xFF) == 1 && (mcurTag.video.codeId & 0xFF) == 7) {

                //关键帧
                framType[0] = "I";
                PESbuf = new byte[inLen + 1024];
                packPES(inBuf, 11, inLen - 11, framType[0], mcurTag.head.timeStamp, PESbuf, PESLen);
                if (PESLen[0] > 0) {
                    if(isFisrt)packPSI();
                    //CallBack(PAT[:], TS_CHUNK_SIZE)
                    //CallBack(PMT[:], TS_CHUNK_SIZE)

                    int max = (PESLen[0] / 184 + 1) * TS_CHUNK_SIZE + TS_CHUNK_SIZE * 2;
                    TSbuf = BufFacade.buffer(max);//new byte[max] ;//(sizeof(byte)*(max));
                    //new byte[max] ;//(sizeof(byte)*(max));

                    packTS(PESbuf, PESLen, framType[0],
                            (byte) 0x01, (byte) 0x01,
                            mvmcc, Vpid,
                            mcurTag.head.timeStamp,
                            TSbuf, TSlen);
                    //MpegUtil.memcpy(TsStream , StreamLen[0], mPAT, mPAT.length);
                    TsStream.writeBytes(mPAT);
                    StreamLen[0] += TS_CHUNK_SIZE;
                    //MpegUtil.memcpy(TsStream , StreamLen[0], mPMT, mPMT.length);
                    TsStream.writeBytes(mPMT);
                    StreamLen[0] += TS_CHUNK_SIZE;
                    // MpegUtil.memcpy(TsStream , StreamLen[0], TSbuf, TSlen[0]);
                    TsStream.writeBytes(TSbuf);
                    StreamLen[0] += TSlen[0];
                    //CallBack(TSbuf, TSlen) //送入回调函数

                } else {
                    System.out.println("ts error packPES file %s line %d" + "视频");
                }
                isFisrt = false;
            } else {
                PESbuf = new byte[inLen + 1024];
                packPES(inBuf, 11, inLen - 11, framType[0], mcurTag.head.timeStamp, PESbuf, PESLen);
                if (PESLen[0] > 0) {
                    TSbuf = BufFacade.buffer((inLen / 184 + 1) * TS_CHUNK_SIZE + TS_CHUNK_SIZE * 2);
                    //new byte[(inLen / 184 + 1) * TS_CHUNK_SIZE + TS_CHUNK_SIZE * 2];
                    packTS(PESbuf, PESLen, framType[0], (byte) 0x01, (byte) 0x01, mvmcc, Vpid, mcurTag.head.timeStamp, TSbuf, TSlen);
                    //MpegUtil.memcpy(TsStream, StreamLen[0], TSbuf, TSlen[0]);
                    TsStream.writeBytes(TSbuf);
                    StreamLen[0] += TSlen[0];
                    //CallBack(TSbuf, TSlen) //送入回调函数
                } else {
                    System.out.println("ts error packPES file %s line %d" + "其他");
                }
            }

        } else if ((mcurTag.head.tagType & 0xFF) == 0x12) { //scriptTag
            mscript = dealSTag(inBuf, 11, mcurTag.head.dataSize);
            //packPSI()
            int[] sdtLen = {TS_CHUNK_SIZE};
            byte[] SDT = new byte[TS_CHUNK_SIZE];

            setSDT(SDT, 0, sdtLen);
            //MpegUtil.memcpy(TsStream, StreamLen[0], SDT, sdtLen[0]);
            TsStream.writeBytes(SDT);
            StreamLen[0] += TS_CHUNK_SIZE;
            //CallBack(SDT[:], TS_CHUNK_SIZE)
		/*CallBack(PAT[:], TS_CHUNK_SIZE)
		CallBack(PMT[:], TS_CHUNK_SIZE)*/
            minfoTag = true;
            return 0;
        }
        outPts[0] = mcurTag.head.timeStamp * 90 + mcurTag.head.deviation * 90;
        return 0;

    }

    byte[] gSDT = {0x47, 0x40, 0x11, 0x10, 0x00, 0x42, (byte) 0xf0, 0x25, 0x00, 0x01, (byte) 0xc1, 0x00, 0x00, 0x00, 0x01, (byte) 0xff, 0x00, 0x01, (byte) 0xfc, (byte) 0x80,
            0x14, 0x48, 0x12, 0x01, 0x06, 0x46, 0x46, 0x6d, 0x70, 0x65, 0x67, 0x09, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x30,
            0x31, (byte) 0xa7, 0x79, (byte) 0xa0, 0x03, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    void setSDT(byte[] SDT, int index, int[] outLen) {

        MpegUtil.memcpy(SDT, index, gSDT, outLen[0]);
    }

    //处理audioTag
    public static SAudioInfo dealATag(byte[] inBuf, int index, int inLen) {
        SAudioInfo aInfo = new SAudioInfo();
        aInfo.codeType = (byte) ((inBuf[index] & 0xFF) >>> 4);
        aInfo.rate = (byte) ((inBuf[index] & 0xC) >>> 2);
        aInfo.precision = (byte) ((inBuf[index] & 0x2) >>> 1);
        aInfo.audioType = (byte) (inBuf[index] & 0x1);
        return aInfo;
    }

    //将一个Tag数据先打成PES包
    public static int packPES(byte[] inBuf, int inBufStart, int inLen, String framType, long timeStamp, byte[] pespack, int[] outLen) {

        outLen[0] = 0;
        int[] headlen = {0}; //整个头部长度
        int[] packLen = {0};     //整个包长度
        //在PES层, DTS（解码时间戳）和PTS（显示时间戳）
        long pts = 0;
        // 解码时间戳
        long dts = 0;


        int cts = ((inBuf[inBufStart + 2]) << 16 | (inBuf[inBufStart + 3]) << 8 | (inBuf[inBufStart + 4])) ; //偏移量cts
        if (framType.equalsIgnoreCase("A")) {
            cts = 0;
        }
        dts = (timeStamp) * 90; //flv中记录的时间戳是DTS(TS中的时间是FLV中的1/90)
        pts = dts + cts * 90;

        if (pts < dts) {
            pts = dts;
            System.out.println("[PackTS] ERROR timeStamp:  pts < dts");
        }

        //包头由起始码前缀、数据流识别及PES包长信息3部分构成。包起始码前缀是用23个连续“0”和1个“1”构成的
        //3 0x000001 3字节的前缀
        //1	0xe0-0xef:v 0xc0-0xef:a
        //2 包长
        byte[] head = new byte[1024];
        head[0] = 0;
        head[1] = 0;
        head[2] = 1;
        head[3] = (byte) 0xE0; //pos= 3 原始流的类型和数目，取值从1100到1111之间
        //数据流识别, 数据流（视频，音频，或其它）
        head[4] = 0x00; //pos= 4 表示从此字节之后PES包长(单位字节)。
        head[5] = 0x03;
        headlen[0] += 6;

        //fmt.Printf("[PackPES] timeStamp CTS dts PTS %d %d %d %d\n", curTag.head.timeStamp, curTag.head.deviation, dts, pts)

        byte PDflags = 0;
        //类型数值未确定
        if (framType.equalsIgnoreCase("I")) {
            //I帧
            PDflags = 0x03;
        } else if (framType.equalsIgnoreCase("P")) {
            //P帧
            PDflags = 0x03;
        } else if (framType.equalsIgnoreCase("B")) {
            //B帧
            PDflags = 0x03;
        } else if (framType.equalsIgnoreCase("A")) {
            //音频
            head[3] = (byte) 0xC0;
            PDflags = 0x02; //音频没有DTS
        }
        //包头识别标志由12个部分
        //head[6] = 0x80 //10000000
        byte fixBit = 0x02;//2 填充字节
        byte PESscramblingControl = 0; //2 PES有效负载的加密模式。0表示不加密，其余表示用户自定义。
        byte PESpriority = 0;           //1 PES数据包的优先级
        byte dataAlignmentIndicator = 0; //1 为时，表明此分组头部之后紧跟着数据流描述子中定义的访问单元类型
        byte copyright = 0;           //1 版权，表示有版权
        byte originalOrCopy = 0;       //1 pos= 6 1表示原始数据，0表示备份
        head[6] = (byte) ((fixBit << 6 | PESscramblingControl << 4 | PESpriority << 3 | dataAlignmentIndicator << 2 | copyright << 1 | originalOrCopy) & 0xFF);

        byte PTSDTSflags = PDflags;     //2 10表示含有PTS字段，11表示含有PTS和DTS字段，00表示不含有pts_dts和DTS。
        byte ESCRflag = 0;         //1 1表示ESCR在PES首部出现，0表示不出现
        byte ESrateFlag = 0;      //1 1表示PES分组含有ES_rate字段。0表示不含有。
        byte DSMtrickModeFlag = 0;    //1 1表示有位的trick_mode_flag字段，0表示不出现此字段。只对DSM有效。
        byte additionalCopyInfoFlag = 0;//1 1表示有copy_info_flag字段，0表示不出现此字段。
        byte PESCRCflag = 0;           //1 1表示PES分组中有CRC字段，0表示不出现此字段。
        byte PESextensionFlag = 0;      //1 pos= 7 1表示扩展字段在PES包头存在，0表示扩展字段不存在

        head[7] = (byte) ((PTSDTSflags << 6 | ESCRflag << 5 | ESrateFlag << 4 | DSMtrickModeFlag << 3 | additionalCopyInfoFlag << 2 | PESCRCflag << 1 | PESextensionFlag) & 0xFF);
        head[8] = 0; // PESheaderDataLength = 0;         //表示可选字段和填充字段所占的字节数。

        headlen[0] += 3;

        if ((PTSDTSflags & 0x02) > 0) {
            byte[] PTSbuf = new byte[5];
            //注意前四位的填充
           /* PTSbuf[0] = (byte) ((PTSDTSflags << 4 | ((pts >>> 28) & 0x0e) | 0x01) & 0xFF); //pts&0x1 11000000 00000000 00000000 00000000
            PTSbuf[1] = (byte) ((pts >>> 21) & 0xff);                    //pts&0x 00111111 11000000 00000000 00000000
            PTSbuf[2] = (byte) ((((pts >>> 13) & 0xfe) | 0x01) & 0xFF);                   //pts&0x 00000000 00111111 10000000 00000000
            PTSbuf[3] = (byte) ((pts >>> 6) & 0xff);                         //pts&0x 00000000 00000000 01111111 10000000
            PTSbuf[4] = (byte) ((((pts << 2) & 0xfc)  | 0x01) & 0xFF);   */                          //pts&0x 00000000 00000000 00000000 01111111

            PTSbuf[0] = (byte) (((PTSDTSflags >> 2) & 0x30)/* 0011/0010 */ | (((pts >> 30) & 0x07) << 1) /* PTS 30-32 */ | 0x01) /* marker_bit */;
            PTSbuf[1] = (byte) ((pts >> 22) & 0xFF); /* PTS 22-29 */
            PTSbuf[2] = (byte) (((pts >> 14) & 0xFE) /* PTS 15-21 */ | 0x01) /* marker_bit */;
            PTSbuf[3] = (byte) ((pts >> 7) & 0xFF); /* PTS 7-14 */
            PTSbuf[4] = (byte) (((pts << 1) & 0xFE) /* PTS 0-6 */ | 0x01) /* marker_bit */;

            head[8] += 5;
            MpegUtil.memcpy(head, headlen[0], PTSbuf, PTSbuf.length);
            headlen[0] += 5;

		/*pts
		 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8
		|0|0|1|1|.|.|.|1|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|1|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|1|
				pts1     pts2                            pts3
		*/
        }
        if ((PTSDTSflags & 0x01) > 0) {
            byte[] DTSbuf = new byte[5];

            DTSbuf[0] = (byte) (0x10 /* 0001 */ | (((dts >> 30) & 0x07) << 1) /* DTS 30-32 */ | 0x01) /* marker_bit */;
            DTSbuf[1] = (byte) ((dts >> 22) & 0xFF); /* DTS 22-29 */
            DTSbuf[2] = (byte) (((dts >> 14) & 0xFE) /* DTS 15-21 */ | 0x01) /* marker_bit */;
            DTSbuf[3] = (byte) ((dts >> 7) & 0xFF); /* DTS 7-14 */
            DTSbuf[4] = (byte) (((dts << 1) & 0xFE) /* DTS 0-6 */ | 0x01) /* marker_bit */;

            /*DTSbuf[0] = (byte) ((0x01 << 4 |  ((dts >>> 28) & 0x0e) | 0x01) & 0xFF);
            DTSbuf[1] = (byte) ((dts >>> 21) & 0xFF);
            DTSbuf[2] = (byte) ((((dts  >>> 13) & 0xfe) | 0x01) & 0xFF);
            DTSbuf[3] = (byte) ((dts  >>> 6)  & 0xFF);
            DTSbuf[4] = (byte) (((dts << 2) & 0xfc) & 0xFF);*/

            head[8] += 5;
            MpegUtil.memcpy(head, headlen[0], DTSbuf, DTSbuf.length);
            headlen[0] += 5;
		/*dts
		 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8
		|0|0|0|1|.|.|.|1|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|1|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|1|
				 dts1    dts2                            dts3
		*/
        }

        if (ESCRflag > 0) {

        }

        if (ESrateFlag > 0) {

        }

        if (ESrateFlag > 0) {

        }

        if (DSMtrickModeFlag > 0) {

        }

        if (additionalCopyInfoFlag > 0) {

        }

        if (PESextensionFlag > 0) {
            //这里其实还有一层
        }

        if (framType.equalsIgnoreCase("I")) { //H264视频码流
            byte[] NALbuf = new byte[inLen + 256];
            int[] NALlen = {0};
            CMux.parseNAL(inBuf, inBufStart, inLen, NALbuf, NALlen);
            if (NALlen[0] <= 0) { //为SPS/PPS的Tag，不单独写成PES包(放在I帧前和I帧一起打包)
                //System.out.println("ts error parseNAL file %s line %d", __FILE__, __LINE__);
                // outBuf = null;//xfree(*outBuf);
                outLen[0] = 0;
                return 0;
            }

            packLen[0] = headlen[0] + mSpsNalLen + mPpsNalLen + 6 + NALlen[0];
            head[4] = 0;// (byte) (((packLen[0] - 6) >>> 8) & 0xFF);
            head[5] = 0;//(byte) ((packLen[0] - 6) & 0xFF);

            //pesheader 到 输出流
            MpegUtil.memcpy(pespack, 0, head, headlen[0]);
            outLen[0] += headlen[0];

            pespack[headlen[0]] = 0;
            pespack[headlen[0] + 1] = 0;
            pespack[headlen[0] + 2] = 0;
            pespack[headlen[0] + 3] = 1;
            pespack[headlen[0] + 4] = 9;
            pespack[headlen[0] + 5] = (byte) 0xf0;                              //添加slice前加上0x00 0x00 0x00 0x01 0x09 0xf0 起始码
            MpegUtil.memcpy(pespack, headlen[0] + 6, mSpsNal, mSpsNalLen);//添加I帧前添加一次Sps
            outLen[0] += mSpsNalLen;
            MpegUtil.memcpy(pespack, headlen[0] + 6 + mSpsNalLen, mPpsNal, mPpsNalLen); //添加I帧前添加一次Pps
            outLen[0] += mPpsNalLen;
            MpegUtil.memcpy(pespack, headlen[0] + mSpsNalLen + mPpsNalLen + 6, NALbuf, NALlen[0]);
            outLen[0] += NALlen[0];
        } else if (framType.equalsIgnoreCase("A")) {

            if ((inBuf[inBufStart + 0] & 0xFF) >>> 4 == 10) { //AAC
                mAstreamType = 0x0f;
                byte[] AACbuf = new byte[inLen + 128];
                int[] AAClen = {0};
                CMux.parseAAC(inBuf, inBufStart, inLen, AACbuf, AAClen);
                if (AAClen[0] <= 0) {
                    // 				System.out.println("ts error parseAAC file %s line %d", __FILE__, __LINE__);
                    //outBuf= null;//xfree(*outBuf);
                    outLen[0] = 0;
                    return 0;
                }
                packLen[0] = headlen[0] + AAClen[0];
                head[4] = (byte) (((packLen[0] - 6) >>> 8) & 0xFF);
                head[5] = (byte) ((packLen[0] - 6) & 0xFF);
                MpegUtil.memcpy(pespack, 0, head, headlen[0]);
                outLen[0] += headlen[0];
                MpegUtil.memcpy(pespack, headlen[0], AACbuf, AAClen[0]);
                outLen[0] += AAClen[0];
            } else if ((inBuf[inBufStart] & 0xFF) >>> 4 == 2) { //Mp3
                mAstreamType = 0x03; //音频编码类型
                packLen[0] = headlen[0] + inLen - 1;
                head[4] = (byte) (((packLen[0] - 6) >>> 8) & 0xFF); //去掉6位头
                head[5] = (byte) ((packLen[0] - 6) & 0xFF);
                MpegUtil.memcpy(pespack, 0, head, headlen[0]);
                outLen[0] += headlen[0];
                MpegUtil.memcpy(pespack, headlen[0], inBuf, 1, inLen - 1);
                outLen[0] += inLen - 1;

            } else { //非AAC目前做简单处理
                //fmt.Println("[PackPES] Audio Is Not AAC")
                packLen[0] = headlen[0] + inLen - 1;
                head[4] = (byte) (((packLen[0] - 6) >>> 8) & 0xFF);//去掉6位头
                head[5] = (byte) ((packLen[0] - 6) & 0xFF);
                MpegUtil.memcpy(pespack, 0, head, headlen[0]);
                outLen[0] += headlen[0];
                MpegUtil.memcpy(pespack, headlen[0], inBuf, 1, inLen - 1);
                outLen[0] += inLen - 1;
            }
        } else {
            byte[] NALbuf = new byte[inLen + 256];
            int[] NALlen = {0};
            CMux.parseNAL(inBuf, inBufStart, inLen, NALbuf, NALlen);
            if (NALlen[0] <= 0) {
                // 			System.out.println("ts error parseNAL file %s line %d", __FILE__, __LINE__);
                ///outBuf = null;//xfree(*outBuf);
                outLen[0] = 0;
                return 0;
            }
            packLen[0] = headlen[0] + NALlen[0] + 6;
            head[4] = (byte) (((packLen[0] - 6) >>> 8) & 0xFF); //去掉6位头
            head[5] = (byte) ((packLen[0] - 6) & 0xFF);

            MpegUtil.memcpy(pespack, 0, head, headlen[0]);
            outLen[0] += headlen[0];
            pespack[headlen[0]] = 0;
            pespack[headlen[0] + 1] = 0;
            pespack[headlen[0] + 2] = 0;
            pespack[headlen[0] + 3] = 1;
            pespack[headlen[0] + 4] = 9;
            pespack[headlen[0] + 5] = (byte) 0xf0; //添加slice前加上0x00 0x00 0x00 0x01 0x09 0xf0 起始码
            outLen[0] += 6;
            MpegUtil.memcpy(pespack, headlen[0] + 6, NALbuf, NALlen[0]);
            outLen[0] += NALlen[0];
        }
        return 0;
    }


    //
    public static int parseAAC1(byte[] inBuf, int index, int inLen, byte[] outBuf, int[] outLen) {

        // MpegUtil.memset(out,0, (byte) 0, inLen + 128);
        outLen[0] = 0;
        if ((inBuf[index + 1] & 0xFF) == 0x00) { //AACPacketType为0x00说明是AAC sequence header
            mAudioSpecificConfig.ObjectType = (byte) (inBuf[index + 2] >>> 3);
            mAudioSpecificConfig.SamplerateIndex = (byte) ((inBuf[index + 2] & 0x07) << 1 | inBuf[index + 3] >>> 7);
            mAudioSpecificConfig.Channels = (byte) ((inBuf[index + 3] << 1) >>> 4);
            mAudioSpecificConfig.FramLengthFlag = (byte) ((inBuf[index + 3] & 4) >>> 2);
            mAudioSpecificConfig.DependOnCCoder = (byte) ((inBuf[index + 3] & 2) >>> 1);
            mAudioSpecificConfig.ExtensionFlag = (byte) (inBuf[index + 3] & 1);

            if ((mAudioSpecificConfig.ExtensionFlag & 0xFF) > 0) {

            }
            mAACFLag = false;
            outBuf = null;//xfree(outBuf);
            return 0;
        } else {
            int aacFrameLength = inLen - 2 + 7; //这里是带上ADTS头7字节的全帧长度，不是帧数据长度
            outBuf[0] = (byte) 0xFF;
            outBuf[1] = (byte) 0xF1;
            outBuf[2] = (byte) (((mAudioSpecificConfig.ObjectType - 1) << 6) |
                    ((mAudioSpecificConfig.SamplerateIndex & 0x0F) << 2) |
                    (mAudioSpecificConfig.Channels >>> 7));
            outBuf[3] = (byte) ((mAudioSpecificConfig.Channels << 6) | (byte) ((aacFrameLength & 0x1800) >>> 11));
            outBuf[4] = (byte) ((aacFrameLength & 0x7f8) >>> 3);
            outBuf[5] = (byte) (((aacFrameLength & 0x7) << 5) | 0x1f);
            outBuf[6] = (byte) (0xFC | 0x00);
		/*
			//adts_fixed_header
				syncword 				//12 同步头 总是0xFFF, all bits must be 1，代表着一个ADTS帧的开始
				ID						//1 MPEG Version: 0 for MPEG-4, 1 for MPEG-2
				Layer					//2 always: '00'
				protectionAbsent		//1
				profile					//2 表示使用哪个级别的AAC，有些芯片只支持AAC LC 。在MPEG-2 AAC中定义了3种：
				samplingFrequencyIndex 	//4 表示使用的采样率下标
				privateBit				//1
				channelConfiguration	//3
				originalCopy			//1
				home					//1

			//adts_variable_header
				copyrightIBit			//1
				copyrightIStart			//1
				frameLength	 			//13 一个ADTS帧的长度包括ADTS头和AAC原始流.
				adtsBufferFullness 	    //11 0x7FF 说明是码率可变的码流
				NumOfRawDataBlockInFrame//2*/

            MpegUtil.memcpy(outBuf, 7, inBuf, index + 2, inLen - 2);
            outLen[0] = inLen + 5;
            return 0;
        }
    }

    //将一个PES包打成TS包(不包含PSI)  pusi  1是起始 0 是循环
    public static int packTS(byte[] inBuf, int[] inLen,
                             String framType, byte pusi,
                             byte afc, byte[] mcc,
                             char pid, int timeStamp,
                             BufFacade TSstream, int[] streamLen) {
        int max = (inLen[0] / 184 + 1) * TS_CHUNK_SIZE + TS_CHUNK_SIZE * 2;
        //outLen[0] = 0;
        streamLen[0] = 0;
        boolean StuffFlag = false;
        int StuffLen = 0;

        // 	if len(inBuf) < inLen {
        // 		fmt.Println("[PackTS] ERROR inLen :len(inBuf),inLen ", len(inBuf), inLen)
        // 		inLen = len(inBuf)
        // 	}

        long DTS = timeStamp * 90; //flv中记录的时间戳是DTS(TS中的时间是FLV中的1/90)

        int dealPos = 0;
        if (inLen[0] > 65536 * 4) {
            System.out.println("[slice][PackTS] too long inLen");
        }
        //除去PSI的打包
        for (dealPos = 0; dealPos < inLen[0]; ) {
            byte[] TSpack = new byte[TS_CHUNK_SIZE];
            int headlen = 0;
            byte[] head = new byte[4];
            byte[] adaptionFiled = new byte[184];
            int adaptationFiledLength = 0;

            byte synByte = 0x47;         //同步字节, 固定为,表示后面的是一个TS分组
            byte transportErrorIndicator = 0;     //传输误码指示符
            byte payloadUnitStartIndicator = pusi; //有效荷载单元起始指示符, 1标明为负载起始包
            byte transport_priority = 0;           //传输优先, 1表示高优先级,传输机制可能用到，解码用不着
            char PID = pid;                     //PID = 0x100

            byte transportScramblingControl = 0; //传输加扰控制(加密)
            byte adaptionFieldControl = afc;     //自适应控制 01仅含有效负载，10仅含调整字段，11都有
            byte continuityCounter = mcc[0];      //连续计数器 一个4bit的计数器，范围0-15
            head[0] = synByte;
            head[1] = (byte) ((transportErrorIndicator << 7 | payloadUnitStartIndicator << 6 | transport_priority << 5 | ((PID << 3) >>> 11))& 0xFF);
            head[2] = (byte) (PID & 0xFF);
            head[3] = (byte) ((transportScramblingControl << 6 | adaptionFieldControl << 4 | continuityCounter)& 0xFF);;
            headlen += 4;

            if (inLen[0] - dealPos < 182 && (pusi & 0xFF) == 1) { //数据不够，需要填充满188字节
                adaptionFieldControl = 0x03;
                head[3] = (byte) ((transportScramblingControl << 6 | adaptionFieldControl << 4 | continuityCounter)& 0xFF);
                StuffFlag = true;
            } else if (inLen[0] - dealPos < 184) {
                if ((pusi & 0xFF) == 0) {
                    adaptionFieldControl = 0x03;
                    head[3] = (byte) ((transportScramblingControl << 6 | adaptionFieldControl << 4 | continuityCounter)& 0xFF);
                    StuffFlag = true;
                } else if ((pusi & 0xFF) == 1 && !framType.equalsIgnoreCase("I") && !framType.equalsIgnoreCase("A")) {
                    adaptionFieldControl = 0x03;
                    head[3] = (byte) ((transportScramblingControl << 6 | adaptionFieldControl << 4 | continuityCounter)& 0xFF);
                    StuffFlag = true;
                }
            }
            if ((framType.equalsIgnoreCase("B") ||framType.equalsIgnoreCase("P") ||framType.equalsIgnoreCase("I") || framType.equalsIgnoreCase("A")) && (pusi & 0xFF) == 1) { //包含原始流节点
                adaptionFieldControl = 0x03;
                head[3] = (byte) ((transportScramblingControl << 6 | adaptionFieldControl << 4 | continuityCounter)& 0xFF);
            }

            if ((adaptionFieldControl & 0xFF) > 1) {
                byte discontinuityIndicator = 0; //不连续状态指示
                byte randomAccessIndicator = 0; //
                if ((framType.equalsIgnoreCase("B") ||framType.equalsIgnoreCase("P") ||framType.equalsIgnoreCase("I") || framType.equalsIgnoreCase("A")) && (pusi & 0xFF) == 1) {
                    randomAccessIndicator = 1;
                }
                byte elementaryStreamPriorityIndicator = 0; //为1当前分组有较高优先级,有多个负载在会用到（可以不考虑）

                byte pcrFlag = 0;
                if ((pusi & 0xFF) == 1 && /*(inLen[0] - dealPos >= 184 ) &&*/ ( framType.equalsIgnoreCase("I") || framType.equalsIgnoreCase("P") || framType.equalsIgnoreCase("B"))) {
                    pcrFlag = 1;
                }
                /*byte pcrFlag = 0;                      //pcr标志   pes包的前面 （待定）
                if ((pusi & 0xFF) == 1 && ( framType.equalsIgnoreCase("I") || framType.equalsIgnoreCase("P") || framType.equalsIgnoreCase("B"))) {
                    pcrFlag = 1;
                }*/
                byte opcrFlag = 0;               //（待定）
                byte splicingPointFlag = 0;     //拼接点（貌似没用）
                byte transportPrivateDataFlag = 0;//私用数据字节（可以不考虑）
                byte adaptationFiledExtension = 0;//扩展自适应字段
                adaptionFiled[1] = (byte) ((discontinuityIndicator << 7 | randomAccessIndicator << 6 | elementaryStreamPriorityIndicator << 5 | pcrFlag << 4 | opcrFlag << 3 | splicingPointFlag << 2 | transportPrivateDataFlag << 1 | adaptationFiledExtension)& 0xFF);
                adaptationFiledLength += 2;


               if ((pcrFlag & 0xFF) > 0) {
                    byte[] pcrBuf = new byte[6];
                    int[] pcrLen = {0};
                    setPcr(DTS, pcrBuf, pcrLen);
                    MpegUtil.memcpy(adaptionFiled, adaptationFiledLength, pcrBuf, pcrLen[0]);
                    adaptationFiledLength += 6;
                    // 48 bit
                }
                if ((opcrFlag & 0xFF) > 0) {
                    // 48 bit
                }
                if ((splicingPointFlag & 0xFF) > 0) {
                    // 8 bit
                }
                if ((transportPrivateDataFlag & 0xFF) > 0) {
                    byte transportPrivateDataLen = 0; // 8 bit
                    if (transportPrivateDataLen > 0) {
                        transportPrivateDataLen += 0;
                    }

                }
                if ((adaptationFiledExtension & 0xFF) > 0) {
                    //这里也还有一层
                    byte adaptationExFiledLength = 0;
                    if (false) {
                        adaptationExFiledLength++;
                    }
                }

                if (StuffFlag) { //需要填充
                    StuffLen = 184 - (inLen[0] - dealPos) - 2;
                    for (int i = adaptationFiledLength; StuffLen > 0 && i < StuffLen + adaptationFiledLength; i++) {
                        adaptionFiled[i] = (byte) 0xFF;
                    }
                    adaptationFiledLength += StuffLen;
                }
                adaptionFiled[0] = (byte) (adaptationFiledLength - 1);
                MpegUtil.memcpy(TSpack, 4, adaptionFiled, adaptationFiledLength);
                headlen += adaptationFiledLength;
            }
            mcc[0] = (byte) ((mcc[0] + 1) % 16);
            MpegUtil.memcpy(TSpack, 0, head, head.length);
            if (dealPos + TS_CHUNK_SIZE - headlen <= inLen[0]) {
                MpegUtil.memcpy(TSpack, headlen, inBuf, dealPos, TS_CHUNK_SIZE - headlen);
            } else {
                //fmt.Printf(" [PackTS]dealPos+TS_CHUNK_SIZE-headlen, inLen ", dealPos+TS_CHUNK_SIZE-headlen, inLen)
                MpegUtil.memcpy(TSpack, headlen, inBuf, dealPos, cmsMin(inLen[0] - dealPos, TS_CHUNK_SIZE - headlen));
            }

            if (streamLen[0] + TS_CHUNK_SIZE > max) {
                System.out.println(String.format("[slice][PackTS] too big [max, inLen, streamLen+TS_CHUNK_SIZE] ", max, inLen, streamLen[0] + TS_CHUNK_SIZE));
            }
            dealPos = dealPos + TS_CHUNK_SIZE - headlen;
            //MpegUtil.memcpy(TSstream , streamLen[0], TSpack, TSpack.length);
            TSstream.writeBytes(TSpack);
            streamLen[0] = streamLen[0] + TS_CHUNK_SIZE;
            pusi = 0;
        }

        return 0;
    }

    //处理videoTag
    static SVideoInfo dealVTag(byte[] inBuf, int index) {
        SVideoInfo vInfo = new SVideoInfo();
        vInfo.framType = (byte) ((inBuf[index] >>> 4) & 0xF);
        vInfo.codeId = (byte) (inBuf[index] & 0xF);
        return vInfo;
    }

    //打包PSI，这里只打包PAT和PMT表
    public void packPSI() {
        int[] PATlen = {0};
        int[] PMTlen = {0};
        byte[] tPAT = new byte[TS_CHUNK_SIZE];
        byte[] tPMT = new byte[TS_CHUNK_SIZE];
        //PAT
        byte[] head = new byte[5];

	/*byte synByte  = 0x47 ;              //同步字节, 固定为0x47,表示后面的是一个TS分组
	byte transportErrorIndicator  = 0 ;   //传输误码指示符
	byte payloadUnitStartIndicator  = 1 ; //有效荷载单元起始指示符, 1标明为负载起始包
	byte transport_priority  = 0 ;        //传输优先, 1表示高优先级,传输机制可能用到，解码用不着
	int16 PID  = PATpid        ;          //PID
	byte transportScramblingControl  = 0; //传输加扰控制(加密)
	byte adaptionFieldControl  = 1;       //自适应控制 01仅含有效负载，10仅含调整字段，11都有
	byte continuityCounter  = 0;          //连续计数器 一个4bit的计数器，范围0-15(patpmt不计入连续)
	head[0] = synByte;
	head[1] = transportErrorIndicator<<7 | payloadUnitStartIndicator<<6 | transport_priority<<5 | byte((PID<<3)>>>11);
	head[2] = byte(PID);
	head[3] = transportScramblingControl<<6 | adaptionFieldControl<<4 | continuityCounter;*/
        head[0] = 0x47;
        head[1] = 0x40;
        head[2] = 0x00;
        head[3] = (byte) (0x10 | mpatmcc[0]);
        mpatmcc[0] = (byte) ((mpatmcc[0] + 1) % 16);
        head[4] = 0; //负载与头间有个0x00的pointer_field
        setPAT(PMTpid, tPAT, PATlen);
        MpegUtil.memcpy(mPAT, 0, head, head.length);
        MpegUtil.memcpy(mPAT, 5, tPAT, PATlen[0]);
        tPAT = null;//xfree(tPAT);
        for (int pos = PATlen[0] + 5; pos < TS_CHUNK_SIZE; pos++) {
            mPAT[pos] = (byte) 0xFF;
        }

        //PMT
	/*synByte = 0x47;               //同步字节, 固定为,表示后面的是一个TS分组
	transportErrorIndicator = 0;    //传输误码指示符
	payloadUnitStartIndicator = 1;  //有效荷载单元起始指示符, 1标明为负载起始包
	transport_priority = 0 ;        //传输优先, 1表示高优先级,传输机制可能用到，解码用不着
	PID = PMTpid;                   //PID
	transportScramblingControl = 0; //传输加扰控制(加密)
	adaptionFieldControl = 1;       //自适应控制 01仅含有效负载，10仅含调整字段，11都有
	continuityCounter = 0  ;        //连续计数器 一个4bit的计数器，范围0-15(patpmt不计入连续)
	head[0] = synByte;
	head[1] = transportErrorIndicator<<7 | payloadUnitStartIndicator<<6 | transport_priority<<5 | byte((PID<<3)>>>11);
	head[2] = byte(PID);
	head[3] = transportScramblingControl<<6 | adaptionFieldControl<<4 | continuityCounter;*/
        head[0] = 0x47;
        head[1] = 0x50;
        head[2] = 0x00;
        head[3] = (byte) (0x10 | mpmtmcc[0]);
        mpmtmcc[0] = (byte) ((mpmtmcc[0] + 1) % 16);
        head[4] = 0; //负载与头间有个0x00的pointer_field
        setPMT(Apid, Vpid, PCRpid, mAstreamType, tPMT, PMTlen);
        MpegUtil.memcpy(CMux.mPMT, 0, head, head.length);
        MpegUtil.memcpy(CMux.mPMT, 5, tPMT, PMTlen[0]);
        //tPMT = null;//xfree(tPMT);
        for (int pos = PMTlen[0] + 5; pos < TS_CHUNK_SIZE; pos++) {
            mPMT[pos] = (byte) 0xFF;
        }
    }

    CmsAmf0 amf0 = new CmsAmf0();

    //处理scriptTag
    public SDataInfo dealSTag(byte[] inBuf, int index, int inLen) {
        //fmt.Println("dealSTag")
        //跳过第一个AMF,一般第一个AMF为固定信息
        SDataInfo dInfo = new SDataInfo();
        byte amfType = inBuf[index + 13];//
        if ((amfType & 0xFF) == 1) {
            //示例，无意义
        }
        int amfSize = (int) (inBuf[index + 14]) << 24 | (int) (inBuf[index + 15]) << 16 | (int) (inBuf[index + 16]) << 8 | (int) (inBuf[index + 17]);

        float temp = 0;
        Amf0Block block = null;
        if (inLen - 15 >= amfSize) {
            block = amf0.amf0Parse(inBuf, index + 15, amfSize);
        }
        if (block != null) {
            String[] strValue = new String[0];
            amf0.amf0Block5Value(block, "duration", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.duration = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "width", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.width = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "height", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.height = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "videodatarate", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.videodatarate = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "framerate", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.framerate = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "videocodecid", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.videocodecid = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "audiosamplerate", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.audiosamplerate = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "audiosamplesize", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.audiosamplesize = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "stereo", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.stereo = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "audiocodecid", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.audiocodecid = (int) temp;
            }

            strValue[0] = "";
            amf0.amf0Block5Value(block, "filesize", strValue);
            if (strValue.length != 0) {
                temp = Float.parseFloat(strValue[0]);
                dInfo.filesize = (int) temp;
            }

            amf0.amf0BlockRelease(block);
        }

        return dInfo;
    }
}
