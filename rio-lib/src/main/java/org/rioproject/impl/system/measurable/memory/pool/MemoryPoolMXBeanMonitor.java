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
package org.rioproject.impl.system.measurable.memory.pool;

import org.rioproject.impl.system.measurable.MeasurableMonitor;
import org.rioproject.system.measurable.memory.pool.MemoryPoolUtilization;
import org.rioproject.watch.ThresholdValues;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * Monitoring for JMX memory pool.
 */
public class MemoryPoolMXBeanMonitor implements MeasurableMonitor<MemoryPoolUtilization> {
    private String id;
    private ThresholdValues thresholdValues;
    private MemoryPoolMXBean memoryPoolBean;
    
    public synchronized MemoryPoolMXBean getMemoryPoolMXBean() {
        if(memoryPoolBean != null)
            return memoryPoolBean;
        for(MemoryPoolMXBean mBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if(mBean.getName().contains(id)) {
                memoryPoolBean = mBean;
                break;
            }
        }
        return memoryPoolBean;
    }

    public void terminate() {
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public ThresholdValues getThresholdValues() {
        return thresholdValues;
    }

    public void setThresholdValues(ThresholdValues thresholdValues) {
        this.thresholdValues = thresholdValues;
        getMemoryPoolMXBean();
        if(memoryPoolBean==null)
            return;
        long maxMemory = memoryPoolBean.getUsage().getMax();
        long warningThreshold = (long) (maxMemory * thresholdValues.getCurrentHighThreshold());
        memoryPoolBean.setUsageThreshold(warningThreshold);
    }
    
    public MemoryPoolUtilization getMeasuredResource() {
        getMemoryPoolMXBean();
        if(memoryPoolBean==null)
            return null;
        double utilization = (double)memoryPoolBean.getUsage().getUsed()/(double)memoryPoolBean.getUsage().getMax();
        return new MemoryPoolUtilization(getId(),
                                         utilization,
                                         memoryPoolBean.getUsage().getCommitted(),
                                         memoryPoolBean.getUsage().getInit(),
                                         memoryPoolBean.getUsage().getMax(),
                                         memoryPoolBean.getUsage().getUsed(),
                                         getThresholdValues());
    }

}
