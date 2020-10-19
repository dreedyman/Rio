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
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.impl.event.DynamicEventConsumer;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.tools.ui.ChainedRemoteEventListener;

import java.io.IOException;

/**
 * Handles the creation of event consumers.
 *
 * @author Dennis Reedy
 */
public class RemoteEventConsumerManager {
    private ChainedRemoteEventListener remoteEventListener;
    private DynamicEventConsumer provisionFailureEventConsumer;
    private DynamicEventConsumer provisionMonitorEventConsumer;
    private DynamicEventConsumer serviceLogEventConsumer;
    private DynamicEventConsumer slaThresholdEventConsumer;
    private PendingEventCollectorRegistration pendingEventCollectionRegistration;

    public void registerForEventCollectorNotification(RemoteEventListener eventListener,
                                                      Configuration config) throws IOException,
                                                                                   ConfigurationException {

        /* Create the event consumer for EventCollector notification */
        if (remoteEventListener==null) {
            remoteEventListener = new ChainedRemoteEventListener(eventListener, config);
        }
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

    public void terminate() {

        if (remoteEventListener!=null) {
            remoteEventListener.terminate();
            remoteEventListener = null;
        }
        if (provisionFailureEventConsumer!=null) {
            provisionFailureEventConsumer.terminate();
            provisionFailureEventConsumer = null;
        }
        if (provisionMonitorEventConsumer!=null) {
            provisionMonitorEventConsumer.terminate();
            provisionMonitorEventConsumer = null;
        }
        if (serviceLogEventConsumer!=null) {
            serviceLogEventConsumer.terminate();
            serviceLogEventConsumer = null;
        }
        if (slaThresholdEventConsumer!=null) {
            slaThresholdEventConsumer.terminate();
            slaThresholdEventConsumer = null;
        }
    }

    private static class PendingEventCollectorRegistration {
        private final RemoteEventListener eventListener;
        private final Configuration config;

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
