/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.resources.persistence;

import com.sun.jini.reliableLog.LogHandler;
import com.sun.jini.reliableLog.ReliableLog;
import com.sun.jini.thread.ReadersWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class that stores a server's state to disk. Basically a wrapper around ReliableLog 
 * with the addition of lock management.
 */
public class PersistentStore {
    /** Object we use to reliable and penitently log updates to our state */
    private ReliableLog log;
    /**
     * No mutation of persistent state can occur during a snapshot,
     * however, we can have multiple mutators, use
     * <code>ReadersWriter</code> object to manage this invariant.
     * Note as far as the lock is concerned the mutators are the
     * readers and snapshot thread (the reader) since for us mutation
     * is the non-exclusive operation
     */
    final private ReadersWriter mutatorLock = new ReadersWriter();
    /**
     * Thread local that tracks if (and how many times) the current thread
     * has acquired a non-exclusive mutator lock.
     */
    final static private ThreadLocal<Long> lockState = new ThreadLocal<Long>();

    /** Cache a <code>Long</code> object with a zero value */
    final static private Long zero = (long) 0;

    /** Location of the persistent store */
    final private File storeLocation;

    /** The server this PersistentStore serves */
    final private SnapshotHandler snapshotHandler;

    /** Number of updates since last snapshot */
    private int updateCount;

    /** A list of all of the substores */
    private List<SubStore> subStores = new java.util.LinkedList<SubStore>();

    /**
     * Construct a store that will persist its data to the specified
     * directory.
     * @param logDir Directory where the store should persist its data.
     *               must exist.
     * @param logHandler Object that will process the log and last snapshot
     *               to recover the server's state
     * @param snapshotHandler the server is called back after an update so it can
     *               decide whether or not to do a snapshot.
     * @throws StoreException if there is a problem setting up the store
     */
    public PersistentStore(String logDir, 
                           LogHandler logHandler, 
                           SnapshotHandler snapshotHandler) throws StoreException {
        this.snapshotHandler = snapshotHandler;
        storeLocation = new File(logDir);

        try {
            log = new ReliableLog(storeLocation.getCanonicalPath(), logHandler);
        } catch(IOException e) {
            throw new CorruptedStoreException("Failure creating reliable log", e);
        }

        try {
            log.recover();
        } catch(IOException e) {
            throw new CorruptedStoreException("Failure recovering reliable log", e);       
        }
    }

    /**
     * Destroy the store
     * @throws IOException if it has difficulty removing the log files 
     */
    public void destroy() throws IOException {
        // Prep all the sub-stores to be destroyed
        for (SubStore subStore : subStores) {
            subStore.prepareDestroy();
        }

        log.deletePersistentStore();
        FileSystem.destroy(storeLocation, true);
    }

    /**
     * Get the absolute path location for the store location
     *
     * @return The absolute path location for the store location
     */
    public String getStoreLocation(){
        return(storeLocation.getAbsolutePath());
    }

    /**
     * Inform the store of a sub-store
     *
     * @param subStore The SubStore to add
     *
     * @throws StoreException if the SubStore cannot be added
     */
    public void addSubStore(SubStore subStore) throws StoreException {
        try {
            final String subDir = subStore.subDirectory();

            if(subDir == null) {
                subStore.setDirectory(storeLocation);
            } else {
                subStore.setDirectory(new File(storeLocation, subDir));
            }

            subStores.add(subStore);
        } catch(IOException e) {
            throw new StoreException("Failure adding substore " + subStore, e);

        }
    }

    /////////////////////////////////////////////////////////////////
    // Methods for obtaining and releasing the locks on the store

    /**
     * Block until we can acquire a non-exclusive mutator lock on the
     * servers 's persistent state.  This lock should be acquired in a
     * <code>try</code> block and a <code>releaseMutatorLock</code>
     * call should be placed in a <code>finally</code> block.
     */
    public void acquireMutatorLock() {
        // Do we already hold a lock?

        Long lockStateVal = lockState.get();
        if(lockStateVal == null)
            lockStateVal = zero;

        final long longVal = lockStateVal;

        if(longVal == 0) {
            // No, this thread currently does not hold a lock,
            // grab non-exclusive lock (which for mutatorLock is a
            // read lock) 
            mutatorLock.readLock();
        }

        // Ether way, bump the lock count and update our thread state
        lockState.set(longVal + 1);
    }

    /**
     * Release one level of  mutator locks if this thread holds at lease one.
     */
    public void releaseMutatorLock() {
        Long lockStateVal = lockState.get();
        if(lockStateVal == null)
            lockStateVal = zero;

        final long longVal = lockStateVal;

        if(longVal == 0)
            // No lock to release, return
            return;

        if(longVal == 1) {
            // Last one on stack release lock
            // Using read lock because we want a non-exclusive lock
            mutatorLock.readUnlock();
            lockStateVal = zero;
        } else {
            lockStateVal = longVal - 1;
        }

        lockState.set(lockStateVal);
    }

    //////////////////////////////////////////////////////////////////
    // Methods for writing records to the log and taking and
    // coordinating snapshots

    /**
     * Log an update. Will flush to disk before returning.
     *
     * @param o Update argument
     *
     * @throws IllegalStateException if the current thread does not hold
     * a non-exclusive mutator lock
     * @throws IOException If errors accessing the file system occur
     */
    public void update(Object o) throws IOException {
        final Long lockStateVal = lockState.get();
        if(lockStateVal == null || lockStateVal == 0)
            throw new IllegalStateException("PersistentStrore.update:" +
                                            "Must acquire mutator lock before calling update()");
        synchronized (this) {
            log.update(o, true);
            updateCount++;
            snapshotHandler.updatePerformed(updateCount);
        }
    }

    /**
     * Generate a snapshot, will perform the necessary locking to ensure no
     * threads are mutating the state of the server before creating the 
     * snapshot.
     * @throws IOException see 
     * @see com.sun.jini.reliableLog.ReliableLog#snapshot
     */
    public void snapshot() throws IOException {
        try {
            // Using write lock because we want an exclusive lock
            mutatorLock.writeLock();
            updateCount = 0;

            // Don't need to sync on this because
            // mutatorLock.writeLock() gives us an exclusive lock
            log.snapshot();
        } finally {
            // Using write lock because we want an exclusive lock
            mutatorLock.writeUnlock();
        }
    }
    
    /**
     * Close this PersistentStore without destroying its contents
     *
     * @throws IOException If file system access errors are encountered
     */
    public void close() throws IOException {
        try {
            // Using write lock because we want an exclusive lock
            mutatorLock.writeLock();
            updateCount = 0;

            // Don't need to sync on this because
            // mutatorLock.writeLock() gives us an exclusive lock
            log.close();
        } finally {
            // Using write lock because we want an exclusive lock
            mutatorLock.writeUnlock();
        }
    }
}

