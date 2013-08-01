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
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.resources.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;

import static java.lang.String.format;

/**
 * The <code>DispatchEventHandler</code> provides an implementation of an
 * <code>EventHandler</code> which supports the notification of events to all
 * registered event listeners with a valid lease.
 * <p>
 * Event registrations are leased and explicitly checked prior to each
 * notification being sent to the remote event listener
 * <p>
 * The sequence number for events is incremented after each subsequent fire
 * invocation.
 *
 * @author Dennis Reedy
 */
public class DispatchEventHandler extends EventHandler {
    static Logger logger = LoggerFactory.getLogger(DispatchEventHandler.class);

    /**
     * Construct a DispatchEventHandler with an EventDescriptor and default
     * lease maximum and time allocation
     *
     * @param descriptor The EventDescriptor
     *
     * @throws IOException If a landlord lease manager cannot be created
     */
    public DispatchEventHandler(EventDescriptor descriptor) throws IOException {
        super(descriptor);
    }

    /**
     * Construct a DispatchEventHandler with an EventDescriptor and a
     * Configuration object
     *
     * @param descriptor The EventDescriptor
     * @param config The configuration object
     *
     * @throws IOException If a landlord lease manager cannot be created
     */
    public DispatchEventHandler(EventDescriptor descriptor, Configuration config) throws IOException {
        super(descriptor, config);
    }

    /**
     * Implement the <code>fire</code> method from <code>EventHandler</code>
     */
    public void fire(RemoteServiceEvent event) {
        event.setEventID(descriptor.eventID);
        event.setSequenceNumber(sequenceNumber);
        ServiceResource[] resources = resourceMgr.getServiceResources();
        if(logger.isTraceEnabled())
            logger.trace(format("DispatchEventHandler: notify [%d] listeners " +
                                "with event [%s]", resources.length,
                                event.getClass().getName()));
        for (ServiceResource sr : resources) {
            EventRegistrationResource er =
                (EventRegistrationResource) sr.getResource();
            if (!landlord.ensure(sr)) {
                if (logger.isTraceEnabled())
                    logger.trace(format("DispatchEventHandler.fire() Could not ensure " +
                                        "lease for ServiceResource " +
                                        "[%s] resources count now : %d",
                                        er.getListener().getClass().getName(),
                                        resourceMgr.getServiceResources().length));
                try {
                    resourceMgr.removeResource(sr);
                    landlord.remove(sr);
                } catch (Exception e) {
                    if (logger.isTraceEnabled())
                        logger.trace("Removing Resource and Cancelling Lease", e);
                }
                continue;
            }
            try {
                RemoteEventListener listener = er.getListener();
                MarshalledObject handback = er.getHandback();
                event.setHandback(handback);
                t0 = System.currentTimeMillis();
                listener.notify(event);
                t1 = System.currentTimeMillis();
                sendTime = t1 - t0;
                if (responseWatch != null)
                    responseWatch.setElapsedTime(sendTime, t1);
                sent++;
                printStats();
            } catch (UnknownEventException uee) {
                if (logger.isTraceEnabled())
                    logger.trace(format("UnknownEventException for EventDescriptor [%s]", descriptor.toString()), uee);
                /* We are allowed to cancel the lease here */
                try {
                    resourceMgr.removeResource(sr);
                    landlord.cancel(sr.getCookie());
                } catch (Exception e) {
                    if (logger.isTraceEnabled())
                        logger.warn("Removing resource and cancelling Lease", e);
                }
            } catch (RemoteException re) {
                if (logger.isTraceEnabled())
                    logger.trace(format("fire() for EventDescriptor [%s]", descriptor.toString()), re);
                /* Cancel the Lease if the EventConsumer is unreachable */
                if(!ThrowableUtil.isRetryable(re)) {
                    try {
                        resourceMgr.removeResource(sr);
                        landlord.cancel(sr.getCookie());
                    } catch (Exception e) {
                        if (logger.isTraceEnabled())
                            logger.trace("Removing resource and cancelling Lease", e);
                    }
                }
            }
        }
        sequenceNumber++;
    }
}
