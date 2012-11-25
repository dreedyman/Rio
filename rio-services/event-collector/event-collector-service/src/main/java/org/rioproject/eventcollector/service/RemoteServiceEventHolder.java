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
package org.rioproject.eventcollector.service;

import org.rioproject.event.RemoteServiceEvent;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.MarshalledObject;

/**
 * Holds the {@code RemoteServiceEvent}
 *
 * @author Dennis Reedy
 */
public class RemoteServiceEventHolder implements Serializable {
    private static final long serialVersionUID = 1L;
    private final MarshalledObject<RemoteServiceEvent> marshalledEvent;

    public RemoteServiceEventHolder(RemoteServiceEvent event) throws IOException {
        marshalledEvent = new MarshalledObject<RemoteServiceEvent>(event);
    }

    public RemoteServiceEvent getRemoteServiceEvent() throws ClassNotFoundException {
        RemoteServiceEvent remoteServiceEvent;
        try {
            remoteServiceEvent = marshalledEvent.get();
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to create client-side remote event", e);
        }
        return remoteServiceEvent;
    }
}
