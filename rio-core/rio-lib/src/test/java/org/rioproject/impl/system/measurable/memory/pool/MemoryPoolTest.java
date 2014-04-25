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

import net.jini.config.EmptyConfiguration;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.impl.system.measurable.SimpleThresholdListener;
import org.rioproject.watch.ThresholdValues;

/**
 * @author Dennis Reedy
 */
public class MemoryPoolTest {

    @Before
    public void checkBeforeJava1dot8() {
        String sVersion = System.getProperty("java.version");
        String[] parts = sVersion.split("\\.");
        Double version = Double.parseDouble(String.format("%s.%s", parts[0], parts[1]));
        Assume.assumeTrue(version<1.8);
    }

    @Test
    public void createAndVerifyMemoryClass() {
        String poolName = "Perm Gen";
        MemoryPool mem = new MemoryPool(poolName, EmptyConfiguration.INSTANCE, new ThresholdValues(0.0, 0.80));
        SimpleThresholdListener l = new SimpleThresholdListener();
        mem.start();
        mem.checkValue();
        double utilization = mem.getUtilization();
        Assert.assertTrue("Utilization should be > 0, got: " + utilization, utilization > 0);
        Assert.assertTrue(l.getType()== null);
    }
}
