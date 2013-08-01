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
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import org.rioproject.resources.servicecore.LandlordLessor;
import org.rioproject.resources.servicecore.LeasedListManager;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.watch.StopWatch;
import org.rioproject.watch.Watch;
import org.rioproject.watch.WatchDataSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.NoSuchElementException;

/**
 * The EventHandler is an abstract class that handles the basic event plumbing.
 * The EventHandler sets up a LandLordLessor for the event type and creates
 * leased event registrations for event registrants
 *
 * @author Dennis Reedy
 */
public abstract class EventHandler {
    protected EventDescriptor descriptor;
    protected LandlordLessor landlord;
    protected int sent = 0;
    protected long sktime, ektime;
    protected float tmp;
    protected StopWatch responseWatch = null;
    protected WatchDataSourceRegistry watchRegistry = null;
    protected LeasedListManager resourceMgr;
    public static final String RESPONSE_WATCH = "Response Time - ";
    protected long t0, t1, sendTime;
    static final Logger logger = LoggerFactory.getLogger(EventHandler.class);
    /**
     * The sequence number is an increasing value that will act as a hint to the
     * number of occurrences of an event type. The sequence number should differ
     * if and only if the RemoteEvent objects are a response to different
     * events. The sequence number should be increased only after an event has
     * been sent
     */
    protected long sequenceNumber = 0;

    /**
     * Use this constructor to create an EventHandler for a given
     * EventDescriptor with a LandlordLessor created with default values used
     * for LeasePeriodPolicy
     * 
     * @param descriptor EventDescriptor for the event to handle
     *
     * @throws IOException If a landlord lease manager cannot be created
     */
    public EventHandler(EventDescriptor descriptor) throws IOException {
        this(descriptor, net.jini.config.EmptyConfiguration.INSTANCE);
    }

    /**
     * Use this constructor to create an EventHandler for a given
     * EventDescriptor with a LandlordLessor created with specific values used
     * for LeaseDurationPolicy. If either of the values specified for the lease
     * duration are null defaults will be used to create the LandlordLessor.
     * 
     * @param descriptor EventDescriptor for the event to handle
     * @param config A Configuration object
     *
     * @throws IOException If a landlord lease manager cannot be created
     */
    public EventHandler(EventDescriptor descriptor, Configuration config) throws IOException {
        if(descriptor == null)
            throw new IllegalArgumentException("descriptor is null");
        this.descriptor = descriptor;
        resourceMgr = new LeasedListManager();
        landlord = new LandlordLessor(config);
        landlord.addLeaseListener(resourceMgr);
    }

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
     * @throws LeaseDeniedException If the lease manager denies the lease
     */
    public EventRegistration register(Object eventSource,
                                      RemoteEventListener listener,
                                      MarshalledObject handback, 
                                      long duration) throws LeaseDeniedException {
        EventRegistrationResource resource = new EventRegistrationResource(listener, handback);
        ServiceResource sr = new ServiceResource(resource);
        Lease lease = landlord.newLease(sr, duration);
        EventRegistration registration = new EventRegistration(descriptor.eventID, eventSource, lease, sequenceNumber);
        if(logger.isTraceEnabled())
            logger.trace("Total registrations for {} {}", descriptor.toString(), getRegistrantCount());
        return (registration);
    }

    /**
     * Terminates this EventHandler. This causes all event registrant leases to
     * be cancelled , and if any watches have been created those watches will be
     * destroyed
     */
    public void terminate() {
        landlord.removeAll();
        landlord.stop(true);
        destroyWatch();
    }

    /**
     * Create a response time watch for this EventHandler, which will track the
     * response time for event consumers, measured by how long each fire()
     * invocation takes
     *
     * @param watchRegistry The WatchRegistry to register the StopWatch
     */
    public void createWatch(WatchDataSourceRegistry watchRegistry) {
        responseWatch = new StopWatch(RESPONSE_WATCH + descriptor.toString());
        if(watchRegistry == null)
            return;
        this.watchRegistry = watchRegistry;
        watchRegistry.register(responseWatch);
    }

    /**
     * Destroys the response time watch. Once this method is called the watches
     * will be rendered useless
     */
    public void destroyWatch() {
        if(watchRegistry != null) {
            watchRegistry.deregister(responseWatch);
        }
        if(responseWatch != null) {
            try {
                responseWatch.getWatchDataSource().clear();
                responseWatch.getWatchDataSource().close();
            } catch(RemoteException e) {
                logger.error("Destroying Watches", e);
            }
        }
        responseWatch = null;
    }

    /**
     * Get the response time watch for this EventHandler
     *
     * @return The response time StopWatch. If the watch was not created this
     * method returns null
     */
    public Watch getWatch() {
        return (responseWatch);
    }

    /**
     * The fire method must be overridden by classes that extend this
     * EventHandler, hence the reason the method is declared abstract.
     *
     * @param event The event to send
     *
     * @throws NoEventConsumerException This method may choose to throw a
     * NoEventConsumerException if there are no event consumers registered
     */
    public abstract void fire(RemoteServiceEvent event)
        throws NoEventConsumerException;

    /**
     * Gets the total number of ServiceResource instances contained by the
     * LandlordLessor used by this EventHandler
     *
     * @return The registrant count
     */
    public int getRegistrantCount() {
        return (landlord.total());
    }

    /**
     * Used to get the next <code>ServiceResource</code> from a
     * <code>LandlordLessor</code>
     * 
     * @return The next <code>ServiceResource</code> contained by the
     * <code>LandlordLessor</code>. If there are no
     * <code>ServiceResource</code> instances available in the
     * <code>LandlordLessor</code> then a value a null is returned
     */
    protected ServiceResource getNextServiceResource() {
        ServiceResource sr = null;
        try {
            while (true) {
                sr = resourceMgr.getNext();
                if(sr == null)
                    break;
                if(!landlord.ensure(sr)) {
                    if(logger.isTraceEnabled())
                        logger.trace("Could not ensure resource lease for {}", sr);
                    resourceMgr.removeResource(sr);
                    landlord.remove(sr);
                } else {
                    break;
                }
            }
        } catch(NoSuchElementException e) {
            if(logger.isTraceEnabled())
                logger.trace("No ServiceResource instances");
        }
        return (sr);
    }

    /**
     * Convenience method to print statistics for every thousand events sent.
     * This method will only print result if the
     * <code>-Dorg.rioproject.debug</code> flag is set
     */
    protected void printStats() {
        if(!logger.isTraceEnabled())
            return;
        if(sent == 0)
            sktime = System.currentTimeMillis();
        int m = sent % 1000;
        if(m == 0 && sent > 0) {
            ektime = System.currentTimeMillis();
            tmp = (ektime - sktime) / 1000.f;
            logger.trace("Sent [{}]]\t[1000/{}]\t[{}/Second]", sent, tmp, (1000.f/tmp));
            sktime = System.currentTimeMillis();
        }
    }

    /**
     * Container class for event registration objects that are created and
     * behave as the resource that is being leased and controlled by the
     * ServiceResource
     */
    protected static class EventRegistrationResource {
        private RemoteEventListener listener;
        private MarshalledObject handback;

        public EventRegistrationResource(RemoteEventListener listener,
            MarshalledObject handback) {
            this.listener = listener;
            this.handback = handback;
        }

        /**
         * Returns the event listener.
         * 
         * @return The event listener
         */
        public RemoteEventListener getListener() {
            return (listener);
        }

        /**
         * Returns the handback object.
         * 
         * @return The handback object
         */
        public MarshalledObject getHandback() {
            return (handback);
        }
    }
}
