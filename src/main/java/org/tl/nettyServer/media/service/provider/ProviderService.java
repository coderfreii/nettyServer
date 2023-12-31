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

package org.tl.nettyServer.media.service.provider;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.messaging.IMessageInput;
import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.messaging.InMemoryPullPullPipe;
import org.tl.nettyServer.media.scope.*;
import org.tl.nettyServer.media.service.IStreamableFileService;
import org.tl.nettyServer.media.service.IStreamableFileServiceFactory;
import org.tl.nettyServer.media.stream.DefaultStreamFilenameGenerator;
import org.tl.nettyServer.media.stream.IStreamFilenameGenerator;
import org.tl.nettyServer.media.stream.base.IBroadcastStream;
import org.tl.nettyServer.media.stream.client.IClientBroadcastStream;
import org.tl.nettyServer.media.stream.provider.FileProvider;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Set;

@Slf4j
public class ProviderService implements IProviderService {


    // whether or not to support FCS/FMS/AMS live-wait (default to off)
    private boolean liveWaitSupport;

    /**
     * {@inheritDoc}
     */
    public INPUT_TYPE lookupProviderInput(IScope scope, String name, int type) {
        INPUT_TYPE result = INPUT_TYPE.NOT_FOUND;
        if (scope.getBasicScope(ScopeType.BROADCAST, name) != null) {
            // we have live input
            log.debug(String.format("we have live input for <%s>", name));
            result = INPUT_TYPE.LIVE;
        } else {
            File file = getStreamFile(scope, name);
            if (file == null) {
                if (type == -2 && liveWaitSupport) {
                    result = INPUT_TYPE.LIVE_WAIT;
                    log.debug(String.format("we have a live wait for <%s>", name));
                }
            } else {
                // "default" to VOD as a missing file will be picked up later on
                log.debug(String.format("we have VOD file input for <%s>", name));
                result = INPUT_TYPE.VOD;
            }
        }
        log.debug(String.format("request Stream <%s> not found", name));
        return result;
    }


    public IMessageInput getProviderInput(IScope scope, String name) {
        IMessageInput msgIn = getLiveProviderInput(scope, name, false);
        if (msgIn == null) {
            return getVODProviderInput(scope, name);
        }
        return msgIn;
    }


