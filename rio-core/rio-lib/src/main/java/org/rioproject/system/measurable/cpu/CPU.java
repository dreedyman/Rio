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
package org.rioproject.system.measurable.cpu;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import org.rioproject.costmodel.ResourceCostModel;
import org.rioproject.costmodel.ZeroCostModel;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.system.measurable.MeasurableMonitor;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

/**
 * The <code>CPU</code> object is a <code>MeasurableCapability</code> which
 * refers to the platform's capability describing CPU utilization, capacity, and
 * load.
 *
 * @author Dennis Reedy
 */
public class CPU extends MeasurableCapability {
    public static final String VIEW = "org.rioproject.system.measurable.cpu.CalculableCPUView";
    /** Iteration value for calculating utilization of sampleSize >1 */
    private int count;
    /** Temporary utilization value */
    private double tempUtilization;    
    /** The CPU Utilization */
    private double utilization;
    /** Component for Configuration and Logging */
    static final String COMPONENT = "org.rioproject.system.measurable.cpu";
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Construct a new CPU object
     * 
     * @param config Configuration object
     */
    public CPU(Configuration config) {
        this(config, SystemWatchID.SYSTEM_CPU, false);
    }

    /**
     * Construct a new CPU object
     *
     * @param config Configuration object
     * @param id The identifier for the CPU watch
     * @param monitorJVM If true, monitor the JVM's CPU utlization.
     * If false, monitor the machine's CPU utilization
     */
    public CPU(Configuration config, String id, boolean monitorJVM) {
        super(id, (monitorJVM?COMPONENT+".jvm":COMPONENT), config);
        String configComponent = monitorJVM ? COMPONENT + ".jvm" : COMPONENT;
        if(!isEnabled())
            return;
        setView(VIEW);
        logger.trace("Creating [{}]", id);
        try {
            ThresholdValues defaultThresholdVals;
            int numCPUs = Runtime.getRuntime().availableProcessors();
            defaultThresholdVals = new ThresholdValues(0.0, numCPUs);
            ThresholdValues tVals =
                (ThresholdValues)config.getEntry(configComponent,
                                                 "thresholdValues",
                                                 ThresholdValues.class,
                                                 defaultThresholdVals);
            setThresholdValues(tVals);
            ResourceCostModel rCostModel =
                (ResourceCostModel)config.getEntry(configComponent,
                                                   "resourceCost",
                                                   ResourceCostModel.class,
                                                   new ZeroCostModel());
            setResourceCostModel(rCostModel);

            sampleSize = Config.getIntEntry(config,
                                            configComponent,
                                            "sampleSize",
                                            1,   /* default */
                                            1,   /* min */
                                            10); /* max */
            setSampleSize(sampleSize);

            MeasurableMonitor defaultMonitor;
            if(monitorJVM) {
                ProcessCPUHandler cpuHandler = new ProcessCPUHandler();
                cpuHandler.setMXBean(ManagementFactory.getOperatingSystemMXBean());
                cpuHandler.setStartTime(ManagementFactory.getRuntimeMXBean().getStartTime());
                defaultMonitor = cpuHandler;
            } else {
                SystemCPUHandler cpuHandler = new SystemCPUHandler();
                cpuHandler.setID(id);
                cpuHandler.setThresholdValues(tVals);
                defaultMonitor = cpuHandler;
            }

            MeasurableMonitor monitor =
                (MeasurableMonitor)config.getEntry(configComponent,
                                                   "cpuMonitor",
                                                   MeasurableMonitor.class,
                                                   defaultMonitor);
            long reportRate = Config.getLongEntry(config,
                                                  configComponent,
                                                  "reportRate",
                                                  DEFAULT_PERIOD,     /* default */
                                                  1000,               /* min */
                                                  Integer.MAX_VALUE); /* max */
            setPeriod(reportRate);
            setMeasurableMonitor(monitor);

        } catch (Throwable e) {
            logger.error("Getting CPU Configuration", e);
        }
    }
    
    /**
     * Get the computed utilization for this <code>CPU</code> object
     * 
     * @return Utilization computed for this component
     */
    public double getUtilization() {
        return (utilization);
    }    
    
    /* (non-Javadoc)
     * @see org.rioproject.watch.PeriodicWatch#checkValue()
     */
    public void checkValue() {
        count++;
        if(monitor==null)
            return;
        long now = System.currentTimeMillis();
        MeasuredResource mRes = monitor.getMeasuredResource();
        //tempUtilization += monitor.getValue();
        tempUtilization += mRes.getValue();
        if(count==sampleSize) {
            utilization = tempUtilization/sampleSize;                                    
            count = 0;
            tempUtilization = 0;            
        }
        logger.trace("{}: utilization={}", getId(), utilization);

        if(mRes instanceof CpuUtilization)
            addWatchRecord(new CalculableCPU(getId(), (CpuUtilization)mRes, now));
        else if (mRes instanceof ProcessCpuUtilization)
            addWatchRecord(new CalculableProcessCPU(getId(), (ProcessCpuUtilization)mRes, now));
        else
            addWatchRecord(new CalculableProcessCPU(getId(), mRes.getValue(), now));
        
        setLastMeasuredResource(mRes);
    }

    public MeasurableMonitor getMeasurableMonitor() {
        return monitor;
    }

}
