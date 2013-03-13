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
package org.rioproject.system.measurable.memory;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.costmodel.ResourceCostModel;
import org.rioproject.costmodel.ZeroCostModel;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.system.measurable.MeasurableMonitor;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.MemoryNotificationInfo;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The Memory object is a MeasurableCapability that measures the amount of
 * memory available for the JVM
 *
 * @author Dennis Reedy
 */

/* Suppress PMD warnings for the invocation of getComponentName() and createMeasurableMonitor() during object
 * construction. This is by design. */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class Memory extends MeasurableCapability {
    /** Iteration value for calculating utilization of sampleSize >1 */
    private int count;
    /** Temporary utilization value */
    private double tempUtilization;
    /** Computed utilization value */
    private double utilization;
    private static final String VIEW = "org.rioproject.system.memory.CalculableMemoryView";
    /** Total memory arena */
    double totalArena;
    /** Component for Configuration and Logging */
    private static final String COMPONENT = "org.rioproject.system.measurable.memory";
    private JMXNotificationListener jmxListener;
    /** For handling JX event notifications */
    private final BlockingQueue<Notification> eventQ = new LinkedBlockingQueue<Notification>();
    /** A Logger for this class */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Construct a Memory object
     *
     * @param config Configuration object
     */
    public Memory(Configuration config) {
        this(SystemWatchID.JVM_MEMORY, config);
    }

    /**
     * Construct a Memory object
     *
     * @param id Identifier to use
     * @param config Configuration object
     */
    public Memory(String id, Configuration config) {
        this(id, COMPONENT, config);
    }

    /**
     * Construct a Memory object
     *
     * @param id Identifier to use
     * @param componentName The configuration component name
     * @param config Configuration object
     */
    public Memory(String id, String componentName, Configuration config) {
        super(id, componentName, config);
        if(!isEnabled())
            return;
        setView(VIEW);
        try {           
            ThresholdValues tVals = (ThresholdValues)config.getEntry(getComponentName(),
                                                                     "thresholdValues",
                                                                     ThresholdValues.class,
                                                                     new ThresholdValues(0.0, 1.0));
            logger.trace("{} threshold values: {}", getId(), tVals.toString());
            setThresholdValues(tVals);
            
            ResourceCostModel rCostModel = (ResourceCostModel)config.getEntry(getComponentName(),
                                                                              "resourceCost",
                                                                              ResourceCostModel.class,
                                                                              new ZeroCostModel());
            setResourceCostModel(rCostModel);
            sampleSize = Config.getIntEntry(config,
                                            getComponentName(),
                                            "sampleSize",
                                            1,   /* default */ 
                                            1,   /* min */
                                            10); /* max */
            setSampleSize(sampleSize);

            setMeasurableMonitor(createMeasurableMonitor(config));

            long reportRate = Config.getLongEntry(config,
                                                  getComponentName(),
                                                  "reportRate",
                                                  DEFAULT_PERIOD,     /* default */
                                                  5000,               /* min */
                                                  Integer.MAX_VALUE); /* max */
            setPeriod(reportRate);
            
            totalArena = Runtime.getRuntime().maxMemory();
            
        } catch (ConfigurationException e) {
            logger.warn("Getting Memory Configuration", e);
        }
    }

    protected String getComponentName() {
        return COMPONENT;
    }

    protected MeasurableMonitor createMeasurableMonitor(Configuration config) throws ConfigurationException {
        MeasurableMonitor mMon = (MeasurableMonitor)config.getEntry(getComponentName(),
                                                                    "monitor",
                                                                    MeasurableMonitor.class,
                                                                    new ProcessMemoryMonitor());
        if(mMon instanceof ProcessMemoryMonitor) {
            NotificationEmitter emitter = (NotificationEmitter)((ProcessMemoryMonitor)mMon).getMXBean();
            jmxListener = new JMXNotificationListener();
            emitter.addNotificationListener(jmxListener, null, null);
        }
        return mMon;
    }

    public MeasurableMonitor getMeasurableMonitor() {
        return monitor;
    }

    /**
     * Override PeriodicWatch.start() to get an initial reading prior to
     * scheduling 
     */
    public void start() {
        checkValue();
        super.start();
    }


    @Override
    public void stop() {
        super.stop();
        if(jmxListener!=null)
            jmxListener.getExecutorService().shutdownNow();
    }

    /**
     * Get the computed utilization for this Memory object
     *
     * @return The utilization computed for this component
     */
    public double getUtilization() {
        return(utilization);
    }
        
    
    /* (non-Javadoc)
     * @see org.rioproject.watch.PeriodicWatch#checkValue()
     */
    public void checkValue() {
        addWatchRecord(createCalculableMemory());
    }

    protected double calculateUtilization(MeasuredResource mRes) {
        count++;
        tempUtilization += mRes.getValue();
        if(count==sampleSize) {
            utilization = tempUtilization/sampleSize;
            count = 0;
            tempUtilization = 0;
        }
        logger.trace("Memory : {} utilization={}", getComponentName(), utilization);

        return utilization;
    }

    private CalculableMemory createCalculableMemory() {
        CalculableMemory  calculableMemory ;
        MeasuredResource mRes = monitor.getMeasuredResource();
        utilization = calculateUtilization(mRes);
        long now = System.currentTimeMillis();
        if(mRes instanceof ProcessMemoryUtilization)
            calculableMemory = new CalculableMemory(getId(),
                                                utilization,
                                                (ProcessMemoryUtilization)mRes,
                                                now);
        else
            calculableMemory = new CalculableMemory(getId(), utilization, now);
        setLastMeasuredResource(mRes);
        return calculableMemory;
    }

    class JMXNotificationListener implements NotificationListener {
        private final ExecutorService execService = Executors.newSingleThreadExecutor();

        JMXNotificationListener() {
            execService.submit(new JMXNotificationHandler());
        }

        ExecutorService getExecutorService() {
            return execService;
        }

        public void handleNotification(Notification notification, Object handback) {
            eventQ.add(notification);
        }
    }

    class JMXNotificationHandler implements Runnable {
        public void run() {
            while (true) {
                Notification notification;
                try {
                    notification = eventQ.take();
                } catch (InterruptedException e) {
                    logger.debug("JMXNotificationHandler breaking out of main loop");
                    break;
                }
                if (notification.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                    CalculableMemory calculableMemory = createCalculableMemory();
                    /* We may have a reading that lags behind the notification memory threshold notification.
                     * Force the value to be greater than the high threshold */
                    if(calculableMemory.getValue()<=getThresholdValues().getCurrentHighThreshold()) {
                        logger.info("JMX MEMORY_THRESHOLD_EXCEEDED, adjusting CalculableMemory value " +
                                    "from [{}] to [{}] to enforce SLA actions to occur.",
                                    calculableMemory.getValue(),
                                    (getThresholdValues().getCurrentHighThreshold()+0.01));
                        calculableMemory.setValue(getThresholdValues().getCurrentHighThreshold()+0.01);
                    }
                    addWatchRecord(createCalculableMemory());
                }
            }
        }
    }

}

