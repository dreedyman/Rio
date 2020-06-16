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
package org.rioproject.test.idle;

import org.junit.Assert;
import net.jini.core.lookup.ServiceItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.impl.event.BasicEventConsumer;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.test.RioTestConfig;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.util.TimeUtil;

/**
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
@RioTestConfig (
        groups = "IdleServiceTest",
        numCybernodes = 1,
        numMonitors = 1,
        numLookups = 1,
        opstring = "src/test/resources/opstring/idle.groovy"
)
public class IdleServiceTest {
    @SetTestManager
    static TestManager testManager;

    @Test
    public void testIdleService() throws Exception {
        ServiceItem[] items = testManager.getServiceItems(ProvisionMonitor.class);
        Assert.assertEquals(1, items.length);
        ProvisionClientEventConsumer eventConsumer = new ProvisionClientEventConsumer();
        BasicEventConsumer clientEventConsumer = new BasicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(),
                                                     eventConsumer);
        clientEventConsumer.register(items[0]);
        testManager.deploy();
        int wait = 0;
        long t0 = System.currentTimeMillis();
        while(!eventConsumer.undeployed && wait < 150) {
            Thread.sleep(500);
            wait++;
        }
        System.out.println("Waited: "+ TimeUtil.format((System.currentTimeMillis() - t0)));
        Assert.assertTrue(eventConsumer.undeployed);
    }

    class ProvisionClientEventConsumer implements RemoteServiceEventListener<ProvisionMonitorEvent> {
        boolean undeployed = false;
        public void notify(ProvisionMonitorEvent event) {
            if(event.getAction().equals(ProvisionMonitorEvent.Action.OPSTRING_UNDEPLOYED)) {
                undeployed = true;
            } else {
                System.out.println("===> "+event.getAction().name());
            }
        }
    }

}
