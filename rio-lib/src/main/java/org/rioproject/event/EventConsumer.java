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

import net.jini.core.event.RemoteEventListener;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;

/**
 * The EventConsumer defines the semantics for a client to register and receive
 * remote event notifications
 *
 * @author Dennis Reedy
 */
public interface EventConsumer extends RemoteEventListener {
    /**
     * Informs the EventConsumer to register for event notifications to all
     * discovered {@link EventProducer} instances that provide support for the event
     * described in an {@link EventDescriptor}
     * 
     * @param listener The RemoteServiceEventListener
     * @return True if successful, otherwise false
     *
     * @throws RemoteException if communication errors occur
     */
    boolean register(RemoteServiceEventListener listener)
        throws RemoteException;

    /**
     * Informs the EventConsumer to register for event notifications to all
     * discovered {@link EventProducer} instances that provide support for the event
     * described in an EventDescriptor. This method provides the ability to pass
     * a MarshalledObject as part of the registration
     * 
     * @param listener The RemoteServiceEventListener
     * @param handback The MarshalledObject to use when registering
     * @return True if successful, otherwise false
     *
     * @throws RemoteException if communication errors occur
     */
    boolean register(RemoteServiceEventListener listener,
                     MarshalledObject handback) throws RemoteException;

    /**
     * Informs the EventConsumer to de-register for event notifications across
     * all discovered {@link EventProducer} instances. Invocation of this method will
     * result in the cancellation of any leases involved with event registration
     * and the removal from the event notification pool
     * 
     * @param listener The RemoteServiceEventListener
     * @return True if successful, otherwise false
     *
     * @throws RemoteException if communication errors occur
     */
    boolean deregister(RemoteServiceEventListener listener)
        throws RemoteException;

    /**
     * Returns the source object returned as part of the 
     * {@link net.jini.core.event.EventRegistration}
     * 
     * @param eventID The eventID
     * @return The source object
     *
     * @throws RemoteException if communication errors occur
     */
    Object getEventRegistrationSource(long eventID) throws RemoteException;

    /**
     * The terminate method will de-register for event notifications across all
     * discovered {@link EventProducer} instances. Invocation of this method will result
     * in the cancellation of any leases involved with event registration and
     * the removal from the event notification pool. This method will also
     * unexport the EventConsumer, removing it from the RMI runtime
     *
     * @throws RemoteException if communication errors occur
     */
    void terminate() throws RemoteException;
}
