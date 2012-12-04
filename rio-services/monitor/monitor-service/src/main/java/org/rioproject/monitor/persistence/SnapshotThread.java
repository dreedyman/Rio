package org.rioproject.monitor.persistence;

import com.sun.jini.reliableLog.LogException;
import org.rioproject.resources.persistence.PersistentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;

/**
 * A Thread that will perform snapshots. Snapshots, done in a separate
 * thread so it will not hang up in progress remote calls
 */
public class SnapshotThread extends Thread {
    static Logger logger = LoggerFactory.getLogger(SnapshotThread.class.getName());
    PersistentStore store;

    SnapshotThread(String name, PersistentStore store) {
        super(name + ":SnapshotThread");
        setDaemon(true);
        this.store = store;
    }

    /**
     * Signal this thread that it should take a snapshot
     */
    public synchronized void takeSnapshot() {
        notifyAll();
    }

    public void run() {
        while (!isInterrupted()) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
            try {
                store.snapshot();
            } catch (InterruptedIOException e) {
                // Someone wants us dead
                return;
            } catch (Exception e) {
                if (e instanceof LogException
                    && ((LogException) e).detail instanceof
                           InterruptedIOException)
                    return;
                /*
                * If taking the snapshot fails for any reason, then one of
                * the following must be done: -- output the problem to a
                * file and exit -- output the problem to a file and
                * continue -- set an "I have a problem" attribute and then
                * send a notification this issue will be addressed at a
                * later time
                */
                logger.warn("Snapshotting ServiceBean", e);
            }
        }
    }
}