    public IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate) {
        log.debug("Get live provider input for {} scope: {}", name, scope);
        //make sure the create is actually needed
        IBroadcastScope broadcastScope = scope.getBroadcastScope(name);
        if (broadcastScope == null && needCreate) {
            synchronized (scope) {
                // re-check if another thread already created the scope
                broadcastScope = scope.getBroadcastScope(name);
                if (broadcastScope == null) {
                    broadcastScope = new BroadcastScope(scope, name);
                    scope.addChildScope(broadcastScope);
                }
            }
        }
        return broadcastScope;
    }


    public IMessageInput getVODProviderInput(IScope scope, String name) {
        log.debug("getVODProviderInput - scope: {} name: {}", scope, name);
        File file = getVODProviderFile(scope, name);
        if (file == null) {
            return null;
        }
        IPipe pipe = new InMemoryPullPullPipe();
        pipe.subscribe(new FileProvider(scope, file), null);
        return pipe;
    }


    public File getVODProviderFile(IScope scope, String name) {
        if (log.isDebugEnabled()) {
            log.debug("getVODProviderFile - scope: {} name: {}", scope, name);
        }
        File file = getStreamFile(scope, name);
        if (file == null || !file.exists()) {
            //if there is no file extension this is most likely a live stream
            if (name.indexOf('.') > 0) {
                log.info("File was null or did not exist: {}", name);
            } else {
                log.trace("VOD file {} was not found, may be live stream", name);
            }
        }
        return file;
    }


    public boolean registerBroadcastStream(IScope scope, String name, IBroadcastStream bs) {
        if (log.isDebugEnabled()) {
            log.debug("Registering - name: {} stream: {} scope: {}", new Object[]{name, bs, scope});
            ((Scope) scope).dump();
        }
        IBroadcastScope broadcastScope = scope.getBroadcastScope(name);
        if (broadcastScope == null) {
            log.debug("Creating a new scope");
            broadcastScope = new BroadcastScope(scope, name);
            if (scope.addChildScope(broadcastScope)) {
                log.debug("Broadcast scope added");
            } else {
                log.warn("Broadcast scope was not added to {}", scope);
            }
        }

        // set the client broadcast stream if we have a broadcast scope
        if (broadcastScope != null && bs instanceof IClientBroadcastStream) {
            broadcastScope.setClientBroadcastStream((IClientBroadcastStream) bs);
        }

        if (log.isDebugEnabled()) {
            log.debug("Subscribing scope {} to provider {}", broadcastScope, bs.getProvider());
        }
        return broadcastScope.subscribe(bs.getProvider(), null);
    }


    public Set<String> getBroadcastStreamNames(IScope scope) {
        return scope.getBasicScopeNames(ScopeType.BROADCAST);
    }


    public boolean unregisterBroadcastStream(IScope scope, String name) {
        return unregisterBroadcastStream(scope, name, null);
    }


    public boolean unregisterBroadcastStream(IScope scope, String name, IBroadcastStream bs) {
        if (log.isDebugEnabled()) {
            log.debug("Unregistering - name: {} stream: {} scope: {}", new Object[]{name, bs, scope});
            ((Scope) scope).dump();
        }
        IBroadcastScope broadcastScope = scope.getBroadcastScope(name);
        if (bs != null) {
            log.debug("Unsubscribing scope {} from provider {}", broadcastScope, bs.getProvider());
            broadcastScope.unsubscribe(bs.getProvider());
            // if the scope has no listeners try to remove it
            if (!((BasicScope) broadcastScope).hasEventListeners()) {
                if (log.isDebugEnabled()) {
                    log.debug("Scope has no event listeners attempting removal");
                }
                scope.removeChildScope(broadcastScope);
            }
        } else {
            log.debug("Broadcast scope was null for {}", name);
        }
        // verify that scope was removed
        return scope.getBasicScope(ScopeType.BROADCAST, name) == null;
    }

    @SneakyThrows
    private File getStreamFile(IScope scope, String name) {
        if (log.isDebugEnabled()) {
            log.debug("getStreamFile - name: {}", name);
        }
        IStreamableFileServiceFactory factory = (IStreamableFileServiceFactory) ScopeUtils.getScopeService(scope, IStreamableFileServiceFactory.class);
        if (name.indexOf(':') == -1 && name.indexOf('.') == -1) {
            // Default to .flv files if no prefix and no extension is given.
            name = "flv:" + name;
        }
        // ams sends an asterisk at the start of the name on mp4, so remove it
        if (name.charAt(0) == '*') {
            name = name.substring(1);
            if (log.isTraceEnabled()) {
                log.trace("Removed star prefix: {}", name);
            }
        }
        for (IStreamableFileService service : factory.getServices()) {
            if (name.startsWith(service.getPrefix() + ':')) {
                name = service.prepareFilename(name);
                break;
            }
        }
        // look for a custom filename gen class
        IStreamFilenameGenerator filenameGenerator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
        // get the filename
        String filename = filenameGenerator.generateFilename(scope, name, IStreamFilenameGenerator.GenerationType.PLAYBACK);
        // start life as null and only update upon positive outcome
        File file = null;
        // get ahead of the game with the direct check first
        File tmp = Paths.get(filename).toFile();
        // most likely case first
        if (tmp.exists()) {
            file = tmp;
        } else if (!filenameGenerator.resolvesToAbsolutePath()) {
            String appScopeName = ScopeUtils.findApplication(scope).getName();
            file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, filename));
        }

        return file;
    }


    public boolean isLiveWaitSupport() {
        return liveWaitSupport;
    }


    public void setLiveWaitSupport(boolean liveWaitSupport) {
        this.liveWaitSupport = liveWaitSupport;
    }

}
