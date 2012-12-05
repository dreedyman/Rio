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
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Test Cybernode enlist/release semantics
 */
@RunWith(RioTestRunner.class)
public class EnlistReleaseTest {
    @SetTestManager
    static TestManager testManager;
    Cybernode cybernode;
    ProvisionMonitor monitor;

    @Before
    public void setup() {
        monitor = (ProvisionMonitor)testManager.waitForService(ProvisionMonitor.class);
        cybernode = (Cybernode)testManager.waitForService(Cybernode.class);
    }

    @Test
    public void testEnlist() {
        ServiceBeanInstantiator[] sbis = null;
        Throwable thrown = null;
        int waited=0;
        try {
            Assert.assertTrue(cybernode.isEnlisted());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        long t0 = System.currentTimeMillis();
        while(waited<10) {
            try {
                sbis = monitor.getServiceBeanInstantiators();
                if(sbis.length==0) {
                    Thread.sleep(500);
                    waited++;
                } else {
                    long t1 = System.currentTimeMillis();
                    System.err.println("Waited "+(t1-t0)+" milliseconds for registration");
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                thrown = e;
                break;
            }
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(sbis);
        Assert.assertTrue("Expected Cybernode count to be 1, was "+sbis.length, sbis.length==1);
    }

    @Test
    public void testRelease() {
        ServiceBeanInstantiator[] sbis = null;
        Throwable thrown = null;
        try {
            cybernode.release(true);
        } catch (RemoteException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        try {
            Assert.assertFalse(cybernode.isEnlisted());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        thrown = null;
        try {
            sbis = monitor.getServiceBeanInstantiators();
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(sbis);
        Assert.assertTrue(sbis.length==0);
    }

    @Test
    public void testReleaseEnlist() {
        ServiceBeanInstantiator[] sbis = null;
        Throwable thrown = null;
        try {
            cybernode.release(true);
        } catch (RemoteException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        try {
            Assert.assertFalse(cybernode.isEnlisted());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        thrown = null;
        try {
            sbis = monitor.getServiceBeanInstantiators();
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(sbis);
        Assert.assertTrue(sbis.length==0);

        thrown = null;
        try {
            cybernode.enlist();
        } catch (RemoteException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        thrown = null;
        try {
            sbis = monitor.getServiceBeanInstantiators();
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(sbis);
        Assert.assertTrue(sbis.length==1);
    }
}
