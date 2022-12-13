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

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.net.rtmp.conn.IConnection;


/**
 * Scope security handler providing positive results to any allow request.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@Slf4j
public class ScopeSecurityHandler implements IScopeSecurityHandler {


    protected boolean connectionAllowed = true;

    protected boolean scopeAllowed = true;

    @Override
    public boolean allowed(IConnection conn) {
        log.debug("Allowing: {} connection: {}", connectionAllowed, conn);
        return connectionAllowed;
    }

    @Override
    public boolean allowed(IScope scope) {
        log.debug("Allowing: {} scope: {}", scopeAllowed, scope);
        return scopeAllowed;
    }

    public void setConnectionAllowed(boolean connectionAllowed) {
        this.connectionAllowed = connectionAllowed;
    }

    public void setScopeAllowed(boolean scopeAllowed) {
        this.scopeAllowed = scopeAllowed;
    }

}
