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

package org.tl.nettyServer.media.event;

import java.util.Set;

/**
 * IEventObservable hold functionality of the well-known Observer pattern, that is it has a list of objects that listen to events.
 * 观察者模式
 */
public interface IEventObservable {
 
    public boolean addEventListener(IEventListener listener); 
    
    public boolean removeEventListener(IEventListener listener);
 
    public Set<IEventListener> getEventListeners();

}
