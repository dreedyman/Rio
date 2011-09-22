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
package org.rioproject.test.memory;

import net.jini.core.lookup.ServiceItem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.File;
import java.io.IOException;

/**
 * Test out of memory behavior
 */
@RunWith(RioTestRunner.class)
public class OutOfMemoryTest {
    @SetTestManager
    static TestManager testManager;

    @Test
    public void testForkedServiceOutOfMemory() {
        Assert.assertNotNull(testManager);
        sleep(1000);
        ServiceItem[] items = testManager.getServiceItems(ProvisionMonitor.class);
        Assert.assertNotNull(items);
        Assert.assertTrue(items.length>0);
        Listener l= new Listener();
        Throwable thrown = null;
        File opstring = new File(System.getProperty("user.dir")+File.separator+
                                    "src"+File.separator+
                                    "test"+File.separator+
                                    "resources"+File.separator+
                                    "opstring"+File.separator+
                                    "outofmemory_test.groovy");
        OperationalStringManager mgr = testManager.deploy(opstring);
        OutOfMemory outOfMemory = (OutOfMemory)testManager.waitForService(OutOfMemory.class);
        try {
            BasicEventConsumer eventConsumer = new BasicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(), l);
            eventConsumer.register(items[0]);
        } catch (Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull(thrown);
        thrown = null;
        try {
            outOfMemory.createOOME();
        } catch (IOException e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull(thrown);
        long waited = wait(l, true);
        System.out.println("Waited "+waited/1000+" seconds for failure to be observed");
        Assert.assertTrue("OutOfMemory should have failed", l.failed);
        waited = wait(l, false);
        System.out.println("Waited "+waited/1000+" seconds for re-creation to be observed");
        Assert.assertNotNull("OutOfMemory should be re-allocated", outOfMemory);

        thrown = null;
        try {
            sleep(10*1000);
            testManager.undeploy(mgr.getOperationalString().getName());
        } catch (IOException e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull(thrown);
    }

    private long wait(Listener l, boolean checkForFailure) {
        long t0 = System.currentTimeMillis();
        while(true) {
            if(checkForFailure) {
                if(l.failed)
                    break;
            } else {
                if(l.added)
                    break;
            }
            sleep(1000);                    
        }
        long t1 = System.currentTimeMillis();
        return t1-t0;
    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class Listener implements RemoteServiceEventListener {
        boolean added;
        boolean failed;

        public void notify(RemoteServiceEvent event) {
            ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
            System.out.println("ProvisionMonitorEvent->"+pme.getAction());
            if(pme.getAction().equals(ProvisionMonitorEvent.Action.SERVICE_FAILED))
                failed = true;
            if(pme.getAction().equals(ProvisionMonitorEvent.Action.SERVICE_PROVISIONED))
                added = true;
        }
    }
}
