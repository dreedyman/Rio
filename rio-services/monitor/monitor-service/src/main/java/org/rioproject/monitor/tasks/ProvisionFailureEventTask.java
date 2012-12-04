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
package org.rioproject.monitor.tasks;

import org.rioproject.event.EventHandler;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to notify registered event consumers of a ProvisionFailureEvent
 */
public class ProvisionFailureEventTask implements Runnable {
    private ProvisionFailureEvent event;
    private EventHandler failureHandler;
    private final Logger logger = LoggerFactory.getLogger(ProvisionFailureEventTask.class.getName());

    public ProvisionFailureEventTask(ProvisionFailureEvent event, EventHandler failureHandler) {
        this.event = event;
        this.failureHandler = failureHandler;
        if (failureHandler==null)
            throw new IllegalArgumentException("failureHandler is null");
    }

    public void run() {
        try {
            failureHandler.fire(event);
        } catch (Exception e) {
            logger.warn("Exception notifying ProvisionFailureEvent consumers", e);
        }

    }
}
