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

package org.tl.nettyServer.servers.event;

/**
 * ievent接口是每个事件都应该实现的基本接口
 */
public interface IEvent {

    public Type getType();

    public Object getObject();

    public boolean hasSource();

    /*事件监听*/
    public IEventListener getSource();

    enum Type {
        /*系统*/
        SYSTEM,
        /*状态*/
        STATUS,
        /*服务调用*/
        SERVICE_CALL,
        /*公用对象*/
        SHARED_OBJECT,
        /*流动作*/
        STREAM_ACTION,
        /*流控制*/
        STREAM_CONTROL,
        /*流数据*/
        STREAM_DATA,
        /*连接*/
        CLIENT,
        /*连接 实例化*/
        CLIENT_INVOKE,
        /*连接 通知*/
        CLIENT_NOTIFY,
        /*容器*/
        SERVER
    }

}
