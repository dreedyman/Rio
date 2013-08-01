/*
 * Copyright 2008 to the original author or authors.
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

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.measurable.MeasurableMonitor;
import org.rioproject.system.MeasuredResource;

/**
 * The SystemMemory object measures the memory utilization for a physical
 * machine
 *
 * @author Dennis Reedy
 */
public class SystemMemory extends Memory {
    private static final String VIEW = "org.rioproject.system.memory.CalculableSystemMemoryView";
    /**
     * Component for Configuration and Logging
     */
    private static final String COMPONENT = "org.rioproject.system.measurable.systemMemory";

    /**
     * Construct a Memory object
     *
     * @param config Configuration object
     */
    public SystemMemory(Configuration config) {
        super(SystemWatchID.SYSTEM_MEMORY, COMPONENT, config);
        setView(VIEW);
    }

    protected String getComponentName() {
        return COMPONENT;
    }

    @Override
    public void checkValue() {
        MeasuredResource mRes = monitor.getMeasuredResource();
        double utilization = calculateUtilization(mRes);

        long now = System.currentTimeMillis();
        if(mRes instanceof SystemMemoryUtilization)
            addWatchRecord(new CalculableSystemMemory(getId(),
                                                      utilization,
                                                      now,
                                                      (SystemMemoryUtilization)mRes));
        else
            addWatchRecord(new CalculableMemory(getId(), utilization, now));
        setLastMeasuredResource(mRes);
    }

    @Override
    protected MeasurableMonitor createMeasurableMonitor(Configuration config)
        throws ConfigurationException {
        return (MeasurableMonitor) config.getEntry(getComponentName(),
                                                   "monitor",
                                                   MeasurableMonitor.class,
                                                   new SystemMemoryMonitor());
    }
}
