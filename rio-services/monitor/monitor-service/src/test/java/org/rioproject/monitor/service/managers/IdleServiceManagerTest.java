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
package org.rioproject.monitor.service.managers;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.admin.ServiceActivityProvider;
import org.rioproject.monitor.service.TestUtil;
import org.rioproject.monitor.service.channel.ServiceChannel;
import org.rioproject.monitor.service.channel.ServiceChannelEvent;
import org.rioproject.monitor.service.channel.ServiceChannelListener;
import org.rioproject.opstring.ServiceElement;

/**
 * @author Dennis Reedy
 */
public class IdleServiceManagerTest {
    @Test
    public void testIdleServiceManager() throws InterruptedException {
        ServiceElement serviceElement = TestUtil.makeServiceElement("bar", "foo", 2);
        IdleServiceManager idleServiceManager = new IdleServiceManager(3000L, serviceElement);
        TestServiceActivityProvider sap1 = new TestServiceActivityProvider();
        TestServiceActivityProvider sap2 = new TestServiceActivityProvider();
        idleServiceManager.addService(sap1);
        Listener l = new Listener();
        ServiceChannel.getInstance().subscribe(l, serviceElement, ServiceChannelEvent.Type.IDLE);
        Thread.sleep(1000);
        idleServiceManager.addService(sap2);
        sap1.active = false;

        Thread.sleep(1000);
        sap2.active = false;

        int i = 0;
        long t0 = System.currentTimeMillis();
        while(l.notified==0 && i<10) {
            Thread.sleep(500);
            i++;
        }
        System.out.println("Waited "+(System.currentTimeMillis()-t0)+" millis");
        Assert.assertTrue(l.notified == 1);
    }

    private class TestServiceActivityProvider implements ServiceActivityProvider {
        boolean active = true;

        @Override
        public boolean isActive()  {
            return active;
        }
    }

    class Listener implements ServiceChannelListener {
        int notified = 0;
        @Override
        public void notify(ServiceChannelEvent event) {
            notified++;
        }
    }
}
