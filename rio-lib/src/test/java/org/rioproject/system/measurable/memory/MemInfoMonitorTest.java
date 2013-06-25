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

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.watch.ThresholdValues;

/**
 * Test MemInfoMonitor
 * @author Dennis Reedy
 */
public class MemInfoMonitorTest {
    @Test
    public void testParsing() {
        MemInfoMonitor memInfoMonitor = new MemInfoMonitor();
        memInfoMonitor.setID("System Memory");
        memInfoMonitor.setThresholdValues(new ThresholdValues(0, 1.0));
        memInfoMonitor.setMemInfoFile("src/test/resources/meminfo");
        SystemMemoryUtilization memoryUtilization = memInfoMonitor.getMeasuredResource();
        Assert.assertNotNull(memoryUtilization);
        System.out.println("Free:   "+memoryUtilization.getFree()+" MB");
        System.out.println("Active: "+memoryUtilization.getUsed()+" MB");
        System.out.println("Total:  "+memoryUtilization.getTotal()+" MB");
        System.out.println("Used %: "+memoryUtilization.getUsedPercentage());
        System.out.println("Free %: "+memoryUtilization.getFreePercentage());
        Assert.assertTrue(1.0==memoryUtilization.getUsedPercentage()+memoryUtilization.getFreePercentage());
    }
}
