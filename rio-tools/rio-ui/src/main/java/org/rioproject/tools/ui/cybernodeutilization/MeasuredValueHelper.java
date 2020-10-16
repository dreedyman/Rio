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
package org.rioproject.tools.ui.cybernodeutilization;

import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.measurable.cpu.CpuUtilization;
import org.rioproject.system.measurable.disk.DiskSpaceUtilization;
import org.rioproject.system.measurable.memory.ProcessMemoryUtilization;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;
import org.rioproject.tools.ui.Constants;

/**
 * @author Dennis Reedy
 */
public final class MeasuredValueHelper {
    public static final double KB = 1024;
    //private static final double MB = Math.pow(KB, 2);
    public static final double GB = Math.pow(KB, 3);

    private MeasuredValueHelper() {
    }

    static Double getMeasuredValue(final String label,
                                   final ComputeResourceUtilization cru) {
        Double value = Double.NaN;
        for (MeasuredResource mRes : cru.getMeasuredResources()) {
            if (mRes.getIdentifier().equals(label)) {
                value = mRes.getValue();
                break;
            }
        }
        return value;
    }

    @SuppressWarnings("unused")
    static double getMeasuredResourceValue(final String cName,
                                           final ComputeResourceUtilization cru) {
        double value = 0;
        if (cName.equals(Constants.UTIL_PERCENT_CPU)) {
            CpuUtilization cpu = cru.getCpuUtilization();
            value = (cpu == null ? 0 : cpu.getValue());

        } else if (cName.equals(Constants.UTIL_PERCENT_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getValue());

        } else if (cName.equals(Constants.UTIL_TOTAL_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getTotal());

        } else if (cName.equals(Constants.UTIL_FREE_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getFree());

        } else if (cName.equals(Constants.UTIL_USED_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getUsed());

        } else if (cName.equals(Constants.UTIL_PERCENT_DISK)) {
            DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
            value = (disk == null ? 0 : disk.getValue());

        } else if (cName.equals(Constants.UTIL_AVAIL_DISK)) {
            DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
            value = (disk == null ? 0 : disk.getAvailable() / GB);

        } else if (cName.equals(Constants.UTIL_TOTAL_DISK)) {
            DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
            value = (disk == null ? 0 : disk.getCapacity() / GB);

        } else if (cName.equals(Constants.UTIL_PERCENT_CPU_PROC)) {
            value = getMeasuredValue("CPU (JVM)", cru);

        } else if (cName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
            value = getMeasuredValue("Memory", cru);

        } else if (cName.equals(Constants.UTIL_HEAP_MEM_JVM)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            value = (mem == null ? 0 : mem.getUsedHeap());

        } else if (cName.equals(Constants.UTIL_HEAP_MEM_AVAIL)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            value = (mem == null ? 0 : mem.getCommittedHeap());

        } else if (cName.equals(Constants.UTIL_REAL_MEM_PROC)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            value = (mem == null ? 0 : mem.getResident());
        }

        return value;
    }
}
