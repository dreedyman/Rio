/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.monitor.tasks;

import org.rioproject.event.EventHandler;
import org.rioproject.monitor.ProvisionMonitorEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to notify registered event consumers of a
 * ProvisionMonitorEvent
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorEventTask implements Runnable {
    EventHandler eventHandler;
    ProvisionMonitorEvent event;
    private static final String COMPONENT = "org.rioproject.monitor";
    private Logger logger = Logger.getLogger(COMPONENT);

    /**
     * Create a ProvisionMonitorEventTask
     * 
     * @param eventHandler The {@link org.rioproject.event.EventHandler} to
     * send the event
     * @param event The event to send
     */
    public ProvisionMonitorEventTask(EventHandler eventHandler,
                                     ProvisionMonitorEvent event) {
        if(eventHandler==null)
            throw new IllegalArgumentException("eventHandler is null");
        if(event==null)
            throw new IllegalArgumentException("event is null");
        this.eventHandler = eventHandler;
        this.event = event;
    }

    public void run() {
        try {
            eventHandler.fire(event);
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "Exception notifying ProvisionMonitorEvent consumers",
                       e);
        }
    }
}
