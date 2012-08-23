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

import net.jini.config.Configuration;
import net.jini.core.event.RemoteEvent;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.event.EventDescriptor;

import java.io.File;
import java.util.concurrent.BlockingQueue;

/**
 * Holds properties for the event collector
 * @author Dennis Reedy
 */
public class EventCollectorContext {
    private final Configuration config;
    private final BlockingQueue<RemoteEvent> eventQueue;
    private final EventDescriptor[] eventDescriptors;
    private final DiscoveryManagement discoveryManager;
    private final File persistentDirectoryRoot;

    public EventCollectorContext(Configuration config,
                                 BlockingQueue<RemoteEvent> eventQueue,
                                 EventDescriptor[] eventDescriptors,
                                 DiscoveryManagement discoveryManager,
                                 File persistentDirectoryRoot) {
        this.config = config;
        this.eventQueue = eventQueue;
        this.eventDescriptors = eventDescriptors;
        this.discoveryManager = discoveryManager;
        this.persistentDirectoryRoot = persistentDirectoryRoot;
    }

    public Configuration getConfiguration() {
        return config;
    }

    public BlockingQueue<RemoteEvent> getEventQueue() {
        return eventQueue;
    }

    public EventDescriptor[] getEventDescriptors() {
        return eventDescriptors;
    }

    public DiscoveryManagement getDiscoveryManager() {
        return discoveryManager;
    }

    public File getPersistentDirectoryRoot() {
        return persistentDirectoryRoot;
    }
}
