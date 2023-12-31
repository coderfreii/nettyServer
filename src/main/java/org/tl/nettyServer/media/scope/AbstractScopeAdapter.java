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


import org.tl.nettyServer.media.client.IClient;
import org.tl.nettyServer.media.event.IEvent;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.service.call.IServiceCall;

import java.util.Map;

/**
 * Base scope handler implementation. Meant to be subclassed.
 */
public abstract class AbstractScopeAdapter implements IScopeHandler {

    //private static Logger log = LoggerFactory.getLogger(AbstractScopeAdapter.class);

    /**
     * Can start flag.
     *
     * <code>
     * true
     * </code>
     * <p>
     * if scope is ready to be activated,
     *
     * <code>
     * false
     * </code>
     * <p>
     * otherwise
     */
    private boolean canStart = true;

    /**
     * Can connect flag.
     *
     * <code>
     * true
     * </code>
     * <p>
     * if connections to scope are allowed,
     *
     * <code>
     * false
     * </code>
     * <p>
     * otherwise
     */
    private boolean canConnect;

    /**
     * Can join flag.
     *
     * <code>
     * true
     * </code>
     * <p>
     * if scope may be joined by users,
     *
     * <code>
     * false
     * </code>
     * <p>
     * otherwise
     */
    private boolean canJoin = true;

    /**
     * Can call service flag.
     *
     * <code>
     * true
     * </code>
     * <p>
     * if remote service calls are allowed for the scope,
     *
     * <code>
     * false
     * </code>
     * <p>
     * otherwise
     */
    private boolean canCallService = true;

    /**
     * Can add child scope flag.
     *
     * <code>
     * true
     * </code>
     * <p>
     * if scope is allowed to add child scopes,
     *
     * <code>
     * false
     * </code>
     * <p>
     * otherwise
     */
    private boolean canAddChildScope = true;

    /**
     * Can handle event flag.
     *
     * <code>
     * true
     * </code>
     * <p>
     * if events handling is allowed,
     *
     * <code>
     * false
     * </code>
     * <p>
     * otherwise
     */
    private boolean canHandleEvent = true;

    /**
     * Setter for can start flag.
     *
     * @param canStart <code>
     *                 true
     *                 </code>
     *                 <p>
     *                 if scope is ready to be activated,
     *
     *                 <code>
     *                 false
     *                 </code>
     *                 <p>
     *                 otherwise
     */
    public void setCanStart(boolean canStart) {
        this.canStart = canStart;
    }

    /**
     * Setter for can call service flag
     *
     * @param canCallService <code>
     *                       true
     *                       </code>
     *                       <p>
     *                       if remote service calls are allowed for the scope,
     *
     *                       <code>
     *                       false
     *                       </code>
     *                       <p>
     *                       otherwise
     */
    public void setCanCallService(boolean canCallService) {
        //log.trace("setCanCallService: {}", canCallService);
        this.canCallService = canCallService;
    }

    /**
     * Setter for can connect flag
     *
     * @param canConnect <code>
     *                   true
     *                   </code>
     *                   <p>
     *                   if connections to scope are allowed,
     *
     *                   <code>
     *                   false
     *                   </code>
     *                   <p>
     *                   otherwise
     */
    public void setCanConnect(boolean canConnect) {
        this.canConnect = canConnect;
    }

    /**
     * Setter for 'can join' flag
     *
     * @param canJoin <code>
     *                true
     *                </code>
     *                <p>
     *                if scope may be joined by users,
     *
     *                <code>
     *                false
     *                </code>
     *                <p>
     *                otherwise
     */
    public void setJoin(boolean canJoin) {
        this.canJoin = canJoin;
    }

    /**
     * {@inheritDoc}
     */
    public boolean start(IScope scope) {
        return canStart;
    }

    /**
     * {@inheritDoc}
     */
    public void stop(IScope scope) {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    public boolean connect(IConnection conn, IScope scope, Object[] params) {
        return canConnect;
    }

    /**
     * {@inheritDoc}
     */
    public void disconnect(IConnection conn, IScope scope) {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    public boolean join(IClient client, IScope scope) {
        return canJoin;
    }

    /**
     * {@inheritDoc}
     */
    public void leave(IClient client, IScope scope) {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    public boolean serviceCall(IConnection conn, IServiceCall call) {
        //log.trace("serviceCall - canCallService: {} scope: {} method: {}", canCallService, conn.getScope().getName(), call.getServiceMethodName());
        return canCallService;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addChildScope(IBasicScope scope) {
        return canAddChildScope;
    }

    /**
     * {@inheritDoc}
     */
    public void removeChildScope(IBasicScope scope) {
    }

    /**
     * {@inheritDoc}
     */
    public boolean handleEvent(IEvent event) {
        return canHandleEvent;
    }

    /**
     * Calls the checkBandwidth method on the current client.
     *
     * @param o Object passed from Flash, not used at the moment
     */
    public void checkBandwidth(Object o) {
        //Incoming object should be null
//        IClient client = Red5.getConnectionLocal().getClient();
//        if (client != null) {
//            client.checkBandwidth();
//        }
    }

    /**
     * Calls the checkBandwidthUp method on the current client.
     *
     * @param params Object passed from Flash
     * @return bandwidth results map
     */
    public Map<String, Object> checkBandwidthUp(Object[] params) {
        //Incoming object should be null
        //TODO
//        IClient client = Red5.getConnectionLocal().getClient();
//        if (client != null) {
//            return client.checkBandwidthUp(params);
//        }
        return null;
    }

}
