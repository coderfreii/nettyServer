
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

package org.tl.nettyServer.media.messaging;

/**
 * A pipe is an object that connects message providers and message consumers. Its main function is to transport messages in kind of ways it provides.
 *
 * Pipes fire events as they go, these events are common way to work with pipes for higher level parts of server.
 * 管道是连接消息提供者和消息使用者的对象
 * 管道在触发事件时，这些事件是处理管道的常用方法
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IPipe extends IMessageInput, IMessageOutput {
 
    void addPipeConnectionListener(IPipeConnectionListener listener);
 
    void removePipeConnectionListener(IPipeConnectionListener listener);

}
