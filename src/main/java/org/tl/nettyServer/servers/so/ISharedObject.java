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

package org.tl.nettyServer.servers.so;


import org.tl.nettyServer.servers.scope.IBasicScope;
import org.tl.nettyServer.servers.statistics.ISharedObjectStatistics;

/**
 * Serverside access to shared objects.
 * 服务器端对共享对象的访问
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ISharedObject extends IBasicScope, ISharedObjectBase, ISharedObjectSecurityService {

    /**
     * Prevent shared object from being released. Each call to
     *
     * <pre>
     * acquire
     * </pre>
     * <p>
     * must be paired with a call to
     *
     * <pre>
     * release
     * </pre>
     * <p>
     * so the SO isn't held forever.
     * 防止释放共享对象。每个要获取的呼叫都必须与要释放的呼叫配对，这样SO就不会永远保持
     * This method basically is a no-op for persistent SOs as their data is stored and they can be released without losing their contents.
     * 这种方法基本上是持久性SoS的禁止操作，因为它们的数据是存储的，并且可以在不丢失内容的情况下释放它们。
     */
    public void acquire();

    /**
     * Check if shared object currently is acquired.
     *
     * @return <pre>
     * true
     * </pre>
     * <p>
     * if the SO is acquired, otherwise
     *
     * <pre>
     * false
     * </pre>
     */
    public boolean isAcquired();

    /**
     * Release previously acquired shared object. If the SO is non-persistent, no more clients are connected the SO isn't acquired any more, the data is released.
     */
    public void release();

    /**
     * Return statistics about the shared object.
     *
     * @return statistics
     */
    public ISharedObjectStatistics getStatistics();

    /**
     * Sets a "dirty" flag to indicate that the attributes have been modified.
     *
     * @param dirty if dirty / modified
     */
    @Deprecated
    public void setDirty(boolean dirty);

    /**
     * Sets a "dirty" flag to indicate that the named attribute has been modified.
     *
     * @param name attribute key which is now dirty
     */
    void setDirty(String name);

}