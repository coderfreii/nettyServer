package org.tl.nettyServer.media.media.cms;

import java.util.ArrayList;
import java.util.List;

public class TsChunkArray {
    public int mtotalMemSize;
    public int mchunkTotalSize;	//ts 有效数据长度
    public List<TsChunk> mtsChunkArray = new ArrayList<>();
}
