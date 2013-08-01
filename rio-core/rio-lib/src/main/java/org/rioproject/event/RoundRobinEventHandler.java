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

import net.jini.config.Configuration;
import net.jini.config.EmptyConfiguration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import org.rioproject.resources.servicecore.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;

/**
 * The <code>RoundRobinEventHandler</code> provides an implementation of an
 * <code>EventHandler</code> which supports the notification of events using
 * round-robin semantic. This event handler will send notification of an event
 * to a valid remote event listener (one that has an active lease) is the
 * collection of event registrants. For each subsequent notification the next
 * listener in the list will be notified.
 * <p>
 * If remote event listeners are removed from the collection of event
 * registrants and the notification ordinal references the removed registrant,
 * then the notification ordinal will reference the next registrant in the
 * collection.
 * <p>
 * The sequence number for events is incremented after each subsequent fire
 * invocation.
 *
 * @author Dennis Reedy
 */
public class RoundRobinEventHandler extends EventHandler {
    static Logger logger = LoggerFactory.getLogger(RoundRobinEventHandler.class);

    /**
     * Construct a RoundRobinEventHandler with an EventDescriptor and default
     * lease maximum and time allocation
     *
     * @param descriptor The EventDescriptor
     *
     * @throws IOException If a landlord lease manager cannot be created
     */
    public RoundRobinEventHandler(final EventDescriptor descriptor) throws IOException {
        this(descriptor, EmptyConfiguration.INSTANCE);
    }

    /**
     * Construct a RoundRobinEventHandler with an EventDescriptor and a
     * Configuration object
     *
     * @param descriptor The EventDescriptor
     * @param config The configuration object
     *
     * @throws IOException If a landlord lease manager cannot be created
     */
    public RoundRobinEventHandler(final EventDescriptor descriptor, Configuration config) throws IOException {
        super(descriptor, config);
    }

    /**
     * Implement the <code>fire</code> method from <code>EventHandler</code>.
     * This method will get the next available listener and send it an event. If
     * there are no event registrants a NoEventConsumerException is thrown
     *
     * @param event The event to send
     * 
     * @throws NoEventConsumerException is there are no event registrants to
     * send the event to.
     */
    public void fire(final RemoteServiceEvent event) throws NoEventConsumerException {
        event.setEventID(descriptor.eventID);
        event.setSequenceNumber(sequenceNumber);
        while (true) {
            ServiceResource sr = getNextServiceResource();
            if(sr == null)
                throw new NoEventConsumerException("No event consumers");
            try {
                EventRegistrationResource er = (EventRegistrationResource)sr.getResource();
                RemoteEventListener listener = er.getListener();
                MarshalledObject handback = er.getHandback();
                event.setHandback(handback);
                t0 = System.currentTimeMillis();
                listener.notify(event);
                t1 = System.currentTimeMillis();
                sendTime = t1 - t0;
                if(responseWatch != null)
                    responseWatch.setElapsedTime(sendTime, t1);
                sequenceNumber++;
                sent++;
                printStats();
                break;
            } catch(UnknownEventException uee) {
                // We are allowed to cancel the lease here
                try {
                    resourceMgr.removeResource(sr);
                    landlord.cancel(sr.getCookie());
                } catch(Exception ex) {
                    logger.warn("Removing/Cancelling an EventConsumer from UnknownEventException", ex);
                }
            } catch(RemoteException re) {
                // Not sure if we are allowed to cancel the lease here, but
                // the assumption being made here is that if we cant send the
                // notification to the RemoteEventListener then the
                // RemoteEventListener
                // must not be alive. Therefore, cancel the lease
                try {
                    resourceMgr.removeResource(sr);
                    landlord.cancel(sr.getCookie());
                } catch(Exception ex) {
                    logger.warn("Removing/Cancelling an EventConsumer from RemoteException", ex);
                }
            }
        }
    }
}
