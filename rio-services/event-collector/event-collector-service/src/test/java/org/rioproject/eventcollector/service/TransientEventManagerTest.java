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

import java.util.ArrayList;
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
        for(RemoteServiceEvent event : createRemoteServiceEvents(50)) {
            eventManager.notify(event);
        }
        Assert.assertEquals(10, eventManager.getEvents(40).size());
    }

    @Test
    public void testGetEventsWithNegativeIndex() throws Exception {
        for(RemoteServiceEvent event : createRemoteServiceEvents(50)) {
            eventManager.notify(event);
        }
        Assert.assertEquals(50, eventManager.getEvents(-1).size());
    }

    @Test
    public void testGetIndex() throws Exception {
        for(RemoteServiceEvent event : createRemoteServiceEvents(50)) {
            eventManager.notify(event);
        }
        Assert.assertEquals(new Integer(50), eventManager.getIndex());
    }

    private Iterable<RemoteServiceEvent> createRemoteServiceEvents(int count) {
        List<RemoteServiceEvent> list = new ArrayList<RemoteServiceEvent>();
        for(int i=0; i<count; i++) {
            list.add(new RemoteServiceEvent("Test Event "+i));
        }
        return list;
    }
}
