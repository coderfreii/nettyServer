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

package org.tl.nettyServer.media.plugin;


import org.tl.nettyServer.media.adapter.MultiThreadedApplicationAdapter;

import java.util.Map;

/**
 * Base interface for handlers originating from plug-ins.
 * 
 * @author Paul Gregoire
 */
public interface IRed5PluginHandler {

    /**
     * Initialize the plug-in handler.
     */
    void init();

    /**
     * Set the application making use of this plug-in handler.
     * 
     * @param application
     *            application adapter
     */
    void setApplication(MultiThreadedApplicationAdapter application);

    /**
     * Set properties to be used by this handler.
     * 
     * @param props
     *            plugin properties map
     */
    void setProperties(Map<String, Object> props);

}
