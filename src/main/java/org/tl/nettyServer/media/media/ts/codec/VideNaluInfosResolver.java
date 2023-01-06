package org.tl.nettyServer.media.media.ts.codec;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.media.flv.FLVUtils;
import org.tl.nettyServer.media.media.h264.H264Utils;
import org.tl.nettyServer.media.media.h264.H264CodecConfigParts;
import org.tl.nettyServer.media.media.ts.TSPacketFragment;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.util.BufferUtil;


import java.util.ArrayList;
import java.util.List;


/**
 * * Video data event
 * <p>
 * FrameType            UB[4]
 * CodecID              UB[4]             1
 * AVCPacketType        UI8               1
 * CompositionTime(cst) SI24              3         5                表示PTS相对于DTS的偏移值 ）
 * ConfigurationVersion UI8               1         6
 * AVCProfileIndication UI8               1         7
 * ProfileCompatibility UI8               1         8
 * AVCLevelIndication   UI8               1         9
 * Reserved             UB[6]
 * LengthSizeMinusOne   UB[2]             1         10
 * Reserved             UB[3]
 * Number of SPS NALUs  UB[5]                                       <=== here  11 byte
 */
@Data
@Slf4j
public class VideNaluInfosResolver {
    private H264CodecConfigParts h264CodecConfigPart;
    private int videoCodec;

    public static final int TIME_SCALE = 90;   //TODO:因为之后播放器要除以90 所以这里先乘以90？ 单位毫秒
    List<TSPacketFragment> naluStartCodeAndNaluList = new ArrayList<>();
    private int allNaluLength = 0;
    private int naluNum = 0;
    private long pts;
    private int ptsDtsFlag;
    private int FrameType;
    private long lastDts;
    private boolean continueTs = false;
    private boolean resolved = false;

