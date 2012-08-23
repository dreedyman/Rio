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

import net.jini.core.lease.LeaseDeniedException;
import org.rioproject.resources.servicecore.Service;

import java.io.IOException;

/**
 * The {@code EventCollector} interface allows clients to use a third party for the purpose of storing and
 * retrieving events.
 *
 * @author Dennis Reedy
 */
public interface EventCollector extends Service {
    /**
     * Clients first register with the {@code EventCollector} service in order to be notified of stored events.
     *
     * @param duration The requested lease duration in milliseconds
     *
     * @return A new {@code EventCollectorRegistration}
     *
     * @throws IOException If there is a communication failure between the client and the service.
     * @throws LeaseDeniedException If the {@code EventCollector} service is unable or unwilling to grant this registration request.
     * @throws IllegalArgumentException If {@code duration} is not positive or Lease.ANY.
     */
    EventCollectorRegistration register(long duration) throws IOException, LeaseDeniedException;
}
