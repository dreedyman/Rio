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
package org.rioproject.jsb;

import java.io.IOException;
import java.util.Date;

/**
 * Provides a standard MBean to use when administering a ServiceBean using JMX
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanAdapterMBean {
    /**
     * The advertise method provides the capability for a ServiceBean to
     * advertise itself on the network providing access to all clients. If the
     * ServiceBean is already advertised, this method returns immediately.
     *
     * If the ServiceBean is not advertised, this method will obtain attributes
     * from the ServiceBeanContext.getConfiguration(), and additionally create
     * the following attributes and add them to the Collection of attributes
     * for the service:
     * <ul>
     * <li>ComputeResourceUtilization
     * <li>ComputeResourceInfo
     * <li>StandardServiceType
     * </ul>
     *
     * @throws IOException If errors occur accessing underlying communication
     * mechanisms
     * @throws IllegalStateException If the state transition is illegal
     */
    void advertise() throws IOException;

    /**
     * The unadvertise method informs the ServiceBean to cancel all
     * advertisements (registrations, etc...) it has made on the network. The
     * ServiceBean must still be available to accept incoming communications
     *
     * @throws IOException If errors occur accessing underlying communication
     * mechanisms
     * @throws IllegalStateException If the state transition is illegal
     */
    void unadvertise() throws IOException;

    /**
     * The destroy method will destroy the ServiceBean forceably
     *
     * @throws IllegalStateException If the state transition is illegal
     */
    void destroy();

    /**
     * The destroy method is used to destroy a ServiceBeanAdapter. This method
     * will unadvertise and stop the ServiceBean
     *
     * @param force If true, unexports the ServiceBean even if there are
     * pending or in-progress calls; if false, only unexports the ServiceBean
     * if there are no pending or in-progress calls
     *
     * If the <code>force</code> parameters is <code>false</code>, unexporting
     * the ServiceBean will be governed by the following configuration
     * properties:
     * <ul>
     * <li><tt>maxUnexportDelay</tt> Indicates the maximum amount of time to
     * wait for unexport attempts
     * <li><tt>unexportRetryDelay</tt> Length of time to sleep between unexport
     * attempts
     * </ul>
     *
     * @throws IllegalStateException If the state transition is illegal
     */
    void destroy(boolean force);

    /**
     * Get the Date the ServiceBean was started
     */
    Date getStarted();

    /**
     * Get the discovery groups
     */
    String[] getLookupGroups();

    /**
     * Set the discovery groups
     */
    void setLookupGroups(String[] groups);


}
