/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.core.jsb;

import net.jini.id.Uuid;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceBeanConfig;

import javax.management.NotificationBroadcasterSupport;

/**
 * The ServiceBeanManager provides a mechanism for the ServiceBean to obtain a
 * {@link org.rioproject.core.jsb.DiscardManager}, have the 
 * {@link org.rioproject.opstring.ServiceBeanConfig} updated to known
 * {@link org.rioproject.opstring.OperationalStringManager} instance(s), and
 * increment, decrement oe relocate instances of the service
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanManager {
    /**
     * Update a ServiceBean's ServiceBeanConfig to known
     * {@link org.rioproject.opstring.OperationalStringManager} instance(s)
     *
     * @param sbConfig The updated ServiceBeanConfig instance
     *
     * @throws ServiceBeanManagerException if any errors occur communicating with the
     * OperationalStringManager, or with the OperationalStringManager's
     * processing of the request
     */
    void update(ServiceBeanConfig sbConfig) throws ServiceBeanManagerException;

    /**
     * Increment (increase the number of) instances of the ServiceBean by one.
     * This will cause the provisioning of a ServiceBean to an available
     * compute resource which meets the operational criteria specified by the
     * ServiceElement.
     *
     * @param listener A ServiceProvisionListener that will be notified of
     * the result of the increment request
     *
     * @throws ServiceBeanManagerException if any errors occur communicating with the
     * OperationalStringManager, or with the OperationalStringManager's
     * processing of the request
     */
    void increment(ServiceProvisionListener listener) throws ServiceBeanManagerException;

    /**
     * Decrement (decrease the number of) and remove this ServiceBean instance
     * from the OperationalString.
     *
     * @param destroy If true, destroy the ServiceBean upon removal,
     * otherwise just remove
     *
     * @throws ServiceBeanManagerException if any errors occur communicating with the
     * OperationalStringManager, or with the OperationalStringManager's
     * processing of the request
     */
    void decrement(boolean destroy) throws ServiceBeanManagerException;

    /**
     * Relocate (move) a ServiceBean instance to another compute resource
     *
     * @param listener A ServiceProvisionListener that will be notified of
     * the result of the relocate request
     * @param uuid The uuid of a
     * {@link org.rioproject.deploy.ServiceBeanInstantiator} to relocate
     * to. If null, the OperationalStringManager will determine a suitable
     * resource
     *
     * @throws ServiceBeanManagerException if any errors occur communicating with the
     * OperationalStringManager, or with the OperationalStringManager's
     * processing of the request
     */
    void relocate(ServiceProvisionListener listener, Uuid uuid) throws ServiceBeanManagerException;

    /**
     * Get the DiscardManager for the ServiceBean. A ServiceBean must obtain
     * the DiscardManager upon termination. The DiscardManager performs
     * necessary cleanup tasks associated with the termination of a ServiceBean
     *
     * @return DiscardManager
     */
    DiscardManager getDiscardManager();

    /**
     * Get the OperationalStringManager instance which is managing the
     * OperationalString the ServiceBean is a part of
     *
     * @return The OperationalStringManager instance
     * which is managing the OperationalString the ServiceBean is a part of
     */
    OperationalStringManager getOperationalStringManager();

    /**
     * Get the {@link org.rioproject.admin.ServiceBeanControl}.
     *
     * @return the {@link org.rioproject.admin.ServiceBeanControl}
     *
     * @throws ServiceBeanManagerException if the proxy cannot be obtained, or if the proxy is not
     * {@link net.jini.admin.Administrable} or if the proxy does not implement {@link ServiceBeanControl}.
     */
    ServiceBeanControl getServiceBeanControl() throws ServiceBeanManagerException;

    /**
     * Get the universally unique identifier for the ServiceBean
     *
     * @return The universally unique identifier for the ServiceBean
     */
    Uuid getServiceID();

    /**
     * Get the ServiceBeanInstance for the ServiceBean
     *
     * @return The ServiceBeanInstance for the ServiceBean
     */
    ServiceBeanInstance getServiceBeanInstance();

    /**
     * Register for notification of ServiceElement changes. The
     * ServiceElementChangeListener will be notified each time the
     * ServiceElement is changed. If the parameter value duplicates (using
     * equals) another element in the set of listeners, no action is taken. If
     * the parameter  value is null, a NullPointerException is  thrown
     * 
     * @param l A ServiceElementChangeListener instance to add. 
     */
    void addListener(ServiceElementChangeListener l);

    /**
     * Remove a currently registered ServiceElementChangeListener instance from
     * the set of listeners currently registered with the ServiceBeanManager.
     * If the parameter value is null, or if the parameter value does not exist
     * in the managed set of listeners, no action is taken
     * 
     * @param l A ServiceElementChangeListener instance to remove. 
     */
    void removeListener(ServiceElementChangeListener l);

    /**
     * Get the class that can be used as the class that sends JMX notifications
     * for the MBean
     *
     * @return A {@link javax.management.NotificationBroadcasterSupport}
     */
    NotificationBroadcasterSupport getNotificationBroadcasterSupport();
}
