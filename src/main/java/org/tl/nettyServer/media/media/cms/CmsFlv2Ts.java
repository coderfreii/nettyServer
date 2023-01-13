package org.tl.nettyServer.media.media.cms;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.conf.ExtConfiguration;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.flv.IKeyFrameDataAnalyzer;
import org.tl.nettyServer.media.media.mp4.MP4Service;
import org.tl.nettyServer.media.service.IStreamableFileService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.tl.nettyServer.media.media.cms.CmsTsChunk.TS_CHUNK_SIZE;

public class CmsFlv2Ts {

    public static boolean isVideo = false;
    public static boolean isAudio = false;
    public static boolean	mFAFlag = true;	//是否读到首帧音频
    public static boolean	mFVFlag = true;	//是否读到首帧视频(SPS/PPS)
    public static boolean   needHandle = true;
    public static boolean   mbFIFrame = false;  //首个I帧，用来做切片的参考 = true;


    public static byte VideoTypeAVCKey = 0x17;
    public static byte VideoTypeAVC = 0x07;
    public static byte VideoTypeHEVCKey = 0x1A;
    public static byte VideoTypeHEVC = 0x0A;


    public static CMux mMux = new CMux();

    public static void main(String[] args) throws IOException {
        IStreamableFileService service = new MP4Service();//new MP3Service();
        ITagReader reader = null;
        IStreamableFile streamFile;
        File file = new File("D:\\demo_file\\3.mp4");
        streamFile = service.getStreamableFile(file);
        reader = streamFile.getReader();
        IKeyFrameDataAnalyzer.KeyFrameMeta keymeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();

        while (reader.hasMoreTags()) {
            ITag s = reader.readTag();
            isVideo = ITag.TYPE_VIDEO == s.getDataType();
            isAudio = ITag.TYPE_AUDIO == s.getDataType();

            int uiTimestamp =  s.getTimestamp();
            int  mtimeStamp = 0; //读到的上一帧的时间戳，有时时间戳重置了会用到

            BufFacade dataBuff = s.getBody().asReadOnly();
            dataBuff.markReaderIndex();
            int dataLen = dataBuff.readableBytes();
            byte[] mData = new byte[dataLen];
            dataBuff.readBytes(mData);
            dataBuff.resetReaderIndex();
            String frameType = "";
            if (isAudio) {
                frameType = "A";

                if ((mData[0] & 0xF0) >> 4 == 10)
                {
                    mMux.setAudioType((byte) 0x0f);
                }
                else if ((mData[0] & 0xF0) >> 4 == 2)
                {
                    mMux.setAudioType((byte) 0x03);
                    mFAFlag = true;
                }
                if (!mFAFlag)
                {
                    needHandle = false;
                }
            }else if (isVideo)
            {
                frameType = "P";
                if (mData[0] == VideoTypeAVCKey/*0x17*/)
                {
                    frameType = "I";
                }
                if (mData.length > 0 && frameType.equalsIgnoreCase("I") && mbFIFrame == false) {
                    mbFIFrame = true;
                    //log.debug("[slice][OnTime]Found The First Key Frame !!!DataLen=%d", s.miDataLen);
                }
                if (!mFVFlag)
                {
                    needHandle = false;
                    // logs.error("[slice][OnTime] Never Found SPS And PPS!!! %s", murl.c_str());
                }
            }
            if (needHandle) {
                pushData(s, frameType, s.getTimestamp());
                mtimeStamp =  s.getTimestamp();
            }
        }

    }
    /*输入一帧TS数据
    -- inbuf 送入的数据缓冲区
    -- length 数据大小
    -- framtype	帧类型(用于切片)
    -- timestamp 时间戳
    */
    public static int	msliceCount = 0;    //切片计数
    public static long	msliceIndx = 0;     //当前切片的序号
    public static int	mtsSaveNum = ExtConfiguration.HLS_SEGMENT_MAX;     //缓存保留的切片个数 = 0;
    public static int	mtsDuration = ExtConfiguration.HLS_SEGMENT_TIME;    //单个切片时长
    public static List<SSlice> msliceList = new ArrayList<>(); //切片列表
    public static TsChunkArray mlastTca;//节省空间
    public static String murl;		//拼接用的URL
    public static int		mtotalMemSize = 0; //ts总内存大小
    public static int		mtotalDataSize = 0;//ts有效数据大小
    public static int		mthreadID = 0;//ts有效数据大小


