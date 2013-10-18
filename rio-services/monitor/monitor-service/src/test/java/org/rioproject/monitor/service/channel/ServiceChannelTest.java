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
package org.rioproject.monitor.service.channel;


import junit.framework.Assert;
import org.junit.Test;
import org.rioproject.monitor.service.TestUtil;
import org.rioproject.opstring.ServiceElement;

/**
 * @author Dennis Reedy
 */
public class ServiceChannelTest {
    @Test
    public void testSubscribe() {
        ServiceChannel serviceChannel = ServiceChannel.getInstance();
        ServiceElement serviceElement = TestUtil.makeServiceElement("bar", "foo");
        Listener l1 = new Listener();
        serviceChannel.subscribe(l1, serviceElement, ServiceChannelEvent.Type.PROVISIONED);
        Listener l2 = new Listener();
        serviceChannel.subscribe(l2, serviceElement, ServiceChannelEvent.Type.FAILED);
        Listener l3 = new Listener();
        serviceChannel.subscribe(l3, serviceElement, ServiceChannelEvent.Type.IDLE);

        serviceChannel.broadcast(new ServiceChannelEvent(new Object(), serviceElement, ServiceChannelEvent.Type.PROVISIONED));
        Assert.assertTrue(l1.notified);
        Assert.assertFalse(l2.notified);
        Assert.assertFalse(l3.notified);
        l1.notified = false;

        serviceChannel.broadcast(new ServiceChannelEvent(new Object(), serviceElement, ServiceChannelEvent.Type.FAILED));
        Assert.assertFalse(l1.notified);
        Assert.assertTrue(l2.notified);
        Assert.assertFalse(l3.notified);
        l2.notified = false;

        serviceChannel.broadcast(new ServiceChannelEvent(new Object(), serviceElement, ServiceChannelEvent.Type.IDLE));
        Assert.assertFalse(l1.notified);
        Assert.assertFalse(l2.notified);
        Assert.assertTrue(l3.notified);
        l3.notified = false;
    }

    class Listener implements ServiceChannelListener {
        boolean notified = false;
        @Override
        public void notify(ServiceChannelEvent event) {
            notified = true;
        }
    }
}
