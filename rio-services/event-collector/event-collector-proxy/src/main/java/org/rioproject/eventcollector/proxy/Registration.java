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

import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.id.ReferentUuid;
import net.jini.id.ReferentUuids;
import net.jini.id.Uuid;
import org.rioproject.eventcollector.api.EventCollectorRegistration;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;

import java.io.IOException;
import java.io.Serializable;

/**
 * Implementation of the {@code EventCollectorRegistration}.
 *
 * @author Dennis Reedy
 */
public class Registration implements Serializable, EventCollectorRegistration, ReferentUuid {
    private static final long serialVersionUID = 1L;
    /**
     * Unique identifier for this registration
     */
    private final Uuid registrationID;
    /**
     * The service's registration lease
     */
    private final Lease lease;
    /**
     * Reference to service implementation
     */
    private final EventCollectorBackend eventCollector;

    public Registration(EventCollectorBackend eventCollector, Uuid registrationID, Lease lease) {
        this.eventCollector = eventCollector;
        this.registrationID = registrationID;
        this.lease = lease;
    }

    public Lease getLease() {
        return lease;
    }

    public void enableDelivery(RemoteEventListener remoteEventListener) throws UnknownEventCollectorRegistration,
                                                                               IOException {
        eventCollector.enableDelivery(registrationID, remoteEventListener);
    }

    public void disableDelivery() throws UnknownEventCollectorRegistration, IOException {
        eventCollector.disableDelivery(registrationID);
    }

    public Uuid getReferentUuid() {
        return registrationID;
    }

    /**
     * Proxies with the same registrationID have the same hash code.
     */
    public int hashCode() {
        return registrationID.hashCode();
    }

    /**
     * Proxies with the same registrationID are considered equal.
     */
    public boolean equals(Object o) {
        return ReferentUuids.compare(this, o);
    }
}
