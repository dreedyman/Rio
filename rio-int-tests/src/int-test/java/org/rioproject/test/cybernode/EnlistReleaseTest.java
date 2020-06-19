/*
 * Copyright 2011 the original author or authors.
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
package org.rioproject.test.cybernode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.test.RioTestConfig;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.IOException;

/**
 * Test Cybernode enlist/release semantics
 */
@RunWith(RioTestRunner.class)
@RioTestConfig (
        groups = "EnlistReleaseTest",
        numCybernodes = 1,
        numMonitors = 1,
        numLookups = 1,
        autoDeploy = false
)
public class EnlistReleaseTest {
    @SetTestManager
    static TestManager testManager;
    Cybernode cybernode;
    ProvisionMonitor monitor;

    @Before
    public void setup() {
        monitor = testManager.waitForService(ProvisionMonitor.class);
        cybernode = testManager.waitForService(Cybernode.class);
    }

    @Test
    public void testEnlistRelease() throws IOException, InterruptedException {
        testEnlist();
        testRelease();
        testReleaseEnlist();
    }

    void testEnlist() throws IOException, InterruptedException {
        ServiceBeanInstantiator[] sbis = null;
        int waited=0;
        long t0 = System.currentTimeMillis();
        while(waited<10) {
            sbis = monitor.getServiceBeanInstantiators();
            if(sbis.length==0) {
                Thread.sleep(500);
                waited++;
            } else {
                long t1 = System.currentTimeMillis();
                System.err.println("Waited "+(t1-t0)+" milliseconds for registration");
                break;
            }
        }
        Assert.assertNotNull(sbis);
        Assert.assertTrue("Expected Cybernode count to be 1, was "+sbis.length, sbis.length==1);
    }

    void testRelease() throws IOException {
        ServiceBeanInstantiator[] sbis;
        cybernode.release(true);
        Assert.assertFalse(cybernode.isEnlisted());
        sbis = monitor.getServiceBeanInstantiators();
        Assert.assertNotNull(sbis);
        Assert.assertTrue(sbis.length==0);
    }

    void testReleaseEnlist() throws IOException {
        ServiceBeanInstantiator[] sbis;

        cybernode.release(true);
        Assert.assertFalse(cybernode.isEnlisted());

        sbis = monitor.getServiceBeanInstantiators();
        Assert.assertNotNull(sbis);
        Assert.assertTrue(sbis.length==0);

        cybernode.enlist();
        Assert.assertTrue(cybernode.isEnlisted());

        sbis = monitor.getServiceBeanInstantiators();
        Assert.assertNotNull(sbis);
        Assert.assertTrue("Expected 1, got: "+sbis.length, sbis.length==1);
    }
}
