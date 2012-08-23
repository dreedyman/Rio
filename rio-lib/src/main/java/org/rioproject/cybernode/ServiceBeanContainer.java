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
package org.rioproject.cybernode;

import net.jini.config.Configuration;
import net.jini.id.Uuid;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.event.EventHandler;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.system.ComputeResource;

/**
 * The ServiceBeanContainer defines the semantics required to instantiate service 
 * instances described by a ServiceElement
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanContainer {
    
    /**
     * Get the shared configuration which can be used to delegate Configuration
     * information to
     * 
     * @return The Configuration object that can be used to resolve system set 
     * attributes
     * 
     * @see org.rioproject.config.AggregateConfig
     */
    Configuration getSharedConfiguration();
    
    /**
     * Set the computeResource property. The computeResource attribute represents 
     * the qualitative and quantitative capabilities of the compute resource on 
     * which the ServiceBeanContainer is running.
     * 
     * @param computeResource The ComputeResource Object to use
     */
    void setComputeResource(ComputeResource computeResource);
    
    /**
      * This method is used to get the computeResource attribute. The
     * computeResource attribute represents the qualitative and quantitative 
     * capabilities of the compute resource on which the ServiceBeanContainer is 
     * running.
     * 
     * @return The ComputeResource Object
     */
    ComputeResource getComputeResource();
    
    /**
     * Informs the ServiceBeanContainer that a service has been started
     * 
     * @param identifier Object which can be used by the ServiceBeanContainer to 
     * identify the service
     */
    void started(Object identifier);

    /**
     * Informs the ServiceBeanContainer that a service has been discarded
     * 
     * @param identifier Object which can be used by the ServiceBeanContainer to 
     * identify the service
     */
    void discarded(Object identifier);

    /**
     * Informs the ServiceBeanContainer to remove a service
     * 
     * @param identifier Object which can be used by the ServiceBeanContainer to 
     * identify the service
     */
    void remove(Object identifier);

    /**
     * Get the amount of active services for this ServiceBeanContainer
     *
     * @return The number of active services
     */
    int getServiceCounter();
    
    /**
     * Load and start a service defined by provided attributes
     * 
     * @param sElem The ServiceElement
     * @param opStringMgr The {@link org.rioproject.opstring.OperationalStringManager}
     * that has deployed and is managing the service
     * @param slaEventHandler The EventHandler which is used to send 
     * SLAThresholdEvent notifications for SLAs that have been declared
     * by the service which match quantitative resources the Cybernode has created
     * 
     * @return ServiceBeanInstance
     * 
     * @throws org.rioproject.deploy.ServiceBeanInstantiationException If there are problems loading or
     * instantiating the ServiceBean
     */
    ServiceBeanInstance activate(ServiceElement sElem,
                                 OperationalStringManager opStringMgr,
                                 EventHandler slaEventHandler)
    throws ServiceBeanInstantiationException;
    
    
    /**
     * Invoked to update instantiated service instances of changes in 
     * their ServiceElement objects and OperationalStringManager references. This 
     * method invoked is typically triggered when the OperationalString has been 
     * updated, or the OperationalStringManager has been changed, and provides 
     * somewhat of batch update mechanism.
     * 
     * @param sElems Array of ServiceElement instances to update
     * @param opStringMgr The OperationalStringManager which is performing
     * the update
     */
    void update(ServiceElement[] sElems, OperationalStringManager opStringMgr);

    /**
     * Get all ServiceRecord instances for service instances that have been
     * activated by the ServiceBeanContainer
     * 
     * @return Array of ServiceRecord instances. A new array is allocated each
     * time. If there are no active ServiceBean instances, return a zero-length
     * array
     */
    ServiceRecord[] getServiceRecords();

    /**
     * Get all {@link org.rioproject.deploy.ServiceBeanInstance} objects for a
     * {@link org.rioproject.opstring.ServiceElement}
     *
     * @param element The ServiceElement to obtain ServiceBeanInstance objects
     * for.
     *
     * @return An array of ServiceBeanInstance objects. If the
     * <code>element</code> parameter is <code>null</code> return all
     * ServiceBeanInstance objects. A new array is allocated each time.
     * If there are no matching ServiceBeanInstance objects a zero-length array
     * is returned
     */
    ServiceBeanInstance[] getServiceBeanInstances(ServiceElement element);

    /**
     * Set the Uuid
     *
     * @param uuid The Uuid for the ServiceBeanContainer
     */
    void setUuid(Uuid uuid);

    /**
     * Get the Uuid
     *
     * @return The Uuid for the ServiceBeanContainer
     */
    Uuid getUuid();
    
    /**
     * Get the number of services that are currently being activated
     *
     * @return The number of services currently being activated
     */
    int getActivationInProcessCount();
    
    /**
     * Adds a listener to the set of listeners for this ServiceBeanContainer,
     * provided that it is not the same as some listener already in the set. The
     * order in which notifications will be delivered to multiple listener is
     * not specified. <br>
     * 
     * @param l A ServiceBeanContainerListener to be added.
     */
    void addListener(ServiceBeanContainerListener l);

    /**
     * Removes a listener from the set of listeners for this
     * ServiceBeanContainer, provided that the listener is in the set of known
     * listeners
     *
     * @param l A ServiceBeanContainerListener to be removed.
     */
    void removeListener(ServiceBeanContainerListener l);

    /**
     * Get the ServiceBeanDelegate for a service Uuid
     *
     * @param serviceUuid The identifier of the service
     *
     * @return The ServiceBeanDelegate for the identified service or null if
     * not found
     */
    ServiceBeanDelegate getServiceBeanDelegate(Uuid serviceUuid);
            
    /**
     * Terminate the ServiceBeanContainer. This will terminate all ServiceBeans 
     * running in the ServiceBeanContainer, and shutdown all resources created by 
     * the host and exit. After this call is invoked, attempts to start new 
     * ServiceBean instances are undefined
     */
    void terminate();
    
    /**
     * This will terminate all ServiceBeans running in the ServiceBeanContainer
     */
    void terminateServices();
    
}
