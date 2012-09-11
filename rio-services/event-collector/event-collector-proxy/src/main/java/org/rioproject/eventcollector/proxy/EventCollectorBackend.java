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

import com.sun.jini.landlord.Landlord;
import net.jini.core.event.RemoteEventListener;
import net.jini.id.Uuid;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;

import java.io.IOException;

/**
 * Backend interface for the {@code EventCollector}.
 *
 * @author Dennis Reedy
 */
public interface EventCollectorBackend extends EventCollector, Landlord {
    void enableDelivery(Uuid uuid, RemoteEventListener remoteEventListener) throws UnknownEventCollectorRegistration,
                                                                                   IOException;
    void disableDelivery(Uuid uuid) throws UnknownEventCollectorRegistration, IOException;
}
