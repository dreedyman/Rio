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
package org.rioproject.impl.system;

import org.junit.Test;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.SystemWatchID;

import java.text.NumberFormat;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dennis Reedy
 */
public class ComputeResourceAccessorTest {
    private final NumberFormat numberFormatter = NumberFormat.getNumberInstance();
    private final NumberFormat percentFormatter = NumberFormat.getPercentInstance();
    @Test
    public void getComputeResource() throws Exception {

        ComputeResource computeResource = ComputeResourceAccessor.getComputeResource();
        ComputeResourceUtilization cru = computeResource.getComputeResourceUtilization();
        assertNotNull(cru);
        String systemCPUPercent = formatPercent(cru.getCpuUtilization().getValue());
        assertNotNull(systemCPUPercent);
        String processCPUUtilization = formatPercent(getMeasuredValue(SystemWatchID.PROC_CPU, cru));
        assertNotNull(processCPUUtilization);
        String systemMemoryTotal = format(cru.getSystemMemoryUtilization().getTotal(), "MB");
        assertNotNull(systemMemoryTotal);
        String systemMemoryUtilPercent = formatPercent(cru.getSystemMemoryUtilization().getValue());
        assertNotNull(systemMemoryUtilPercent);
        String processMemoryTotal = format(cru.getProcessMemoryUtilization().getUsedHeap()," MB");
        assertNotNull(processMemoryTotal);
        String processMemoryUtilPercent = formatPercent(getMeasuredValue(SystemWatchID.JVM_MEMORY, cru));
        assertNotNull(processMemoryUtilPercent);
    }

    private Double getMeasuredValue(final String label,
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

    private String formatPercent(final Double value) {
        if (value != null && !Double.isNaN(value))
            return (percentFormatter.format(value.doubleValue()));
        return ("?");
    }

    private String format(final Double value, final String units) {
        if (value != null && !Double.isNaN(value) && value!=-1)
            return (numberFormatter.format(value.doubleValue())+units);
        return ("?");
    }

}
