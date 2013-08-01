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
package org.rioproject.event;

import net.jini.config.EmptyConfiguration;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.resources.servicecore.LandlordLessor;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.Watch;
import org.rioproject.watch.WatchDataSourceRegistry;

import java.io.File;
import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests event consumer behavior
 */
public class BasicEventConsumerTest {

    @BeforeClass
    public static void setEnv() {
        System.setProperty(ResolverHelper.RESOLVER_JAR,
                           System.getProperty("user.dir")+
                           File.separator+"target"+
                           File.separator+"test-classes"+
                           File.separator+"phony.jar");
    }

    @Test
    public void testCreateBasicEventConsumerWithNullEventDescriptor() throws Exception {
        /* Expect that we should be able to create a BEC with a null EventDescriptor */
        new BasicEventConsumer((EventDescriptor) null);
    }

    @Test
    public void testCreateBasicEventConsumerWithNullRemoteServiceEventListener() throws Exception {
        /* Expect that we should be able to create a BEC with a null RemoteServiceEventListener */
        new BasicEventConsumer((RemoteServiceEventListener) null);
    }

    @Test
    public void testCreateBasicEventConsumerWithEventDescriptor() throws Exception {
        new BasicEventConsumer(new EventDescriptor());
    }

    @Test
    public void testCreateBasicEventConsumerWithRemoteServiceEventListener() throws Exception {
        new BasicEventConsumer(new RemoteServiceEventListener() {
            public void notify(RemoteServiceEvent event) {
            }
        });
    }

    @Test
    public void testRegisteringAnEventProducerAndFiringAnEvent() throws Exception {
        BasicEventConsumer consumer = new BasicEventConsumer(getEventDescriptor());
        Listener listener = new Listener();
        consumer.register(listener);
        Producer p = new Producer();
        p.createDEH();
        ServiceItem serviceItem = createServiceItem(p);
        EventRegistration eventRegistration = consumer.register(serviceItem);
        Assert.assertNotNull(eventRegistration);
        p.fire();
        listener.countDown.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("Expected listener count to be > 0, found: " + listener.counter.get(),
                          listener.counter.get() > 0);
    }

    @Test
    public void testRegisteringAnEventProducerAndFiringAnEventWithLeaseBeingDropped() throws Exception {
        BasicEventConsumer consumer = new BasicEventConsumer(getEventDescriptor());
        Listener listener = new Listener();
        consumer.register(listener);
        Producer p = new Producer();
        p.setLeaseTime(3*1000);
        p.createDEH();
        ServiceItem serviceItem = createServiceItem(p);
        EventRegistration eventRegistration = consumer.register(serviceItem);
        Assert.assertNotNull(eventRegistration);
        System.err.println("Waiting 5 seconds for lease to timeout...");
        Thread.sleep(5*1000);
        p.fire();
        Assert.assertTrue("Should have not been notified, but got: "+listener.counter.get(), listener.counter.get()==0);
    }

    @Test
    public void testUsingWatch() throws Exception {
        BasicEventConsumer consumer = new BasicEventConsumer(getEventDescriptor());
        WatchDataSourceRegistry watchRegistry = new WatchDataSourceRegistry();
        Watch watch = consumer.createWatch(watchRegistry);
        Assert.assertNotNull(watch);
        Assert.assertNotNull("Check Watch accessor", consumer.getWatch());
        Assert.assertTrue("Only one watch should be created, calling createWatch twice should return the same watch",
                          watch.equals(consumer.createWatch(watchRegistry)));
        Listener listener = new Listener();
        consumer.register(listener);
        Producer p = new Producer();
        p.createDEH();
        ServiceItem serviceItem = createServiceItem(p);
        EventRegistration eventRegistration = consumer.register(serviceItem);
        Assert.assertNotNull(eventRegistration);
        p.fire();
        p.fire();
        Assert.assertTrue("Should have gotten 2, got "+watch.getCalculables().values().size(),
                          watch.getCalculables().values().size()==2);
        Calculable[] calculables = watch.getWatchDataSource().getCalculable();
        Assert.assertTrue("Should have gotten 2, got "+calculables.length, calculables.length==2);
        for (Calculable calculable : calculables)
            System.out.println("==> " + calculable.getValue());

    }

