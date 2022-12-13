package org.tl.nettyServer.media.stream.consumer;


import org.tl.nettyServer.media.io.INettyTag;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;

import java.util.Comparator;

/**
 * Comparator for queued media data in file consumers.
 *
 * @author mondain
 */
public class QueuedMediaDataComparator implements Comparator<QueuedMediaData> {

    @Override
    public int compare(QueuedMediaData o1, QueuedMediaData o2) {
        int result = 0;
        // config data needs precedence over non-config
        byte type1 = o1.tag.getDataType();
        byte type2 = o2.tag.getDataType();
        if (type1 == type2) {
            byte[] buf1 = o1.tag.getBody().array();
            byte[] buf2 = o2.tag.getBody().array();
            if (type1 == INettyTag.TYPE_AUDIO) {
                // dont forget about silence!
                if (buf1.length > 0 && buf2.length > 0) {
                    // if audio, check codec config
                    if ((((buf1[0] & 0xff) & INettyTag.MASK_SOUND_FORMAT) >> 4) == AudioCodec.AAC.getId()) {
                        if (buf1[1] == 0 && buf2[1] != 0) {
                            result = -1;
                        } else if (buf1[1] != 0 && buf2[1] == 0) {
                            result = 1;
                        }
                    }
                }
            } else if (type1 == INettyTag.TYPE_VIDEO) {
                // if video, check codec config
                if (((buf1[0] & 0xff) & INettyTag.MASK_VIDEO_CODEC) == VideoCodec.AVC.getId()) {
                    if (buf1[1] == 0 && buf2[1] != 0) {
                        result = -1;
                    } else if (buf1[1] != 0 && buf2[1] == 0) {
                        result = 1;
                    }
                }
            }
            if (o1.tag.getTimestamp() > o2.tag.getTimestamp()) {
                result += 1;
            } else if (o1.tag.getTimestamp() < o2.tag.getTimestamp()) {
                result -= 1;
            }
        } else {
            if (o1.tag.getTimestamp() > o2.tag.getTimestamp()) {
                result = 1;
            } else if (o1.tag.getTimestamp() < o2.tag.getTimestamp()) {
                result = -1;
            }
        }
        return result;
    }

}