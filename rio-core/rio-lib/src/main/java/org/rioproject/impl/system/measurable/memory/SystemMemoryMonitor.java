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
import org.rioproject.impl.system.measurable.SigarHelper;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * The <code>SystemMemoryMonitor</code> object provides feedback information to
 * the {@link SystemMemory} object, providing memory usage information for the
 * system using SIGAR. If SIGAR is not available, this utility returns a -1 for
 * system memory utilization.
 *
 * @author Dennis Reedy
 */
public class SystemMemoryMonitor implements MeasurableMonitor<SystemMemoryUtilization> {
    private String id;
    private ThresholdValues tVals;
    private SigarHelper sigar;
    private static double KB = 1024;
    private static double MB = Math.pow(KB, 2);
    private static double GB = Math.pow(KB, 3);
    private static Logger logger = LoggerFactory.getLogger(SystemMemoryMonitor.class);

    public SystemMemoryMonitor() {
        sigar = SigarHelper.getInstance();
    }

    public void terminate() {
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public SystemMemoryUtilization getMeasuredResource() {
        SystemMemoryUtilization smu;
        if(sigar==null) {
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

            smu = new SystemMemoryUtilization(id,
                                              utilization,
                                              totalMB,
                                              freeMB,
                                              usedMB,
                                              freeSystemPercent,
                                              utilization,
                                              Double.NaN,
                                              tVals);
        } else {
            long total = sigar.getTotalSystemMemory();
            long free = sigar.getFreeSystemMemory();
            long used = sigar.getUsedSystemMemory();

            double utilization = computeUtilization(used, total);

            if(logger.isTraceEnabled()) {
                StringBuilder builder = new StringBuilder();
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(2);
                long ram = sigar.getRam();
                double d = ((double)total)/MB;
                double e = ((double)used)/MB;
                double f = ((double)ram)/KB;
                double g = ((double)free)/MB;
                String usedPerc = nf.format(sigar.getUsedSystemMemoryPercent());
                String freePerc = nf.format(sigar.getFreeSystemMemoryPercent());
                builder.append("\nTotal:       ").append(nf.format(d)).append(" MB\n");
                builder.append("Used:        ").append(nf.format(e)).append(" MB, ").append(usedPerc).append(" %\n");
                builder.append("Free:        ").append(nf.format(g)).append(" MB, ").append(freePerc).append(" %\n");
                builder.append("RAM:         ").append(nf.format(f)).append(" GB\n");
                builder.append("Utilization: ").append(utilization).append(" %");
                logger.trace(builder.toString());
            }

            smu = new SystemMemoryUtilization(id,
                                              utilization,
                                              ((double)total)/MB,
                                              ((double)free)/MB,
                                              ((double)used)/MB,
                                              sigar.getFreeSystemMemoryPercent(),
                                              sigar.getUsedSystemMemoryPercent(),
                                              sigar.getRam(),
                                              tVals);
        }
        return smu;
    }

    private double computeUtilization(long used, long total) {
        double u = (double)used/(double)total;
        BigDecimal bd = new BigDecimal(u).setScale(2, RoundingMode.FLOOR);
        return bd.doubleValue();
    }
}
