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
package org.rioproject.test.system.measurable.memory;

import net.jini.config.EmptyConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.system.measurable.SigarHelper;
import org.rioproject.test.system.measurable.SimpleThresholdListener;
import org.rioproject.system.measurable.memory.Memory;
import org.rioproject.watch.ThresholdValues;

/**
 * Simple test of Memory class using SIGAR
 */
public class MemoryTestUsingSigar {

    @Before
    public void checkSigar() {
        Assert.assertTrue(SigarHelper.sigarAvailable());
    }

    @Test
    public void createAndVerifyMemoryClass() {
        Memory mem = new Memory(EmptyConfiguration.INSTANCE);
        SimpleThresholdListener l = new SimpleThresholdListener();
        mem.start();
        mem.checkValue();
        double utilization = mem.getUtilization();
        Assert.assertTrue("Utilization should be > 0", utilization>0);
        Assert.assertTrue(l.getType()== null);
    }

    @Test
    public void createAndVerifyMemoryWithLowerThresholdBeingCrossed() {
        Memory mem = new Memory(EmptyConfiguration.INSTANCE);
        ThresholdValues tVals = new ThresholdValues(0.30, 0.90);
        mem.setThresholdValues(tVals);
        SimpleThresholdListener l = new SimpleThresholdListener();
        mem.start();
        mem.checkValue();
        double utilization = mem.getUtilization();
        Assert.assertTrue("Utilization should be > 0", utilization>0);
        Assert.assertTrue(l.getType()== null);
    }

    @Test
    public void createAndVerifyMemoryWithUpperThresholdBeingCrossed() {
        Memory mem = new Memory(EmptyConfiguration.INSTANCE);
        ThresholdValues tVals = new ThresholdValues(0.00, 0.05);
        mem.setThresholdValues(tVals);
        SimpleThresholdListener l = new SimpleThresholdListener();
        mem.start();
        mem.checkValue();
        double utilization = mem.getUtilization();
        Assert.assertTrue("Utilization should be > 0", utilization>0);
        Assert.assertTrue(l.getType()== null);
    }

}
