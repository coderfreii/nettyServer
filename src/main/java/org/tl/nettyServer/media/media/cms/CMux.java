package org.tl.nettyServer.media.media.cms;


import org.tl.nettyServer.media.media.mpeg.MpegUtil;
import org.tl.nettyServer.media.util.BufferUtil;

import static org.tl.nettyServer.media.media.cms.CmsTsChunk.TS_CHUNK_SIZE;

public class CMux {
    public CMux() {
    }

    /*public CMux(int threadID){
        mthreadID = threadID;
        mheadFlag = false;
        minfoTag = false;

        mheadBufDL = 0;
        mTagSize = -1;
        mTagSizeBufDL = 0;
        mTagBufDLen = 0;

        mamcc = 0;
        mvmcc = 0;
        mpatmcc = 0;
        mpmtmcc = 0;
        mAstreamType = 0x0f;
    }*/

    public void init(int threadID) {
        mthreadID = threadID;
        mheadFlag = false;
        minfoTag = false;

        mheadBufDL = 0;
        mTagSize = -1;
        mTagSizeBufDL = 0;
        mTagBufDLen = 0;

        mamcc[0] = 0;
        mvmcc[0] = 0;
        mpatmcc[0] = 0;
        mpmtmcc[0] = 0;
        mAstreamType = 0x0f;
    }

    public void reset() {
    }

    public void release() {
    }

    public int tag2Ts(byte[] inBuf, int inLen, byte[] outBuf, int outLen, byte outType, long outPts) {
        return 0;
    }

