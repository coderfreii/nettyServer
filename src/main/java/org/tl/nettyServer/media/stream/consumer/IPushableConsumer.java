
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

package org.tl.nettyServer.media.stream.consumer;

import org.tl.nettyServer.media.messaging.IMessage;
import org.tl.nettyServer.media.messaging.IPipe;

import java.io.IOException;

/**
 * A consumer that supports event-driven message handling and message pushing through pipes.
 * 支持事件驱动消息处理和消息推送管道的使用者。
 *
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IPushableConsumer extends IConsumer {

    public static final String KEY = IPushableConsumer.class.getName();

    /**
     * 推送消息
     * <p>
     * 对 message 做修改则需要复制一份
     *
     * @param pipe    管
     * @param message 消息
     * @throws IOException io exception
     */
    void pushMessage(IPipe pipe, IMessage message) throws IOException;
}
