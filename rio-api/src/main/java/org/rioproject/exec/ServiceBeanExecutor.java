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
package org.rioproject.exec;

import net.jini.id.Uuid;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.watch.Watchable;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines the interface for a utility that will fork a service into
 * it's own process
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanExecutor extends Watchable, Remote {

    /**
     * Get the {@link org.rioproject.system.ComputeResourceUtilization} for the
     * service's JVM
     *
     * @return The ComputeResourceUtilization
     *
     * @throws RemoteException If communication errors happen
     */
    ComputeResourceUtilization getComputeResourceUtilization() throws RemoteException;

    /**
     * If the forked service required provisioning of additional software,
     * apply the installed {@code PlatformCapability} objects.
     *
     * @param pCaps Array of {@link org.rioproject.system.capability.PlatformCapability}
     * to apply. If the array is null or empty, no action is taken.
     *
     * @throws RemoteException If communication errors happen
     */
    void applyPlatformCapabilities(PlatformCapability[] pCaps) throws RemoteException;

    /**
     * This method is invoked by the Cybernode to activate a service in it's own
     * VM. The Cybernode sends the
     * {@link org.rioproject.deploy.ServiceProvisionEvent} it received
     * to the <tt>ServiceBeanExecutor</tt>
     *
     * @param sElem The ServiceElement
     * @param opStringMgr The {@link org.rioproject.opstring.OperationalStringManager}
     * that has deployed and is managing the service
     * @return ServiceBeanInstance- A ServiceBeanInstance object
     *
     * @throws org.rioproject.deploy.ServiceBeanInstantiationException if there are problems
     * loading or instantiating the ServiceBean
     * @throws RemoteException if communication errors occur
     */
    ServiceBeanInstance instantiate(ServiceElement sElem, OperationalStringManager opStringMgr)
        throws ServiceBeanInstantiationException, RemoteException;

    /**
     * Invoked to update an instantiated service instance of changes in the
     * {@link org.rioproject.opstring.ServiceElement}, and {@link
     * org.rioproject.opstring.OperationalStringManager} reference. This method
     * invocation is typically triggered when the {@link
     * org.rioproject.opstring.OperationalString} has been updated, or the
     * {@code OperationalStringManager} has been changed.
     * <p/>
     * ServiceElement updates can trigger changes in running services.
     * ServiceElement attributes (and contained class attributes) which may
     * trigger behavior changes as follows: <ol> <li>Additions/removals or
     * changes to declared {@link org.rioproject.sla.SLA} instances
     * <li>Additions/removals or changes to declared {@code
     * org.rioproject.associations.Association} instances <li>Additions/removals to
     * declared parameters </ol>
     *
     * @param elem ServiceElement to update
     * @param opStringMgr The OperationalStringManager which is performing the
     * update
     * @throws RemoteException If communication errors happen
     */
    void update(ServiceElement elem, OperationalStringManager opStringMgr)
        throws RemoteException;

    /**
     * Get the {@link ServiceBeanInstance} for a service that has been instantiated by the
     * {@code ServiceBeanExecutor}
     *
     * @return The {@link ServiceBeanInstance} for a service that has been instantiated by the
     * {@code ServiceBeanExecutor}
     *
     * @throws RemoteException If communication errors happen
     */
    ServiceBeanInstance getServiceBeanInstance() throws RemoteException;

    /**
     * Get the {@code ID} for the JVM the forked service has been created in.
     *
     * @return The {{@code ID} for the JVM the forked service has been created in.
     *
     * @throws RemoteException If there are communication problems
     */
    String getID() throws RemoteException;

    /**
     * Set the Uuid for the ServiceBeanExecutor. This will be used to create the
     * {@link org.rioproject.deploy.ServiceBeanInstance}, and points to the
     * Cybernode that is forking the service bean
     *
     * @param uuid the Uuid
     * @throws RemoteException If there are communication problems
     */
    void setUuid(Uuid uuid) throws RemoteException;

    /**
     * Set the callback, allowing the ServiceBeanExecutor to notify the delegate
     * of lifecycle changes
     *
     * @param listener The ServiceBeanExecListener
     * @throws RemoteException If for some reason communication fails
     */
    void setServiceBeanExecListener(ServiceBeanExecListener listener) throws RemoteException;

}
