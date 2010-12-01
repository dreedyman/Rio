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
package org.rioproject.fdh;

import net.jini.lookup.LookupCache;

/**
 * The FaultDetectionHandler is loaded by client entities do determine whether a
 * service is reachable. Developers may choose to implement custom fault
 * detection algorithms and protocols to determine service reachability and
 * use those approaches in concrete implementations of this interface
 *
 * @author Dennis Reedy
 */
public interface FaultDetectionHandler<T> {
    /**
     * Set configuration attributes for the FaultDetectionHandler.
     * 
     * @param configArgs Configuration attributes a FaultDetectionHandler will
     * use to monitor the service. Values are specific to a concrete instance of
     * the FaultDetectionHandler
     */
    void setConfiguration(String[] configArgs);

    /**
     * Register a FaultDetectionListener
     * 
     * @param listener The FaultDetectionListener to register
     */
    void register(FaultDetectionListener<T> listener);

    /**
     * Unregister a FaultDetectionListener
     * 
     * @param listener The FaultDetectionListener to unregister
     */
    void unregister(FaultDetectionListener<T> listener);

    /**
     * Begin monitoring the service
     * 
     * @param service The service that the FaultDetectionHandler will monitor
     * @param serviceID An Object representing a unique service identifier for
     * the service being monitored.
     * @param lCache A LookupCache instance to be used to be notified of service
     * transition events from a Jini Lookup Service
     * @throws Exception If there are abnormal conditions encountered
     */
    void monitor(Object service, T serviceID, LookupCache lCache)
    throws Exception;

    /**
     * Terminate the FaultDetectionHandler
     */
    void terminate();
}
