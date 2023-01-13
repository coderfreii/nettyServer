package org.tl.nettyServer.media.net.hls.ts.codec;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.media.acc.AACFrame;
import org.tl.nettyServer.media.media.acc.AACUtils;
import org.tl.nettyServer.media.media.flv.FLVUtils;
import org.tl.nettyServer.media.media.h264.H264CodecConfigParts;
import org.tl.nettyServer.media.media.h264.H264Utils;
import org.tl.nettyServer.media.media.mp3.MP3BufferedDecoder;
import org.tl.nettyServer.media.media.mp3.MP3HeaderData;
import org.tl.nettyServer.media.media.ts.IFLV2MPEGTSWriter;
import org.tl.nettyServer.media.media.ts.TSPacketFragment;
import org.tl.nettyServer.media.media.ts.TransportStreamUtils;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.util.BinaryUtils;
import org.tl.nettyServer.media.util.BufferUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.tl.nettyServer.media.media.ts.TransportStreamUtils.*;


/**
 * FLV TO Mpeg2TS
 *
 * @author pengliren
 */
public class FLV2MPEGTSWriter {

    private static Logger log = LoggerFactory.getLogger(FLV2MPEGTSWriter.class);

    /* 转换的数据是从这流提供的，这里流是在SegmentFacade中注册的 */
    private IFLV2MPEGTSWriter writer;
    /* 视频pid */
    public final static int videoPID = 0x102;
    /* 音频pid */
    public final static int audioPID = 0x101;
    /* 视频流id */
    private byte videoStreamID = (byte) 0xE0;
    /* 音频流id */
    private byte audioStreamID = (byte) 0xC0;
    /* 数据块大小 */
    private byte[] block = new byte[TS_PACKETLEN];
    /* 数据块大小 */
    protected long videoCCounter = -1L;

    protected long audioCCounter = -1L;

    protected long patCCounter = 0L;

    protected AACFrame aacFrame = null;

    protected H264CodecConfigParts h264CodecConfigPart = null;

    protected long lastAudioTimeCode = -1L;

    protected long lastVideoTimeCode = -1L;

    protected long lastPCRTimeCode = -1L;

    protected boolean isFirstAudioPacket = true;

    protected boolean isFirstVideoPacket = true;

    // fix rtmp mp3 to mpegts ts
    private int lastMP3SampleRate = -1;

    private long lastMP3TimeCode = -1;

    private byte[] mp3HeaderBuf;

    private MP3HeaderData mp3HeaderData;

    // fix rtmp aac to mpegts ts
    private int lastAACSampleRate = -1;

    private long lastAACTimeCode = -1L;

    protected WaitingAudio waitingAudio = new WaitingAudio();

    protected int pcrBufferTime = 750;

    protected int mpegtsAudioGroupCount = 3;

    protected int videoCodec = STREAM_TYPE_VIDEO_UNKNOWN;

    protected int audioCodec = TransportStreamUtils.STREAM_TYPE_AUDIO_UNKNOWN;

    public FLV2MPEGTSWriter(IFLV2MPEGTSWriter writer, BufFacade videoConfig, BufFacade audioConfig) {
        this.writer = writer;
        if (videoConfig != null) {
            videoCodec = (byte) FLVUtils.getVideoCodec(videoConfig.getByte(0));
            if (videoCodec == VideoCodec.AVC.getId()) {
                videoConfig.readerIndex(0);
                h264CodecConfigPart = H264Utils.breakApartAVCC(videoConfig);
            }
        }

        if (audioConfig != null) {
            audioCodec = (byte) FLVUtils.getAudioCodec(audioConfig.getByte(0));
            if (audioCodec == AudioCodec.AAC.getId()) {
                audioConfig.readerIndex(2);
                aacFrame = AACUtils.decodeAACCodecConfig(audioConfig);
            }
        }
    }

