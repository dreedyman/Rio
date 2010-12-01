/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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

import net.jini.admin.Administrable;
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
import org.rioproject.core.ClassBundle;
import org.rioproject.fdh.FaultDetectionHandler;
import org.rioproject.fdh.FaultDetectionListener;
import org.rioproject.core.jsb.ServiceBeanAdmin;
import org.rioproject.fdh.FaultDetectionHandlerFactory;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;

import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
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
    /** Table of service IDs to FaultDetectionHandler instances, one for each service */
    private Hashtable<ServiceID, FaultDetectionHandler> fdhTable =
        new Hashtable<ServiceID, FaultDetectionHandler>();
    static Logger logger = BasicEventConsumer.logger;

    /**
     * Create a DynamicEventConsumer with an EventDescriptor
     * 
     * @param edTemplate The EventDescriptor template
     * @param dMgr The DiscoveryManagement instance
     *
     * @throws Exception if the DynamicEventConsumer cannot be created
     */
    public DynamicEventConsumer(EventDescriptor edTemplate,
                                DiscoveryManagement dMgr) throws Exception {
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
    public DynamicEventConsumer(EventDescriptor edTemplate,
                                RemoteServiceEventListener listener, 
                                DiscoveryManagement dMgr) throws Exception {
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
    public DynamicEventConsumer(EventDescriptor edTemplate,
                                RemoteServiceEventListener listener, 
                                MarshalledObject handback,
                                DiscoveryManagement dMgr) throws Exception {
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
    public DynamicEventConsumer(EventDescriptor edTemplate,
                                RemoteServiceEventListener listener, 
                                MarshalledObject handback,
                                DiscoveryManagement dMgr, 
                                Configuration config) throws Exception {
        super(edTemplate, listener, handback, config);
        ServiceTemplate template = new ServiceTemplate(null,
                                                       null,
                                                       new Entry[]{edTemplate});
        config = (config==null?EmptyConfiguration.INSTANCE:config);
        sdm = new ServiceDiscoveryManager(dMgr, 
                                          new LeaseRenewalManager(config), 
                                          config);
        lCache = sdm.createLookupCache(template, null,  null);
        
        lCache.addListener(new EventProducerManager());
    }

    /**
     * Override parent's terminate() method to terminate the
     * ServiceDiscoveryManager
     */
    public void terminate() {
        /* Stop all FaultDetectionHandler instances */
        ArrayList<FaultDetectionHandler> list = Collections.list(fdhTable.elements());
        for (FaultDetectionHandler fdh : list) {
            fdh.terminate();            
        }
        /* If this utility created a ServiceDiscoveryManager terminate it */
        if(sdm!=null) {
            try {
                sdm.terminate();
            } catch (Throwable t) {
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
    public boolean register(RemoteServiceEventListener listener,
                            MarshalledObject handback) {
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
     * Returns an array of all EventProducer proxy objects that have been
     * discovered
     * 
     * @return An array of EventProducer proxy objects
     */
    public EventProducer[] getEventProducers() {
        ServiceItem[] items = lCache.lookup(null, Integer.MAX_VALUE);
        EventProducer[] producers = new EventProducer[items.length];
        for(int i = 0; i < items.length; i++)
            producers[i] = (EventProducer)items[i].service;
        return (producers);
    }
    
    /**
     * The EventProducerManager responds to serviceAdded transitions for 
     * EventProducer instances which match the EventDescriptor
     */
    class EventProducerManager extends ServiceDiscoveryAdapter {
        /**
         * An EventProducer has been added
         */
        public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
            // sdEvent.getPreEventServiceItem() == null
            ServiceItem item = sdEvent.getPostEventServiceItem();
            try {
                if(item != null && item.service != null) {
                    if(logger.isLoggable(Level.FINEST)) {
                        String name = item.service.getClass().getName();
                        logger.log(Level.FINEST,
                                   "EventProducer discovered {0}",
                                   name);
                    }
                    FaultDetectionHandler<ServiceID> fdh = null;
                    if(item.service instanceof Administrable) {
                        try {                        
                            Object admin = ((Administrable)item.service).getAdmin();
                            if(admin instanceof ServiceBeanAdmin) {
                                ServiceBeanAdmin sbAdmin = (ServiceBeanAdmin)admin;                
                                ClassBundle fdhBundle = 
                                    sbAdmin.getServiceElement().
                                    getFaultDetectionHandlerBundle();
                                fdh = FaultDetectionHandlerFactory.
                                          getFaultDetectionHandler(
                                            fdhBundle, 
                                            item.service.getClass().getClassLoader());                                
                            } else {
                                if(logger.isLoggable(Level.FINEST)) {
                                    String name = item.service.getClass().getName();
                                    logger.log(Level.FINEST,
                                               "EventProducer {0} does not "+
                                               "implement ServiceBeanAdmin, "+
                                               "respond only to serviceRemoved "+
                                               "notifications for service failure",
                                               name);
                                }
                            }
                        } catch (Exception e) {
                            if(logger.isLoggable(Level.FINEST)) 
                                logger.log(Level.WARNING,
                                           "Unable to create FaultDetectionHandler "+
                                           "for EventProducer "+
                                           item.service.toString(),
                                           e);
                            else
                                logger.log(Level.WARNING,
                                           "Unable to create FaultDetectionHandler "+
                                           "for EventProducer "+
                                           item.service.toString());
                        }
                    } else {
                        if(logger.isLoggable(Level.FINEST)) {
                            String name = item.service.getClass().getName();
                            logger.log(Level.FINEST,
                                       "EventProducer {0} does not "+
                                       "implement Administrable, "+
                                       "respond only to serviceRemoved "+
                                       "notifications for service failure",
                                       name);
                        }
                    }
                    
                    ServiceFaultListener faultListener;
                    if(fdh!=null) {
                        faultListener = new ServiceFaultListener(false, 
                                                                 item.serviceID);
                        fdh.register(faultListener);
                        fdhTable.put(item.serviceID, fdh);
                        fdh.monitor(item.service, item.serviceID, lCache);                        
                    }
                    if(eventSubscribers.size() > 0)
                        register(item);
                } else {
                    logger.log(Level.WARNING,
                               "Unable to register EventProducer {0}",
                               item);
                }
            } catch(Throwable t) {
                logger.log(Level.SEVERE, "Adding EventProducer", t);
            }
        }              
    }
    
    /**
     * Manage service failure notifications
     */
    class ServiceFaultListener extends ServiceDiscoveryAdapter 
    implements FaultDetectionListener<ServiceID> {
        boolean useLookupCache;
        ServiceID serviceID;
        
        ServiceFaultListener(boolean useLookupCache, ServiceID serviceID) {
            this.useLookupCache = useLookupCache;
            this.serviceID = serviceID;
        }
        
        /**
         * @see org.rioproject.fdh.FaultDetectionListener#serviceFailure
         */
        public synchronized void serviceFailure(Object proxy, ServiceID serviceID) {
            if(logger.isLoggable(Level.FINEST)) {
                String name = proxy.getClass().getName();
                logger.log(Level.FINEST, 
                           "EventProducer removed {0}", 
                           name);
            }            
            terminate();
        }
        
        /**
         * An EventProducer has been removed
         */
        public void serviceRemoved(ServiceDiscoveryEvent sdEvent) {
            if(!useLookupCache)
                return;
            ServiceItem item = sdEvent.getPreEventServiceItem();
            if(item.service != null) {
                if(item.serviceID.equals(serviceID)) {
                    if(logger.isLoggable(Level.FINEST)) {
                        String name = item.service.getClass().getName();
                        logger.log(Level.FINEST, 
                                   "EventProducer removed {0}", 
                                   name);
                    }
                    terminate();                    
                }
            } else {
                logger.log(Level.SEVERE,
                           "Unable to deregister EventProducer {0}, unknown service",
                           item);
            }
        }
        
        /**
         * Stop listening and remove from local tables
         */
        void terminate() {
            fdhTable.remove(serviceID);
            lCache.removeListener(this);
            deregister(serviceID);
        }
    }
}
