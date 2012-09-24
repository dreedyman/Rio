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

import org.junit.*;
import org.rioproject.cybernode.StaticCybernode;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.eventcollector.api.EventCollectorAdmin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Tests getting and using the {@code EventCollectorAdmin}
 * @author Dennis Reedy
 */
public class ITEventCollectorAdminTest {
    static StaticCybernode cybernode;
    EventCollectorImpl eventCollector;

    @BeforeClass
    public static void createCybernode() {
        cybernode = new StaticCybernode();
    }

    @Before
    public void createEventCollector() throws Exception {
        Map<String, Object> map = cybernode.activate(new File("src/test/opstring/EventCollector.groovy"));
        eventCollector = (EventCollectorImpl) map.get("Bones");
    }

    @After
    public void stopEventCollector() {
        cybernode.deactivate(eventCollector);
    }
    @Test
    public void testGettingTheAdminProxy() {
        EventCollectorAdmin admin = (EventCollectorAdmin) eventCollector.getAdmin();
        Assert.assertNotNull(admin);
    }

    @Test
    public void testUsingTheAdminProxy() throws IOException, InterruptedException {
        EventCollectorAdmin admin = (EventCollectorAdmin) eventCollector.getAdmin();
        Assert.assertNotNull(admin);
        PersistentEventManager eventManager = (PersistentEventManager)eventCollector.getEventManager();
        eventManager.addRemoteEvents(TransientEventManagerTest.createRemoteServiceEvents(10));
        /* Writing events to disk is an async operation, sleep for a bit before asserting */
        waitForFileCount(10, eventManager.getPersistentEventDirectory());
        Assert.assertEquals("Expected 10 events, got "+eventManager.getNumberOfCollectedEvents(),
                            10, eventManager.getNumberOfCollectedEvents());

        Collection<RemoteServiceEvent> events = eventManager.getEvents();
        int deleted = admin.delete(events);
        Assert.assertEquals("Expected 10, got "+deleted, 10, deleted);
        Assert.assertEquals("Expected 0 events, got "+eventManager.getNumberOfCollectedEvents(),
                            0, eventManager.getNumberOfCollectedEvents());
        File directory = eventManager.getPersistentEventDirectory();
        Assert.assertEquals("Expected 0 event files, got " + directory.list().length,
                            0, directory.list().length);
    }

    @Test
    public void testOnlyDeleteSome() throws IOException, InterruptedException {
        EventCollectorAdmin admin = (EventCollectorAdmin) eventCollector.getAdmin();
        Assert.assertNotNull(admin);
        PersistentEventManager eventManager = (PersistentEventManager)eventCollector.getEventManager();
        eventManager.addRemoteEvents(TransientEventManagerTest.createRemoteServiceEvents(10));
        waitForFileCount(10, eventManager.getPersistentEventDirectory());
        Assert.assertEquals("Expected 10 events, got "+eventManager.getNumberOfCollectedEvents(),
                            10, eventManager.getNumberOfCollectedEvents());
        List<RemoteServiceEvent> eventsToDelete = new ArrayList<RemoteServiceEvent>();
        eventsToDelete.addAll(eventManager.getEvents());
        for(int i=0; i<3; i++)
            eventsToDelete.remove(0);
        int deleted = admin.delete(eventsToDelete);
        Assert.assertEquals("Expected 7, got "+deleted, 7, deleted);
        Assert.assertEquals("Expected 3 events, got "+eventManager.getNumberOfCollectedEvents(),
                            3, eventManager.getNumberOfCollectedEvents());
        File directory = eventManager.getPersistentEventDirectory();
        Assert.assertEquals("Expected 3 event files, got "+directory.list().length,
                            3, directory.list().length);
        /* Now delete the rest */
        eventsToDelete.clear();
        eventsToDelete.addAll(eventManager.getEvents());
        deleted = admin.delete(eventsToDelete);
        Assert.assertEquals("Expected 3, got "+deleted, 3, deleted);
        Assert.assertEquals("Expected 0 events, got "+eventManager.getNumberOfCollectedEvents(),
                            0, eventManager.getNumberOfCollectedEvents());
        Assert.assertEquals("Expected 0 event files, got "+directory.list().length,
                            0, directory.list().length);
    }

    private void waitForFileCount(int count, File directory) throws InterruptedException {
        long waited = 0;
        while(waited<5) {
            if(directory.list().length<count) {
                Thread.sleep(1000);
                waited++;
            } else {
                break;
            }
        }
    }
}
