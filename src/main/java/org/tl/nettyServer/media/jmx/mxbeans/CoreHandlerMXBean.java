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

package org.tl.nettyServer.media.jmx.mxbeans;



import org.tl.nettyServer.media.client.IClient;
import org.tl.nettyServer.media.event.IEvent;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.scope.IBasicScope;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.call.IServiceCall;

import javax.management.MXBean;

/**
 * Base IScopeHandler implementation
 *
 * @author The Red5 Project
 */
@MXBean
public interface CoreHandlerMXBean {

    public boolean connect(IConnection conn, IScope scope);

    public boolean connect(IConnection conn, IScope scope, Object[] params);

    public void disconnect(IConnection conn, IScope scope);

    public boolean join(IClient client, IScope scope);

    public void leave(IClient client, IScope scope);

    public void removeChildScope(IBasicScope scope);

    public boolean serviceCall(IConnection conn, IServiceCall call);

    public boolean start(IScope scope);

    public void stop(IScope scope);

    public boolean handleEvent(IEvent event);

}