    public void resolve(VideoData data) {
        BufFacade dataBuff = data.getData().asReadOnly();
        if (dataBuff.readableBytes() >= 2) {
            byte[] dataBytes = getDataBytesArr(dataBuff);
            int dataLen = dataBuff.readableBytes();
            long dts = data.getTimestamp() * TIME_SCALE;  //rtmp的时间戳是dts
            this.lastDts = dts;

            int firstByte = dataBuff.readByte();

            this.videoCodec = FLVUtils.getVideoCodec(firstByte);
            this.FrameType = FLVUtils.getFrameType(firstByte);

            //以下为video data

            int AVCPacketType = dataBuff.readByte();

            //AVCDecoderConfigurationRecord in case  h264CodecConfigPart is null
            if ((this.videoCodec == VideoCodec.AVC.getId()) && (AVCPacketType != 1)) {
                if (AVCPacketType == 0) {
                    dataBuff.readerIndex(0);
                    if (this.h264CodecConfigPart == null) {
                        this.h264CodecConfigPart = H264Utils.breakApartAVCC(dataBuff);
                        dataBuff.readerIndex(5);
                    }
                } else if (AVCPacketType == 2) {
                } else {
                    log.error("data is not valid");
                }
            } else if (this.videoCodec == VideoCodec.AVC.getId()) {   //1
                //第三个byte开始 读取三位为整数
                int CompositionTime = BufferUtil.byteArrayToInt(dataBytes, 2, 3);

                if (CompositionTime == 0) {
                    if (this.getFrameType() == 1) {
                        //System.out.println("I");
                    } else {
                        //System.out.println("P");
                    }
                } else {
                    //System.out.println("B");
                }

                //PTS = DTS + CTS
                int cts = CompositionTime * TIME_SCALE;  //cts 单位毫秒
                this.pts = dts + cts;

                this.ptsDtsFlag = 1;

                dataBuff.readerIndex(5);  //第六个byte

                int byteArrReaderIndex = 5;


                // h264 start code 0x00 0x01
                byte[] h264StartCode = new byte[4];
                h264StartCode[3] = 1;

                int naluLen;  //nalu的长度

                //序列参数集 SPS
                int sps = 0;
                //图像参数集 PPS
                int pps = 0;
                //分隔符
                int pd = 0;

                int naluLengthSize = 4;  //normal is 4 so make it default
                if (this.h264CodecConfigPart != null) {
                    naluLengthSize = this.h264CodecConfigPart.getNaluLengthSizeMinusOne() + 1;
                }

                while (byteArrReaderIndex + 4 <= dataLen) {
                    //读取byte的长度
                    naluLen = BufferUtil.byteArrayToInt(dataBytes, byteArrReaderIndex, naluLengthSize);  //读第6个byte
                    byteArrReaderIndex += 4;

                    //检查剩余字节数
                    if ((naluLen <= 0) || (byteArrReaderIndex + naluLen > dataLen)) break;

                    //读取naluType
                    int naluType = dataBytes[byteArrReaderIndex] & 0x1F;

                    //统计需要的类型   //如下几个条件暂时永远不会触发(说明没有？ 需要生成？)
                    if (naluType == 7) { // sps
                        sps = 1;
                    } else if (naluType == 8) { // pps
                        pps = 1;
                    } else if (naluType == 9) { // pd
                        pd = 1;
                    }

                    //构造Annex B格式的 h264
                    this.naluStartCodeAndNaluList.add(new TSPacketFragment(h264StartCode, 0, h264StartCode.length));
                    this.naluStartCodeAndNaluList.add(new TSPacketFragment(dataBytes, byteArrReaderIndex, naluLen));

                    //累加出Annex B格式的nalu的总长度
                    allNaluLength += (naluLen + h264StartCode.length);
                    //统计nalu的个数
                    naluNum++;
                    //移动指针
                    byteArrReaderIndex += naluLen;

                    //再次检查可读
                    if (byteArrReaderIndex >= dataLen) break;
                }

                int idx = 0;
                if (pd == 0) {
                    byte[] annex = new byte[6];
                    annex[3] = 0x01;
                    annex[4] = 0x09;
                    if (FrameType == 1) {
                        annex[5] = 16;
                    } else if (FrameType == 3) {
                        annex[5] = 80;
                    } else {
                        annex[5] = 48;
                    }
                    this.naluStartCodeAndNaluList.add(idx, new TSPacketFragment(annex, 0, annex.length));
                    idx++;
                    allNaluLength += annex.length;
                    naluNum++;
                    naluLen = 1;
                } else {
                    idx = 2;
                }

                // sps and pps
                if (FrameType == 1 && (pps == 0 || pd == 0)) {
                    //TODO: 这里为啥等于0执行
                    if (FrameType == 1 && sps == 0 && this.h264CodecConfigPart != null && this.h264CodecConfigPart.getSps() != null) {
                        byte[] h264SpsStartCode = new byte[4];
                        h264SpsStartCode[3] = 1;
                        this.naluStartCodeAndNaluList.add(idx, new TSPacketFragment(h264SpsStartCode, 0, h264SpsStartCode.length));
                        idx++;
                        this.naluStartCodeAndNaluList.add(idx, new TSPacketFragment(this.h264CodecConfigPart.getSps(), 0, this.h264CodecConfigPart.getSps().length));
                        idx++;
                        allNaluLength += (this.h264CodecConfigPart.getSps().length + h264SpsStartCode.length);
                        naluNum++;
                        naluLen = 1;
                    }

                    if (FrameType == 1 && pps == 0 && this.h264CodecConfigPart != null && this.h264CodecConfigPart.getPpss() != null) {
                        List<byte[]> ppss = this.h264CodecConfigPart.getPpss();
                        for (byte[] b : ppss) {
                            byte[] h264PpsStartCode = new byte[4];
                            h264PpsStartCode[3] = 1;
                            this.naluStartCodeAndNaluList.add(idx, new TSPacketFragment(h264PpsStartCode, 0, h264PpsStartCode.length));
                            idx++;
                            this.naluStartCodeAndNaluList.add(idx, new TSPacketFragment(b, 0, b.length));
                            idx++;
                            allNaluLength += (b.length + h264PpsStartCode.length);
                            naluNum++;
                            naluLen = 1;
                        }
                    }
                }
            }
            resolved = true;
            if (this.naluNum > 0) this.continueTs = true;
        } else {
            resolved = false;
        }
    }


    byte[] getDataBytesArr(BufFacade dataBuff) {
        dataBuff.markReaderIndex();
        int dataLen = dataBuff.readableBytes();
        byte[] dataBytes = new byte[dataLen];
        dataBuff.readBytes(dataBytes);
        dataBuff.resetReaderIndex();
        return dataBytes;
    }

}