    @Test
    public void testRegisteringAnEventProducerAndFiringEventsUsingRoundRobin() throws Exception {
        Producer p = new Producer();
        p.createRoundRobin();
        ServiceItem serviceItem = createServiceItem(p);

        BasicEventConsumer consumer1 = new BasicEventConsumer(getEventDescriptor());
        Listener listener1 = new Listener(2);
        consumer1.register(listener1);

        BasicEventConsumer consumer2 = new BasicEventConsumer(getEventDescriptor());
        Listener listener2 = new Listener(2);
        consumer2.register(listener2);

        BasicEventConsumer consumer3 = new BasicEventConsumer(getEventDescriptor());
        Listener listener3 = new Listener(2);
        consumer3.register(listener3);

        Assert.assertNotNull(consumer1.register(serviceItem));
        Assert.assertNotNull(consumer2.register(serviceItem));
        Assert.assertNotNull(consumer3.register(serviceItem));

        for(int i=0; i<6; i++)
            p.fire();

        Assert.assertTrue("Should have gotten 2, got "+listener1.counter.get(), listener1.countDown.await(5, TimeUnit.SECONDS));
        Assert.assertTrue("Should have gotten 2, got "+listener2.counter.get(), listener2.countDown.await(5, TimeUnit.SECONDS));
        Assert.assertTrue("Should have gotten 2, got "+listener3.counter.get(), listener3.countDown.await(5, TimeUnit.SECONDS));
    }

    private ServiceItem createServiceItem(Producer p) throws Exception {
        UUID uuid = UUID.randomUUID();
        ServiceID sid = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        Entry[] attributes = new Entry[]{getEventDescriptor()};
        return new ServiceItem(sid,  p, attributes);
    }

    private static EventDescriptor getEventDescriptor() {
        return new EventDescriptor(Object.class, (long)1);
    }

    public static class Producer implements EventProducer {
        Object remote;
        AtomicLong sequenceCounter = new AtomicLong();
        LandlordLessor landlordLessor;
        EventHandler eventHandler;
        long leaseTime = 30*1000;

        Producer() throws Exception {
            Exporter exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                                      new BasicILFactory(),
                                                      false,
                                                      true);
            remote = exporter.export(this);
            landlordLessor = new LandlordLessor(EmptyConfiguration.INSTANCE);
        }

        void createDEH() throws Exception {
            eventHandler = new DispatchEventHandler(getEventDescriptor());
        }

        void createRoundRobin() throws Exception {
            eventHandler = new RoundRobinEventHandler(getEventDescriptor());
        }

        void setLeaseTime(long leaseTime) {
            this.leaseTime = leaseTime;
        }

        @Override
        public EventRegistration register(EventDescriptor descriptor,
                                          RemoteEventListener listener,
                                          MarshalledObject handback,
                                          long duration) throws LeaseDeniedException, UnknownEventException {
            eventHandler.register(remote, listener, handback, leaseTime);
            return new EventRegistration(descriptor.eventID,
                                         remote,
                                         landlordLessor.newLease(new ServiceResource(new Object()), leaseTime),
                                         sequenceCounter.incrementAndGet());
        }

        void fire() throws NoEventConsumerException {
            TestEvent testEvent = new TestEvent(remote);
            testEvent.setSequenceNumber(sequenceCounter.incrementAndGet());
            eventHandler.fire(testEvent);
        }
    }

    static class TestEvent extends RemoteServiceEvent implements Serializable {
        public TestEvent(Object source) {
            super(source);
        }
    }

    class Listener implements RemoteServiceEventListener {
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch countDown;

        Listener() {
            countDown = new CountDownLatch(1);
        }

        Listener(int startCountFrom) {
            countDown = new CountDownLatch(startCountFrom);
        }

        public void notify(RemoteServiceEvent event) {
            counter.incrementAndGet();
            countDown.countDown();
        }
    }
}
