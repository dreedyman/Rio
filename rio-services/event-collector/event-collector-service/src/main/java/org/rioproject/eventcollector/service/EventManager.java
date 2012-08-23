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

import java.util.Collection;

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
     * Get all known {@code RemoteEvent}s.
     *
     * @return A {@code Collection} of recorded {@code RemoteEvent}s. If there are no
     * {@code RemoteEvent}s, and empty {@code Collection} is returned.
     */
    Collection<RemoteEvent> getEvents();

    /**
     * Get all {@code RemoteEvent}s that occur after an index.
     *
     * @return A {@code Collection} of recorded {@code RemoteEvent}s. If there are no
     * {@code RemoteEvent}s, and empty {@code Collection} is returned.
     */
    Collection<RemoteEvent> getEvents(int index);

    /**
     * Get the current index into the collection of recorded {@code RemoteEvent}s.
     *
     * @return The index of the last recorded event.
     */
    Integer getIndex();

    /**
     * A request to be notified of the event history.
     *
     * @param registeredNotification A {@code RegisteredNotification}, must not be {@code null}.
     *
     * @throws IllegalArgumentException if the {@code registeredNotification} is {@code null}.
     */
    void historyNotification(RegisteredNotification registeredNotification);

    /**
     * Terminate the {@code EventManager}.
     */
    void terminate();
}
