package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Link;
import net.lazygun.micromuse.Room;
import net.lazygun.micromuse.Route;
import net.lazygun.micromuse.Transaction;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.impl.transaction.LockException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Ewan
 */
public class GraphTransaction implements Transaction {

    private static final ConcurrentMap<String, Long> propContainerThreadMap = new ConcurrentHashMap<>();
    private static final ThreadLocks threadLocks = new ThreadLocks();
    private static final ThreadLocks newLocks = new ThreadLocks();

    private final org.neo4j.graphdb.Transaction transaction;

    public GraphTransaction(org.neo4j.graphdb.Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void failure() {
        transaction.failure();
    }

    @Override
    public void success() {
        transaction.success();
    }

    private void cleanupNewLocks() {
        Map<String, Lock> locks = newLocks.get();
        for (String id : locks.keySet()) {
            Lock lock = locks.remove(id);
            lock.release();
        }
    }

    private void saveNewLocks() {
        threadLocks.get().putAll(newLocks.get());
        newLocks.get().clear();
    }

    @Override
    public synchronized void acquireLock(Room room, boolean write) {
        try {
            innerAcquireLock(room, write);
            saveNewLocks();
        } catch (Exception ex) {
            cleanupNewLocks();
            throw ex;
        }
    }

    private void innerAcquireLock(Room room, boolean write) {
        if (!(room instanceof RoomNode)) {
            throw new IllegalArgumentException("Room must be an instance of RoomNode");
        }
        RoomNode node = (RoomNode) room;
        for (Relationship relationship : node.getExitRelationships()) {
            lock(relationship, relationship.getId(), write);
        }
        lock(node, node.getId(), write);
    }

    @Override
    public synchronized void acquireLock(Link link, boolean write) {
        try {
            innerAcquireLock(link, write);
            saveNewLocks();
        } catch (Exception ex) {
            cleanupNewLocks();
            throw ex;
        }
    }

    private void innerAcquireLock(Link link, boolean write) {
        if (link.getFrom() instanceof RoomNode) {
            innerAcquireLock(link.getFrom(), write);
        }
        if (link.getTo() instanceof RoomNode) {
            innerAcquireLock(link.getTo(), write);
        }
    }

    @Override
    public synchronized void acquireLock(Route route, boolean write) {
        try {
            innerAcquireLock(route, write);
            saveNewLocks();
        } catch (Exception ex) {
            cleanupNewLocks();
            throw ex;
        }
    }

    private void innerAcquireLock(Route route, boolean write) {
        for (Link link : route) {
            innerAcquireLock(link, write);
        }
    }

    public void lock(PropertyContainer object, long id, boolean writeLock) {
        long threadId = Thread.currentThread().getId();
        String key = object.getClass().getName() + id;
        System.err.print("Locking " + key + " to thread " + threadId + "...");
        Long lockedThreadId = propContainerThreadMap.putIfAbsent(key, threadId);
        if (lockedThreadId == null || lockedThreadId == threadId) {
            while (true) {
                try {
                    Lock lock;
                    if (writeLock) {
                        lock = transaction.acquireWriteLock(object);
                    } else {
                        lock = transaction.acquireReadLock(object);
                    }
                    newLocks.get().put(key, lock);
                    System.err.println("done.");
                    break;
                } catch (DeadlockDetectedException e) {
                    //System.err.println(e.getLocalizedMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        } else {
            throw new LockException("Object is already locked by another thread");
        }
    }

    @Override
    public void close() {
        transaction.close();
        for (String id : threadLocks.get().keySet()) {
            propContainerThreadMap.remove(id);
        }
        threadLocks.get().clear();
    }

    private static class ThreadLocks extends ThreadLocal<Map<String, Lock>> {
        @Override
        protected Map<String, Lock> initialValue() {
            return new HashMap<>();
        }
    }
}