    /**
     * * Video data event
     * *
     * FrameType            UB[4]
     * CodecID              UB[4]             1
     * AVCPacketType        UI8               1
     * CompositionTime(cst) SI24              3         5
     * ConfigurationVersion UI8               1         6
     * AVCProfileIndication UI8               1         7
     * ProfileCompatibility UI8               1         8
     * AVCLevelIndication   UI8               1         9
     * Reserved             UB[6]
     * LengthSizeMinusOne   UB[2]             1         10
     * Reserved             UB[3]
     * Number of SPS NALUs  UB[5]                                       <=== here  11 byte
     * 前缀为下划线_的变量为设置值(控制流程用)  没有的为写入值
     *
     * @param data
     */
    public void handleVideo(VideoData data) {
        VideNaluInfosResolver videNaluInfosResolver = new VideNaluInfosResolver();
        if (this.h264CodecConfigPart != null) {
            videNaluInfosResolver.setH264CodecConfigPart(this.h264CodecConfigPart);
        }
        if (this.videoCodec != STREAM_TYPE_VIDEO_UNKNOWN) {
            videNaluInfosResolver.setVideoCodec(this.videoCodec);
        }

        videNaluInfosResolver.resolve(data);

        if (!videNaluInfosResolver.isResolved()) {
            return;
        }

        if (this.h264CodecConfigPart == null) {
            this.h264CodecConfigPart = videNaluInfosResolver.getH264CodecConfigPart();
        }
        if (this.videoCodec == STREAM_TYPE_VIDEO_UNKNOWN) {
            this.videoCodec = videNaluInfosResolver.getVideoCodec();
        }

        this.lastVideoTimeCode = videNaluInfosResolver.getLastDts();

        boolean continueTs = videNaluInfosResolver.isContinueTs();
        //解析完nalu开始封装ts
        if (continueTs) {
            long dts = videNaluInfosResolver.getLastDts();
            int allNaluLength = videNaluInfosResolver.getAllNaluLength();
            long pts = videNaluInfosResolver.getPts();
            int ptsDtsFlag = videNaluInfosResolver.getPtsDtsFlag();
            int FrameType = videNaluInfosResolver.getFrameType();
            List<TSPacketFragment> naluStartCodeAndNaluList = videNaluInfosResolver.getNaluStartCodeAndNaluList();


            TSPacketFragment tsPacket = naluStartCodeAndNaluList.remove(0);
            int offset = tsPacket.getOffset();
            int len = tsPacket.getLen();
            byte[] buff = tsPacket.getBuffer();


            long pcr = getPCRTimeCode();

            int readLenOfAllNaluLength = 0;

            int pesPayloadWritten = 0;

            //一个nalu 生成多个ts packet
                   /*
                        sync_byte	                    8bit	同步字节，固定为0x47    1
                        transport_error_indicator	    1bit	传输错误指示符，表明在ts头的adapt域后由一个无用字节，通常都为0，这个字节算在adapt域长度内
                        payload_unit_start_indicator	1bit	负载单元起始标示符，一个完整的数据包开始时标记为1
                        transport_priority		        1bit    传输优先级，0为低优先级，1为高优先级，通常取0
                        pid		                        13bit    pid值(Packet ID号码，唯一的号码对应不同的包)
                        transport_scrambling_control	2bit	传输加扰控制，00表示未加密
                        adaptation_field_control		2bit    是否包含自适应区，‘00’保留；‘01’为无自适应域，仅含有效负载；‘10’为仅含自适应域，无有效负载；‘11’为同时带有自适应域和有效负载。
                        continuity_counter		        4bit    递增计数器，从0-f，起始值不一定取0，但必须是连续的
                    */
            while (true) {
                int unReadLenOfAllNaluLength = allNaluLength - readLenOfAllNaluLength;
                if (unReadLenOfAllNaluLength > 32725) unReadLenOfAllNaluLength = 32725; //maxPesDataLen is 32725

                int currentLoopReadPayloadLen = 0;
                //payload_unit_start_indicator
                //可知这个包的负载部分有PES包头，换句话说，也就是帧头所在
                int payload_unit_start_indicator = 1;

                // adaptation_field_control
                //00：是保留值。
                //01：负载中只有有效载荷 也就是PES包。
                //10：负载中只有自适应字段。
                //11：先有自适应字段，再有有效载荷。
                int adaptation_field_control = 3;

                //开始写入ts packet
                while (true) {
                    int tsWriterIdx = 0;
                    //>--------------- pts header 4 byte  ------------

                    //sync_byte---
                    byte sync_byte = SYNCBYTE;
                    this.block[tsWriterIdx] = sync_byte; // pts header start sync_byte
                    tsWriterIdx++;


                    //transport_error_indicator---
                    //payload_unit_start_indicator---
                    //transport_priority---
                    //pid
                    int transport_error_indicator = 0;
                    int transport_priority = 0;
                    int[] high3bitsArr = {transport_error_indicator, payload_unit_start_indicator, transport_priority};
                    int high3bitInt = BinaryUtils.bits2Int(high3bitsArr, 5);

                    int PID = videoPID;
                    byte PIDHigh5Bits = (byte) (0x1F & (PID >> 8));
                    this.block[tsWriterIdx] = (byte) (high3bitInt + PIDHigh5Bits);
                    tsWriterIdx++;

                    byte PIDLow8Bits = (byte) (PID & 0xFF);
                    this.block[tsWriterIdx] = PIDLow8Bits;
                    tsWriterIdx++;


                    if (videoCCounter == -1L) videoCCounter = 1L;
                    else videoCCounter += 1L;

                    //transport_scrambling_control--- 有效负载加密模式标志，占位2bit，00表示未加密
                    int transport_scrambling_control = 0; //
                    long continuity_counter = (videoCCounter & 0xF);

                    //transport_scrambling_control
                    //adaptation_field_control---
                    //continuity_counter
                    this.block[tsWriterIdx] = (byte) ((transport_scrambling_control << 6) + (adaptation_field_control << 4) + continuity_counter); // pts header end
                    tsWriterIdx++;
                    //--------------- pts header 4 byte  ------------<

                    /*
                        pes start code	        3B	开始码，固定为0x000001
                        stream id	            1B	音频取值（0xc0-0xdf），通常为0xc0 视频取值（0xe0-0xef），通常为0xe0
                        pes packet length	    2B	后面pes数据的长度，0表示长度不限制，只有视频数据长度会超过0xffff
                        flag	                1B	通常取值0x80，表示数据不加密、无优先级、备份的数据
                        flag	                1B	取值0x80表示只含有pts，取值0xc0表示含有pts和dts
                        pes data length	        1B	后面数据的长度，取值5或10
                        pts	                    5B	33bit值
                        dts	                    5B  33bit值
                     */
                    int pesHeaderLen = 0;  //这里计算出来是为了预留位置
                    if (payload_unit_start_indicator != 0x00 && payload_unit_start_indicator != 0x10) {
                        pesHeaderLen = 9 + (ptsDtsFlag != 0 ? 10 : 5);
                    }

                    //只要tsWriterIdx  unReadLenOfAllNaluLength  currentLoopReadPayloadLen   就需要重新计算
                    int currentTsBlockCanWritablePayloadLength = calculatorTsPayloadLength(tsWriterIdx, pesHeaderLen, unReadLenOfAllNaluLength, currentLoopReadPayloadLen);

                    /*
                        adaptation_field_length	    1B	自适应域长度，后面的字节数
                        flag	                    1B	取0x50表示包含PCR或0x40表示不包含PCR
                        PCR	                        5B	Program Clock Reference，节目时钟参考，用于恢复出与编码端一致的系统时序时钟STC（System Time Clock）。
                        stuffing_bytes	            xB	填充字节，取值0xff     at least 1B so default to 1B                                                                      8B
                     */

                    int adaptation_field_length;
                    int needFillNullLen;
                    long tempPts;
                    long tempDts;
                    long tempPcr;

                    int hasAdaptationField = BinaryUtils.getBinaryFromInt(adaptation_field_control, 2, 0, 1);
                    if (hasAdaptationField == 1) {
                        if (adaptation_field_control == 3) {   //adaptation_field_control 为 1x  有调整字段
                            //tsPayloadLength 应该留一部分给adaptation_field
                            if (currentTsBlockCanWritablePayloadLength > 7) {  //代表可写 adaptation_field_length  又可写至少 1 Byte的数据信息
                                adaptation_field_length = 0x00;
                                int markAdaptationFieldLengthIndex = tsWriterIdx;
                                this.block[tsWriterIdx] = (byte) adaptation_field_length;
                                tsWriterIdx++;   //1


                                int flags = (isFirstVideoPacket ? 0x80 : 0x10) | (FrameType == 1 ? 0x40 : 0);
                                BinaryUtils.BinaryGetterByBit binaryGetterByBitForFlag = BinaryUtils.binaryGetterByBit(8);
                                int discontinuity_indicator = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);
                                int random_access_indicator = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);
                                int elementary_stream_priority_indicator = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);
                                int PCR_flag = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);
                                int OPCR_flag = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);
                                int splicing_point_flag = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);
                                int transport_private_data_flag = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);
                                int adaptation_field_extension_flag = binaryGetterByBitForFlag.getOneBitAndMovePointer(flags);


