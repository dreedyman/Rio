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
package org.rioproject.cybernode.exec;

import net.jini.id.Uuid;
import org.rioproject.core.JSBInstantiationException;
import org.rioproject.core.OperationalStringManager;
import org.rioproject.core.ServiceBeanInstance;
import org.rioproject.core.ServiceElement;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.watch.Watchable;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines the interface for a utility that will fork a service bean into
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
     * If the forked service bean required provisioning of additional software,
     * apply the installed
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
     * {@link org.rioproject.core.provision.ServiceProvisionEvent} it received
     * to the <tt>ServiceBeanExecutor</tt>
     *
     * @param sElem The ServiceElement
     * @param opStringMgr The {@link org.rioproject.core.OperationalStringManager}
     * that has deployed and is managing the service
     * @return ServiceBeanInstance- A ServiceBeanInstance object
     *
     * @throws org.rioproject.core.JSBInstantiationException if there are problems
     * loading or instantiating the ServiceBean
     * @throws RemoteException if communication errors occur
     */
    ServiceBeanInstance instantiate(ServiceElement sElem,
                                    OperationalStringManager opStringMgr)
        throws JSBInstantiationException, RemoteException;

    /**
     * Invoked to update an instantiated ServiceBean instance of changes in the
     * {@link org.rioproject.core.ServiceElement}, and {@link
     * org.rioproject.core.OperationalStringManager} reference. This method
     * invocation is typically triggered when the {@link
     * org.rioproject.core.OperationalString} has been updated, or the
     * OperationalStringManager has been changed.
     * <p/>
     * ServiceElement updates can trigger changes in running services.
     * ServiceElement attributes (and contained class attributes) which may
     * trigger behavior changes as follows: <ol> <li>Additions/removals or
     * changes to declared {@link org.rioproject.sla.SLA} instances
     * <li>Additions/removals or changes to declared {@link
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
     * The ServiceBeanExecutor will create an RMI Registry. This method gets the
     * port the Registry has been created on
     *
     * @return The port the ServiceBeanExecutor has created an RMI Registry on
     *
     * @throws RemoteException If there are communication problems
     */
    int getRegistryPort() throws RemoteException;

    /**
     * Set the Uuid for the ServiceBeanExecutor. This will be used to create the
     * {@link org.rioproject.core.ServiceBeanInstance}, and points to the
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
    void setServiceBeanExecListener(ServiceBeanExecListener listener) throws
                                                                      RemoteException;

}
