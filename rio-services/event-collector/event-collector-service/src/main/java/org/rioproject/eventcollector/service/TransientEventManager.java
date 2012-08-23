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

import net.jini.core.event.RemoteEvent;
import org.rioproject.event.RemoteServiceEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@code EventManager} that keeps an in-memory collection of events.
 *
 * @author Dennis Reedy
 */
public class TransientEventManager extends AbstractEventManager {
    private final List<RemoteEvent> events = Collections.synchronizedList(new LinkedList<RemoteEvent>());
    private static final Logger logger = Logger.getLogger(TransientEventManager.class.getName());

    @Override
    public void postNotify(RemoteServiceEvent event) {
        events.add(event);
    }

    @Override
    public Collection<RemoteEvent> getEvents() {
        return events;
    }

    @Override
    public Collection<RemoteEvent> getEvents(int index) {
        if(index==-1)
            return events;
        if(logger.isLoggable(Level.FINE))
            logger.fine(String.format("Getting sublist from index %d, size of event collection is %d",
                                      index, events.size()));
        return events.subList(index, events.size());
    }

    @Override
    public Integer getIndex() {
        return events.size();
    }

    protected List<RemoteEvent> getRemoteEventList() {
        return events;
    }
}
