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

import net.jini.config.Configuration;

import java.rmi.RemoteException;
import java.util.logging.Level;

/**
 * A Watch that provides threshold processing semantics
 */
public class ThresholdWatch extends Watch implements ThresholdWatchMBean {
    /** The graphical view for this Watch */
    static final String VIEW = "org.rioproject.watch.ThresholdCalculableView";
    /** The ThresholdManager */
    private ThresholdManager thresholdManager = new BoundedThresholdManager();

    /**
     * Create a new ThresholdWatch
     * 
     * @param id The identifier for this watch
     */
    public ThresholdWatch(String id) {
        super(id);
        setView(VIEW);
    }

    /**
     * Creates new ThresholdWatch, creates and exports a
     * WatchDataSourceImpl if the WatchDataSource is null using the
     * Configuration object provided
     *
     * @param id The identifier for this watch
     * @param config Configuration object used for constructing a
     * WatchDataSource
     */
    public ThresholdWatch(String id, Configuration config) {
        super(id, config);
        setView(VIEW);
    }    
    
    /**                                                                          `
     * Create a new ThresholdWatch
     * 
     * @param watchDataSource The watch data source associated with this watch
     * @param id The identifier for this watch
     */    
    public ThresholdWatch(WatchDataSource watchDataSource, String id) {
        super(watchDataSource, id);
        setView(VIEW);
    }
    
    /**
     * Get the ThresholdManager for the ThresholdWatch
     * 
     * @return The ThresholdManager for the ThresholdWatch.
     * The ThresholdManager is the keeper of ThresholdValues and determines when
     * Calculable items exceed thresholds and provides specific behavior
     * processing to assist in threshold management processing
     */
    public ThresholdManager getThresholdManager() {        
        return (thresholdManager);
    }
    
    /**
     * Add a ThresholdListener
     * 
     * @param listener The ThresholdListener
     */
    public void addThresholdListener(ThresholdListener listener) {
        if(listener==null)
            throw new IllegalArgumentException("listener is null");
        thresholdManager.addThresholdListener(listener);
    }

    /**
     * Remove a ThresholdListener
     * 
     * @param listener The ThresholdListener
     */
    public void removeThresholdListener(ThresholdListener listener) {
        if(listener==null)
            throw new IllegalArgumentException("listener is null");
        thresholdManager.removeThresholdListener(listener);
    }

    /**
     * Set the ThresholdValues for the ThresholdManager
     * 
     * @param tValues The ThresholdValues
     */
    public void setThresholdValues(ThresholdValues tValues) {
        if(tValues == null)
            throw new IllegalArgumentException("tValues is null");
        thresholdManager.setThresholdValues(tValues);
        if(localRef!=null)
            localRef.setThresholdValues(getThresholdValues());
        else {
            if(watchDataSource!=null) {
                try {
                    watchDataSource.setThresholdValues(getThresholdValues());
                } catch(RemoteException e) {
                    logger.log(Level.WARNING,
                               "Setting ThresholdValues for a remote "+
                               "WatchDataSource",
                               e);
                }
            } else {
                logger.warning("No WatchDataSource set for ["+getId()+"] " +
                               "watch, unable to apply threshold values: ["+tValues+"]");
            }
        }
    }

    /**
     * Get the ThresholdValues from the ThresholdManager
     * 
     * @return ThresholdValues The ThresholdValues
     */
    public ThresholdValues getThresholdValues() {        
        return (thresholdManager.getThresholdValues());
    }
    
    /**
     * Override parent's addRecord to check for threshold(s) being crossed
     */
    @Override
    public void addWatchRecord(Calculable record) {
        super.addWatchRecord(record);        
        thresholdManager.checkThreshold(record);
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#getHighThreshold
     */
    public double getHighThreshold() {
        return getThresholdValues().getHighThreshold();
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#getLowThreshold
     */
    public double getLowThreshold() {
        return getThresholdValues().getLowThreshold();
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#getCurrentHighThreshold
     */
    public double getCurrentHighThreshold() {
        return getThresholdValues().getCurrentHighThreshold();
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#setCurrentHighThreshold
     */
    public void setCurrentHighThreshold(double threshold) {
        ThresholdValues tVals = getThresholdValues();
        tVals.setCurrentHighThreshold(threshold);
        thresholdManager.setThresholdValues(tVals);
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#setCurrentLowThreshold
     */
    public void setCurrentLowThreshold(double threshold) {
        ThresholdValues tVals = getThresholdValues();
        tVals.setCurrentLowThreshold(threshold);
        thresholdManager.setThresholdValues(tVals);
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#getCurrentLowThreshold
     */
    public double getCurrentLowThreshold() {
        return getThresholdValues().getCurrentLowThreshold();
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#getBreachedCount
     */
    public long getBreachedCount() {
        return getThresholdValues().getThresholdBreachedCount();
    }

    /**
     * @see org.rioproject.watch.ThresholdWatchMBean#getClearedCount
     */
    public long getClearedCount() {
        return getThresholdValues().getThresholdClearedCount();
    }

}
