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
package org.rioproject.eventcollector.api;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;

import java.io.IOException;

/**
 * The {@code EventCollectorRegistration} defines the interface through which a client manages its registration
 * and its notification processing. Event Collector clients use this interface to:
 *
 * <ul>
 * <li>Manage the lease for this registration</li>
 * <li>Enable or disable the delivery of any stored events for this registration.</li>
 * </ul>
 *
 * @author Dennis Reedy
 */
public interface EventCollectorRegistration {
    /**
     * Returns the {@code Lease} object associated with this registration. The client can renew or cancel the
     * registration with the Event Collector through this lease object.
     *
     * @return The lease object associated with this registration
     */
    Lease getLease();

    /**
     * Initiates delivery of stored notifications to the supplied target listener, if any.
     * If a target listener already exists, then it will be replaced with the specified target listener.
     * Passing {@code null} as the target parameter has the same effect as calling the {@code disableDelivery} method.
     *
     * @param remoteEventListener The listener to be notified of stored events, if any.
     *
     * @throws UnknownEventCollectorRegistration if the {@code EventCollector} has no corresponding
     * {@code EventCollectorRegistration}
     * @throws IOException If there is a communication failure between the client and the service.
     */
    void enableDelivery(RemoteEventListener remoteEventListener) throws UnknownEventCollectorRegistration,
                                                                        IOException;

    /**
     * Ceases delivery of stored notifications to the existing target listener, if any.
     *
     * @throws UnknownEventCollectorRegistration if the {@code EventCollector} has no corresponding
     * {@code EventCollectorRegistration}
     * @throws IOException If there is a communication failure between the client and the service.
     */
    void disableDelivery() throws UnknownEventCollectorRegistration, IOException;
}
