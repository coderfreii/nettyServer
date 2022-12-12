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

package org.tl.nettyServer.servers.net.rtmp.handler.packet;

import org.tl.nettyServer.servers.net.rtmp.codec.RTMP;
import org.tl.nettyServer.servers.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.servers.net.rtmp.message.Packet;
import org.tl.nettyServer.servers.net.rtmp.session.SessionFacade;

/**
 * RTMP events handler
 */
public interface IRtmpPacketHandler {
    public void messageReceived(SessionFacade conn, Packet packet) throws Exception;

    public void messageSent(SessionFacade conn, Packet packet);

    public void connectionClosed(SessionFacade conn);

    public void connectionClosed(SessionFacade conn, RTMP state);
}
