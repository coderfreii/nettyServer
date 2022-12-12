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

package org.tl.nettyServer.servers.client;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.servers.exception.ClientNotFoundException;
import org.tl.nettyServer.servers.exception.ClientRejectedException;
import org.tl.nettyServer.servers.jmx.mxbeans.ClientRegistryMXBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry for clients, wherein clients are mapped by their id.
 * 客户机的注册表，其中客户机通过其ID进行映射。
 *
 * @author The Red5 Project
 */
public class ClientRegistry implements IClientRegistry, ClientRegistryMXBean {

    private Logger log = LoggerFactory.getLogger(ClientRegistry.class);

    /**
     * Clients map
     */
    private ConcurrentMap<String, IClient> clients = new ConcurrentHashMap<String, IClient>(6, 0.9f, 2);

    /**
     * Next client id
     */
    private AtomicInteger nextId = new AtomicInteger(0);

    /**
     * The identifier for this client registry
     */
    private String name;

    public ClientRegistry() {
    }

    //allows for setting a "name" to be used with jmx for lookup
    public ClientRegistry(String name) {
        this.name = name;
        if (StringUtils.isNotBlank(this.name)) {
            try {
                MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
                ObjectName oName = new ObjectName("org.red5.server:type=ClientRegistry,name=" + name);
                mbeanServer.registerMBean(new StandardMBean(this, ClientRegistryMXBean.class, true), oName);
            } catch (Exception e) {
                log.warn("Error on jmx registration", e);
            }
        }
    }

    /**
     * Add client to registry
     */
    public void addClient(IClient client) {
        addClient(client.getId(), client);
    }

    /**
     * Add the client to the registry
     */
    private void addClient(String id, IClient client) {
        // check to see if the id already exists first
        if (!hasClient(id)) {
            clients.put(id, client);
        } else {
            log.debug("Client id: {} already registered", id);
        }
    }

    public Client getClient(String id) throws ClientNotFoundException {
        Client result = (Client) clients.get(id);
        if (result == null) {
            throw new ClientNotFoundException(id);
        }
        return result;
    }

    /**
     * Returns a list of Clients.
     */
    public ClientList<Client> getClientList() {
        ClientList<Client> list = new ClientList<Client>();
        for (IClient c : clients.values()) {
            list.add((Client) c);
        }
        return list;
    }

    /**
     * Check if client registry contains clients.
     */
    protected boolean hasClients() {
        return !clients.isEmpty();
    }

    /**
     * Return collection of clients
     *
     * @return Collection of clients
     */
    @SuppressWarnings("unchecked")
    protected Collection<IClient> getClients() {
        if (!hasClients()) {
            // avoid creating new Collection object if no clients exist.
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableCollection(clients.values());
    }

    /**
     * Check whether registry has client with given id
     */
    public boolean hasClient(String id) {
        if (id == null) {
            // null ids are not supported
            return false;
        }
        return clients.containsKey(id);
    }

    /**
     * Return client by id
     */
    public IClient lookupClient(String id) throws ClientNotFoundException {
        return getClient(id);
    }

    /**
     * Return client from next id with given params
     */
    public IClient newClient(Object[] params) throws ClientNotFoundException, ClientRejectedException {
        // derive client id from the connection params or use next
        String id = nextId();
        IClient client = new Client(id, this);
        addClient(id, client);
        return client;
    }

    /**
     * Return next client id
     *
     * @return Next client id
     */
    public String nextId() {
        String id = "-1";
        do {
            // when we reach max int, reset to zero
            if (nextId.get() == Integer.MAX_VALUE) {
                nextId.set(0);
            }
            id = String.format("%d", nextId.getAndIncrement());
        } while (hasClient(id));
        return id;
    }

    /**
     * Return previous client id
     */
    public String previousId() {
        return String.format("%d", nextId.get());
    }

    protected void removeClient(IClient client) {
        clients.remove(client.getId());
    }

}
