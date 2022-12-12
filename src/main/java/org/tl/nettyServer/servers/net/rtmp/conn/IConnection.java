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

package org.tl.nettyServer.servers.net.rtmp.conn;

import org.tl.nettyServer.servers.net.rtmp.ICoreObject;
import org.tl.nettyServer.servers.net.rtmp.session.SessionFacade;

import java.util.Map;

/**
 * iconnection
 * The connection object.
 * <p>
 * * 每个连接都有一个关联的客户端和作用域。
 * * 连接可以是持久的、轮询的或暂时的。此接口的目的是提
 * * 供不同类型连接之间共享的基本连接方法。
 *
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 * @date 2022/12/12
 */
public interface IConnection extends ICoreObject {
    void setSession(SessionFacade sessionFacade);

    SessionFacade getSession();

    /**
     * Encoding type.
     */
    public static enum Encoding {
        AMF0, AMF3, WEBSOCKET, SOCKETIO, RTP, SRTP, RAW
    }

    /**
     * Duty type.
     */
    public static enum Duty {
        UNDEFINED, PUBLISHER, SUBSCRIBER, PROXY, REMOTING
    }

    /**
     * Connection type.
     */
    public static enum Type {
        PERSISTENT, // Persistent connection type, eg RTMP
        POLLING, // Polling connection type, eg RTMPT
        TRANSIENT, // Transient connection type, eg Remoting, HTTP, etc
        UNKNOWN // all others not matching known types
    }


    /**
     * Get the connection type.
     */
    public String getType(); // PERSISTENT | POLLING | TRANSIENT


    /**
     * Get the object encoding in use for this connection.
     */
    public Encoding getEncoding();

    /**
     * Get the duty for this connection; this is not meant nor expected to remain static.
     */
    public Duty getDuty();

    public void close();

    public Map<String, Object> getConnectParams();

    public String getSessionId();

    public long getReadBytes();

    public long getWrittenBytes();

    public long getReadMessages();

    public long getWrittenMessages();

    public long getDroppedMessages();

    public long getPendingMessages();

    public long getClientBytesRead();

    public void ping();

    public int getLastPingTime();

    public void setBandwidth(int mbits);

    public Number getStreamId();

    public void setStreamId(Number id);

    public String getProtocol();
}
