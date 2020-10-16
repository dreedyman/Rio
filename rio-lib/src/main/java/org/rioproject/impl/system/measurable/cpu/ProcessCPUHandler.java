/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.impl.system.measurable.cpu;

import org.rioproject.impl.system.measurable.MXBeanMonitor;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.measurable.cpu.ProcessCpuUtilization;
import org.rioproject.watch.ThresholdValues;

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
    private String id;
    private ThresholdValues tVals;

    public void setMXBean(OperatingSystemMXBean mxBean) {
        if (mxBean instanceof com.sun.management.OperatingSystemMXBean) {
            osMBean = (com.sun.management.OperatingSystemMXBean)mxBean;
        }
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public ProcessCpuUtilization getMeasuredResource() {
        return getUtilization();
    }

    public void terminate() {
    }

    private synchronized ProcessCpuUtilization getUtilization() {
        double percent = getUtilizationUsingJMX();
        return new ProcessCpuUtilization(id, percent, tVals);
    }

    private double getUtilizationUsingJMX() {
        checkMXBean();
        if (osMBean == null) {
            return 0;
        }
        return osMBean.getProcessCpuLoad();
    }

    private void checkMXBean() {
        if (osMBean == null) {
            OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
            setMXBean(mxBean);
        }
    }
}
