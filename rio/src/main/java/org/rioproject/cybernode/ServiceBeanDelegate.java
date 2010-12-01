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
package org.rioproject.cybernode;

import org.rioproject.core.*;
import org.rioproject.core.provision.ServiceRecord;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.jsb.JSBControlException;
import org.rioproject.event.EventHandler;

/**
 * Defines the semantics of a class that the Cybernode delegates management of
 * created service instances
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanDelegate {

    /**
     * Set the <tt>OperationalStringManager</tt>
     *
     * @param opStringMgr The {@link org.rioproject.core.OperationalStringManager}
     * that has deployed and is managing the service
     */
    void setOperationalStringManager(OperationalStringManager opStringMgr);

    /**
     * Set the <tt>ServiceElement</tt>
     *
     * @param sElem The {@link org.rioproject.core.ServiceElement} detailing
     * the service and it's properties
     */
    void setServiceElement(ServiceElement sElem);

    /**
     * Set the <tt>EventHandler</tt>
     *
     * @param slaEventHandler The EventHandler which is used to send
     * {@link org.rioproject.sla.SLAThresholdEvent} notifications for
     * {@link org.rioproject.sla.SLA}s that have been declared
     * by the service which match quantitative resources the Cybernode has
     * created
     */
    void setEventHandler(EventHandler slaEventHandler);

    /**
     * Get the ServiceRecord
     *
     * @return ServiceRecord
     */
    ServiceRecord getServiceRecord();

    /**
     * Get the ServiceElement
     *
     * @return The ServiceElement used by this JSBDelegate
     */
    ServiceElement getServiceElement();

    /**
     * Loads and starts a service, blocking until the service has been started
     *
     * @return The ServiceBeanInstance
     *
     * @throws JSBInstantiationException if there are erros loading the service
     * bean
     */
    ServiceBeanInstance load() throws JSBInstantiationException;

    /**
     * Get the ServiceBeanInstance
     *
     * @return The ServiceBeanInstance
     */
    ServiceBeanInstance getServiceBeanInstance();

    /**
     * Advertise the ServiceBean, making it available to all clients
     *
     * @throws org.rioproject.jsb.JSBControlException if the service bean cannot be advertised
     */
    void advertise() throws JSBControlException;
    
    /*
     * Update the ServiceElement and potentially the OperationalStringManager
     * if the ServiceElement is the same as the ServiceElement the delegate
     * has created a ServiceBean instance of
     */
    boolean update(ServiceElement newElem, OperationalStringManager opMgr);

    /**
     * Get the ComputeResourceUtilization
     *
     * @return The ComputeResourceUtilization for the represented service. If
     * the service is contained within the CYbernode, return the
     * ComputeResourceUtilization of the Cybernode. Oherwise reetun the
     * ComputeResourceUtilization of the process hosting the represented service
     */
    ComputeResourceUtilization getComputeResourceUtilization();

    /**
     * Terminate the ServiceBeanDelegate and the represented service
     */
    void terminate();
}
