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

package org.tl.nettyServer.media.service.consumer;

import org.tl.nettyServer.media.messaging.IMessageOutput;
import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.messaging.InMemoryPushPushPipe;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.stream.base.IClientStream;
import org.tl.nettyServer.media.stream.conn.IStreamCapableConnection;
import org.tl.nettyServer.media.stream.OutputStream;
import org.tl.nettyServer.media.stream.consumer.ConnectionConsumer;

/**
 * Basic consumer service implementation. Used to get pushed messages at consumer endpoint.
 */
public class ConsumerService implements IConsumerService {

    /** {@inheritDoc} */
    public IMessageOutput getConsumerOutput(IClientStream stream) {
        IStreamCapableConnection streamConn = stream.getConnection();
        if (streamConn != null && streamConn instanceof RTMPConnection) {
            RTMPConnection conn = (RTMPConnection) streamConn;
            // TODO Better manage channels.
            // now we use OutputStream as a channel wrapper.
            OutputStream o = conn.createOutputStream(stream.getStreamId());
            IPipe pipe = new InMemoryPushPushPipe();
            pipe.subscribe(new ConnectionConsumer(conn, o.getVideo(), o.getAudio(), o.getData()), null);
            return pipe;
        }
        return null;
    }

}
