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
package org.rioproject.gnostic;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for ProvisionMonitorEvent notifications
 */
class CEPEventConsumer implements RemoteServiceEventListener {
    private StatefulKnowledgeSession session;
    private WorkingMemoryEntryPoint provisionEventsStream;

    private static final Logger logger = LoggerFactory.getLogger(Gnostic.class.getName());

    public CEPEventConsumer(StatefulKnowledgeSession session) {
        this.session = session;
        this.provisionEventsStream = session.getWorkingMemoryEntryPoint(Constants.PROVISION_EVENTS_STREAM);
        if(provisionEventsStream==null)
            throw new IllegalStateException(String.format("The [%s], could not be created. The Drools setup must be invalid",
                                                          Constants.PROVISION_EVENTS_STREAM));
    }

    public void notify(RemoteServiceEvent event) {
        if (!(event instanceof ProvisionMonitorEvent || event instanceof ProvisionFailureEvent)) {
            logger.warn("Unrecognized event type {}", event.getClass().getName());
            return;
        }
        
        provisionEventsStream.insert(event);
        logger.info("Inserted into CEP engine event {}", event);
        session.fireAllRules();     // TODO: make sure this makes sense to fire each time we insert!!
    }
}
