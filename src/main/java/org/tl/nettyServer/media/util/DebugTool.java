package org.tl.nettyServer.media.util;

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.net.rtmp.message.Packet;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试工具(检查时间戳)
 *
 * @author TL
 * @date 2022/12/21
 */
@Slf4j
public class DebugTool {
    private boolean disabled = true;

    protected List<IRTMPEvent> irtmpEvents = new ArrayList<>();
    AudioData last = null;
    private int check = 40;

    public void setCheck(int check) {
        this.check = check;
    }

    public void addPacket(Packet packet) {
        IRTMPEvent message = packet.getMessage();
        if (message instanceof AudioData) {
            if (last != null) {
                int i = message.getTimestamp() - last.getTimestamp();
                if (i > check) {
                    log.debug(i + "");
                } else {
                    log.trace(i + "");
                }
            }
            irtmpEvents.add(message);
            last = (AudioData) message;
        }
    }
}
