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
package org.rioproject.monitor;

import net.jini.config.Configuration;
import org.rioproject.event.DispatchEventHandler;
import org.rioproject.event.EventHandler;
import org.rioproject.monitor.tasks.ProvisionMonitorEventTask;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Sends {@link ProvisionMonitorEvent}s.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorEventProcessor {
    /**
     * ThreadPool for sending ProvisionMonitorEvent notifications
     */
    private Executor monitorEventPool;
    private EventHandler monitorEventHandler;

    public ProvisionMonitorEventProcessor(Configuration config) throws Exception {
        monitorEventPool = Executors.newCachedThreadPool();
        monitorEventHandler = new DispatchEventHandler(ProvisionMonitorEvent.getEventDescriptor(), config);
    }

    public EventHandler getMonitorEventHandler() {
        return monitorEventHandler;
    }

    /**
     * Sends a ProvisionMonitorEvent using a thread obtained from a thread pool
     *
     * @param event The ProvisionMonitorEvent to send
     */
    public void processEvent(ProvisionMonitorEvent event) {
        monitorEventPool.execute(new ProvisionMonitorEventTask(monitorEventHandler, event));
    }
}
