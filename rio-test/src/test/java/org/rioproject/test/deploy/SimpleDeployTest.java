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
package org.rioproject.test.deploy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.ServiceMonitor;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.simple.Simple;

import java.io.File;

/**
 * Tests simple deploy scenarios
 */
@RunWith(RioTestRunner.class)
public class SimpleDeployTest {
    @SetTestManager
    static TestManager testManager;
    Cybernode cybernode;

    @Before
    public void setup() {
        cybernode = (Cybernode)testManager.waitForService(Cybernode.class);
    }

    @Test
    public void testSimple() {
        Assert.assertNotNull(testManager);
        Assert.assertNotNull(cybernode);
        Throwable thrown = null;
        try {
            /* Count the generated groovy config files in /tmp. We want to
             * make sure that if we generate a temp config file it is removed*/
            int configFilesInTmp = countTempConfigFiles();
            OperationalStringManager mgr = testManager.deploy();
            Assert.assertNotNull("Expected non-null OperationalStringManager", mgr);
            OperationalString opstring = mgr.getOperationalString();
            Assert.assertNotNull(opstring);
            Assert.assertEquals(1, opstring.getServices().length);
            testManager.waitForDeployment(mgr);
            ServiceBeanInstance[] instances =
                cybernode.getServiceBeanInstances(opstring.getServices()[0]);
            Assert.assertEquals(1, instances.length);
            Simple simple = (Simple)instances[0].getService();
            Assert.assertEquals("Hello visitor : 1", simple.hello("hi"));

            Assert.assertEquals(configFilesInTmp, countTempConfigFiles());

            testManager.undeploy(opstring.getName());
        } catch(Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull("Should not have thrown an exception", thrown);
    }

    @Test
    public void testSimpleIncrement() {
        Assert.assertNotNull(testManager);
        Assert.assertNotNull(cybernode);
        Throwable thrown = null;
        try {
            OperationalStringManager mgr = testManager.deploy();
            Assert.assertNotNull("Expected non-null OperationalStringManager", mgr);
            OperationalString opstring = mgr.getOperationalString();
            Assert.assertNotNull(opstring);
            Assert.assertEquals(1, opstring.getServices().length);
            testManager.waitForDeployment(mgr);
            ServiceBeanInstance[] instances =
                cybernode.getServiceBeanInstances(opstring.getServices()[0]);
            Assert.assertEquals(1, instances.length);            
            mgr.increment(opstring.getServices()[0], true, null);
            ServiceMonitor<Simple> sMon =
                new ServiceMonitor<Simple>(testManager.getServiceDiscoveryManager(), Simple.class);
            sMon.waitFor(2);
            instances =
                cybernode.getServiceBeanInstances(opstring.getServices()[0]);
            Assert.assertEquals(2, instances.length);
            for(ServiceBeanInstance sbi : instances) {
                Simple h = (Simple)sbi.getService();
                Assert.assertEquals("Hello visitor : 1", h.hello("hi"));
            }
            testManager.undeploy(opstring.getName());
        } catch(Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull("Should not have thrown an exception", thrown);
    }

    @Test
    public void testSimpleDecrement() {
        Assert.assertNotNull(testManager);
        Throwable thrown = null;
        try {
            OperationalStringManager mgr = testManager.deploy();
            Assert.assertNotNull("Expected non-null OperationalStringManager", mgr);
            OperationalString opstring = mgr.getOperationalString();
            Assert.assertNotNull(opstring);
            Assert.assertEquals(1, opstring.getServices().length);
            testManager.waitForDeployment(mgr);
            ServiceBeanInstance[] instances =
                cybernode.getServiceBeanInstances(opstring.getServices()[0]);
            Assert.assertEquals(1, instances.length);
            Simple simple = (Simple)instances[0].getService();
            Assert.assertEquals("Hello visitor : 1", simple.hello("hi"));
            mgr.decrement(instances[0], true, true);
            ServiceMonitor<Simple> sMon =
                new ServiceMonitor<Simple>(testManager.getServiceDiscoveryManager(), Simple.class);
            sMon.waitFor(0);
            instances =
                cybernode.getServiceBeanInstances(opstring.getServices()[0]);
            Assert.assertEquals(0, instances.length);
            testManager.undeploy(opstring.getName());
        } catch(Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull("Should not have thrown an exception", thrown);
    }

    private int countTempConfigFiles() {
        int count = 0;
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        for(String s : tmpDir.list()) {
            if(s.endsWith(".groovy") || s.endsWith(".config"))
                count++;
        }
        return count;
    }
}
