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
package org.rioproject.system.measurable.memory.pool;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.system.measurable.MeasurableMonitor;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * The MemoryPool is used to monitor a JMX memory pool.
 */
/* Suppress PMD warnings for the invocation of createMeasurableMonitor() during object
 * construction. This is by design. */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class MemoryPool extends MeasurableCapability {
    /** Iteration value for calculating utilization of sampleSize >1 */
    private int count;
    /** Temporary utilization value */
    private double tempUtilization;
    /** Computed utilization value */
    private double utilization;
    private MeasurableMonitor<MemoryPoolUtilization> monitor;
    /** Component for Configuration and Logging */
    private static final String COMPONENT = "org.rioproject.system.memory.pool";
    private static final String VIEW = "org.rioproject.system.memory.CalculableMemoryPoolView";
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Construct a MemoryPool object
     *
     * @param id Identifier to use
     * @param config Configuration object
     * @param thresholdValues ThresholdValues to use
     */
    public MemoryPool(String id, Configuration config, ThresholdValues thresholdValues)  {
        super(id, COMPONENT, config);
        setThresholdValues(thresholdValues);
        monitor = createMeasurableMonitor(id);
        setView(VIEW);
        if(monitor==null) {
            setEnabled(false);
            StringBuilder poolNames = new StringBuilder();
            for(MemoryPoolMXBean mBean : ManagementFactory.getMemoryPoolMXBeans()) {
                if(poolNames.length()>0)
                    poolNames.append(", ");
                poolNames.append(mBean.getName());
            }
            logger.warn("Unable to obtain a monitor for {}, available memory pools are [{}]. " +
                        "Cannot monitor the requested memory pool", id, poolNames);
        }
        try {
            sampleSize = Config.getIntEntry(config,
                                            COMPONENT,
                                            "sampleSize",
                                            1,   /* default */
                                            1,   /* min */
                                            10); /* max */
            setSampleSize(sampleSize);
        } catch (ConfigurationException e) {
            logger.warn("Unable to obtain {}.sampleSize from configuration, use default and continue", COMPONENT);
        }

        try {
            long reportRate = Config.getLongEntry(config,
                                                  COMPONENT,
                                                  "reportRate",
                                                  DEFAULT_PERIOD,     /* default */
                                                  5000,               /* min */
                                                  Integer.MAX_VALUE); /* max */
            setPeriod(reportRate);
        } catch (ConfigurationException e) {
            logger.warn("Unable to obtain {}.reportRate from configuration, use default and continue", COMPONENT);
        }
    }

    public double getUtilization() {
        return utilization;
    }

    /**
     * Override PeriodicWatch.start() to get an initial reading prior to
     * scheduling
     */
    @Override
    public void start() {
        checkValue();
        super.start();
    }    

    public void checkValue() {
        MemoryPoolUtilization mRes = monitor.getMeasuredResource();
        double utilization = calculateUtilization(mRes);
        long now = System.currentTimeMillis();
        addWatchRecord(new CalculableMemoryPool(getId(), utilization, now, mRes));
        setLastMeasuredResource(mRes);
    }
        
    protected MeasurableMonitor<MemoryPoolUtilization> createMeasurableMonitor(String id) {
        MemoryPoolMXBeanMonitor m = new MemoryPoolMXBeanMonitor();
        m.setID(id);
        m.setThresholdValues(getThresholdValues());
        MemoryPoolMXBean memoryBean = m.getMemoryPoolMXBean();
        if(memoryBean==null)
            m = null;
        return m;

    }

    double calculateUtilization(MeasuredResource mRes) {
        count++;
        tempUtilization += mRes.getValue();
        if(count==sampleSize) {
            utilization = tempUtilization/sampleSize;
            count = 0;
            tempUtilization = 0;
        }
        logger.trace("Memory : utilization={}", utilization);
        return utilization;
    }
}
