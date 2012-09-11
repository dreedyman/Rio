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

import net.jini.export.Exporter;
import net.jini.id.UuidFactory;
import org.rioproject.admin.ServiceAdmin;
import org.rioproject.admin.ServiceAdminImpl;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.eventcollector.api.EventCollectorAdmin;
import org.rioproject.eventcollector.proxy.EventCollectorAdminProxy;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * Provides support for administering the {@link org.rioproject.eventcollector.api.EventCollector}
 *
 * @author Dennis Reedy
 */
public class EventCollectorAdminImpl extends ServiceAdminImpl implements EventCollectorAdmin {
    private final EventCollectorImpl eventCollector;

    public EventCollectorAdminImpl(EventCollectorImpl service, Exporter exporter) {
        super(service, exporter);
        eventCollector = service;
    }

    /**
     * Override parents getServiceAdmin method
     */
    public ServiceAdmin getServiceAdmin() throws RemoteException {
        if(adminProxy==null) {
            EventCollectorAdmin remoteRef = (EventCollectorAdmin)exporter.export(this);
            adminProxy =  EventCollectorAdminProxy.getInstance(remoteRef, UuidFactory.generate());
        }
        return(adminProxy);
    }

    @Override
    public int delete(Collection<RemoteServiceEvent> events) {
        return eventCollector.delete(events);
    }
}
