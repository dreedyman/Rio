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
package org.rioproject.impl.system.measurable.memory;

import org.rioproject.impl.system.measurable.MXBeanMonitor;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.measurable.memory.ProcessMemoryUtilization;
import org.rioproject.watch.ThresholdValues;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * The <code>ProcessMemoryMonitor</code> object provides feedback information to the
 * <code>Memory</code> object, providing memory usage information for a process
 * obtained using JMX .
 *
 * <p>JMX is used to obtain detailed information on heap and non-heap usage.
 *
 * @author Dennis Reedy
 */
public class ProcessMemoryMonitor implements MXBeanMonitor<MemoryMXBean> {
    private MemoryMXBean memBean;
    private String id;
    private ThresholdValues tVals;
    private static final double KB = 1024;
    private static final double MB = Math.pow(KB, 2);

    public ProcessMemoryMonitor() {
        memBean = ManagementFactory.getMemoryMXBean();
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.measurable.MeasurableMonitor#terminate()
     */
    public void terminate() {
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public MeasuredResource getMeasuredResource() {
        MemoryUsage heapUsage = null;
        MemoryUsage nonHeapUsage = null;
        double utilization = 0;

        if(memBean!=null) {
            heapUsage = memBean.getHeapMemoryUsage();
            nonHeapUsage = memBean.getNonHeapMemoryUsage();
            utilization = (double)heapUsage.getUsed()/(double)heapUsage.getMax();
        }
        return getJvmMemoryUtilization(utilization, heapUsage, nonHeapUsage);
    }

    public void setMXBean(MemoryMXBean mxBean) {
        this.memBean = mxBean;
    }

    public MemoryMXBean getMXBean() {
        return memBean;
    }

    private ProcessMemoryUtilization getJvmMemoryUtilization(double utilization,
                                                             MemoryUsage heapUsage,
                                                             MemoryUsage nonHeapUsage) {
        return new ProcessMemoryUtilization(id,
                                            utilization,
                                            getInit(heapUsage),
                                            getUsed(heapUsage),
                                            getMax(heapUsage),
                                            getCommitted(heapUsage),
                                            getInit(nonHeapUsage),
                                            getUsed(nonHeapUsage),
                                            getMax(nonHeapUsage),
                                            getCommitted(nonHeapUsage),
                                            tVals);
    }

    private double getInit(MemoryUsage mUsage) {
        return mUsage==null?-1:mUsage.getInit()/MB;
    }

    private double getUsed(MemoryUsage mUsage) {
        return mUsage==null?-1:mUsage.getUsed()/MB;
    }

    private double getMax(MemoryUsage mUsage) {
        return mUsage==null?-1:mUsage.getMax()/MB;
    }

    private double getCommitted(MemoryUsage mUsage) {
        return mUsage==null?-1:mUsage.getCommitted()/MB;
    }

}