    public static int parseAAC(byte[] inBuf, int index, int inLen, byte[] outBuf, int[] outLen) {

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

            /*int aacFrameLength = inLen - 2 + 7; //这里是带上ADTS头7字节的全帧长度，不是帧数据长度
            outBuf[0] = (byte) 0xFF;
            outBuf[1] = (byte) 0xF1;
            outBuf[2] = (byte) (((mAudioSpecificConfig.ObjectType - 1) << 6) |
                    ((mAudioSpecificConfig.SamplerateIndex & 0x0F) << 2) |
                    (mAudioSpecificConfig.Channels >>> 7));
            outBuf[3] = (byte) ((mAudioSpecificConfig.Channels << 6) | ((aacFrameLength & 0x1800) >>> 11));
            outBuf[4] = (byte) ((aacFrameLength & 0x7f8) >>> 3);
            outBuf[5] = (byte) (((aacFrameLength & 0x7) << 5) | 0x1f);
            outBuf[6] = (byte) (0xFC | 0x00);
            MpegUtil.memcpy(outBuf, 7, inBuf, index + 2, inLen - 2);
            outLen[0] = inLen + 5;*/


            return 0;
        } else {
            int aacFrameLength = inLen - 2 + 7; //这里是带上ADTS头7字节的全帧长度，不是帧数据长度
            outBuf[0] = (byte) 0xFF;
            outBuf[1] = (byte) 0xF1;
            outBuf[2] = (byte) (((mAudioSpecificConfig.ObjectType - 1) << 6) |
                    ((mAudioSpecificConfig.SamplerateIndex & 0x0F) << 2) |
                    (mAudioSpecificConfig.Channels >>> 7));
            outBuf[3] = (byte) ((mAudioSpecificConfig.Channels << 6) | ((aacFrameLength & 0x1800) >>> 11));
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
    /*public int parseAAC(byte[] inBuf, int inLen, byte[] out, int[] outLen) {
        out = new byte[inLen + 128];
        MpegUtil.memset(out, 0, (byte) 0, inLen + 128);
        byte[] outBuf = out;
        outLen[0] = 0;
        if (inBuf[1] == 0x00) { //AACPacketType为0x00说明是AAC sequence header
            mAudioSpecificConfig.ObjectType = (byte) (inBuf[2] >>> 3);
            mAudioSpecificConfig.SamplerateIndex = (byte) ((inBuf[2] & 0x07) << 1 | inBuf[3] >>> 7);
            mAudioSpecificConfig.Channels = (byte) ((inBuf[3] << 1) >>> 4);
            mAudioSpecificConfig.FramLengthFlag = (byte) ((inBuf[3] & 4) >>> 2);
            mAudioSpecificConfig.DependOnCCoder = (byte) ((inBuf[3] & 2) >>> 1);
            mAudioSpecificConfig.ExtensionFlag = (byte) (inBuf[3] & 1);

            if (mAudioSpecificConfig.ExtensionFlag > 0) {

            }
            mAACFLag = false;
            outBuf = null;// xfree(outBuf);
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
		*//*
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
				NumOfRawDataBlockInFrame//2*//*

            MpegUtil.memcpy(outBuf, 7, inBuf, 2, inLen - 2);
            outLen[0] = inLen + 5;
            return 0;
        }
    }*/

    public static int parseNAL(byte[] inBuf, int inBufStart, int inLen, byte[] outBuf, int[] outLen) {

        //MpegUtil.memset(outBuf, 0, (byte) 0, inLen + 256);
        outLen[0] = 0;
        int NALlen = 0;
        byte curNALtype = 0;

        //var NALtype byte = 0
        if ((inBuf[inBufStart + 1] & 0xFF) == 0x00) { //AVCPacketType为0x00说明是SPS/PPS
            int pos = 0;
            mSpsNalLen = 0;
            mPpsNalLen = 0;
            /*configurationVersion := inBuf[5]
            AVCProfileIndication := inBuf[6]
            profile_compatibility := inBuf[7]
            AVCLevelIndication := inBuf[8]
            lengthSizeMinusOne := inBuf[9]*/

            //<- 非常重要，是 H.264 视频中 NALU 的长度，计算方法是 1 + (lengthSizeMinusOne & 3)
            int numOfSequenceParameterSets = (inBuf[inBufStart + 10] & 0x1F);              //<- SPS 的个数，计算方法是 numOfSequenceParameterSets & 0x1F
            //11,12位为sps的长度
            int sequenceParameterSetLength = (inBuf[inBufStart + 11] & 0xFF) << 8 | (inBuf[inBufStart + 12] & 0xFF); // <- SPS 的长度

            for (int i = 0; i < numOfSequenceParameterSets; i++) {
                mSpsNal[mSpsNalLen] = 0x00;
                mSpsNal[mSpsNalLen + 1] = 0x00;
                mSpsNal[mSpsNalLen + 2] = 0x00;
                mSpsNal[mSpsNalLen + 3] = 0x01;
                MpegUtil.memcpy(mSpsNal, mSpsNalLen + 4, inBuf, inBufStart + 13 + i * sequenceParameterSetLength, sequenceParameterSetLength);
                mSpsNalLen += (4 + sequenceParameterSetLength);
            }

            pos = 13 + sequenceParameterSetLength * numOfSequenceParameterSets;

            int numOfPictureParameterSets = (inBuf[inBufStart + pos]);                        //<- PPS 的个数
            int pictureParameterSetLength = (inBuf[inBufStart + pos + 1]) << 8 | (inBuf[inBufStart + pos + 2]); //<- PPS 的长度
            for (int i = 0; i < numOfPictureParameterSets; i++) {
                mPpsNal[mPpsNalLen] = 0x00;
                mPpsNal[mPpsNalLen + 1] = 0x00;
                mPpsNal[mPpsNalLen + 2] = 0x00;
                mPpsNal[mPpsNalLen + 3] = 0x01;
                MpegUtil.memcpy(mPpsNal, mPpsNalLen + 4, inBuf, inBufStart + pos + 3 + i * pictureParameterSetLength, pictureParameterSetLength);
                mPpsNalLen += (4 + pictureParameterSetLength);
            }


            return 0;
        } else if (inBuf[inBufStart + 1] == 0x01) { //AVCPacketType为slice
            for (int i = 5; i < inLen - 4; ) {

                NALlen = BufferUtil.byteArrayToInt(inBuf, inBufStart+i, 4); // MpegUtil.byteToInt(inBuf,inBufStart+i);//inBuf[inBufStart + i]) << 24 | (inBuf[inBufStart + i + 1]) << 16 |  (inBuf[inBufStart + i + 2]) << 8 |  (inBuf[inBufStart + i + 3]);
                if (NALlen < 0) {
                    System.out.println("[slice][ParserNAL] parse NALlen ERROR: %d" + NALlen);

                    break;
                }
                curNALtype = (byte) (inBuf[inBufStart + i + 4] & 0x1f);
                if (curNALtype > 0 && curNALtype < 7 /*&& curNALtype != 6*/) {
                    if (NALlen + i + 4 > inLen) {
                        NALlen = inLen - 4 - i;
                    }

                    outBuf[outLen[0]] = 0x00;
                    outBuf[outLen[0] + 1] = 0x00;
                    outBuf[outLen[0] + 2] = 0x01;

                    if (NALlen > 655356) {
                        System.out.println(String.format("[slice][ParserNAL] A BIG NAL outLen NALlen  curNALtype [%d %d %d]\n", outLen, NALlen, curNALtype));
                        System.out.println(String.format("[slice][ParserNAL] inLen NALlen inBuf[i-5] inBuf[i-4] inBuf[i-3] inBuf[i-2] inBuf[i-1] inBuf[i] inBuf[i+1] %d %d 0x%x 0x%x 0x%x 0x%x 0x%x 0x%x 0x%x",
                                inLen, NALlen, inBuf[inBufStart + i - 5], inBuf[inBufStart + i - 4], inBuf[inBufStart + i - 3], inBuf[inBufStart + i - 2], inBuf[inBufStart + i - 1], inBuf[inBufStart + i], inBuf[inBufStart + i + 1]));
                    }
                    MpegUtil.memcpy(outBuf, outLen[0] + 3, inBuf, inBufStart + i + 4, NALlen);
                    //outBuf.put(inb);
                    outLen[0] += (NALlen + 4);
                } else {
                    if (curNALtype > 6 && curNALtype < 13) {

                    } else {
                        System.out.println("[slice][ParserNAL] unusual NAL curNALtype %d" + curNALtype);
                    }
                    if (curNALtype == 7) {
                        mSpsNalLen = 0;
                        mSpsNal[mSpsNalLen] = 0x00;
                        mSpsNal[mSpsNalLen + 1] = 0x00;
                        mSpsNal[mSpsNalLen + 2] = 0x00;
                        mSpsNal[mSpsNalLen + 3] = 0x01;
                        MpegUtil.memcpy(mSpsNal, mSpsNalLen + 4, inBuf, inBufStart + i + 4, NALlen);
                        mSpsNalLen += (4 + NALlen);
                    } else if (curNALtype == 8) {
                        mPpsNalLen = 0;
                        mPpsNal[mPpsNalLen] = 0x00;
                        mPpsNal[mPpsNalLen + 1] = 0x00;
                        mPpsNal[mPpsNalLen + 2] = 0x00;
                        mPpsNal[mPpsNalLen + 3] = 0x01;
                        MpegUtil.memcpy(mPpsNal, mPpsNalLen + 4, inBuf, inBufStart + i + 4, NALlen);
                        mPpsNalLen += (4 + NALlen);

                    }
                }
                i += (NALlen + 4);
            }
            return 0;
        }
        outBuf = null;//xfree(outBuf);
        return 0;
	/*
	 FLV中的NAL和TS中的NAl不一样，TS中是由 0x00 0x00 0x00 0x01分割
	*/
	/*
			 Nalu_type:
				0x67 (0 11 00111) SPS  	非常重要       	type = 7
				0x68 (0 11 01000) PPS  	非常重要       	type = 8
				0x65 (0 11 00101) IDR帧 	关键帧  非常重要 	type = 5
				0x41 (0 10 00001) P帧   	重要         	type = 1
				0x01 (0 00 00001) B帧   	不重要        	type = 1
				0x06 (0 00 00110) SEI   不重要        	type = 6
	*/
    }

    public int packTS(byte[] inBuf, int inLen, byte framType, byte pusi, byte afc, byte[] mcc, char pid, int timeStamp, byte[] outBuf, int outLen) {
        return 0;
    }

        public void onData(TsChunkArray tca, byte[] inBuf, int inLen, String framType, long timestamp) {
        byte[] StartCode = {0x00, 0x00, 0x00, 0x01, 0x09, (byte) 0xf0};
        int StartCodeLen = 6;
        byte[] NalDivide = {0x00, 0x00, 0x00, 0x01};
        int NalDivideLen = 4;
        int PcrLen = 6;

        //基本属性设置
        byte PcrFlag = 0;
        byte RadomAFlag = 0;
        byte DTSFlag = 0;
        boolean StartCodeFlag = false;
        boolean SpsPpsFlag = false;

        byte[] data = null;
        int[] dataLen = {0};
        int[] pesLen = {0};
        int[] StuffLen = {0};
        byte[] mcc = mvmcc;
        char pid = Vpid;

        int PackRemain = 0;

        long dts = (timestamp) * 90;
        long cts = (inBuf[2] & 0xFF) << 16 | (inBuf[3] & 0xFF) << 8 | (inBuf[4] & 0xFF); //偏移量cts

        if (framType.equalsIgnoreCase("I")) {
            PcrFlag = 1;
            RadomAFlag = 1;
            DTSFlag = 1;
            StartCodeFlag = true;
            SpsPpsFlag = true;

            parseNAL(inBuf,0, inLen,data, dataLen);
            if (dataLen[0] <= 0) {
                System.out.println("[OnData] GetSlice Failure");
                return;
            }
            pesLen[0] += PcrLen + mSpsNalLen + mPpsNalLen + StartCodeLen + dataLen[0];

        } else if (framType.equalsIgnoreCase("A")) {
            RadomAFlag = 1;
            cts = 0;
            mcc[0] = mamcc[0];
            pid = Apid;
            if (mAstreamType == 0x0f) {
                parseAAC(inBuf,0, inLen, data, dataLen);
            } else {
                dataLen[0] = inLen - 1;
                data = new byte[dataLen[0]];
                MpegUtil.memcpy(data, 0, inBuf, 1, dataLen[0]);
            }

            if (dataLen[0] <= 0) {
                return;
            }
            pesLen[0] += dataLen[0];
        } else {
            DTSFlag = 1;
            StartCodeFlag = true;

            parseNAL(inBuf,0, inLen, data, dataLen);
            if (dataLen[0] <= 0) {
                System.out.println("[OnData] GetSlice Failure");
                return;
            }
            pesLen[0] += StartCodeLen + dataLen[0];
        }

        //写PES头
        long pts = dts + cts * 90;
        byte[] PESHead = new byte[64];//= { 0 };
        int PESHeadLen = 0;
        pesHeadPack(DTSFlag, pts, dts, dataLen[0] * (~(DTSFlag & 0x01)), PESHead, PESHeadLen);
        pesLen[0] += PESHeadLen;
        byte pusi = 1;
        byte afc = 0x03;
        if (pesLen[0] < 182) {
            StuffLen[0] = 184 - pesLen[0];
        }
        byte[] tsHead = new byte[TS_CHUNK_SIZE];// = { 0 };
        int tsHeadLen = 0;
        tsHeadPack(afc, pusi, mcc, pid, PcrFlag, RadomAFlag, StuffLen, timestamp, tsHead, tsHeadLen); //打包首个TS头
        pusi = 0;
        StuffLen[0] = 0;
        RadomAFlag = 0;
        PcrFlag = 0;
        afc = 0x01;
        PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, tsHead, 0, tsHeadLen);
        PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, PESHead, 0, PESHeadLen);

