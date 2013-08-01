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
package org.rioproject.system.measurable.cpu;

import org.rioproject.system.MeasuredResource;
import org.rioproject.system.measurable.MXBeanMonitor;
import org.rioproject.system.measurable.SigarHelper;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * CPU monitor that obtains process CPU utilization. This utility uses either
 * Hyperic SIGAR, or {@link com.sun.management.OperatingSystemMXBean} to obtain
 * process CPU time. Hyperic SIGAR is preferred. If not available
 * the <tt>com.sun.management.OperatingSystemMXBean</tt> will be used.
 *
 * @author Dennis Reedy
 */
public class ProcessCPUHandler implements MXBeanMonitor<OperatingSystemMXBean> {
    private com.sun.management.OperatingSystemMXBean osMBean;
    private double startTime;
    private double cpuBefore;
    private String id;
    private ThresholdValues tVals;
    private long pid;
    private SigarHelper sigar;
    static Logger logger =
        LoggerFactory.getLogger(ProcessCPUHandler.class.getPackage().getName());

    public ProcessCPUHandler() {
        sigar = SigarHelper.getInstance();
        if(sigar!=null) {
            pid = sigar.getPid();
        }
    }

    public ProcessCPUHandler(long pid) {
        sigar = SigarHelper.getInstance();
        if (sigar!=null) {
            this.pid = pid;
        }
    }

    public void setMXBean(OperatingSystemMXBean mxBean) {
        if(mxBean instanceof com.sun.management.OperatingSystemMXBean) {
            osMBean = (com.sun.management.OperatingSystemMXBean)mxBean;
        }
    }

    public void setPID(long pid) {
        this.pid = pid;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public MeasuredResource getMeasuredResource() {
        return getUtilization();
    }

    public void terminate() {
    }

    private synchronized ProcessCpuUtilization getUtilization() {
        ProcessCpuUtilization pCpu;
        if (sigar!=null) {
            try {
                /* On rare occasions the percentage has been a negative value,
                 * always make sure its a postive value */
                double percent = Math.abs(sigar.getProcessCpuPercentage(pid));
                long sys = sigar.getProcessCpuSys(pid);
                long user = sigar.getProcessCpuUser(pid);
                //System.out.println("User time....." + user);
                //System.out.println("Sys time......" + sys);
                //System.out.println("Percent......." + percent);
                //System.out.println("------------")
                pCpu = new ProcessCpuUtilization(id, percent, sys, user, tVals);
            } catch(Exception e) {
                logger.warn("SIGAR exception getting ProcessCpu, get CPU process utilization using JMX", e);
                double percent = getUtilizationUsingJMX();
                pCpu = new ProcessCpuUtilization(id, percent, tVals);
            }
        } else {
            double percent = getUtilizationUsingJMX();
            pCpu = new ProcessCpuUtilization(id, percent, tVals);
        }

        return pCpu;
    }

    private double getUtilizationUsingJMX() {
        double utilization = 0;
        checkMXBean();
        if(osMBean==null)
            return 0;
        double cpuAfter = osMBean.getProcessCpuTime();
        double endTime = System.nanoTime();

        if(endTime>startTime) {
            utilization = (cpuAfter-cpuBefore)/(endTime-startTime);
        }
        startTime = endTime;
        cpuBefore = cpuAfter;
        return utilization;
    }

    private void checkMXBean() {
        if(osMBean==null) {
            OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
            setMXBean(mxBean);
        }
    }
}