                                this.block[tsWriterIdx] = (byte) (flags);
                                tsWriterIdx++;  //2
                                adaptation_field_length++;


                                //TODO: not clear about this step
                                tempPcr = pcr;
                                tempPcr <<= 7;
                                byte[] pcrData = BufferUtil.longToByteArray(tempPcr);

                                this.block[tsWriterIdx] = (byte) (pcrData[3] & 0xFF);
                                tsWriterIdx++;
                                adaptation_field_length++;
                                this.block[tsWriterIdx] = (byte) (pcrData[4] & 0xFF);
                                tsWriterIdx++;
                                adaptation_field_length++;
                                this.block[tsWriterIdx] = (byte) (pcrData[5] & 0xFF);
                                tsWriterIdx++;
                                adaptation_field_length++;
                                this.block[tsWriterIdx] = (byte) (pcrData[6] & 0xFF);
                                tsWriterIdx++;
                                adaptation_field_length++;
                                this.block[tsWriterIdx] = (byte) ((pcrData[7] & 0x80) + 126);
                                tsWriterIdx++;
                                adaptation_field_length++;


                                currentTsBlockCanWritablePayloadLength = calculatorTsPayloadLength(tsWriterIdx, pesHeaderLen, unReadLenOfAllNaluLength, currentLoopReadPayloadLen);

