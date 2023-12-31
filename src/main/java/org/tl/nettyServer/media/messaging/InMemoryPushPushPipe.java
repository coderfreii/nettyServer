
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

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.buf.ReleaseUtil;
import org.tl.nettyServer.media.stream.consumer.IConsumer;
import org.tl.nettyServer.media.stream.consumer.IPushableConsumer;
import org.tl.nettyServer.media.stream.message.Duplicateable;
import org.tl.nettyServer.media.stream.message.RTMPMessage;
import org.tl.nettyServer.media.stream.message.Releasable;

import java.io.IOException;
import java.util.Map;

/**
 * A simple in-memory version of push-push pipe. It is triggered by an active provider to push messages through it to an event-driven consumer.
 * 推送管道的简单内存版本。它由活动提供者触发，将消息推送到事件驱动的使用者。
 *
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@Slf4j
public class InMemoryPushPushPipe extends AbstractPipe {

    public InMemoryPushPushPipe() {
        super();
    }

    public InMemoryPushPushPipe(IPipeConnectionListener listener) {
        this();
        addPipeConnectionListener(listener);
    }

    @Override
    public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {

        if (!(consumer instanceof IPushableConsumer)) {
            throw new IllegalArgumentException("Non-pushable consumer not supported by PushPushPipe");
        }

        boolean success = super.subscribe(consumer, paramMap);

        log.debug("Consumer subscribe{} {} params: {}", new Object[]{(success ? "d" : " failed"), consumer, paramMap});

        if (success) {
            fireConsumerConnectionEvent(consumer, PipeConnectionEvent.EventType.CONSUMER_CONNECT_PUSH, paramMap);
        }
        return success;

    }


    @Override
    public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
        boolean success = super.subscribe(provider, paramMap);

        log.debug("Provider subscribe{} {} params: {}", new Object[]{(success ? "d" : " failed"), provider, paramMap});

        if (success) {
            fireProviderConnectionEvent(provider, PipeConnectionEvent.EventType.PROVIDER_CONNECT_PUSH, paramMap);
        }
        return success;
    }


    public IMessage pullMessage() {
        return null;
    }


    public IMessage pullMessage(long wait) {
        return null;
    }

    /**
     * Pushes a message out to all the PushableConsumers.
     */
    public void pushMessage(IMessage message) throws IOException {
        log.debug("pushMessage: {} to {} consumers", message, consumers.size());
        if (consumers.size() == 0) {
            if (message instanceof Releasable) {
                ReleaseUtil.releaseAll(message);
            }
        }
        /* 把消息推给消费者 */
        for (IConsumer consumer : consumers) {
            try {
                if (message instanceof RTMPMessage) {
                    RTMPMessage rtmpMessage = Duplicateable.doDuplicate((RTMPMessage) message);
                    ((IPushableConsumer) consumer).pushMessage(this, rtmpMessage);
                } else {
                    ((IPushableConsumer) consumer).pushMessage(this, message);
                }
            } catch (Throwable t) {
                if (t instanceof IOException) {
                    throw (IOException) t;
                }
                log.error("Exception pushing message to consumer", t);
            }
        }
        if (message instanceof RTMPMessage) {
            ReleaseUtil.releaseAll(message);
            message = null;
        }
    }

}
