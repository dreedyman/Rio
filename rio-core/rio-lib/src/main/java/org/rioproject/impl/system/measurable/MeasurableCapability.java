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
package org.rioproject.impl.system.measurable;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.costmodel.ResourceCost;
import org.rioproject.costmodel.ResourceCostModel;
import org.rioproject.costmodel.ResourceCostProducer;
import org.rioproject.costmodel.ZeroCostModel;
import org.rioproject.impl.watch.PeriodicWatch;
import org.rioproject.impl.watch.ThresholdManager;
import org.rioproject.sla.SLA;
import org.rioproject.system.MeasuredResource;
import org.rioproject.watch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A MeasurableCapability refers to a depletion oriented resource or capability on
 * a ComputeResource
 *
 * @author Dennis Reedy
 */
public abstract class MeasurableCapability extends PeriodicWatch implements ResourceCostProducer,
                                                                            MeasurableCapabilityMBean {
    /**
     * Collection of secondary ThresholdManager instances
     */
    private final Collection<ThresholdManager> thresholdManagers = new ArrayList<ThresholdManager>();
    /**
     * The SLA for the MeasurableCapability
     */
    private SLA sla;
    /**
     * The ResourceCostModel, determining how to charge for use of the 
     * MeasurableCapability
     */
    private ResourceCostModel costModel;
    /**
     * The sampleSize property specifies the amount of samples the 
     * MeasurableCapability will accumulate in the period defined by the reportRate 
     * in order to produce a result.
     */
    protected int sampleSize = 1;
    /** Whether or not this measurable capability is enabled */
    private AtomicBoolean isEnabled = new AtomicBoolean(true);
    /** The {@link MeasurableMonitor} to use */
    protected MeasurableMonitor monitor;
    protected MeasuredResource lastMeasured;
    /** Defines the default history size */
    public final static int DEFAULT_COLLECTION_SIZE = 10;
    /** Defines the default history max size */
    public final static int MAX_COLLECTION_SIZE = 100;
    private int collectionSize;
    private static final Logger logger = LoggerFactory.getLogger(MeasurableCapability.class);

    protected MeasurableCapability(final String id, final String componentName, final Configuration config) {
        super(id, config);
        try {
            isEnabled.set((Boolean) config.getEntry(componentName, "enabled", boolean.class, Boolean.TRUE));
        } catch (ConfigurationException e) {
            logger.error("Getting WatchDataSource Size", e);
        }
        if(!isEnabled.get())
            return;


        try {
            collectionSize = Config.getIntEntry(config, componentName,
                                                "collectionSize",
                                                DEFAULT_COLLECTION_SIZE,
                                                1,
                                                MAX_COLLECTION_SIZE);
        } catch(ConfigurationException e) {
            if(logger.isTraceEnabled())
                logger.trace("Getting WatchDataSource collection size", e);
            collectionSize = DEFAULT_COLLECTION_SIZE;
        }
        try {
            WatchDataSource wds = (WatchDataSource)config.getEntry(componentName,
                                                                   "watchDataSource",
                                                                   WatchDataSource.class,
                                                                   null);
            if(wds!=null) {
                doSetWatchDataSource(wds);
            }
        } catch (ConfigurationException e) {
            logger.error("Getting WatchDataSource Size", e);
        }
        if(localRef!=null)
            localRef.setMaxSize(collectionSize);
    }

    public void addWatchDataReplicator(WatchDataReplicator replicator) {
        if(localRef!=null) {
            localRef.addWatchDataReplicator(replicator);
        } else {
            try {
                watchDataSource.addWatchDataReplicator(replicator);
            } catch (RemoteException e) {
                logger.error("Could not add WatchDataReplicator", e);
            }
        }
    }

    protected void setEnabled(boolean enabled) {
        this.isEnabled.set(enabled);
    }

    /**
     * Get is this measurable capability is enabled
     *
     * @return If the measurable capability is enabled return true, otherwise
     * false. If the measurable capability is not enabled, it will not be added
     */
    public boolean isEnabled() {
        return isEnabled.get();
    }

    private void checkEnabled() {
        if(!isEnabled.get())
            throw new IllegalStateException("The MeasurableCapability ["+getId()+"] is not enabled");
    }
    
    /**
     * Override parent's setWatchDataSource to set the size
     */
    @Override
    public void setWatchDataSource(WatchDataSource watchDataSource) {
        doSetWatchDataSource(watchDataSource);
    }

    private void doSetWatchDataSource(WatchDataSource watchDataSource) {
        checkEnabled();
        super.setWatchDataSource(watchDataSource);
        if(watchDataSource!=null) {
            try {
                watchDataSource.setMaxSize(collectionSize);
            } catch(Exception e) {
                logger.error("Setting WatchDataSource Size", e);
            }
        }
    }

    /**
     * Set the SLA for ths MeasurableCapability. 
     * 
     * @param sla The SLA for this MeasurableCapability
     */
    public void setSLA(SLA sla) {
        checkEnabled();
        if(sla == null)
            throw new IllegalArgumentException("sla is null");
        this.sla = sla;
        getThresholdManager().setThresholdValues(sla);
    }

    /**
     * Get the SLA for ths MeasurableCapability. 
     * 
     * @return The SLA for this MeasurableCapability
     */
    public SLA getSLA() {
        return(sla);
    }
    
    /**
     * @see MeasurableCapabilityMBean#setSampleSize
     */
    public void setSampleSize(int sampleSize) {
        checkEnabled();
        this.sampleSize = sampleSize;             
    }

    /**
     * @see MeasurableCapabilityMBean#getSampleSize()
     */
    public int getSampleSize() {
        return(sampleSize);
    }    
    
    /**
     * Add a secondary ThresholdManager to the MeasurableCapability. The primary 
     * ThresholdManager and the ThresholdValues are set when this class is loaded. 
     * By offering secondary ThresholdManager instances, services can set their own 
     * ranges and be notified accordingly
     * 
     * @param thresholdManager The ThresholdManager
     */
    public void addSecondaryThresholdManager(ThresholdManager thresholdManager) {
        checkEnabled();
        if(thresholdManager==null)
            throw new IllegalArgumentException("thresholdManager is null");
        thresholdManagers.add(thresholdManager);
    }

    /**
     * Remove a secondary ThresholdManager from the MeasurableCapability
     * 
     * @param thresholdManager The ThresholdManager
     */
    public void removeSecondaryThresholdManager(ThresholdManager thresholdManager) {
        if(thresholdManager!=null)
            thresholdManagers.remove(thresholdManager);
    }

    /**
     * Set the ResourceCostModel for the MeasurableCapability
     * 
     * @param costModel The ResourceCostModel which will determine the cost of 
     * using this MeasurableCapability
     */
    public void setResourceCostModel(ResourceCostModel costModel) {
        checkEnabled();
        if(costModel==null)
            throw new IllegalArgumentException("costModel is null");
        this.costModel = costModel;
    }

    /**
     * @see org.rioproject.costmodel.ResourceCostProducer#calculateResourceCost
     */
    public ResourceCost calculateResourceCost(double units, long duration) {
        checkEnabled();
        if(costModel==null)
            costModel = new ZeroCostModel();
        double cost = costModel.getCostPerUnit(duration)*units;
        return(new ResourceCost(getId(), 
                                cost, 
                                units, 
                                costModel.getDescription(), 
                                new Date(System.currentTimeMillis())));
    }

    /**
     * Get the MeasuredResource object, which represents this object's measured 
     * capability
     *
     * @return This object's measured capability
     */
    public MeasuredResource getMeasuredResource() {
        checkEnabled();
        if(lastMeasured==null)
            checkValue();

        return lastMeasured;
    }

    protected void setLastMeasuredResource(MeasuredResource lastMeasured) {
        this.lastMeasured = lastMeasured;
    }

    /**
     * Set the {@link MeasurableMonitor}
     *
     * @param monitor The MeasurableMonitor
     */
    public void setMeasurableMonitor(MeasurableMonitor monitor) {
        this.monitor = monitor;
        this.monitor.setID(getId());
        this.monitor.setThresholdValues(getThresholdValues());
    }

    /**
     * Add a Calculable to the watch and update state
     * 
     * @param record A Calculable record
     */
    public void addWatchRecord(Calculable record) {
        checkEnabled();
        super.addWatchRecord(record);
        for (ThresholdManager tManager : getThresholdManagers())
            tManager.checkThreshold(record);
    }

    /**
     * Get the registered {@link ThresholdManager}s
     *
     * @return The registered {@link ThresholdManager}s. A new unmodifiable collection is allocated each time
     */
    protected Collection<ThresholdManager> getThresholdManagers() {
        Collection<ThresholdManager> tMgrs;
        synchronized(thresholdManagers) {
            tMgrs = Collections.unmodifiableCollection(thresholdManagers);
        }
        return tMgrs;
    }

}
