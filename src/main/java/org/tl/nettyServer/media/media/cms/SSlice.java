package org.tl.nettyServer.media.media.cms;

import java.util.ArrayList;
import java.util.List;

public class SSlice {
    public int		mionly;		  //0 表示没被使用，大于0表示正在被使用次数
    public long	msliceRange;  //切片时长
    public int		msliceLen;    //切片大小
    public long	msliceIndex;  //切片序号
    public long	msliceStart;  //切片开始时间戳
    public List<TsChunkArray> marray = new ArrayList<>();	  //切片数据
}
