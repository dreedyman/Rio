/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.watch;

import org.rioproject.core.jsb.ServiceBeanContext;

/**
 * Defines the semantics for a registry of {@link org.rioproject.watch.Watch}
 * instances and their corresponding {@link org.rioproject.watch.WatchDataSource}
 * instances, providing capabilities to search over all registered
 * {@link org.rioproject.watch.Watch} instances.
 */
public interface WatchRegistry {

    /**
     * DeRegisters a Watch from underlying collection and closes the Watch
     * 
     * @param watch The Watch to deregister
     */
    void deregister(Watch... watch);

    /**
     * Closes all WatchDataSource instances
     */
    void closeAll();

    /**
     * Registers a Watch to underlying collection
     * 
     * @param watch The Watch to register
     */
    void register(Watch... watch);

    /**
     * Add a ThresholdListener. The ThresholdListener will be added as a
     * ThresholdListener to any ThresholdWatch instances that match the
     * ThresholdListener when the ThresholdWatch is registered using the
     * <code>register</code> method
     * 
     * @param id The identifier to match
     * @param thresholdListener The ThresholdListener to add
     */
    void addThresholdListener(String id, ThresholdListener thresholdListener);

    /**
     * Remove a ThresholdListener. The ThresholdListener will be removed as a
     * ThresholdListener from any ThresholdWatch instances that match the
     * ThresholdListener
     *
     * @param id The identifier to match
     * @param thresholdListener The ThresholdListener to remove
     */
    void removeThresholdListener(String id, ThresholdListener thresholdListener);

    /**
     * Returns a Watch that matches the provided id
     *
     * @param id The watch id to match
     * @return The first Watch that matches the input id. If
     * there is not a matching Watch, this method returns a null
     */
    Watch findWatch(String id);

    /**
     * Returns an array of WatchDataSource objects which provide a reference to
     * an implementation of WatchDataSource.
     * 
     * @return An array of WatchDataSource objects. If there
     * are no WatchDataSource objects, this method returns an empty array
     */
    WatchDataSource[] fetch();

    /**
     * Returns a WatchDataSource that matches the input <pre>id</pre>, that
     * corresponds to a <pre>Watch</pre> identifier.
     * 
     * @param id the watch id to match
     * @return The WatchDataSource that matches the id.
     * If there is no WatchDataSource, this method returns null
     */
    WatchDataSource fetch(String id);

    /**
     * Set the ServiceBeanContext for the WatchRegistry
     *
     * @param context The {@link org.rioproject.core.jsb.ServiceBeanContext}
     */
    void setServiceBeanContext(ServiceBeanContext context);
}
