/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 *
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tl.nettyServer.media.stream.engin;


import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.stream.message.RTMPMessage;

import java.util.concurrent.TimeUnit;

/**
 * State machine for video frame dropping in live streams.
 * <p>
 * We start sending all frame types. Disposable interframes can be dropped any time without affecting the current state. If a regular interframe is dropped, all future frames up to the next keyframes are dropped as well. Dropped keyframes result in only keyframes being sent. If two consecutive keyframes have been successfully sent, regular interframes will be sent in the next iteration as well. If these frames all
 * went through, disposable interframes are sent again.
 *
 * <p>
 * So from highest to lowest bandwidth and back, the states go as follows:
 * <ul>
 * <li>all frames</li>
 * <li>keyframes and interframes</li>
 * <li>keyframes</li>
 * <li>keyframes and interframes</li>
 * <li>all frames</li>
 * </ul>
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class VideoFrameDropper implements IFrameDropper {

    private int state;

    /**
     * 丢包数
     */
    private int droppedPacketsCount;

    /**
     * 控制丢弃packet日志打印速率用
     */
    private long droppedPacketsCountLastLogTimestamp = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

    /**
     * 控制丢弃packet日志打印速率用
     */
    private long droppedPacketsCountLogInterval = 60 * 1000L;

    public VideoFrameDropper() {
        reset();
    }

    public void reset() {
        reset(SEND_ALL);
    }

    public void reset(int state) {
        this.state = state;
    }

    public boolean canSendPacket(RTMPMessage message, long pending) {
        IRTMPEvent packet = message.getBody();
        boolean result = true;
        // We currently only drop video packets.
        if (packet instanceof VideoData) {
            VideoData video = (VideoData) packet;
            VideoData.FrameType type = video.getFrameType();
            switch (state) {
                case SEND_ALL:
                    // All packets will be sent
                    break;
                case SEND_INTERFRAMES:
                    // Only keyframes and inter frames will be sent.
                    if (type == VideoData.FrameType.KEYFRAME) {
                        if (pending == 0) {
                            // Send all frames from now on.
                            state = SEND_ALL;
                        }
                    } else if (type == VideoData.FrameType.INTERFRAME) {
                    }
                    break;
                case SEND_KEYFRAMES:
                    // Only keyframes will be sent.
                    result = (type == VideoData.FrameType.KEYFRAME);
                    if (result && pending == 0) {
                        // Maybe switch back to SEND_INTERFRAMES after the next keyframe
                        state = SEND_KEYFRAMES_CHECK;
                    }
                    break;
                case SEND_KEYFRAMES_CHECK:
                    // Only keyframes will be sent.
                    result = (type == VideoData.FrameType.KEYFRAME);
                    if (result && pending == 0) {
                        // Continue with sending interframes as well
                        state = SEND_INTERFRAMES;
                    }
                    break;
                default:
            }
            if (result == false) {
                droppedPacketsCount++;
            }
        }
        return result;
    }

    public void dropPacket(RTMPMessage message) {
        IRTMPEvent packet = message.getBody();
        // Only check video packets.
        if (packet instanceof VideoData) {
            droppedPacketsCount++;
            VideoData video = (VideoData) packet;
            VideoData.FrameType type = video.getFrameType();
            switch (state) {
                case SEND_ALL:
                    if (type == VideoData.FrameType.DISPOSABLE_INTERFRAME) {
                        // Remain in state, packet is safe to drop.
                        return;
                    } else if (type == VideoData.FrameType.INTERFRAME) {
                        // Drop all frames until the next keyframe.
                        state = SEND_KEYFRAMES;
                        return;
                    } else if (type == VideoData.FrameType.KEYFRAME) {
                        // Drop all frames until the next keyframe.
                        state = SEND_KEYFRAMES;
                        return;
                    }
                    break;
                case SEND_INTERFRAMES:
                    if (type == VideoData.FrameType.INTERFRAME) {
                        // Drop all frames until the next keyframe.
                        state = SEND_KEYFRAMES_CHECK;
                        return;
                    } else if (type == VideoData.FrameType.KEYFRAME) {
                        // Drop all frames until the next keyframe.
                        state = SEND_KEYFRAMES;
                        return;
                    }
                    break;
                case SEND_KEYFRAMES:
                    // Remain in state.
                    break;
                case SEND_KEYFRAMES_CHECK:
                    if (type == VideoData.FrameType.KEYFRAME) {
                        // Switch back to sending keyframes, but don't move to
                        // SEND_INTERFRAMES afterwards.
                        state = SEND_KEYFRAMES;
                        return;
                    }
                    break;
                default:
            }
        }
    }

    @Override
    public void dropPacket(RTMPMessage message, Runnable log) {
        dropPacket(message);
        if (shouldLogPacketDrop()) {
            log.run();
        }
    }

    public void sendPacket(RTMPMessage message) {
    }


    private boolean shouldLogPacketDrop() {
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        if (now - droppedPacketsCountLastLogTimestamp > droppedPacketsCountLogInterval) {
            droppedPacketsCountLastLogTimestamp = now;
            return true;
        }
        return false;
    }

    public int getDroppedPacketsCount() {
        return droppedPacketsCount;
    }

    @Override
    public boolean canSendPacket(RTMPMessage message, long pending, Runnable r) {
        boolean b = canSendPacket(message, pending);
        if (!b && shouldLogPacketDrop()) {
            r.run();
        }
        return b;
    }
}
