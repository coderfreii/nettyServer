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

package org.tl.nettyServer.media.stream.base;

import org.tl.nettyServer.media.stream.conn.IStreamCapableConnection;

/**
 * A stream that is bound to a client.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IClientStream extends IStream {

    public static final String MODE_READ = "read";

    public static final String MODE_RECORD = "record";

    public static final String MODE_APPEND = "append";

    public static final String MODE_LIVE = "live";

    public static final String MODE_PUBLISH = "publish";
 
    Number getStreamId();
 
    IStreamCapableConnection getConnection();
 
    void setClientBufferDuration(int bufferTime);
 
    int getClientBufferDuration();
 
    void setBroadcastStreamPublishName(String streamName);
 
    String getBroadcastStreamPublishName();

}
