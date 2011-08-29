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
package org.rioproject.event;

import java.util.EventListener;

/**
 * The RemoteServiceListener interface is used to specify support
 * for clients that wish to participate in notification of
 * distributed events. Notification of events using this interface are done within the
 * JVM, remote invocation semantics are not implied by the use of this interface.
 *
 * @author Dennis Reedy
 */
public interface RemoteServiceEventListener extends EventListener {
    /**
     * The notify method is invoked on a <code>RemoteServiceEventListener</code>
     * for {@link RemoteServiceEvent}s that the listener has registered
     * interest on.
     * <p>
     * The notification of RemoteServiceEvent objects is done
     * within a JVM, remote invocation semantics are not implied by the use
     * of this interface.
     * 
     * @param event A RemoteServiceEvent object
     */
    void notify(RemoteServiceEvent event);
}
