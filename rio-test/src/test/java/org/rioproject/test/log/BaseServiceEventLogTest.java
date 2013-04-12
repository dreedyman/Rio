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
package org.rioproject.test.log;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.simple.Simple;

import java.io.File;
import java.rmi.RemoteException;

/**
 * @author Dennis Reedy
 */
public class BaseServiceEventLogTest {
    @SetTestManager
    static TestManager testManager;
    ProvisionMonitor monitor;

    @Before
    public void setup() {
        Assert.assertNotNull(testManager);
        monitor = (ProvisionMonitor)testManager.waitForService(ProvisionMonitor.class);
    }

    @Test
    public void testNotifyWithContainedService() {
        File opstring = new File(System.getProperty("user.dir")+File.separator+
                                 "src"+File.separator+
                                 "test"+File.separator+
                                 "resources"+File.separator+
                                 "opstring"+File.separator+
                                 "logging_simple_opstring.groovy");
        Assert.assertNotNull(opstring);
        testManager.deploy(opstring);
        Simple simple = (Simple)testManager.waitForService(Simple.class);
        Assert.assertNotNull(simple);

        Entry[] attrs = new Entry[]{ServiceLogEvent.getEventDescriptor()};
        ServiceTemplate template = new ServiceTemplate(null, null, attrs);
        ServiceItem[] items = testManager.getServiceDiscoveryManager().lookup(template, Integer.MAX_VALUE, null);
        try {
            Assert.assertEquals("Expected 2 services", 2, items.length);
            doVerifyLogging(items, simple, 1);
        } finally {
            testManager.undeployAll(monitor);
        }
    }

    @Test
    public void testNotifyWithForkedService() {
        Throwable thrown = null;
        try {
            DeployAdmin dAdmin = (DeployAdmin)monitor.getAdmin();
            Assert.assertEquals("Expected no active deployments",
                                0,
                                dAdmin.getOperationalStringManagers().length);
        } catch (RemoteException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        File opstring = new File(System.getProperty("user.dir")+File.separator+
                                 "src"+File.separator+
                                 "test"+File.separator+
                                 "resources"+File.separator+
                                 "opstring"+File.separator+
                                 "logging_simple_forked_opstring.groovy");
        Assert.assertNotNull(opstring);
        OperationalStringManager mgr = testManager.deploy(opstring);
        testManager.waitForDeployment(mgr);
        Simple simple = (Simple)testManager.waitForService(Simple.class, "Simple Logging Forked Simon");
        Assert.assertNotNull(simple);

        Entry[] attrs = new Entry[]{ServiceLogEvent.getEventDescriptor()};
        ServiceTemplate template = new ServiceTemplate(null, null, attrs);
        ServiceItem[] items = testManager.getServiceDiscoveryManager().lookup(template,
                                                                              Integer.MAX_VALUE,
                                                                              null);
        Assert.assertEquals("Expected 3 services to have ServiceLogEvent descriptors", 3, items.length);
        doVerifyLogging(items, simple, 3);

        testManager.undeployAll(monitor);
    }

    private void doVerifyLogging(ServiceItem[] items, Simple simple, int expected) {
        EC ec = new EC();
        BasicEventConsumer bec = null;

        try {
            bec = new BasicEventConsumer(ServiceLogEvent.getEventDescriptor(), ec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(bec);
        for(ServiceItem item : items)
            bec.register(item);

        Throwable thrown = null;
        try {
            simple.hello(null);
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertEquals("Expected "+expected+" notifications", expected, ec.getNotificationCount());

        thrown = null;
        String result = null;
        ec.resetNotificationCount();
        try {
            result = simple.hello("Hola");
        } catch (RemoteException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(result);
        Assert.assertEquals("Expected 0 notifications", 0, ec.getNotificationCount());
    }


    class EC implements RemoteServiceEventListener {
        int notifications = 0;

        int getNotificationCount() {
            return notifications;
        }

        void resetNotificationCount() {
            notifications = 0;
        }

        public void notify(RemoteServiceEvent event) {
            Assert.assertTrue(event instanceof ServiceLogEvent);
            notifications++;
        }
    }
}
