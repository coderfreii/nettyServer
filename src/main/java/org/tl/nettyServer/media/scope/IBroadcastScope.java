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
package org.tl.nettyServer.media.scope;

import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.stream.client.IClientBroadcastStream;

/**
 * Broadcast scope is marker interface that represents object that works as basic scope and has pipe connection event dispatching capabilities.
 * 广播作用域是表示作为基本作用域的对象的标记接口，具有管道连接事件调度功能。
 */
public interface IBroadcastScope extends IBasicScope, IPipe {
	
	public static final String STREAM_ATTRIBUTE = TRANSIENT_PREFIX + "_publishing_stream";
	
    public IClientBroadcastStream getClientBroadcastStream();

    public void setClientBroadcastStream(IClientBroadcastStream clientBroadcastStream);

}
