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
package org.rioproject.tools.ui.servicenotification;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.event.DynamicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.EventCollectorAdmin;
import org.rioproject.eventcollector.api.EventCollectorRegistration;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.tools.ui.ChainedRemoteEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles the creation of event consumers, based on whether an {@link org.rioproject.eventcollector.api.EventCollector}
 * is being used, or whether we register for notifications to all discovered services.
 *
 * @author Dennis Reedy
 */
public class RemoteEventConsumerManager {
    private EventCollectorRegistration eventCollectorRegistration;
    private ChainedRemoteEventListener remoteEventListener;
    private DynamicEventConsumer provisionFailureEventConsumer;
    private DynamicEventConsumer provisionMonitorEventConsumer;
    private DynamicEventConsumer serviceLogEventConsumer;
    private DynamicEventConsumer slaThresholdEventConsumer;
    private AtomicBoolean useEventCollector = new AtomicBoolean(true);
    private final List<EventCollector> eventCollectors = new LinkedList<EventCollector>();
    private PendingEventCollectorRegistration pendingEventCollectionRegistration;

    public void registerForEventCollectorNotification(RemoteEventListener eventListener,
                                                      Configuration config) throws IOException,
                                                                                   ConfigurationException,
                                                                                   UnknownEventCollectorRegistration,
                                                                                   LeaseDeniedException {
        if(eventCollectors.isEmpty()) {
            pendingEventCollectionRegistration = new PendingEventCollectorRegistration(eventListener, config);
            return;
        }

        /* Create the event consumer for EventCollector notification */
        if(remoteEventListener==null) {
            remoteEventListener = new ChainedRemoteEventListener(eventListener, config);
        }
        if(eventCollectorRegistration==null && useEventCollector.get()) {
            List<EventCollector> removals = new ArrayList<EventCollector>();
            for(EventCollector eventCollector : eventCollectors) {
                try {
                    eventCollectorRegistration = eventCollector.register(Lease.ANY);
                    eventCollectorRegistration.enableDelivery(remoteEventListener.getRemoteEventListener());
                } catch (IOException e) {
                    if(!ThrowableUtil.isRetryable(e)) {
                        removals.add(eventCollector);
                    } else {
                        throw e;
                    }
                }
            }
            if(!removals.isEmpty()) {
                for(EventCollector eventCollector : removals) {
                    synchronized (eventCollectors) {
                        eventCollectors.remove(eventCollector);
                    }
                }
            }
        }
    }

    public int getEventControllerCount() {
        return eventCollectors.size();
    }


    public void registerForAllServiceNotification(RemoteServiceEventListener remoteEventListener,
                                                  DiscoveryManagement dMgr) throws Exception {
        /* Create the event consumer for ProvisionFailureEvent utilities */

        provisionFailureEventConsumer = new DynamicEventConsumer(ProvisionFailureEvent.getEventDescriptor(),
                                                                 remoteEventListener,
                                                                 dMgr);
        provisionMonitorEventConsumer = new DynamicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(),
                                                                 remoteEventListener,
                                                                 dMgr);
        serviceLogEventConsumer = new DynamicEventConsumer(ServiceLogEvent.getEventDescriptor(),
                                                           remoteEventListener,
                                                           dMgr);
        slaThresholdEventConsumer = new DynamicEventConsumer(SLAThresholdEvent.getEventDescriptor(),
                                                             remoteEventListener,
                                                             dMgr);

    }

    public void setUseEventCollector(boolean useEventCollector) {
        this.useEventCollector.set(useEventCollector);
    }

    public void delete(Collection<RemoteServiceEvent> events) {
        if(events!=null && !events.isEmpty() && useEventCollector.get()) {
            for(EventCollector eventCollector : eventCollectors) {
                try {
                    EventCollectorAdmin admin = (EventCollectorAdmin)eventCollector.getAdmin();
                    admin.delete(events);
                } catch(IOException e) {

                }
            }
        }
    }

    public void addEventCollector(EventCollector eventCollector) throws LeaseDeniedException,
                                                                        IOException,
                                                                        UnknownEventCollectorRegistration {
        eventCollectors.add(eventCollector);
        if(pendingEventCollectionRegistration!=null) {
            try {
                registerForEventCollectorNotification(pendingEventCollectionRegistration.getEventListener(),
                                                      pendingEventCollectionRegistration.getConfig());
                pendingEventCollectionRegistration = null;
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeEventCollector(EventCollector eventCollector) {
        eventCollectors.remove(eventCollector);
    }

    public void refresh() throws UnknownEventCollectorRegistration, IOException {
        if(eventCollectorRegistration!=null) {
            eventCollectorRegistration.enableDelivery(remoteEventListener.getRemoteEventListener());
        }
    }

    public void terminate() {
        if(eventCollectorRegistration!=null) {
            try {
                eventCollectorRegistration.disableDelivery();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                eventCollectorRegistration.getLease().cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
            eventCollectorRegistration = null;
        }
        if(remoteEventListener!=null) {
            remoteEventListener.terminate();
            remoteEventListener = null;
        }
        if(provisionFailureEventConsumer!=null) {
            provisionFailureEventConsumer.terminate();
            provisionFailureEventConsumer = null;
        }
        if(provisionMonitorEventConsumer!=null) {
            provisionMonitorEventConsumer.terminate();
            provisionMonitorEventConsumer = null;
        }
        if(serviceLogEventConsumer!=null) {
            serviceLogEventConsumer.terminate();
            serviceLogEventConsumer = null;
        }
        if(slaThresholdEventConsumer!=null) {
            slaThresholdEventConsumer.terminate();
            slaThresholdEventConsumer = null;
        }
    }

    private class PendingEventCollectorRegistration {
        private RemoteEventListener eventListener;
        private Configuration config;

        private PendingEventCollectorRegistration(RemoteEventListener eventListener, Configuration config) {
            this.eventListener = eventListener;
            this.config = config;
        }

        public RemoteEventListener getEventListener() {
            return eventListener;
        }

        public Configuration getConfig() {
            return config;
        }
    }
}
