package org.tl.nettyServer.media.media.cms;


import org.tl.nettyServer.media.media.mpeg.MpegUtil;

public class CmsTsChunk {
    public static int TS_CHUNK_SIZE = 188;
    public static int TS_SLICE_LEN  =(TS_CHUNK_SIZE*100);
    public static int writeChunk(int idx, TsChunkArray tca, byte[] data,int index, int len)
    {
        //返回一个ts剩余长度
        int left = 0;
        TsChunk tc = null;
        if (tca.mtsChunkArray.isEmpty())
        {
            tc = allocTsChunk(idx, TS_SLICE_LEN);
            tca.mtsChunkArray.add(tc);
            tca.mtotalMemSize += TS_SLICE_LEN;
        }
        else
        {
            tc = tca.mtsChunkArray.get(tca.mtsChunkArray.size() - 1);
            if (tc.mchunkSize - tc.muse < len)
            {
                assert((tc.mchunkSize - tc.muse) % TS_CHUNK_SIZE == 0);
                tc = allocTsChunk(idx, TS_SLICE_LEN);
                tca.mtsChunkArray.add(tc);
                tca.mtotalMemSize += TS_SLICE_LEN;
            }
        }
        left = TS_CHUNK_SIZE - (tc.muse%TS_CHUNK_SIZE);
        assert(len <= left);
        int writeLen = cmsMin(left, len);
        MpegUtil.memcpy(tc.mdata,tc.muse, data,index, writeLen);
        tc.muse += writeLen;
        tca.mchunkTotalSize += writeLen;
        return left - writeLen;
    }
    public static TsChunk allocTsChunk(int idx, int chunkSize)
    {
        TsChunk tc = new TsChunk();
        tc.midxMem = idx;

        tc.mchunkSize = chunkSize;
/*#ifdef __CMS_POOL_MEM__
        tc.mdata = (char*)mallocTsSlice(idx);
#else*/
        tc.mdata = new byte[chunkSize];

        tc.muse = 0;
        return tc;
    }
    public static int cmsMin(int a,int b){
      return   (((a) < (b)) ? (a) : (b));
    }
}
