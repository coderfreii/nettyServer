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

package org.tl.nettyServer.media.stream.data;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.event.IRTMPEvent;
import org.tl.nettyServer.media.stream.message.RTMPMessage;

import java.io.IOException;

/**
 * Stream data packet
 */
public interface IStreamData<T> {

    /**
     * Getter for property 'data'.
     *
     * @return Value for property 'data'
     */
    public BufFacade getData();

    /**
     * Creates a byte accurate copy.
     *
     * @return duplicate of the current data item
     * @throws IOException            on error
     * @throws ClassNotFoundException on class not found
     */
    public IStreamData<T> duplicate() throws IOException, ClassNotFoundException;


    static IRTMPEvent doDuplicate(IRTMPEvent rtmpEvent) {
        if (rtmpEvent instanceof IStreamData) {
            try {
                rtmpEvent = (IRTMPEvent) ((IStreamData) rtmpEvent).duplicate();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return rtmpEvent;
    }


    static RTMPMessage doDuplicate(RTMPMessage rtmpMessage) {
        IRTMPEvent body = rtmpMessage.getBody();
        IRTMPEvent duplicate = doDuplicate(body);
        return RTMPMessage.build(duplicate);
    }

}
