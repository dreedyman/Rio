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
package org.rioproject.impl.system.measurable.disk;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.costmodel.ResourceCostModel;
import org.rioproject.costmodel.ZeroCostModel;
import org.rioproject.impl.system.measurable.MeasurableCapability;
import org.rioproject.impl.system.measurable.MeasurableMonitor;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.measurable.disk.CalculableDiskSpace;
import org.rioproject.system.measurable.disk.DiskSpaceUtilization;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The <code>DiskSpace</code> object is a <code>MeasurableCapability</code> which 
 * monitors a ComputeResource's DiskSpace utilization
 *
 * @author Dennis Reedy
 */
public class DiskSpace extends MeasurableCapability implements DiskSpaceMBean {
    public static final String VIEW = "org.rioproject.system.disk.CalculableDiskSpaceView";
    /** Iteration value for calculating utilization of sampleSize >1 */
    private int count;
    /** Temporary value for used diskspace*/
    private double tempUtilization;    
    /** Computed utilization value */
    private double utilization;
    /** Component for Configuration and Logging */
    static final String COMPONENT = "org.rioproject.system.measurable.disk";
    /** A Logger for this class */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Construct a new DiskSpace object
     * 
     * @param config Configuration object
     */
    public DiskSpace(Configuration config) {
        super(SystemWatchID.DISK_SPACE, COMPONENT, config);
        if(!isEnabled())
            return;
        setView(VIEW);
        try {
            ThresholdValues tVals = 
                (ThresholdValues)config.getEntry(COMPONENT,
                                                 "thresholdValues", 
                                                 ThresholdValues.class,
                                                 new ThresholdValues(0.0, 1.0));            
            setThresholdValues(tVals);
            ResourceCostModel rCostModel = 
                (ResourceCostModel)config.getEntry(COMPONENT,
                                                   "resourceCost", 
                                                   ResourceCostModel.class,
                                                   new ZeroCostModel());

            String fileSystem = (String)config.getEntry(COMPONENT,
                                                        "fileSystem",
                                                        String.class,
                                                        File.separator);
            setResourceCostModel(rCostModel);
            
            sampleSize = Config.getIntEntry(config,
                                            COMPONENT,
                                            "sampleSize",
                                            1,   /* default */ 
                                            1,   /* min */
                                            10); /* max */
            setSampleSize(sampleSize);
            DiskSpaceMonitor defaultMonitor = new DiskSpaceMonitor();

            MeasurableMonitor monitor =
                (MeasurableMonitor)config.getEntry(COMPONENT,
                                                   "diskMonitor",
                                                   MeasurableMonitor.class,
                                                   defaultMonitor);

            if(monitor instanceof DiskSpaceMonitor)
                ((DiskSpaceMonitor)monitor).setFileSystemToMonitor(fileSystem);
            
            long reportRate = Config.getLongEntry(config,
                                            COMPONENT,
                                            "reportRate",
                                            DEFAULT_PERIOD,     /* default */ 
                                            5000,               /* min */
                                            Long.MAX_VALUE);    /* max */
            setPeriod(reportRate);
            setMeasurableMonitor(monitor);

        } catch (ConfigurationException e) {
            logger.warn("Getting DiskSpace Configuration", e);
        }        
    }
    
    /**
     * Override PeriodicWatch.start() to get an initial reading prior to
     * scheduling 
     */
    public void start() {
        checkValue();
        super.start();
    }            
    
    /**
     * Get the computed utilization for this <code>DiskSpace</code> object
     *
     * @return Utilization computed for this component
     */
    public double getUtilization() {
        return utilization;
    }

    /**
     * Get the computed utilization for this <code>DiskSpace</code> object
     *
     * @return Utilization computed for this component
     */
    public double getCapacity() {
        return ((DiskSpaceUtilization)getMeasuredResource()).getCapacity();
    }
    
    public void checkValue() {
        count++;
        if(monitor==null)
            return;
        DiskSpaceUtilization dsUtilization = (DiskSpaceUtilization)monitor.getMeasuredResource();
        tempUtilization += dsUtilization.getValue();
        if(count==sampleSize) {
            utilization = tempUtilization/sampleSize;
            count = 0;
            tempUtilization = 0;
        }
        long now = System.currentTimeMillis();
        addWatchRecord(new CalculableDiskSpace(getId(), dsUtilization, now));
        setLastMeasuredResource(dsUtilization);
    }
}

