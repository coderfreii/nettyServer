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

package org.tl.nettyServer.media.net.rtmp.event;


import org.tl.nettyServer.media.event.IEvent;
import org.tl.nettyServer.media.event.IEventListener;
import org.tl.nettyServer.media.net.rtmp.message.Header;
import org.tl.nettyServer.media.stream.message.Releasable;

public interface IRTMPEvent extends IEvent {

    public byte getDataType();

    public void setSource(IEventListener source);

    public Header getHeader();

    public void setHeader(Header header);

    public int getTimestamp();

    public void setTimestamp(int timestamp);

    public byte getSourceType();

    public void setSourceType(byte sourceType);

    public void retain();

}
