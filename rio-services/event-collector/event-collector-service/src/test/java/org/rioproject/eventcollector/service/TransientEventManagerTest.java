/*
 * Copyright to the original author or authors
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

import junit.framework.Assert;
import net.jini.config.EmptyConfiguration;
import net.jini.core.event.RemoteEvent;
import net.jini.discovery.LookupDiscovery;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test the {@code TransientEventManager}
 *
 * @author Dennis Reedy
 */
public class TransientEventManagerTest {
    TransientEventManager eventManager;

    @Before
    public void setUp() throws Exception {
        eventManager = new TransientEventManager();
        eventManager.initialize( new EventCollectorContext(EmptyConfiguration.INSTANCE,
                                                           new LinkedBlockingQueue<RemoteEvent>(),
                                                           new EventDescriptor[0],
                                                           new LookupDiscovery(new String[0]),
                                                           null));
    }

    @Test
    public void testPostNotify() throws Exception {
        RemoteServiceEvent event = new RemoteServiceEvent("Test Event");
        eventManager.notify(event);
        Assert.assertEquals(1, eventManager.getEvents().size());
    }

    @Test
    public void testGetEvents() throws Exception {
        for(RemoteServiceEvent event : createRemoteServiceEvents(5)) {
            eventManager.notify(event);
        }
        Assert.assertEquals(5, eventManager.getEvents().size());
    }

    @Test
    public void testGetEventsWithIndex() throws Exception {
        List<RemoteServiceEvent> events = createRemoteServiceEvents(50);
        for(RemoteServiceEvent event : events) {
            eventManager.notify(event);
        }
        Date index = events.get(39).getDate();

        DateFormat formatter = new SimpleDateFormat("HH:mm:ss,SSS");
        int i = 1;
        for(RemoteEvent event : eventManager.getEvents()) {
            Date date = ((RemoteServiceEvent)event).getDate();
            System.out.println("["+(i++)+"] event date: "+formatter.format(date));
        }
        System.out.print("\nRequested index from: "+formatter.format(index)+"\n\n");
        Assert.assertEquals(10, eventManager.getEvents(index).size());
    }

    @Test
    public void testGetEventsWithNullIndex() throws Exception {
        for(RemoteServiceEvent event : createRemoteServiceEvents(50)) {
            eventManager.notify(event);
        }
        Assert.assertEquals(50, eventManager.getEvents(null).size());
    }

    static List<RemoteServiceEvent> createRemoteServiceEvents(int count) {
        List<RemoteServiceEvent> list = new ArrayList<RemoteServiceEvent>();
        for(int i=0; i<count; i++) {
            RemoteServiceEvent event = new RemoteServiceEvent("Test Event "+i);
            event.setSequenceNumber(i);
            list.add(event);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assert.assertEquals(count, list.size());
        return list;
    }
}
