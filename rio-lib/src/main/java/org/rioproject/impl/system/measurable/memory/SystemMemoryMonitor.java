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

import org.rioproject.impl.system.measurable.MeasurableMonitor;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;
import org.rioproject.watch.ThresholdValues;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The <code>SystemMemoryMonitor</code> object provides feedback information to
 * the {@link SystemMemory} object, providing memory usage information for the
 * system\.
 *
 * @author Dennis Reedy
 */
public class SystemMemoryMonitor implements MeasurableMonitor<SystemMemoryUtilization> {
    private String id;
    private ThresholdValues tVals;
    private static final double KB = 1024;
    private static final double MB = Math.pow(KB, 2);

    public void terminate() {
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public SystemMemoryUtilization getMeasuredResource() {
        com.sun.management.OperatingSystemMXBean bean =
                (com.sun.management.OperatingSystemMXBean)
                        java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        long total = bean.getTotalPhysicalMemorySize();
        long free = bean.getFreePhysicalMemorySize();
        double totalMB = ((double)total)/MB;
        double freeMB = ((double)free)/MB;
        double usedMB = totalMB-freeMB;
        double utilization = computeUtilization(total-free, total);
        double freeSystemPercent = computeUtilization(free, total);

        return new SystemMemoryUtilization(id,
                                           utilization,
                                           totalMB,
                                           freeMB,
                                           usedMB,
                                           freeSystemPercent,
                                           utilization,
                                           Double.NaN,
                                           tVals);
    }

    private double computeUtilization(long used, long total) {
        double u = (double)used/(double)total;
        BigDecimal bd = new BigDecimal(u).setScale(2, RoundingMode.FLOOR);
        return bd.doubleValue();
    }
}
