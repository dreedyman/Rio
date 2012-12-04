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

import net.jini.core.entry.Entry;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryManager;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.rioproject.resources.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides basic support for an {@code EventManager}.
 *
 * @author Dennis Reedy
 */
public abstract class AbstractEventManager implements RemoteServiceEventListener, EventManager {
    private static final Logger logger = LoggerFactory.getLogger(AbstractEventManager.class.getName());
    private BlockingQueue<RemoteEvent> eventQ;
    private final List<EventProducerManager> eventProducerManagers = new ArrayList<EventProducerManager>();
    private final ExecutorService execService = Executors.newCachedThreadPool();

    public void initialize(final EventCollectorContext context) throws Exception {
        if(context==null)
            throw new IllegalArgumentException("context must not be null");
        this.eventQ = context.getEventQueue();
        ServiceDiscoveryManager sdm = new ServiceDiscoveryManager(context.getDiscoveryManager(),
                                                                  new LeaseRenewalManager(context.getConfiguration()),
                                                                  context.getConfiguration());
        for(EventDescriptor eventDescriptor : context.getEventDescriptors()) {
            ServiceTemplate template = new ServiceTemplate(null, null, new Entry[]{eventDescriptor});
            LookupCache lCache = sdm.createLookupCache(template, null,  null);
            EventProducerManager eventProducerManager = new EventProducerManager(eventDescriptor, this, lCache);
            lCache.addListener(eventProducerManager);
            eventProducerManagers.add(eventProducerManager);
        }

    }

    public void terminate() {
        for(EventProducerManager eventProducerManager : eventProducerManagers) {
            eventProducerManager.terminate();
        }
    }

    public void historyNotification(final RegisteredNotification registeredNotification) {
        if(registeredNotification==null)
            throw new IllegalArgumentException("The RegisteredNotification must not be null");
        execService.submit(new EventHistoryNotifier(registeredNotification));
    }

    public void notify(RemoteServiceEvent event) {
        if(logger.isDebugEnabled())
            logger.debug(event.toString());
        eventQ.offer(event);
        postNotify(event);
    }

    public abstract void postNotify(RemoteServiceEvent event);

    protected ExecutorService getExecutorService() {
        return execService;
    }

    /**
     * The EventProducerManager responds to serviceAdded transitions for
     * EventProducer instances which match the EventDescriptor
     */
    class EventProducerManager extends ServiceDiscoveryAdapter {
        final LookupCache lookupCache;
        BasicEventConsumer eventConsumer;
        final EventDescriptor eventDescriptor;
        final RemoteServiceEventListener eventListener;

        EventProducerManager(final EventDescriptor eventDescriptor,
                             final RemoteServiceEventListener eventListener,
                             final LookupCache lookupCache) throws Exception {
            this.lookupCache = lookupCache;
            this.eventDescriptor = eventDescriptor;
            this.eventListener = eventListener;
        }

        /**
         * An EventProducer has been added
         */
        public void serviceAdded(final ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPostEventServiceItem();
            try {
                if(item != null && item.service != null) {
                    if(logger.isInfoEnabled()) {
                        String name = item.service.getClass().getName();
                        if(logger.isDebugEnabled()) {
                            logger.debug(String.format("EventProducer discovered %s for EventDescriptor %s",
                                                      name, eventDescriptor.toString()));
                        }
                    }
                    if(eventConsumer==null) {
                        final EventDescriptor eventDescriptorToUse;
                        if(eventDescriptor.eventID==null) {
                            eventDescriptorToUse = getEventDescriptor(item.attributeSets, eventDescriptor);
                        } else {
                            eventDescriptorToUse = eventDescriptor;
                        }
                        eventConsumer = new BasicEventConsumer(eventDescriptorToUse, eventListener);
                    }
                    eventConsumer.register(item);
                    ServiceFaultListener faultListener = new ServiceFaultListener(item.serviceID,
                                                                                  lookupCache,
                                                                                  eventConsumer);
                    lookupCache.addListener(faultListener);
                } else {
                    logger.warn("Unable to register EventProducer {}", item);
                }
            } catch(Exception e) {
                logger.error("Adding EventProducer", e);
            }
        }

        void terminate() {
            if(eventConsumer!=null)
                eventConsumer.terminate();
        }

        EventDescriptor getEventDescriptor(Entry[] attributes, EventDescriptor toMatch) {
            EventDescriptor matchedDescriptor = null;
            for(Entry entry : attributes) {
                if(entry instanceof EventDescriptor) {
                    if(((EventDescriptor)entry).matches(toMatch)) {
                        matchedDescriptor = (EventDescriptor)entry;
                        break;
                    }
                }
            }
            return matchedDescriptor;
        }
    }

    /**
     * Manage service failure notifications
     */
    class ServiceFaultListener extends ServiceDiscoveryAdapter {
        final ServiceID serviceID;
        final LookupCache lookupCache;
        final BasicEventConsumer eventConsumer;

        ServiceFaultListener(final ServiceID serviceID, LookupCache lookupCache, final BasicEventConsumer eventConsumer) {
            this.serviceID = serviceID;
            this.lookupCache = lookupCache;
            this.eventConsumer = eventConsumer;
        }

        /**
         * An EventProducer has been removed
         */
        public void serviceRemoved(final ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPreEventServiceItem();
            if(item.service != null) {
                if(item.serviceID.equals(serviceID)) {
                    if(logger.isTraceEnabled()) {
                        String name = item.service.getClass().getName();
                        logger.trace("EventProducer removed {}", name);
                    }
                    terminate();
                }
            } else {
                logger.error("Unable to deregister EventProducer {}, unknown service", item);
            }
        }

        /**
         * Stop listening and remove from local tables
         */
        void terminate() {
            lookupCache.removeListener(this);
            eventConsumer.deregister(serviceID);
        }
    }

    /**
     * This class will read the history and perform the notification to the newly registered listener
     */
    class EventHistoryNotifier implements Runnable {
        final RegisteredNotification registeredNotification;

        EventHistoryNotifier(final RegisteredNotification registeredNotification) {
            this.registeredNotification = registeredNotification;
        }

        public void run() {
            try {
                registeredNotification.setHistoryUpdating(true);
                for(RemoteEvent event : getEvents(registeredNotification.getEventIndex())) {
                    if(!doNotify(event)) {
                        break;
                    }
                }
            } finally {
                registeredNotification.setHistoryUpdating(false);
                for(RemoteEvent event : registeredNotification.getAndClearMissedEvents()) {
                    if(!doNotify(event)) {
                        break;
                    }
                }
            }
        }

        private boolean doNotify(RemoteEvent event) {
            boolean success = false;
            try {
                if(!registeredNotification.isUnknown(event))
                    registeredNotification.getEventListener().notify(event);
                success = true;
            } catch (UnknownEventException e) {
                logger.warn("UnknownEventException return from listener", e);
                registeredNotification.addUnknown(event);
            } catch (RemoteException e) {
                logger.warn("RemoteException return from listener", e);
                if(!ThrowableUtil.isRetryable(e)) {
                    logger.warn(String.format("Unrecoverable exception from %s", registeredNotification));
                }
            }
            return success;
        }
    }
}
