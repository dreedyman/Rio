/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.event;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.LeaseDeniedException;

import java.rmi.MarshalledObject;

/**
 * An {@code EventHandler} is used to fire {@link RemoteServiceEvent}s.
 *
 * @author Dennis Reedy
 */
public interface EventHandler {
    /**
     * The fire method must be overridden by classes that extend this
     * EventHandler, hence the reason the method is declared abstract.
     *
     * @param event The event to send
     *
     * @throws NoEventConsumerException This method may choose to throw a
     * NoEventConsumerException if there are no event consumers registered
     */
    void fire(RemoteServiceEvent event) throws NoEventConsumerException;

    /**
     * Registers a RemoteEventListener for this event type. This method will be
     * delegated from the EventProducer#register method invocation <br>
     *
     * @param eventSource The event source
     * @param listener RemoteEventListener
     * @param handback MarshalledObject
     * @param duration Requested EventRegistration lease <br>
     * @return EventRegistration <br>
     *
     * @throws net.jini.core.lease.LeaseDeniedException If the lease manager denies the lease
     */
    EventRegistration register(Object eventSource,
                               RemoteEventListener listener,
                               MarshalledObject handback,
                               long duration) throws LeaseDeniedException;
}
