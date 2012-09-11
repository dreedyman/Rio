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

import org.rioproject.admin.ServiceAdmin;
import org.rioproject.event.RemoteServiceEvent;

import java.io.IOException;
import java.util.Collection;

/**
 * Administrative interface for the {@link EventCollector}.
 *
 * @author Dennis Reedy
 */
public interface EventCollectorAdmin extends ServiceAdmin {

    /**
     * Delete events from the {@link EventCollector}.
     *
     * @param events A {@code Collection} of {@code RemoteServiceEvent}s to delete from the {@link EventCollector}. If
     * {@code null}, or an empty {@code Collection} is provided, no action is performed.
     *
     * @return The number of events deleted
     *
     * @throws IOException If there is a communication failure between the client and the service.
     */
    int delete(Collection<RemoteServiceEvent> events) throws IOException;
}
