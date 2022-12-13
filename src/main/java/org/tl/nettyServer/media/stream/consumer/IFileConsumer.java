package org.tl.nettyServer.media.stream.consumer;

import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;

public interface IFileConsumer {

    void setAudioDecoderConfiguration(IRTMPEvent audioConfig);

    void setVideoDecoderConfiguration(IRTMPEvent videoConfig);

}