    public static int pushData(ITag s, String frameType, long timestamp)
    {

        long  mbTime = 0;		//最后一个切片的生成时间
        long  mmemTick = System.currentTimeMillis();
        long tt = System.currentTimeMillis();
        SSlice ss = null;
        if (
                /*&& (msliceList.get(msliceCount).msliceStart != 0)
                && timestamp - msliceList.get(msliceCount).msliceStart > (long)(mtsDuration * 900)*/
                 (frameType.equalsIgnoreCase("I") || (mFVFlag == false && frameType.equalsIgnoreCase("A")))) {
            long now = System.currentTimeMillis();
            mbTime = now;
            if (msliceCount + 1 > /*mtsNum+*/mtsSaveNum){
                //要加上追加的长度
                if (mlastTca != null)
                {
                    ss = msliceList.get(msliceCount);
                    if (ss != null)
                    {
                        ss.msliceLen += mlastTca.mchunkTotalSize;
                    }
                }
                System.out.println(String.format("[CMission::pushData] 1 %s pushData one ts succ, " +
                                "count=%d, " +
                                "msliceList.size=%u, " +
                                "frameType=%c," +
                                "cur timestamp=%lu, " +
                                "start timestamp=%lu, " +
                                "timestamp=%lu",
                        murl,
                        msliceCount,
                        msliceList.size(),
                        frameType,
                        timestamp,
                        msliceList.get(msliceCount).msliceStart,
                        timestamp - msliceList.get(msliceCount).msliceStart));

                msliceList.get(msliceCount).msliceRange = ((timestamp - msliceList.get(msliceCount).msliceStart));

                if (!msliceList.get(msliceCount).marray.isEmpty())
                {
                    mtotalDataSize += msliceList.get(msliceCount).marray.get(0).mchunkTotalSize;
                    mtotalMemSize += msliceList.get(msliceCount).marray.get(0).mtotalMemSize;
                }

                msliceIndx++;
                ss = newSSlice();
                ss.msliceIndex = msliceIndx;
                ss.msliceStart = timestamp;
                mMux.packPSI();

                mlastTca = allocTsChunkArray();
                CmsTsChunk.writeChunk(mthreadID, mlastTca,mMux.getPAT(),0, TS_CHUNK_SIZE);
                CmsTsChunk.writeChunk(mthreadID, mlastTca,mMux.getPMT(),0, TS_CHUNK_SIZE);
                BufFacade dataBuff = s.getBody().asReadOnly();
                dataBuff.markReaderIndex();
                int dataLen = dataBuff.readableBytes();
                byte[] mData = new byte[dataLen];
                dataBuff.readBytes(mData);
                dataBuff.resetReaderIndex();

                mMux.onData(mlastTca,mData,mData.length, frameType, timestamp);

                ss.marray.add(mlastTca);
                msliceList.add(ss);

                ss = msliceList.get(0);
                if (!ss.marray.isEmpty())
                {
                    mtotalDataSize -= ss.marray.get(0).mchunkTotalSize;
                    mtotalMemSize -= ss.marray.get(0).mtotalMemSize;
                }
                msliceList.remove(ss);
            } else {
                //要加上追加的长度
                if (mlastTca != null)
                {
                    ss = msliceList.get(msliceCount);
                    if (ss != null)
                    {
                        ss.msliceLen += mlastTca.mchunkTotalSize;
                    }
                }
                System.out.println(String.format("[CMission::pushData] 1 %s pushData one ts succ, ",
                        "count=%d, ",
                        "msliceList.size=%u, ",
                        "frameType=%c,",
                        "cur timestamp=%lu, ",
                        "start timestamp=%lu, ",
                        "timestamp=%lu",
                        murl,
                        msliceCount,
                        msliceList.size(),
                        frameType,
                        timestamp,
                        msliceList.get(msliceCount).msliceStart,
                        timestamp - msliceList.get(msliceCount).msliceStart));
                msliceList.get(msliceCount).msliceRange =((timestamp - msliceList.get(msliceCount).msliceStart));

                if (!msliceList.get(msliceCount).marray.isEmpty())
                {
                    mtotalDataSize += msliceList.get(msliceCount).marray.get(0).mchunkTotalSize;
                    mtotalMemSize += msliceList.get(msliceCount).marray.get(0).mtotalMemSize;
                }

                msliceCount++;
                msliceIndx++;
                ss = newSSlice();
                ss.msliceIndex = msliceIndx;
                ss.msliceStart = timestamp;
                mMux.packPSI();

                mlastTca = allocTsChunkArray();
                CmsTsChunk.writeChunk(mthreadID, mlastTca,mMux.getPAT(),0, TS_CHUNK_SIZE);
                CmsTsChunk.writeChunk(mthreadID, mlastTca,mMux.getPMT(),0, TS_CHUNK_SIZE);

                BufFacade dataBuff = s.getBody().asReadOnly();
                dataBuff.markReaderIndex();
                int dataLen = dataBuff.readableBytes();
                byte[] mData = new byte[dataLen];
                dataBuff.readBytes(mData);
                dataBuff.resetReaderIndex();

                mMux.onData(mlastTca, mData, mData.length, frameType, timestamp);

                ss.marray.add(mlastTca);
                msliceList.add(ss);
            }
        }else{
            /*ss = msliceList.get(msliceCount);
            if (ss.msliceStart == 0)
            {
                ss.msliceStart = timestamp;
            }*/

            BufFacade dataBuff = s.getBody().asReadOnly();
            dataBuff.markReaderIndex();
            int dataLen = dataBuff.readableBytes();
            byte[] mData = new byte[dataLen];
            dataBuff.readBytes(mData);
            dataBuff.resetReaderIndex();

            mMux.onData(mlastTca,mData, mData.length, frameType, timestamp);
        }
        //时间大于1秒的 放入队列
        /*if (tt - mmemTick > 1000)
        {
            makeOneTaskHlsMem(mhash,
                    mtotalMemSize + (mlastTca ? mlastTca.mtotalMemSize : 0),
                    mtotalDataSize + (mlastTca ? mlastTca.mchunkTotalSize : 0));
            mmemTick = tt;
        }*/
        return 0;
    }

    public static SSlice newSSlice()
    {
        SSlice ss = new SSlice();
        ss.mionly = 1;
        ss.msliceRange = 0;  //切片时长
        ss.msliceLen = 0;    //切片大小
        ss.msliceIndex = 0;  //切片序号
        ss.msliceStart = 0;  //切片开始时间戳
        return ss;
    }
    public static TsChunkArray allocTsChunkArray()
    {
        TsChunkArray tca = new TsChunkArray();
        tca.mchunkTotalSize = 0;
        tca.mtotalMemSize = 0;
        return tca;
    }

}