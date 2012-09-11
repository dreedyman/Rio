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

import org.rioproject.event.RemoteServiceEvent;

import java.util.Collection;
import java.util.Date;

/**
 * Defines the semantics for an {@code EventManager}, provided support to manage a collection of events.
 *
 * @author Dennis Reedy
 */
public interface EventManager {

    /**
     * Initialize the {@code EventManager}
     *
     * @param context The context for setting up the {@code EventManager}.
     *
     * @throws Exception If there are problems initializing the {@code EventManager}
     * @throws IllegalArgumentException if any of the arguments is {@code null}.
     */
    void initialize(EventCollectorContext context) throws Exception;

    /**
     * Get all known {@code RemoteServiceEvent}s.
     *
     * @return A {@code Collection} of recorded {@code RemoteEvent}s. If there are no
     * {@code RemoteEvent}s, and empty {@code Collection} is returned.
     */
    Collection<RemoteServiceEvent> getEvents();

    /**
     * Get all {@code RemoteServiceEvent}s that occur after a provided {@link Date}.
     *
     * @param from The {@code Date} to get recorded events from. May be {@code null}.
     *
     * @return A {@code Collection} of recorded {@code RemoteServiceEvent}s. If there are no
     * {@code RemoteServiceEvent}s, and empty {@code Collection} is returned. If the provided {@code Date} is {@code null},
     * return all recorded events.
     */
    Collection<RemoteServiceEvent> getEvents(Date from);

    /**
     * Get the last recorded event {@code Date} from the collection of recorded {@code RemoteServiceEvent}s.
     *
     * @return The {@code Date} of the last recorded event. If there are no recorded events, return {@code null}.
     */
    Date getLastRecordedDate();

    /**
     * A request to be notified of the event history.
     *
     * @param registeredNotification A {@code RegisteredNotification}, must not be {@code null}.
     *
     * @throws IllegalArgumentException if the {@code registeredNotification} is {@code null}.
     */
    void historyNotification(RegisteredNotification registeredNotification);

    /**
     * Delete events from the collection of events
     *
     * @param events A {@code Collection} of events.
     *
     * @return The number of events deleted
     */
    int delete(Collection<RemoteServiceEvent> events);

    /**
     * Terminate the {@code EventManager}.
     */
    void terminate();
}
