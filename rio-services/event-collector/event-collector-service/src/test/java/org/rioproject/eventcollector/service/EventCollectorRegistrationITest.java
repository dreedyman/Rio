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
package org.rioproject.eventcollector.service;

import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.id.UuidFactory;
import org.junit.*;
import org.rioproject.cybernode.StaticCybernode;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.EventCollectorRegistration;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;
import org.rioproject.eventcollector.proxy.Registration;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.util.TimeConstants;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class EventCollectorRegistrationITest {
    private static StaticCybernode cybernode;
    private EventCollectorImpl eventCollectorImpl;
    private EventCollector eventCollector;

    @BeforeClass
    public static void createStaticCybernode() {
        String userDir = System.getProperty("user.dir");
        File eventDir = new File(userDir, "target/events");
        if(eventDir.exists()) {
            FileUtils.remove(eventDir);
        }
        cybernode = new StaticCybernode();
    }

    @Before
    public void createEventCollector() throws Exception {
        //eventCollectorImpl = (EventCollectorImpl)cybernode.activate(EventCollectorImpl.class.getName());
        System.setProperty("event.config", "foo");
        Map<String, Object> map = cybernode.activate(new File("src/test/opstring/EventCollector.groovy"));
        eventCollectorImpl = (EventCollectorImpl) map.get("Bones");
        Assert.assertNotNull(eventCollectorImpl);
        eventCollector = (EventCollector)cybernode.getServiceProxy(eventCollectorImpl);
        Assert.assertNotNull(eventCollector);
    }

    @After
    public void tearDownEventCollector() {
        cybernode.deactivate(eventCollectorImpl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEventCollectorRegistration() throws LeaseDeniedException, IOException {
        eventCollector.register(0);
    }

    @Test
    public void testEventCollectorRegistrationLeaseTimeout() throws LeaseDeniedException, IOException, InterruptedException {
        eventCollector.register(TimeConstants.ONE_SECOND*10);
        Assert.assertTrue(eventCollectorImpl.getRegistrationSize()==1);
        System.out.println("===> Wait 20 seconds...");
        Thread.sleep(TimeConstants.ONE_SECOND*20);
        System.out.println("===> Moment of truth! "+eventCollectorImpl.getRegistrationSize());
        Assert.assertTrue(eventCollectorImpl.getRegistrationSize()==0);
    }

    @Test
    public void testEventCollectorRegistration() throws LeaseDeniedException, IOException, UnknownLeaseException {
        EventCollectorRegistration registration1 = eventCollector.register(Lease.FOREVER);
        verifyRegistration(registration1);
        Assert.assertEquals(1, eventCollectorImpl.getRegistrationSize());
        EventCollectorRegistration registration2 = eventCollector.register(Lease.FOREVER);
        verifyRegistration(registration2);
        Assert.assertEquals(2, eventCollectorImpl.getRegistrationSize());
        registration1.getLease().cancel();
        Assert.assertEquals(1, eventCollectorImpl.getRegistrationSize());
        registration2.getLease().cancel();
        Assert.assertEquals(0, eventCollectorImpl.getRegistrationSize());
    }

    @Test
    public void testEventCollectorPersistedRegistration() throws Exception {
        EventCollectorRegistration registration = eventCollector.register(Lease.FOREVER);
        verifyRegistration(registration);
        Assert.assertEquals(1, eventCollectorImpl.getRegistrationSize());
        tearDownEventCollector();
        createEventCollector();
        BasicEventListener listener = new BasicEventListener();
        System.out.println("===> enableDelivery()");
        registration.enableDelivery(listener.export());
        System.out.println("===> getLease()");
        registration.getLease().cancel();
    }

    @Test(expected = UnknownEventCollectorRegistration.class)
    public void testEventCollectorRegistrationCanceled() throws Exception {
        EventCollectorRegistration registration = eventCollector.register(Lease.FOREVER);
        verifyRegistration(registration);
        Assert.assertEquals(1, eventCollectorImpl.getRegistrationSize());
        BasicEventListener listener = new BasicEventListener();
        registration.getLease().cancel();
        registration.enableDelivery(listener.export());
    }

    @Test(expected = UnknownEventCollectorRegistration.class)
    public void testUnknownEventCollectorRegistration() throws UnknownEventCollectorRegistration, IOException {
        EventCollectorRegistration unknownRegistration = new Registration(eventCollectorImpl,
                                                                          UuidFactory.generate(),
                                                                          null);
        unknownRegistration.enableDelivery(null);

    }

    private void verifyRegistration(EventCollectorRegistration registration) throws IOException {
        Assert.assertNotNull(registration);
        Assert.assertNotNull(registration.getLease());
    }

}