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

package org.rioproject.cybernode;

import org.rioproject.event.EventHandler;
import org.rioproject.sla.SLAThresholdEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used as by a to notify registered event consumers of a SLAThresholdEvent
 *
 * @author Dennis Reedy
 */
public class SLAThresholdEventTask implements Runnable {
    private final SLAThresholdEvent event;
    private final EventHandler thresholdEventHandler;
    private static final Logger logger = LoggerFactory.getLogger(SLAThresholdEventTask.class.getName());

    public SLAThresholdEventTask(SLAThresholdEvent event, EventHandler thresholdEventHandler) {
        this.event = event;
        this.thresholdEventHandler = thresholdEventHandler;
    }

    public void run() {
        try {
            thresholdEventHandler.fire(event);
        } catch(Exception e) {
            logger.error("Fire SLAThresholdEvent", e);
        }
    }
}