                                needFillNullLen = TS_PACKETLEN - (tsWriterIdx + currentTsBlockCanWritablePayloadLength + pesHeaderLen);


                                this.block[markAdaptationFieldLengthIndex] = (byte) (needFillNullLen + adaptation_field_length);
                                if (needFillNullLen > 0) {
                                    System.arraycopy(TransportStreamUtils.FILL, 0, this.block, tsWriterIdx, needFillNullLen);
                                    tsWriterIdx += needFillNullLen;
                                }
                            }
                        } else if (adaptation_field_control == 2) {  //只有adaptation_field_control 字段
                            adaptation_field_length = TS_PACKETLEN - (tsWriterIdx + currentTsBlockCanWritablePayloadLength + pesHeaderLen);
                            if (adaptation_field_length > 0) {
                                if (adaptation_field_length > 1) {
                                    adaptation_field_length--;   //这里就表明adaptation_field_length不包括adaptation_field_length 字段

                                    this.block[tsWriterIdx] = (byte) (adaptation_field_length & 0xFF);
                                    tsWriterIdx++;

                                    this.block[tsWriterIdx] = 0;
                                    tsWriterIdx++;

                                    adaptation_field_length--;

                                    if (adaptation_field_length > 0)
                                        System.arraycopy(TransportStreamUtils.FILL, 0, this.block, tsWriterIdx, adaptation_field_length);

                                    tsWriterIdx += adaptation_field_length;
                                } else {
                                    this.block[tsWriterIdx] = 0;
                                    tsWriterIdx++;
                                }
                            }
                        }
                        adaptation_field_control = 0x01;
                        currentTsBlockCanWritablePayloadLength = calculatorTsPayloadLength(tsWriterIdx, pesHeaderLen, unReadLenOfAllNaluLength, currentLoopReadPayloadLen);
                    }

                    /*
                        pes start code	            3B	开始码，固定为0x000001
                        stream id	                1B	音频取值（0xc0-0xdf），通常为0xc0 视频取值（0xe0-0xef），通常为0xe0
                        pes packet length	        2B	后面pes数据的长度，0表示长度不限制，只有视频数据长度会超过0xffff
                        flag	                    1B	通常取值0x80，表示数据不加密、无优先级、备份的数据
                        flag	                    1B	取值0x80表示只含有pts，取值0xc0表示含有pts和dts
                        pes data length	            1B	后面数据的长度，取值5或10
                        pts	                        5B	33bit值
                        dts	                        5B	33bit值
                     */
                    if (payload_unit_start_indicator == 1) { //pay_load_unit_start_indicator = 1
                        this.block[tsWriterIdx] = 0;
                        tsWriterIdx++;
                        this.block[tsWriterIdx] = 0;
                        tsWriterIdx++;
                        this.block[tsWriterIdx] = 1;
                        tsWriterIdx++;                      //start code，固定为0x000001

                        this.block[tsWriterIdx] = videoStreamID;
                        tsWriterIdx++;                      //stream id

                        int pesDataLength = ptsDtsFlag != 0 ? 10 : 5;


                        int pesPacketLength = unReadLenOfAllNaluLength + pesDataLength + 3;

                        if (pesPacketLength >= 65536) log.warn("toolong: {}", pesPacketLength);
                        this.block[tsWriterIdx] = (byte) ((pesPacketLength >> 8) & 0xFF);
                        tsWriterIdx++;

                        this.block[tsWriterIdx] = (byte) (pesPacketLength & 0xFF);
                        tsWriterIdx++;
                        //pesPacketLength

                        this.block[tsWriterIdx] = (byte) 0x84;
                        tsWriterIdx++;                      //flag
                        this.block[tsWriterIdx] = (byte) (ptsDtsFlag != 0 ? 0xC0 : 0x80);
                        tsWriterIdx++;                      //flag

                        this.block[tsWriterIdx] = (byte) pesDataLength;
                        tsWriterIdx++;                      //pes data length

                        tempPts = pts;
                        this.block[tsWriterIdx + 4] = (byte) (int) (((tempPts & 0x7F) << 1) + 1L);
                        tempPts >>= 7;
                        this.block[tsWriterIdx + 3] = (byte) (int) (tempPts & 0xFF);
                        tempPts >>= 8;
                        this.block[tsWriterIdx + 2] = (byte) (int) (((tempPts & 0x7F) << 1) + 1L);
                        tempPts >>= 7;
                        this.block[tsWriterIdx + 1] = (byte) (int) (tempPts & 0xFF);
                        tempPts >>= 8;
                        this.block[tsWriterIdx] = (byte) (int) (((tempPts & 0x7) << 1) + 1L + (ptsDtsFlag != 0 ? 48 : 32));
                        tsWriterIdx += 5;
                        if (ptsDtsFlag != 0) {
                            tempDts = dts;
                            this.block[tsWriterIdx + 4] = (byte) (int) (((tempDts & 0x7F) << 1) + 1L);
                            tempDts >>= 7;
                            this.block[tsWriterIdx + 3] = (byte) (int) (tempDts & 0xFF);
                            tempDts >>= 8;
                            this.block[tsWriterIdx + 2] = (byte) (int) (((tempDts & 0x7F) << 1) + 1L);
                            tempDts >>= 7;
                            this.block[tsWriterIdx + 1] = (byte) (int) (tempDts & 0xFF);
                            tempDts >>= 8;
                            this.block[tsWriterIdx] = (byte) (int) (((tempDts & 0x7) << 1) + 1L + (ptsDtsFlag != 0 ? 16 : 32));
                            tsWriterIdx += 5;
                        }
                        payload_unit_start_indicator = 0;
                    }

                    int currentTsPacketFragmentLeftBits; //当前取出的tsPacketFragment还剩多少没读
                    while (true) {
                        if (tsWriterIdx >= TS_PACKETLEN) {
                            break;
                        }

                        if (currentLoopReadPayloadLen >= unReadLenOfAllNaluLength || readLenOfAllNaluLength >= allNaluLength) {
                            break;
                        }

                        currentTsPacketFragmentLeftBits = len - pesPayloadWritten;

                        if (currentTsPacketFragmentLeftBits == 0) {
                            if (naluStartCodeAndNaluList.size() > 0) {
                                tsPacket = naluStartCodeAndNaluList.remove(0);
                                offset = tsPacket.getOffset();
                                len = tsPacket.getLen();
                                buff = tsPacket.getBuffer();
                                pesPayloadWritten = 0;
                                currentTsPacketFragmentLeftBits = len;
                            }
                        }

                        int currentLoopNeedWriteBytes = 0;
                        if (currentTsPacketFragmentLeftBits > currentTsBlockCanWritablePayloadLength) {
                            currentLoopNeedWriteBytes = currentTsBlockCanWritablePayloadLength;
                        } else {
                            currentLoopNeedWriteBytes = currentTsPacketFragmentLeftBits;
                        }

                        if (currentLoopNeedWriteBytes > 0) {
                            try {
                                System.arraycopy(buff, offset + pesPayloadWritten, this.block, tsWriterIdx, currentLoopNeedWriteBytes);
                            } catch (Exception e) {
                                log.warn("something wrong");
                            }
                            tsWriterIdx += currentLoopNeedWriteBytes;

                            pesPayloadWritten += currentLoopNeedWriteBytes;

                            currentLoopReadPayloadLen += currentLoopNeedWriteBytes;

                            readLenOfAllNaluLength += currentLoopNeedWriteBytes;

                            currentTsBlockCanWritablePayloadLength -= currentLoopNeedWriteBytes;
                        }
                    }

                    writer.nextBlock(dts, this.block); //ts90 这里暂时无用
                    isFirstVideoPacket = false;
                    if (currentLoopReadPayloadLen >= unReadLenOfAllNaluLength || readLenOfAllNaluLength >= allNaluLength) {
                        break;
                    }
                }

                if (readLenOfAllNaluLength >= allNaluLength) {
                    break;
                }
            }
        } else {
            log.debug("video data is not h264/avc!");
        }

    }

    /**
     * 处理音频
     * <p>
     * SoundFormat       UB [4]
     * SoundRate         UB [2]
     * SoundSize         UB [1]
     * SoundType         UB [1]
     * AACPacketType     IF SoundFormat == 10 UI8
     *
     * @param data 数据
     */
    public void handleAudio(AudioData data) {
        BufFacade dataBuff = data.getData().asReadOnly();
        dataBuff.markReaderIndex();
        int dataLen = dataBuff.readableBytes();
        byte[] dataBytes = new byte[dataLen];
        dataBuff.readBytes(dataBytes);
        dataBuff.resetReaderIndex();

        byte firstByte = dataBuff.readByte();


        int codecId = FLVUtils.getAudioCodec(firstByte);//SoundFormat
        audioCodec = codecId;

        int SoundRate = (firstByte >> 2) & 0x03;
        int SoundSize = (firstByte >> 1) & 0x01;
        int SoundType = firstByte & 0x01;


        byte AACPacketType = dataBuff.readByte();


        if (codecId == AudioCodec.AAC.getId() && AACPacketType == 0) {
            AACFrame tempFrame = AACUtils.decodeAACCodecConfig(dataBuff);
            if (aacFrame == null && tempFrame != null) {
                aacFrame = tempFrame;
            }
            if (tempFrame == null) log.error("audio error configure:{}", dataBuff);
        } else if ((codecId == AudioCodec.AAC.getId() || codecId == AudioCodec.MP3.getId()) && aacFrame != null) {
            long ts = data.getTimestamp() * TIME_SCALE;
            long incTs;
            long fixTs;
            int interval = -1;
            // fix low-resolution timestamp in RTMP to MPEG-TS
            if (codecId == AudioCodec.AAC.getId()) {
                if (lastAACSampleRate == -1 || lastAACSampleRate != aacFrame.getSampleRate()) {
                    lastAACSampleRate = this.aacFrame.getSampleRate();
                    //这是如何计算的
                    lastAACTimeCode = Math.round(data.getTimestamp() / 1000.0D * lastAACSampleRate);
                } else {
                    incTs = lastAACTimeCode + aacFrame.getSampleCount();
                    fixTs = Math.round(incTs * 1000.0D / lastAACSampleRate);
                    interval = (int) Math.abs(fixTs - data.getTimestamp());
                    if (interval <= 1) {
                        ts = Math.round(incTs * 90000L / lastAACSampleRate);
                        lastAACTimeCode = incTs;
                    } else {
                        lastAACTimeCode = Math.round(data.getTimestamp() / 1000.0D * lastAACSampleRate);
                    }
                }

                // aacFrame size = 7 byte(adts header) + AudioTagsSize - 2 byte(AudioTagHeaderSize)
                int adtsHeader = aacFrame.isErrorBitsAbsent() ? 7 : 9;
                int AudioTagsSize = dataBytes.length;
                int AudioTagHeaderSize = 2;
                int AACFrame = AudioTagsSize - AudioTagHeaderSize;
                aacFrame.setSize(adtsHeader + AACFrame);
                byte[] adts = new byte[7];
                AACUtils.frameToADTSBuffer(aacFrame, adts, 0);

                waitingAudio.fragments.add(new TSPacketFragment(adts, 0, adts.length));
                waitingAudio.size += adts.length;

                waitingAudio.fragments.add(new TSPacketFragment(dataBytes, 2, AACFrame));
                waitingAudio.size += AACFrame;
                waitingAudio.codec = codecId;
            } else if (codecId == AudioCodec.MP3.getId()) {
                try {
                    if (mp3HeaderBuf == null) {
                        mp3HeaderBuf = new byte[4];
                        mp3HeaderData = new MP3HeaderData();
                    }
                    System.arraycopy(dataBytes, 1, mp3HeaderBuf, 0, 4);
                    int syncData = MP3BufferedDecoder.syncHeader((byte) 0, mp3HeaderBuf, mp3HeaderData);
                    if (syncData != 0) {
                        MP3BufferedDecoder.decodeHeader(syncData, 0, mp3HeaderData);
                        int sampleCount = MP3BufferedDecoder.samples_per_frame(this.mp3HeaderData);
                        int sampleRate = MP3BufferedDecoder.frequency(mp3HeaderData);

                        if (lastMP3SampleRate == -1 || lastMP3SampleRate != sampleRate) {
                            lastMP3SampleRate = sampleRate;
                            lastMP3TimeCode = Math.round(data.getTimestamp() * lastMP3SampleRate / 1000.0D);
                        } else {
                            incTs = lastMP3TimeCode + sampleCount;
                            fixTs = Math.round(incTs * 1000.0D / lastMP3SampleRate);
                            interval = (int) Math.abs(fixTs - data.getTimestamp());
                            if (interval <= 1) {
                                ts = Math.round(incTs * 90000L / lastMP3SampleRate);
                                lastMP3TimeCode = incTs;
                            } else {
                                lastMP3TimeCode = Math.round(data.getTimestamp() * lastMP3SampleRate / 1000.0D);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("mp3 header parse fail: {}", e.toString());
                }

                waitingAudio.fragments.add(new TSPacketFragment(dataBytes, 1, dataBytes.length - 1));
                waitingAudio.size += dataBytes.length - 1;
                waitingAudio.codec = codecId;
            }

            waitingAudio.count += 1;
            if (waitingAudio.timecode == -1L) waitingAudio.timecode = ts;
            waitingAudio.lastTimecode = ts;
            if (waitingAudio.count >= this.mpegtsAudioGroupCount) {
                lastAudioTimeCode = waitingAudio.timecode;
                writeAudioPackets(waitingAudio);
                waitingAudio.clear();
            }
        }
    }

    /**
     * write mult audio packets
     *
     * @param waitingAudio
     * @throws IOException
     */
    private void writeAudioPackets(WaitingAudio waitingAudio) {

        int size = waitingAudio.size;
        long ts = waitingAudio.timecode;
        long ts90 = waitingAudio.timecode;
        int ptdDtsFlag = 0;
        int totalWritten = 0;
        TSPacketFragment packetFragment = (TSPacketFragment) waitingAudio.fragments.remove(0);
        int writtenLen = 0;
        int offset = packetFragment.getOffset();
        int len = packetFragment.getLen();
        byte[] data = packetFragment.getBuffer();
        int stt = 1; // Payload unit start indicator.
        while (true) {

            int pos = 0;
            block[pos] = SYNCBYTE;
            pos++;
            block[pos] = (byte) ((stt != 0 ? 64 : 0) + (0x1F & audioPID >> 8));
            pos++;
            block[pos] = (byte) (audioPID & 0xFF);
            pos++;
            if (audioCCounter == -1L) audioCCounter = 1L;
            else audioCCounter += 1L;
            block[pos] = (byte) (int) (16L + (audioCCounter & 0xF));
            pos++;
            int pesHeaderLen = 0;
            if (stt != 0) pesHeaderLen = 9 + (ptdDtsFlag != 0 ? 10 : 5); // pes header len
            int count = TS_PACKETLEN - pos - pesHeaderLen;
            if (count > size - totalWritten) count = size - totalWritten;
            int total = 0;
            int pesLen;
            if (pos + count + pesHeaderLen < TS_PACKETLEN) { // 当数据不够188个字节时 需要用自适应区填满
                total = TS_PACKETLEN - (pos + count + pesHeaderLen);
                int thirdByte = 3;
                block[thirdByte] = (byte) (block[thirdByte] | 0x20);
                if (total > 1) {
                    total--;
                    block[pos] = (byte) (total & 0xFF);
                    pos++;
                    block[pos] = 0;
                    pos++;
                    total--;
                    if (total > 0) System.arraycopy(TransportStreamUtils.FILL, 0, block, pos, total);
                    pos += total;
                } else {
                    block[pos] = 0;
                    pos++;
                }
            }
            if (stt != 0) {
                // packet_start_code_prefix 0x000001
                block[pos] = 0x00;
                pos++;
                block[pos] = 0x00;
                pos++;
                block[pos] = 0x01;
                pos++;
                block[pos] = audioStreamID; // stream_id
                pos++;
                total = ptdDtsFlag != 0 ? 10 : 5;
                pesLen = size + total + 3;
                BufferUtil.intToByteArray(pesLen, block, pos, 2); // PES_packet_length
                pos += 2;
                block[pos] = (byte) 0x80;
                pos++;
                block[pos] = (byte) (ptdDtsFlag != 0 ? 0xC0 : 0x80);
                pos++;
                block[pos] = (byte) total;
                pos++;
                block[pos + 4] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
                ts >>= 7;
                block[pos + 3] = (byte) (int) (ts & 0xFF);
                ts >>= 8;
                block[pos + 2] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
                ts >>= 7;
                block[pos + 1] = (byte) (int) (ts & 0xFF);
                ts >>= 8;
                block[pos] = (byte) (int) (((ts & 0x7) << 1) + 1L + (ptdDtsFlag != 0 ? 48 : 32));
                pos += 5;
                if (ptdDtsFlag != 0) {
                    block[pos + 4] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
                    ts >>= 7;
                    block[pos + 3] = (byte) (int) (ts & 0xFF);
                    ts >>= 8;
                    block[pos + 2] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
                    ts >>= 7;
                    block[pos + 1] = (byte) (int) (ts & 0xFF);
                    ts >>= 8;
                    block[pos] = (byte) (int) (((ts & 0x7) << 1) + 1L + 32L);
                    pos += 5;
                }
            }
            while (true) {
                total = count;
                if (total > len - writtenLen) total = len - writtenLen;
                System.arraycopy(data, offset + writtenLen, block, pos, total);
                writtenLen += total;
                pos += total;
                totalWritten += total;
                count -= total;
                if (writtenLen >= len) {
                    writtenLen = 0;
                    if (waitingAudio.fragments.size() > 0) {
                        packetFragment = (TSPacketFragment) waitingAudio.fragments.remove(0);
                        offset = packetFragment.getOffset();
                        len = packetFragment.getLen();
                        data = packetFragment.getBuffer();
                    }
                }
                if (pos >= TS_PACKETLEN || totalWritten >= size) break;
            }
            stt = 0;
            writer.nextBlock(ts90, block);
            if (totalWritten >= size) break;
        }
    }

    /**
     * add pat pmt
     *
     * @return
     */
    public void addPAT(long ts) {
        fillPAT(block, 0, patCCounter);
        this.writeNextBlock(ts);
        fillPMT(block, 0, patCCounter, videoPID, audioPID, videoCodecToStreamType(videoCodec), audioCodecToStreamType(audioCodec));
        this.writeNextBlock(ts);
        this.patCCounter++;
    }

    private void writeNextBlock(long ts) {
        writer.nextBlock(ts, this.block);
        this.block = new byte[TS_PACKETLEN];
    }

    private long getPCRTimeCode() {
        long ts = -1L;
        if ((lastAudioTimeCode >= 0L) && (lastVideoTimeCode >= 0L)) {
            ts = Math.min(lastAudioTimeCode, lastVideoTimeCode); //都存在取最小
        } else if (lastAudioTimeCode >= 0L) {
            ts = lastAudioTimeCode; //取audio
        } else if (lastVideoTimeCode >= 0L) {
            ts = lastVideoTimeCode; //取video
        }

        if ((lastPCRTimeCode != -1L) && (ts < lastPCRTimeCode)) {
            ts = lastPCRTimeCode;  //已经初始化后 应该大于等于前一个  否认取前值
        }

        if (ts < 0L) {
            ts = 0L;   //修正
        }

        if (ts >= pcrBufferTime) {
            ts -= pcrBufferTime; //修正
        }

        this.lastPCRTimeCode = ts;
        return ts;
    }

    public long getLastPCRTimeCode() {
        return lastPCRTimeCode;
    }

    public void setLastPCRTimeCode(long lastPCRTimeCode) {
        this.lastPCRTimeCode = lastPCRTimeCode;
    }

    public long getVideoCCounter() {
        return videoCCounter;
    }

    public int getVideoCodec() {
        return videoCodec;
    }

    public int getAudioCodec() {
        return audioCodec;
    }

    private int calculatorTsPayloadLength(int tsWriterIdx, int pesHeaderLen, int unReadLenOfAllNaluLength, int currentLoopReadPayloadLen) {
        int tsPayloadLength;
        tsPayloadLength = TS_PACKETLEN - tsWriterIdx - pesHeaderLen;
        if (tsPayloadLength > unReadLenOfAllNaluLength - currentLoopReadPayloadLen) {
            tsPayloadLength = unReadLenOfAllNaluLength - currentLoopReadPayloadLen;
        }

        return tsPayloadLength;
    }

    /**
     * @author pengliren
     */
    class WaitingAudio {

        long timecode = -1L;
        long lastTimecode = -1L;
        int count = 0;
        int size = 0;
        int codec = 0;
        List<TSPacketFragment> fragments = new ArrayList<TSPacketFragment>();

        WaitingAudio() {
        }

        public void clear() {
            timecode = -1L;
            lastTimecode = -1L;
            count = 0;
            size = 0;
            fragments.clear();
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public int size() {
            return size;
        }
    }
}
