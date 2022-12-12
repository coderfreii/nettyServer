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

package org.tl.nettyServer.servers.stream;

/**
 * Represents all the states that a stream may be in at a requested point in time.
 * 流状态
 * @author Paul Gregoire (mondain@gmail.com)
 */
public enum StreamState {
    /*初始化*/
    INIT,
    /*未初始*/
    UNINIT,
    /*打开*/
    OPEN,
    /*关闭*/
    CLOSED,
    /*已开始*/
    STARTED,
    /*已关闭*/
    STOPPED,
    /*发布中*/
    PUBLISHING,
    /*播放中*/
    PLAYING,
    /*暂停*/
    PAUSED,
    /*从新开始*/
    RESUMED,
    /*结束*/
    END,
    /*寻找*/
    SEEK;

}
