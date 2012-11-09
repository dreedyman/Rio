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
package org.rioproject.test.sla;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.event.DynamicEventConsumer;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdType;

@RunWith(RioTestRunner.class)
public class SLAThresholdEventNotificationTest {
    @SetTestManager
    static TestManager testManager;

    @Test
    public void testSLAThresholdEventNotification() {
        Assert.assertNotNull(testManager);
        SLAThresholdEventProducer service =
            (SLAThresholdEventProducer)testManager.waitForService(SLAThresholdEventProducer.class);
        EventDescriptor eDesc = SLAThresholdEvent.getEventDescriptor();
        Assert.assertNotNull(service);
        DynamicEventConsumer eventConsumer = null;
        Throwable t = null;
        try {
            eventConsumer = new DynamicEventConsumer(eDesc,
                                                     testManager.getServiceDiscoveryManager().getDiscoveryManager());
        } catch (Exception e) {
            t = e;
            e.printStackTrace();
        }
        Assert.assertNull(t);
        Assert.assertNotNull(eventConsumer);
        Listener listener = new Listener();
        eventConsumer.register(listener);
        int waitCount = 0;
        while(!listener.upperBreachNotification() && waitCount<10) {
            try {
                service.increment();
                Thread.sleep(500);
                waitCount++;
            } catch (Exception e) {
                t = e;
                e.printStackTrace();
            }
        }
        Assert.assertNull(t);
        Assert.assertTrue(listener.upperBreachNotification());
        t = null;
        waitCount = 0;
        while(!listener.lowerBreachNotification() && waitCount<10) {
            try {
                service.decrement();
                Thread.sleep(500);
                waitCount++;
            } catch (Exception e) {
                t = e;
                e.printStackTrace();
            }
        }
        Assert.assertNull(t);
        Assert.assertTrue(listener.lowerBreachNotification());
        eventConsumer.terminate();
    }

    class Listener implements RemoteServiceEventListener {
        boolean upperBreachNotification = false;
        boolean lowerBreachNotification = false;

        boolean upperBreachNotification() {
            return upperBreachNotification;
        }

        boolean lowerBreachNotification() {
            return lowerBreachNotification;
        }

        public void notify(RemoteServiceEvent event) {
            SLAThresholdEvent slaEvent = (SLAThresholdEvent)event;
            SLA sla = slaEvent.getSLA();
            Calculable c = slaEvent.getCalculable();
            String type = slaEvent.getThresholdType().name();
            System.out.println(type+" current: "+c.getValue()+", low: " + sla.getLowThreshold() + ", high: " + sla.getHighThreshold());
            if(slaEvent.getThresholdType()== ThresholdType.BREACHED) {
                if(c.getValue()>sla.getHighThreshold())
                    upperBreachNotification = true;
                if(c.getValue()<sla.getLowThreshold())
                    lowerBreachNotification = true;
            }
        }
    }
}
