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
package org.rioproject.watch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Provides a queued approach to replicate a Watch record.
 */
public abstract class QueuedReplicator implements WatchDataReplicator, Serializable {
    private static final long serialVersionUID = 1L;
    private boolean closed = false;
    private final BlockingQueue<Calculable> replicatorQ = new LinkedBlockingQueue<Calculable>();
    private transient ExecutorService execService;
    private transient CountDownLatch shutdownLatch;
    private static Logger logger = LoggerFactory.getLogger("org.rioproject.watch");

    /**
     * Performs the actual write to the underlying resource
     * 
     * @param calculable the Calculable record to replicate
     *
     * @throws IOException if the write encounters errors
     */
    protected abstract void replicate(Calculable calculable) throws IOException;

    /**
     * Performs the actual write to the underlying resource
     *
     * @param calculables Collection of Calculable records to replicate
     *
     * @throws IOException if the write encounters errors
     */
    protected abstract void bulkReplicate(Collection<Calculable> calculables) throws IOException;

    /**
     * Closes the underlying Resource. This abstract class does not enforce
     * the implementation of this method. If the underlying resource needs to
     * be closed, implementing classes need to override this method.
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void closeResource() { }


    public synchronized void close() {
        closed = true;
        if(replicatorQ!=null && !replicatorQ.isEmpty()) {
            shutdownLatch = new CountDownLatch(1);
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        }
        if (execService != null) {
            execService.shutdownNow();
            execService = null;
        }
        if(replicatorQ!=null)
            replicatorQ.clear();

        closeResource();
    }

    /**
     * Archive a record from the WatchDataSource history by placing it on a
     * queue
     * 
     * @param calculable the Calculable record to archive
     */
    public void addCalculable(Calculable calculable) {
        init();
        replicatorQ.add(calculable);
    }

    private class ReplicatorTask implements Runnable {
        public void run() {
            while(!closed) {
                try {
                    Calculable calculable = replicatorQ.poll(5, TimeUnit.SECONDS);
                    if(calculable!=null) {
                        replicate(calculable);
                    }
                } catch(IOException e) {
                    logger.warn("Replication communication failure: ", e);
                } catch (InterruptedException e) {
                    //logger.warn( "ReplicatorTask interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }

            try {
                List<Calculable> drain = new ArrayList<Calculable>();
                replicatorQ.drainTo(drain);
                int numToDrain = drain.size();
                if(numToDrain>0) {
                    try {
                        bulkReplicate(drain);
                    } catch(IOException e) {
                        logger.warn("Cannot archive (draining): ", e);
                    }
                }
            } finally {
                if(shutdownLatch!=null)
                    shutdownLatch.countDown();
            }
        }
    }

    private void readObject(ObjectInputStream oStream) throws ClassNotFoundException, IOException {
        oStream.defaultReadObject();
        replicatorQ.clear();
        init();
    }

    private synchronized void init() {
        if(execService==null) {
            execService = Executors.newCachedThreadPool();
            execService.submit(new ReplicatorTask());
        }
    }
}
