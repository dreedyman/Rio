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
package org.rioproject.watch;

import net.jini.config.Configuration;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The WatchDataSource interface defines the semantics for a Watch to store
 * Calculable records
 */
public interface WatchDataSource extends Remote {

    /**
     * Set the WatchDataSource configuration
     *
     * @param config The configuration
     *
     * @throws RemoteException If communication errors occur
     */
    void setConfiguration(Configuration config) throws RemoteException;

    /**
     * Set the ID for the WatchDataSource
     *
     * @param id The identifier for the WatchDataSource
     *
     * @throws RemoteException If communication errors occur
     */
    void setID(String id) throws RemoteException;

    /**
     * Get the ID for the WatchDataSource
     *
     * @return The identifier for the WatchDataSource
     *
     * @throws RemoteException If communication errors occur
     */
    String getID() throws RemoteException;

    /**
     * Initialize the WatchDataSource
     *
     * @throws RemoteException if the WatchDataSource cannot be exported
     */
    void initialize() throws RemoteException;

    /**
     * Set the maximum size for the Calculable history
     * 
     * @param size The maximum size for the Calculable history
     *
     * @throws RemoteException If communication errors occur
     */
    void setMaxSize(int size) throws RemoteException;

    /**
     * Get the maximum size for the Calculable history
     *
     * @return The maximum size for the Calculable history
     *
     * @throws RemoteException If communication errors occur
     */
    int getMaxSize() throws RemoteException;

    /**
     * Clears the history
     *
     * @throws RemoteException If communication errors occur
     */
    void clear() throws RemoteException;

    /**
     * Get the current size for the Calculable history
     *
     * @return The current size for the Calculable history
     *
     * @throws RemoteException If communication errors occur
     */
    int getCurrentSize() throws RemoteException;

    /**
     * Add a Calculable record to the Calculable history
     *
     * @param calculable The Calculable record
     *
     * @throws RemoteException If communication errors occur
     */
    void addCalculable(Calculable calculable) throws RemoteException;

    /**
     * Get all Calculable records from the Calculable history
     *
     * @return An array of Calculable records from the Calculable history. If 
     * there are no Calculable records in the history, a zero-length array
     * will be returned
     *
     * @throws RemoteException If communication errors occur
     */
    Calculable[] getCalculable() throws RemoteException;

    /**
     * Get Calculable records from the Calculable history for the specified
     * time range
     *
     * @param from The start time
     * @param to The end time
     *
     * @return An array of Calculable records from the Calculable history
     * within the provided time range. If there are no Calculable records in the
     * range, a zero-length array will be returned
     *
     * @throws RemoteException If communication errors occur
     */
    Calculable[] getCalculable(long from, long to) throws RemoteException;

    /**
     * Gets the last Calculable from the history
     *
     * @return The last Calculable in the history
     *
     * @throws RemoteException If communication errors occur
     */
    Calculable getLastCalculable() throws RemoteException;

    /** 
     * Getter for property thresholdValues.
     *
     * @return Value of property thresholdValues.
     *
     * @throws RemoteException If communication errors occur
     */
    ThresholdValues getThresholdValues() throws RemoteException;

    /**
     * Set the ThresholdValues
     *
     * @param tValues The ThresholdValues to set
     *
     * @throws RemoteException If communication errors occur
     */
    void setThresholdValues(ThresholdValues tValues) throws RemoteException;

    /** 
     * Closes the watch data source and unexports it from the runtime
     *
     * @throws RemoteException If communication errors occur
     */
    void close() throws RemoteException;

    /** 
     * Setter for property view.
     *
     * @param view The view class name, suitable for Class.forName
     *
     * @throws RemoteException If communication errors occur
     */
    void setView(String view) throws RemoteException;

    /** 
     * Getter for property view.
     *
     * @return The Value of the property view
     *
     * @throws RemoteException If communication errors occur
     */
    String getView() throws RemoteException;

    /**
     * Add a {@link WatchDataReplicator},
     *
     * @param replicator The WatchDataReplicator to add.
     *
     * @return true if the WatchDataReplicator was added. If the
     * WatchDataReplicator has already been added, it will not be added
     * again.
     * 
     * @throws RemoteException If communication errors occur
     */
    boolean addWatchDataReplicator(WatchDataReplicator replicator) throws RemoteException;

    /**
     * Remove a {@link WatchDataReplicator}
     *
     * @param replicator The WatchDataReplicator to remove.
     *
     * @return true if the WatchDataReplicator was removed
     *  
     * @throws RemoteException If communication errors occur
     */
    boolean removeWatchDataReplicator(WatchDataReplicator replicator) throws RemoteException;
}
