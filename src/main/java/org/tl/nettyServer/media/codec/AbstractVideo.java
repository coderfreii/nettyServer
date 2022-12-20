package org.tl.nettyServer.media.codec;


import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.io.IoConstants;

import java.util.concurrent.CopyOnWriteArrayList;

public class AbstractVideo implements IVideoStreamCodec, IoConstants {

    /**
     * Current timestamp for the stored keyframe
     */
    protected int keyframeTimestamp;

    /**
     * Storage for key frames
     */
    protected final CopyOnWriteArrayList<FrameData> keyframes = new CopyOnWriteArrayList<>();

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean canDropFrames() {
        return false;
    }

    @Override
    public boolean canHandleData(BufFacade data) {
        return false;
    }

    @Override
    public boolean addData(BufFacade data) {
        return false;
    }

    @Override
    public boolean addData(BufFacade data, int timestamp) {
        return false;
    }

    @Override
    public BufFacade getDecoderConfiguration() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufFacade getKeyframe() {
        if (keyframes.isEmpty()) {
            return null;
        }
        return keyframes.get(0).getFrame();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FrameData[] getKeyframes() {
        return keyframes.toArray(new FrameData[0]);
    }

    @Override
    public int getNumInterframes() {
        return 0;
    }

    @Override
    public FrameData getInterframe(int idx) {
        return null;
    }

}
