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
package org.rioproject.examples.workflow;

import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;
import org.rioproject.bean.PreAdvertise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Master provides submission management, writing/taking WorkflowEntry
 * instances.
 */
public class MasterImpl implements Master {
    private static UUID id = UUID.randomUUID();
    private JavaSpace space;
    private Logger logger = LoggerFactory.getLogger(MasterImpl.class);
    private AtomicBoolean hasBeenAdvertised = new AtomicBoolean(false);

    /* Injected by Rio */
    @SuppressWarnings("unused")
    public void setJavaSpace(JavaSpace space) {
        this.space = space;
    }

    /* Invoked by Rio */
    @SuppressWarnings("unused")
    @PreAdvertise
    public void startup() {
        logger.info("PRE_ADVERTISE Master");
        hasBeenAdvertised.set(true);
    }

    boolean hasBeenAdvertised() {
        return hasBeenAdvertised.get();
    }

    public WorkflowEntry process() throws WorkflowException {
        if (space == null)
            throw new IllegalArgumentException("space is null");

        /* Submit the new Order */
        WorkflowEntry order = new WorkflowEntry(id, State.NEW);
        try {
            space.write(order, null, Lease.FOREVER);
        } catch (Exception e) {
            Throwable cause = (e.getCause() == null ? e : e.getCause());
            throw new WorkflowException("Writing to the space", cause);
        }

        WorkflowEntry template = new WorkflowEntry(id, State.CLOSED);
        logger.info("Waiting for result ...");
        WorkflowEntry result;
        try {
            result = (WorkflowEntry) space.take(template, null, Long.MAX_VALUE);
        } catch (Exception e) {
            Throwable cause = (e.getCause() == null ? e : e.getCause());
            throw new WorkflowException("Taking WorkflowEntry entries", cause);
        }
        return (result);
    }
}
