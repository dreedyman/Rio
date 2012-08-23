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

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.LeaseDeniedException;

import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The EventProducer defines the support for an event consumer to register for 
 * events a service produces.
 *
 * @author Dennis Reedy
 */
public interface EventProducer extends Remote {
    /**
     * The register method creates a leased
     * {@link net.jini.core.event.EventRegistration} for the EventDescriptor
     * type passed in based on the requested lease duration. The implied
     * semantics of notification are dependant on 
     * {@code org.rioproject.event.EventHandler} specializations.
     * 
     * @param descriptor The EventDescriptor to register
     * @param listener A RemoteEventListener
     * @param handback A MarshalledObject referencing a handback object to be
     * used with Event notification
     * @param duration Requested EventRegistration lease duration
     * 
     * @return An EventRegistration
     * 
     * @throws IllegalArgumentException if the descriptor parameter is null
     * @throws UnknownEventException if the service does not produce events described
     * by the EventDescriptor
     * @throws LeaseDeniedException if the duration parameter is not accepted
     * @throws RemoteException if communication errors occur
     */
    EventRegistration register(EventDescriptor descriptor,
                               RemoteEventListener listener,
                               MarshalledObject handback, 
                               long duration)
        throws LeaseDeniedException, UnknownEventException, RemoteException;
}
