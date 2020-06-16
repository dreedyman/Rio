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
package org.rioproject.deploy;

import net.jini.core.event.EventRegistration;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import org.rioproject.system.ResourceCapability;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.List;

/**
 * The ProvisionManager defines the semantics for
 * {@link ServiceBeanInstantiator} instances to register
 * for
 * {@link ServiceProvisionEvent} notifications and to
 * provide a feedback mechanism for 
 * {@link ServiceBeanInstantiator} instances to update
 * their {@link org.rioproject.system.ResourceCapability} and
 * {@link ServiceRecord}s
 *
 * @author Dennis Reedy
 */
public interface ProvisionManager {
    /**
     * Register for notifications of {@link ServiceProvisionEvent}s. The returned
     * {@link net.jini.core.event.EventRegistration} is leased; the lease must be
     * managed by the caller. The event ID in the returned EventRegistration
     * corresponds to the event ID of the ServiceProvisionEvent.
     * <p>
     * While the EventRegistration is valid, ServiceProvisionEvent notifications
     * are sent to the specified ServiceBeanInstantiator whenever the
     * ProvisionManager determines that a ServiceBeanInstantiator meets requirements
     * specified by the ServiceBean.
     *
     * @param instantiator The ServiceBeanInstantiator wrapped in a MarshalledObject
     * @param handback The handback Object to include with event notification
     * @param deployedServices An immutable <tt>List</tt> of {@link DeployedService}
     * objects documenting existing deployed (and active) services. If there
     * are no active services, an empty <tt>List</tt> must be provided.
     * @param resourceCapability The capabilities of the compute resource
     * @param serviceLimit The maximum amount of services the compute resources
     * will accept for provisioning
     * @param duration The requested lease duration
     *
     * @return An EventRegistration
     *
     * @throws LeaseDeniedException If the Lease to the ProvisionManager is denied
     * @throws RemoteException If communication errors happen
     */
    EventRegistration register(MarshalledObject<ServiceBeanInstantiator> instantiator,
                               MarshalledObject handback,
                               ResourceCapability resourceCapability,
                               List<DeployedService> deployedServices,
                               int serviceLimit,
                               long duration) throws LeaseDeniedException, RemoteException;

    /**
     * Provides a feedback mechanism for a {@link ServiceBeanInstantiator} to update it's operational
     * capabilities as described by the {@link org.rioproject.system.ResourceCapability} object, and
     * any changes in the maximum number of services the ServiceBeanInstantiator will accept for
     * service provisioning.
     * <p>
     * The ServiceBeanInstantiator must have an active {@link net.jini.core.lease.Lease} with the
     * ProvisionManager for this method to be successful.
     *
     * @param instantiator The Listener to send events to
     * @param resourceCapability The capabilities of the compute resource
     * @param deployedServices An immutable <tt>List</tt> of {@link DeployedService}
     * objects documenting existing deployed (and active) services. If there
     * are no active services, an empty <tt>List</tt> must be provided.
     * @param serviceLimit The maximum amount of services the compute resources
     * will accept for provisioning
     *
     * @throws UnknownLeaseException If the Lease to the ProvisionManager is
     * unknown
     * @throws RemoteException If communication errors happen
     */
    void update(ServiceBeanInstantiator instantiator,
                ResourceCapability resourceCapability,
                List<DeployedService> deployedServices,
                int serviceLimit) throws UnknownLeaseException, RemoteException;

    /**
     * Get all registered
     * {@link ServiceBeanInstantiator} instances.
     *
     * @return An array of registered
     * {@link ServiceBeanInstantiator} instances.
     * If there are no registered <tt>ServiceBeanInstantiator</tt>s, return
     * a zero-length array. A new array is allocated each time
     *
     * @throws IOException If communication errors occur if there are problems deserializing
     * {@code ServiceBeanInstantiator} instances.
     */
    ServiceBeanInstantiator[] getServiceBeanInstantiators() throws IOException;
}
