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
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryManager;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;

import java.rmi.MarshalledObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A DynamicEventConsumer extends {@link BasicEventConsumer} and provides the 
 * capability to discover when {@link EventProducer} instances join and leave the 
 * network
 *
 * @author Dennis Reedy
 */
public class DynamicEventConsumer extends BasicEventConsumer {
    private ServiceDiscoveryManager sdm;
    private LookupCache lCache;
    private static Logger logger = BasicEventConsumer.logger;

    /**
     * Create a DynamicEventConsumer with an EventDescriptor
     * 
     * @param edTemplate The EventDescriptor template
     * @param dMgr The DiscoveryManagement instance
     *
     * @throws Exception if the DynamicEventConsumer cannot be created
     */
    public DynamicEventConsumer(final EventDescriptor edTemplate,
                                final DiscoveryManagement dMgr) throws Exception {
        this(edTemplate, null, dMgr);
    }

    /**
     * Create a DynamicEventConsumer with an EventDescriptor, a
     * RemoteServiceEventListener and a DiscoveryManagement instance
     * 
     * @param edTemplate The EventDescriptor template
     * @param listener The RemoteServiceEventListener
     * @param dMgr The DiscoveryManagement instance
     *
     * @throws Exception if the DynamicEventConsumer cannot be created
     */
    public DynamicEventConsumer(final EventDescriptor edTemplate,
                                final RemoteServiceEventListener listener,
                                final DiscoveryManagement dMgr) throws Exception {
        this(edTemplate, listener, null, dMgr);
    }

    /**
     * Create a DynamicEventConsumer with an EventDescriptor, a
     * RemoteServiceEventListener a MarshalledObject handback, a specified lease
     * duration to be used for all event registrations and a DiscoveryManagement
     * instance
     * 
     * @param edTemplate The EventDescriptor template
     * @param listener The RemoteServiceEventListener
     * @param handback The MarshalledObject to be used as a handback
     * @param dMgr The DiscoveryManagement instance
     *
     * @throws Exception if the DynamicEventConsumer cannot be created
     */
    public DynamicEventConsumer(final EventDescriptor edTemplate,
                                final RemoteServiceEventListener listener,
                                final MarshalledObject handback,
                                final DiscoveryManagement dMgr) throws Exception {
        this(edTemplate, listener, handback, dMgr, null);
    }

    /**
     * Create a DynamicEventConsumer with an EventDescriptor, a
     * RemoteServiceEventListener a MarshalledObject handback, a specified lease
     * duration to be used for all event registrations and a DiscoveryManagement
     * instance
     * 
     * @param edTemplate The EventDescriptor template
     * @param listener The RemoteServiceEventListener
     * @param handback The MarshalledObject to be used as a handback
     * @param dMgr The DiscoveryManagement instance
     * @param config Configuration object
     *
     * @throws Exception if the DynamicEventConsumer cannot be created
     */
    public DynamicEventConsumer(final EventDescriptor edTemplate,
                                final RemoteServiceEventListener listener,
                                final MarshalledObject handback,
                                final DiscoveryManagement dMgr,
                                final Configuration config) throws Exception {
        super(edTemplate, listener, handback, config);
        ServiceTemplate template = new ServiceTemplate(null, null, new Entry[]{edTemplate});
        Configuration configInstance = config==null?EmptyConfiguration.INSTANCE:config;
        sdm = new ServiceDiscoveryManager(dMgr, new LeaseRenewalManager(configInstance), configInstance);
        lCache = sdm.createLookupCache(template, null,  null);
        lCache.addListener(new EventProducerManager());
    }

    /**
     * Override parent's terminate() method to terminate the
     * ServiceDiscoveryManager
     */
    public void terminate() {
        /* If this utility created a ServiceDiscoveryManager terminate it */
        if(sdm!=null) {
            try {
                sdm.terminate();
            } catch (IllegalStateException t) {
                logger.log(Level.WARNING, "Terminating SDM", t);
            }
        } 
        super.terminate();
    }

    /**
     * Override parent's register method to provide the ability if we've
     * discovered EventProducer instances that match we should register with
     * them
     */
    public boolean register(final RemoteServiceEventListener listener, final MarshalledObject handback) {
        this.handback = handback;
        boolean added = super.register(listener, handback);
        try {
            if(lCache != null) {
                ServiceItem[] items = lCache.lookup(null, Integer.MAX_VALUE);
                for (ServiceItem item : items) {
                    register(item);
                }
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Register RemoteServiceEventListener", e);
        }
        return (added);
    }
    
    /**
     * The EventProducerManager responds to serviceAdded transitions for 
     * EventProducer instances which match the EventDescriptor
     */
    class EventProducerManager extends ServiceDiscoveryAdapter {
        /**
         * An EventProducer has been added
         */
        public void serviceAdded(final ServiceDiscoveryEvent sdEvent) {
            // sdEvent.getPreEventServiceItem() == null
            ServiceItem item = sdEvent.getPostEventServiceItem();
            try {
                if(item != null && item.service != null) {
                    if(logger.isLoggable(Level.FINEST)) {
                        String name = item.service.getClass().getName();
                        logger.log(Level.FINEST, "EventProducer discovered {0}", name);
                    }
                    if(!eventSubscribers.isEmpty()) {
                        register(item);
                        ServiceFaultListener faultListener = new ServiceFaultListener(item.serviceID);
                        lCache.addListener(faultListener);
                    }
                } else {
                    logger.log(Level.WARNING, "Unable to register EventProducer {0}", item);
                }
            } catch(Exception e) {
                logger.log(Level.SEVERE, "Adding EventProducer", e);
            }
        }              
    }
    
    /**
     * Manage service failure notifications
     */
    class ServiceFaultListener extends ServiceDiscoveryAdapter {
        final ServiceID serviceID;
        
        ServiceFaultListener(final ServiceID serviceID) {
            this.serviceID = serviceID;
        }
        
        /**
         * An EventProducer has been removed
         */
        public void serviceRemoved(final ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPreEventServiceItem();
            if(item.service != null) {
                if(item.serviceID.equals(serviceID)) {
                    if(logger.isLoggable(Level.FINEST)) {
                        String name = item.service.getClass().getName();
                        logger.log(Level.FINEST, "EventProducer removed {0}", name);
                    }
                    terminate();                    
                }
            } else {
                logger.log(Level.SEVERE, "Unable to deregister EventProducer {0}, unknown service", item);
            }
        }
        
        /**
         * Stop listening and remove from local tables
         */
        void terminate() {
            lCache.removeListener(this);
            deregister(serviceID);
        }
    }
}
