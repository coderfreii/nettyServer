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

package org.tl.nettyServer.servers.net.rtmp.conn;


import org.tl.nettyServer.servers.net.rtmp.codec.RTMP;
import org.tl.nettyServer.servers.util.CustomizableThreadFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsible for management and creation of RTMP based connections.
 * 负责管理和创建基本的RTMP连接
 *
 * @author The Red5 Project
 */
public class RTMPConnManager implements IConnectionManager<RTMPConnection> {


    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new CustomizableThreadFactory("ConnectionChecker-"));
    //管理所有的连接，key=sessionId,value=conn
    protected ConcurrentMap<String, RTMPConnection> connMap = new ConcurrentHashMap<>();

    protected AtomicInteger conns = new AtomicInteger();

    protected static IConnectionManager<RTMPConnection> instance = new RTMPConnManager();

    protected boolean debug;

    {
        // create a scheduled job to check for dead or hung connections
        executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                // count the connections that need closing
                int closedConnections = 0;
                // get all the current connections
                Collection<RTMPConnection> allConns = getAllConnections();

                for (RTMPConnection conn : allConns) {
                    String sessionId = conn.getSessionId();
                    RTMP rtmp = conn.getState();
                }
                // if there is more than one connection that needed to be closed, request a GC to clean up memory.
                if (closedConnections > 0) {
                    System.gc();
                }
            }
        }, 7000, 30000, TimeUnit.MILLISECONDS);
    }

    public static IConnectionManager<RTMPConnection> getInstance() {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    public RTMPConnection createConnection(Class<?> connCls) {
        RTMPConnection conn = null;
        if (RTMPConnection.class.isAssignableFrom(connCls)) {
            try {
                // create connection
                conn = createConnectionInstance(connCls);
                // add to local map
                connMap.put(conn.getSessionId(), conn);
                // set the scheduler
            } catch (Exception ex) {
            }
        }
        return conn;
    }

    /**
     * {@inheritDoc}
     */
    public RTMPConnection createConnection(Class<?> connCls, String sessionId) {
        throw new UnsupportedOperationException("Not implemented");
    }


    /**
     * Adds a connection.
     *
     * @param conn connection
     */
    public void setConnection(RTMPConnection conn) {

    }


    /**
     * Returns a connection for a given session id.
     *
     * @param sessionId session id
     * @return connection if found and null otherwise
     */
    public RTMPConnection getConnectionBySessionId(String sessionId) {
        RTMPConnection conn = connMap.get(sessionId);
        return conn;
    }

    /**
     * {@inheritDoc}
     */
    public RTMPConnection removeConnection(String sessionId) {
        // remove from map
        RTMPConnection conn = connMap.remove(sessionId);
        if (conn != null) {
            setConnectionLocal(null);
        }
        return conn;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<RTMPConnection> getAllConnections() {
        ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>(connMap.size());
        list.addAll(connMap.values());
        return list;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<RTMPConnection> removeConnections() {
        ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>(connMap.size());
        list.addAll(connMap.values());
        connMap.clear();
        conns.set(0);
        return list;
    }


    /**
     * 创建连接实例
     *
     * @param cls cls
     * @return {@link RTMPConnection}
     * @throws Exception 异常
     */
    private RTMPConnection createConnectionInstance(Class<?> cls) throws Exception {
        RTMPConnection conn = null;
        conn = (RTMPConnection) cls.newInstance();
        return conn;
    }

    private static final ThreadLocal<WeakReference<IConnection>> connThreadLocal = new ThreadLocal<WeakReference<IConnection>>();


    public static void setConnectionLocal(IConnection connection) {
        if (connection != null) {
            connThreadLocal.set(new WeakReference<IConnection>(connection));
        } else {
            // use null to clear the value
            connThreadLocal.remove();
        }
    }
}
