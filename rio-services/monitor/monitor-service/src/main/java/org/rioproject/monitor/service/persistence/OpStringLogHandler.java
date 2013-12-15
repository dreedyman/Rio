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

import com.sun.jini.reliableLog.LogHandler;
import org.rioproject.monitor.service.OpStringManager;
import org.rioproject.monitor.service.OpStringManagerController;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.impl.persistence.SnapshotHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.rmi.MarshalledObject;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class that manages the persistence details behind saving and restoring
 * OperationalStrings
 */
public class OpStringLogHandler extends LogHandler implements SnapshotHandler {
    /**
     * Collection of of recovered Operational Strings to add
     */
    private final Collection<OperationalString> recoveredOpstrings = new ArrayList<OperationalString>();
    /**
     * Collection of of recovered Operational Strings to add
     */
    private final Collection<RecordHolder> updatedOpstrings = new ArrayList<RecordHolder>();
    /**
     * flag to indicate whether OperationalStrings have been recovered
     */
    private boolean opStringsRecovered = false;
    /** Flag to indicate that we are in recover mode */
    private AtomicBoolean inRecovery = new AtomicBoolean(false);
    /** Log File must contain this many records before a snapshot is allowed */
    // TODO - allow this to be a user configurable parameter
    int logToSnapshotThresh = 10;
    OpStringManagerController opStringMangerController;
    SnapshotThread snapshotter;

    /** Log format version */
    static final int LOG_VERSION = 1;

    static Logger logger = LoggerFactory.getLogger(OpStringLogHandler.class.getName());

    void setOpStringMangerController(OpStringManagerController opStringMangerController) {
        this.opStringMangerController = opStringMangerController;
    }

    public boolean inRecovery() {
        return inRecovery.get();
    }

    void setSnapshotter(SnapshotThread snapshotter) {
        this.snapshotter = snapshotter;
    }

    public void snapshot(OutputStream out) throws IOException {
        ObjectOutputStream oostream = new ObjectOutputStream(out);
        oostream.writeUTF(OpStringLogHandler.class.getName());
        oostream.writeInt(LOG_VERSION);
        List<OperationalString> list = new ArrayList<OperationalString>();
        OperationalString[] opStrings = opStringMangerController.getOperationalStrings();
        list.addAll(Arrays.asList(opStrings));
        oostream.writeObject(new MarshalledObject<List<OperationalString>>(list));
        oostream.flush();
    }

    /**
     * Required method implementing the abstract recover() defined in
     * ReliableLog's associated LogHandler class. This callback is invoked
     * from the recover method of ReliableLog.
     */
    @SuppressWarnings("unchecked")
    public void recover(InputStream in) throws Exception {
        inRecovery.set(true);
        ObjectInputStream oistream = new ObjectInputStream(in);
        if (!OpStringLogHandler.class.getName().equals(oistream.readUTF()))
            throw new IOException("Log from wrong implementation");
        if (oistream.readInt() != LOG_VERSION)
            throw new IOException("Wrong log format version");
        MarshalledObject mo = (MarshalledObject) oistream.readObject();
        List<OperationalString> list = (List<OperationalString>) mo.get();
        for (OperationalString opString : list) {
            if (logger.isDebugEnabled())
                logger.debug("Recovered : " + opString.getName());
            //dumpOpString(opString);
            recoveredOpstrings.add(opString);
            opStringsRecovered = true;
        }
    }

    /**
     * Required method implementing the abstract applyUpdate() defined in
     * ReliableLog's associated LogHandler class.
     * <p/>
     * During state recovery, the recover() method defined in the
     * ReliableLog class is invoked. That method invokes the method
     * recoverUpdates() which invokes the method readUpdates(). Both of
     * those methods are defined in ReliableLog. The method readUpdates()
     * retrieves a record from the log file and then invokes this method.
     */
    public void applyUpdate(Object update) throws Exception {
        if (update instanceof MarshalledObject) {
            RecordHolder holder = (RecordHolder) ((MarshalledObject) update).get();
            updatedOpstrings.add(holder);
            opStringsRecovered = true;
        }
    }

    /**
     * Called by <code>PersistentStore</code> after every update to give
     * server a chance to trigger a snapshot <br>
     *
     * @param updateCount Number of updates since last snapshot
     */
    public void updatePerformed(int updateCount) {
        if (updateCount >= logToSnapshotThresh) {
            snapshotter.takeSnapshot();
        }
    }

    /**
     * Delegate snapshot request to PersistentStore
     */
    public void takeSnapshot() {
        snapshotter.takeSnapshot();
    }

    /**
     * Determine if OperationalString objects have been recovered or updated
     *
     * @return boolean <code/true</code> if OperationalString objects have
     *         been recovered or updated, otherwise <code>false</code>
     */
    boolean opStringsRecovered() {
        return (opStringsRecovered);
    }

    /**
     * Process recovered OperationalString objects
     */
    void processRecoveredOpStrings() {
        for (OperationalString opString : recoveredOpstrings) {
            try {
                if (!opStringMangerController.opStringExists(opString.getName())) {
                    Map<String, Throwable> map = new HashMap<String, Throwable>();
                    opStringMangerController.addOperationalString(opString, map, null, null, null);
                    opStringMangerController.dumpOpStringError(map);
                } else {
                    OpStringManager opMgr = opStringMangerController.getOpStringManager(opString.getName());
                    Map map = opMgr.doUpdateOperationalString(opString);
                    opStringMangerController.dumpOpStringError(map);
                }
            } catch (Exception ex) {
                logger.warn("Processing recovered OperationalStrings", ex);
            }
        }
        recoveredOpstrings.clear();
    }

    /**
     * Process updated OperationalString objects
     *
     * @throws org.rioproject.opstring.OperationalStringException
     *          if there are errors processing
     *          the OperationalStrings
     */
    void processUpdatedOpStrings() throws OperationalStringException {
        for (RecordHolder holder : updatedOpstrings) {
            OperationalString opString = holder.getOperationalString();
            try {
                if (holder.getAction() == RecordHolder.MODIFIED) {
                    if (!opStringMangerController.opStringExists(opString.getName())) {
                        Map<String, Throwable> map = new HashMap<String, Throwable>();
                        opStringMangerController.addOperationalString(opString, map, null, null, null);
                        opStringMangerController.dumpOpStringError(map);
                    } else {
                        OpStringManager opMgr = opStringMangerController.getOpStringManager(opString.getName());
                        Map map = opMgr.doUpdateOperationalString(opString);
                        opStringMangerController.dumpOpStringError(map);
                    }
                } /*else {
                    undeploy(opString.getName(), false);
                }*/
            } catch (Exception ex) {
                logger.warn("Processing updated OperationalStrings", ex);
            }
        }
        updatedOpstrings.clear();
    }
}