        //写适应apple的起始码
        if (StartCodeFlag) {
            PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, StartCode, 0, StartCodeLen);
        }

        //写SPS/PPS
        if (SpsPpsFlag) {
            if (PackRemain > mSpsNalLen) {
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, mSpsNal, 0, mSpsNalLen);
            } else { //当SPS大于一个ts包长度
                int totalWriteLen = PackRemain;
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, mSpsNal, 0, PackRemain);
                for (; totalWriteLen < mSpsNalLen; ) {
                    tsHeadLen = 0;
                    tsHeadPack(afc, pusi, mcc, pid, PcrFlag, RadomAFlag, StuffLen, timestamp, tsHead, tsHeadLen);
                    PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, tsHead, 0, tsHeadLen);
                    int writeLen = PackRemain;
                    if (mSpsNalLen - totalWriteLen < writeLen/*184*/) {
                        writeLen = mSpsNalLen - totalWriteLen;
                        System.out.println("[CMux::onData] writeLen = mSpsNalLen - totalWriteLen");
                    }
                    PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, mSpsNal, totalWriteLen, writeLen);
                    totalWriteLen += writeLen;
                }
            }

            if (PackRemain > mPpsNalLen) {
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, mPpsNal, 0, mPpsNalLen);
            } else { //当PPS大于一个ts包长度
                int totalWriteLen = PackRemain;
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, mPpsNal, 0, PackRemain);
                for (; totalWriteLen < mPpsNalLen; ) {
                    tsHeadPack(afc, pusi, mcc, pid, PcrFlag, RadomAFlag, StuffLen, timestamp, tsHead, tsHeadLen);
                    PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, tsHead, 0, tsHeadLen);
                    int writeLen = PackRemain;
                    if (mPpsNalLen - totalWriteLen < writeLen/*184*/) {
                        writeLen = mSpsNalLen - totalWriteLen;
                        System.out.println("[CMux::onData] writeLen = mSpsNalLen - totalWriteLen");
                    }
                    PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, mPpsNal, totalWriteLen, writeLen);
                    totalWriteLen += writeLen;
                }
            }
        }

        //写NAL分隔符
        if (false && !framType.equalsIgnoreCase("A")) {
            if (PackRemain > NalDivideLen) {
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, NalDivide, 0, NalDivideLen);
            } else {
                int writeTotalLen = PackRemain;
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, NalDivide, 0, PackRemain);
                tsHeadPack(afc, pusi, mcc, pid, PcrFlag, RadomAFlag, StuffLen, timestamp, tsHead, tsHeadLen);
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, tsHead, 0, tsHeadLen);
                PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, NalDivide, writeTotalLen, NalDivideLen - writeTotalLen);
            }

        }

        //写数据
        int start = PackRemain;
        if (PackRemain > 0 && PackRemain < TS_CHUNK_SIZE) {
            PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, data, 0, PackRemain);
        } else {
            System.out.println(String.format("[OnData] PackRemain, dataLen %d-%d", PackRemain, dataLen));
        }

        for (; start < dataLen[0]; ) {
            if (dataLen[0] - start < 184) {
                StuffLen[0] = 184 - (dataLen[0] - start);
                afc = 0x03;
            }
            tsHeadPack(afc, pusi, mcc, pid, PcrFlag, RadomAFlag, StuffLen, timestamp, tsHead, tsHeadLen);
            PackRemain = CmsTsChunk.writeChunk(mthreadID, tca, tsHead, 0, tsHeadLen);
            int writeLen = PackRemain;

            if ((dataLen[0] - start) < 184 && tsHeadLen + (dataLen[0] - start) != TS_CHUNK_SIZE) {
                System.out.println(String.format("[OnData] LENERROR dataLen - start,tsHeadLen, StuffLen %d-%d-%d", dataLen[0] - start, tsHeadLen, StuffLen));
            }
            if (writeLen > (dataLen[0] - start)) {
                System.out.println(String.format("[OnData]WRITEERROR writeLen,dataLen - start,tsHeadLen, StuffLen %d-%d-%d-%d", writeLen, dataLen[0] - start, tsHeadLen, StuffLen));
                //writeLen = dataLen - start
            }

            CmsTsChunk.writeChunk(mthreadID, tca, data, start, writeLen);
            start += writeLen;
        }
        if (data != null) {
            data = null;//xfree(data);
        }
    }

    public void packPSI() {
        int[] PATlen = {0};
        int[] PMTlen = {0};
        byte[] tPAT = null;
        byte[] tPMT = null;
        //PAT
        byte[] head = {0, 0, 0, 0, 0}; //byte head[5] = { 0 };

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
        CmsTs.setPAT(PMTpid, tPAT,  PATlen);
        System.out.println("方法内初始化tPAT" + tPAT.length);
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
        CmsTs.setPMT(Apid, Vpid, PCRpid, mAstreamType, tPMT, PMTlen);
        MpegUtil.memcpy(mPMT, 0, head, head.length);
        MpegUtil.memcpy(mPMT, 5, tPMT, PMTlen[0]);
        tPMT = null;//xfree(tPMT);
        for (int pos = PMTlen[0] + 5; pos < TS_CHUNK_SIZE; pos++) {
            mPMT[pos] = (byte) 0xFF;
        }
    }

    public byte[] getPAT() {
        return mPAT;
    }

    ;

    public byte[] getPMT() {
        return mPMT;
    }

    ;

    public void setAudioType(byte a) {
        mAstreamType = a;
    }

    ;


    int packPES(byte[] inBuf, int inLen, byte framType, int timeStamp, byte[] outBuf, int outLen) {
        return 0;
    }

    void setPcr(long DTS, byte[] outBuf, int outLen) {
    }

    void tsHeadPack(byte afc, byte pusi, byte[] mcc, char pid, byte PcrFlag, byte RadomAFlag, int[] StuffLen, long timeStamp, byte[] outBuf, int outLen) {
        boolean StuffFlag = false;
        if (StuffLen[0] > 0) {
            StuffFlag = true;
        }

        long DTS = timeStamp * 90; //flv中记录的时间戳是DTS(TS中的时间是FLV中的1/90)

        outLen = 0;
        int[] headlen = {outLen};
        byte[] head = outBuf;
        int adaptationFiledLength = 0;

        byte synByte = 0x47;                   //同步字节, 固定为,表示后面的是一个TS分组
        byte transportErrorIndicator = 0;      //传输误码指示符
        byte payloadUnitStartIndicator = pusi; //有效荷载单元起始指示符, 1标明为负载起始包
        byte transport_priority = 0;           //传输优先, 1表示高优先级,传输机制可能用到，解码用不着
        char PID = pid;                       //PID = 0x100
        byte transportScramblingControl = 0;   //传输加扰控制(加密)
        byte adaptionFieldControl = afc;       //自适应控制 01仅含有效负载，10仅含调整字段，11都有
        byte continuityCounter = (byte) mcc[0];         //连续计数器 一个4bit的计数器，范围0-15

        head[0] = synByte;
        head[1] = (byte) (transportErrorIndicator << 7 | payloadUnitStartIndicator << 6 | transport_priority << 5 | (byte) ((PID << 3) >>> 11));
        head[2] = (byte) (PID);
        head[3] = (byte) (transportScramblingControl << 6 | adaptionFieldControl << 4 | continuityCounter);
        headlen[0] += 4;

        if (adaptionFieldControl > 1) {
            byte discontinuityIndicator = 0;            //不连续状态指示
            byte randomAccessIndicator = RadomAFlag;    //
            byte elementaryStreamPriorityIndicator = 0; //为1当前分组有较高优先级,有多个负载在会用到（可以不考虑）
            byte pcrFlag = PcrFlag;                   //pcr标志   pes包的前面 （待定）
            byte opcrFlag = 0;                         //（待定）
            byte splicingPointFlag = 0;                //拼接点（貌似没用）
            byte transportPrivateDataFlag = 0;         //私用数据字节（可以不考虑）
            byte adaptationFiledExtension = 0;         //扩展自适应字段
            head[headlen[0] + 1] = (byte) (discontinuityIndicator << 7 | randomAccessIndicator << 6 | elementaryStreamPriorityIndicator << 5 | pcrFlag << 4 | opcrFlag << 3 | splicingPointFlag << 2 | transportPrivateDataFlag << 1 | adaptationFiledExtension);
            adaptationFiledLength += 2;

            if (pcrFlag == 1) {
                byte[] pcrbuf = null;
                int[] pcrLen = new int[1];
                pcrLen[0] = 0;
                CmsTs.setPcr(DTS, pcrbuf, pcrLen);
                MpegUtil.memcpy(head, headlen[0] + adaptationFiledLength, pcrbuf, pcrLen[0]);
                adaptationFiledLength += 6;
                // 48 bit
            }

            if (StuffFlag) { //需要填充
                StuffLen[0] = StuffLen[0] - 2;
                for (int i = headlen[0] + adaptationFiledLength; StuffLen[0] > 0 && i < StuffLen[0] + adaptationFiledLength; i++) {
                    head[i] = (byte) 0xFF;
                }
                adaptationFiledLength += StuffLen[0];
            }
        }
        head[headlen[0]] = (byte) (adaptationFiledLength - 1);
        headlen[0] += adaptationFiledLength;
        mcc[0] = (byte) (((mcc[0] + 1) % 16)&0xFF);

        return;
    }

    void pesHeadPack(byte DTSFlag, long pts, long dts, int dataLen, byte[] outBuf, int outLen) {
        outLen = 0;
        byte[] head = outBuf;
        int headlen = outLen;
        head[0] = 0;
        head[1] = 0;
        head[2] = 1;
        head[3] = (byte) 0xE0;

        head[4] = 0x00;
        head[5] = 0x03;
        headlen += 6;

        byte PDflags = (byte) (0x02 | DTSFlag);
        if (DTSFlag < 1) {
            head[3] = (byte) 0xC0;
        }

        byte fixBit = 0x02;              //2 填充字节
        byte PESscramblingControl = 0;   //2 PES有效负载的加密模式。0表示不加密，其余表示用户自定义。
        byte PESpriority = 0;            //1 PES数据包的优先级
        byte dataAlignmentIndicator = 0; //1 为时，表明此分组头部之后紧跟着数据流描述子中定义的访问单元类型
        byte copyright = 0;              //1 版权，表示有版权
        byte originalOrCopy = 0;         //1 pos= 6 1表示原始数据，0表示备份
        head[6] = (byte) (fixBit << 6 | PESscramblingControl << 4 | PESpriority << 3 | dataAlignmentIndicator << 2 | copyright << 1 | originalOrCopy);

        byte PTSDTSflags = PDflags;      //2 10表示含有PTS字段，11表示含有PTS和DTS字段，00表示不含有pts_dts和DTS。
        byte ESCRflag = 0;               //1 1表示ESCR在PES首部出现，0表示不出现
        byte ESrateFlag = 0;             //1 1表示PES分组含有ES_rate字段。0表示不含有。
        byte DSMtrickModeFlag = 0;       //1 1表示有位的trick_mode_flag字段，0表示不出现此字段。只对DSM有效。
        byte additionalCopyInfoFlag = 0; //1 1表示有copy_info_flag字段，0表示不出现此字段。
        byte PESCRCflag = 0;             //1 1表示PES分组中有CRC字段，0表示不出现此字段。
        byte PESextensionFlag = 0;       //1 pos= 7 1表示扩展字段在PES包头存在，0表示扩展字段不存在

        head[7] = (byte) (PTSDTSflags << 6 | ESCRflag << 5 | ESrateFlag << 4 | DSMtrickModeFlag << 3 | additionalCopyInfoFlag << 2 | PESCRCflag << 1 | PESextensionFlag);
        head[8] = 0; //表示可选字段和填充字段所占的字节数。

        headlen += 3;

        if ((PTSDTSflags & 0x02) > 0) {
            byte[] PTSbuf = new byte[5];
            PTSbuf[0] = (byte) (PTSDTSflags << 4 | ((pts & 0x1C0000000l) >>> 29) | 0x01);    //pts&0x111000000 00000000 00000000 00000000
            PTSbuf[1] = (byte) ((pts & 0x3fc00000) >>> 22);                                //pts&0x 00111111 11000000 00000000 00000000
            PTSbuf[2] = (byte) ((byte) ((pts & 0x3f8000) >>> 14) | 0x01);                        //pts&0x 00000000 00111111 10000000 00000000
            PTSbuf[3] = (byte) ((pts & 0x7f80) >>> 7);                                    //pts&0x 00000000 00000000 01111111 10000000
            PTSbuf[4] = (byte) ((byte) (pts << 1) | 0x01);                                        //pts&0x 00000000 00000000 00000000 01111111

            head[8] += 5;
            MpegUtil.memcpy(head, headlen, PTSbuf, PTSbuf.length);
            headlen += 5;

        }
        if ((PTSDTSflags & 0x01) > 0) {
            byte[] DTSbuf = new byte[5];
            DTSbuf[0] = (byte) (0x01 << 4 | (byte) ((dts & 0x1C0000000l) >>> 29) | 0x01);
            DTSbuf[1] = (byte) ((dts & 0x3fc00000) >>> 22);
            DTSbuf[2] = (byte) ((byte) ((dts & 0x3f8000) >>> 14) | 0x01);
            DTSbuf[3] = (byte) ((dts & 0x7f80) >>> 7);
            DTSbuf[4] = (byte) ((byte) (dts << 1) | 0x01);

            head[8] += 5;
            MpegUtil.memcpy(head, headlen, DTSbuf, DTSbuf.length);
            headlen += 5;
        }

        if (dataLen > 0) {
            int packLen = headlen + dataLen;
            head[4] = (byte) ((packLen - 6) >>> 8);
            head[5] = (byte) (packLen - 6);
        } else {
            head[4] = 0;
            head[5] = 0;
        }
        return;
    }

    int mthreadID;

    boolean mheadFlag;  //收到FVL头
    boolean minfoTag;   //收到scriptTag
    SHead mhead;        //头信息
    static SDataInfo mscript = new SDataInfo();    //TAG信息
    public static STagInfo mcurTag = new STagInfo();    //scriptTag信息

    //byte		mSpsPpsNal[512]; //SpsPps
    //int		mSpsPpsNalLen;       //SpsPps的长度
    static byte[] mSpsNal = new byte[512]; //Sps
    static int mSpsNalLen;        //Sps的长度
    static byte[] mPpsNal = new byte[512]; //Pps
    static int mPpsNalLen;       //Pps的长度

    byte[] mheadBuf = new byte[9]; //FVL头缓存
    int mheadBufDL;     //FVL头缓存数据长度

    int mTagSize;
    byte[] mTagSizeBuf = new byte[8];
    int mTagSizeBufDL;

    byte[] mTagBuf = new byte[65536];
    int mTagBufDLen;

    public static byte[] mPAT = new byte[TS_CHUNK_SIZE];
    public static byte[] mPMT = new byte[TS_CHUNK_SIZE];
    public static byte[] mPCR = new byte[6];

    public static SAudioSpecificConfig mAudioSpecificConfig = new SAudioSpecificConfig();
    public static boolean mAACFLag;
    public static byte[] mamcc = new byte[1];
    public static byte[] mvmcc = {0};
    public static byte[] mpatmcc = new byte[1];
    public static byte[] mpmtmcc = new byte[1];
    public static byte mAstreamType;


    public static int PATpid = 0;
    public static char PMTpid = 0x1000;
    public static char Apid = 0x101;
    public static char Vpid = 0x100;
    public static char PCRpid = 0x100;
}
