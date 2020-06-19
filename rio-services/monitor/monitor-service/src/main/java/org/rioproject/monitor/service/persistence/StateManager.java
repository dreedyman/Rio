/*
 * Copyright to the original author or authors.
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
package org.rioproject.monitor.service.persistence;

import org.rioproject.monitor.service.OpStringManager;
import org.rioproject.monitor.service.OpStringManagerController;
import org.rioproject.impl.persistence.PersistentStore;
import org.rioproject.impl.persistence.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.MarshalledObject;

/**
 * Manages the state of OperationalStrings
 */
public class StateManager {
    private final OpStringLogHandler opStringLogHandler;
    private final PersistentStore store;
    static Logger logger = LoggerFactory.getLogger(StateManager.class.getName());
    /** Snapshot thread */
    SnapshotThread snapshotter;

    public StateManager(String logDirName, OpStringManagerController opStringMangerController) throws StoreException, IOException {
        opStringLogHandler = new OpStringLogHandler();
        opStringLogHandler.setOpStringMangerController(opStringMangerController);
        store = new PersistentStore(logDirName, opStringLogHandler, opStringLogHandler);
        snapshotter = new SnapshotThread(OpStringLogHandler.class.getName(), store);
        opStringLogHandler.setSnapshotter(snapshotter);
        store.snapshot();
    }

    /**
     * Notification of an OperationalString state change. This method is
     * invoked whenever an OperationalString has been added or removed, and
     * whenever elements of an OperationalString have been modified, added or
     * removed.
     *
     * @param opMgr The OpStringManager that has changed
     * @param remove Whether or not the OpStringManager has been removed
     */
    public void stateChanged(OpStringManager opMgr, boolean remove) {
        if(opStringLogHandler.inRecovery() || store == null)
            return;
        if(!opMgr.isActive())
            return;
        try {
            store.acquireMutatorLock();
            int action = (remove? RecordHolder.REMOVED:RecordHolder.MODIFIED);
            store.update(new MarshalledObject<>( new RecordHolder(opMgr.doGetOperationalString(), action)));
        } catch(Exception ise) {
            logger.warn("OperationalString state change notification", ise);
        } finally {
            store.releaseMutatorLock();
        }
    }

    public void processRecoveredOpStrings() {
        opStringLogHandler.processRecoveredOpStrings();
    }

    public void processUpdatedOpStrings() {
        opStringLogHandler.processUpdatedOpStrings();
    }

    public boolean inRecovery() {
        return opStringLogHandler.inRecovery();
    }
}
