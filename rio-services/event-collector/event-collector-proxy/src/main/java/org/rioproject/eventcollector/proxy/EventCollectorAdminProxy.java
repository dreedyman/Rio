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
package org.rioproject.eventcollector.proxy;

import net.jini.id.Uuid;
import org.rioproject.admin.ServiceAdminProxy;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.eventcollector.api.EventCollectorAdmin;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

/**
 * Implementation of the {@link EventCollectorAdminProxy}.
 * @author Dennis Reedy
 */
public final class EventCollectorAdminProxy extends ServiceAdminProxy implements EventCollectorAdmin, Serializable {
    private static final long serialVersionUID = 1L;
    private final EventCollectorAdmin eventCollectorAdmin;

    /**
     * Creates a ProvisionMonitorAdmin proxy, returning an instance that implements
     * RemoteMethodControl if the server does too.
     *
     * @param serviceAdmin The ProvisionMonitorAdmin server
     * @param id The Uuid of the ProvisionMonitorAdmin
     */
    public static EventCollectorAdminProxy getInstance(final EventCollectorAdmin serviceAdmin, final Uuid id) {
        return(new EventCollectorAdminProxy(serviceAdmin, id));
    }

    /*
     * Private constructor
     */
    private EventCollectorAdminProxy(final EventCollectorAdmin serviceAdmin, final Uuid uuid) {
        super(serviceAdmin, uuid);
        eventCollectorAdmin = serviceAdmin;
    }

    @Override
    public int delete(final Collection<RemoteServiceEvent> events) throws IOException {
        if(events==null || events.isEmpty())
            return 0;
        return eventCollectorAdmin.delete(events);
    }


}
