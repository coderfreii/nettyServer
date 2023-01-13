package org.tl.nettyServer.media.net.http.conn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * HTTP Conn Manager
 *
 * @author TL
 */
public class HTTPConnManager implements IHTTPConnManager {

    private static final Logger log = LoggerFactory.getLogger(HTTPConnManager.class);

    private ConcurrentMap<Long, HTTPConnection> connMap = new ConcurrentHashMap<Long, HTTPConnection>();

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final class SingletonHolder {

        private static final HTTPConnManager INSTANCE = new HTTPConnManager();
    }

    protected HTTPConnManager() {

    }

    public static HTTPConnManager getInstance() {

        return SingletonHolder.INSTANCE;
    }

    @Override
    public HTTPConnection getConnection(long clientId) {
        lock.readLock().lock();
        try {
            return connMap.get(clientId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addConnection(HTTPConnection conn, long clientId) {

        lock.writeLock().lock();
        try {
            log.debug("add connection with id: {}", clientId);
            connMap.put(clientId, conn);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public HTTPConnection removeConnection(long clientId) {

        lock.writeLock().lock();
        try {
            log.debug("Removing connection with id: {}", clientId);
            return connMap.remove(clientId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Collection<HTTPConnection> removeConnections() {

        ArrayList<HTTPConnection> list = new ArrayList<HTTPConnection>(connMap.size());
        lock.writeLock().lock();
        try {
            list.addAll(connMap.values());
            return list;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getConnectionCount() {

        lock.writeLock().lock();
        try {
            return connMap.size();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public HTTPConnection CreateConnection() {
        HTTPConnection httpConnection = new HTTPConnection();
        return httpConnection;
    }

}
