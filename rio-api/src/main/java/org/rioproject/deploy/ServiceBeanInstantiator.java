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

import net.jini.core.event.UnknownEventException;
import net.jini.id.Uuid;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;

import java.net.InetAddress;
import java.rmi.RemoteException;

/**
 * The ServiceBeanInstantiator specifies the semantics for a service that
 * provides instantiation and update support for services described by a   
 * {@link org.rioproject.opstring.ServiceElement} object.
 * 
 * The ServiceBeanInstantiator additionally provides semantics allowing clients to
 * obtain information about services that have been started and are currently
 * running using the {@link ServiceStatement} and
 * {@link ServiceRecord}
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanInstantiator {
    /**
     * This method is invoked as a result of event registration to 
     * {@link ProvisionManager} instances. The
     * ServiceBeanInstantiator will register for 
     * {@link ServiceProvisionEvent} notifications.
     * 
     * @param event - The ServiceProvisionEvent
     * 
     * @return The deployed service object
     * 
     * @throws ServiceBeanInstantiationException  f there are problems loading or
     * instantiating the ServiceBean
     * @throws UnknownEventException if it does not recognize the Event ID
     * @throws RemoteException if communication errors occur
     */
    DeployedService instantiate(ServiceProvisionEvent event)
    throws ServiceBeanInstantiationException, UnknownEventException, RemoteException;

    /**
     * Invoked to update instantiated ServiceBean instances of changes in 
     * their {@link org.rioproject.opstring.ServiceElement} objects and
     * {@link org.rioproject.opstring.OperationalStringManager} references. This
     * method invocation is typically triggered when the 
     * {@link org.rioproject.opstring.OperationalString} has been
     * updated, or the OperationalStringManager has been changed, and provides 
     * somewhat of a batch update mechanism.
     * <p>
     * The ServiceBeanInstantiator will match the array of 
     * {@link org.rioproject.opstring.ServiceElement} objects to instantiated service
     * instances. For each match, the instance 
     * will have it's ServiceElement updated with the provided ServiceElement object.
     * <p>
     * ServiceElement updates can trigger changes in running services. ServiceElement
     * attributes (and contained class attributes) which may trigger behavior changes
     * as follows:
     * <ol>
     * <li>Additions/removals or changes to declared
     * {@link org.rioproject.sla.SLA} instances
     * <li>Additions/removals or changes to declared 
     * <code>Association</code> instances
     * <li>Additions/removals to declared parameters
     * </ol>
     * @param sElements Array of ServiceElement instances to update
     * @param opStringMgr The OperationalStringManager which is performing
     * the update
     * @throws RemoteException If communication errors happen
     */
    void update(ServiceElement[] sElements, OperationalStringManager opStringMgr)
    throws RemoteException;
    
    /**
     * This method returns an array of 
     * {@link ServiceStatement} objects which contain
     * information about the ServiceBean instances this ServiceBeanInstantiator
     * has instantiated.
     * 
     * @return An array of ServiceStatement objects. The array will be empty
     * if no ServiceStatement instances are found
     *
     * @throws RemoteException If communication errors happen
     */
    ServiceStatement[] getServiceStatements() throws RemoteException;

    /**
     * This method returns a
     * {@link ServiceStatement} for the
     * {@link org.rioproject.opstring.ServiceElement}
          *
     * @param sElem The ServiceElement to get the ServiceStatement for.
     * If this argument should is null, an IllegalArgumentException is thrown.
     *
     * @return A ServiceStatement for the ServiceElement. If the ServiceElement
     * has never been instantiated by the ServiceBeanInstantiator, a null is
     * returned
     *
     * @throws RemoteException If communication errors happen
     */
    ServiceStatement getServiceStatement(ServiceElement sElem) throws RemoteException;

    /**
     * This method returns an array of  
     * {@link ServiceRecord} objects which contain
     * information about the service instances this ServiceBeanInstantiator
     * has instantiated.
     * 
     * @param filter A filter for ServiceRecord retrieval. ServiceRecord
     * instances will be returned if the ServiceRecord type matches the filter
     * type. The filter can be either ServiceRecord.ACTIVE_SERVICE_RECORD or
     * ServiceRecord.INACTIVE_SERVICE_RECORD.
     * @return An array of ServiceRecord objects. The array will be empty if no 
     * ServiceRecord instances are found which match the filter
     *
     * @throws RemoteException If communication errors happen
     */
    ServiceRecord[] getServiceRecords(int filter) throws RemoteException;

    /**
     * Get all {@link ServiceBeanInstance} objects for a {@link org.rioproject.opstring.ServiceElement}
     *
     * @param element The ServiceElement to obtain ServiceBeanInstance objects for.
     *
     * @return An array of ServiceBeanInstance objects. If the
     * <code>element</code> parameter is <code>null</code> return all
     * ServiceBeanInstance objects. A new array is allocated each time.
     * If there are no matching ServiceBeanInstance objects a zero-length array
     * is returned
     *
     * @throws RemoteException If communication errors happen
     */
    ServiceBeanInstance[] getServiceBeanInstances(ServiceElement element) throws RemoteException;

    /**
     * Get a name for the ServiceBeanInstantiator
     *
     * @return A human readable name for the ServiceBeanInstantiator
     *
     * @throws RemoteException If communication errors happen
     */
    String getName() throws RemoteException;

    /**
     * Get the Uuid which uniquely identifies the ServiceBeanInstantiator
     *
     * @return The Uuid of the ServiceBeanInstantiator
     *
     * @throws RemoteException If communication errors happen
     */
    Uuid getInstantiatorUuid() throws RemoteException;

    /**
     * Get the IP address of the ServiceBeanInstantiator
     *
     * @return The {@link java.net.InetAddress}
     *
     * @throws RemoteException If communication errors happen
     */
    InetAddress getInetAddress() throws RemoteException;
}
