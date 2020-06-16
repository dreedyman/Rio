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
package org.rioproject.admin;

import net.jini.id.Uuid;
import org.rioproject.opstring.ServiceElement;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Get and set the {@link org.rioproject.opstring.ServiceElement} for a service
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanAdmin extends Remote {    

    /**
     * Get the ServiceElement for the ServiceBean
     * 
     * @return The ServiceElement
     *
     * @throws RemoteException If communication errors occur
     */
    ServiceElement getServiceElement() throws RemoteException;
    
    /**
     * Set the ServiceElement for the ServiceBean
     * 
     * @param sElem The ServiceElement
     *
     * @throws RemoteException If communication errors occur
     */
    void setServiceElement(ServiceElement sElem) throws RemoteException;
    
    /**
     * How long the service has been running
     *
     * @return The amount of time (in milliseconds) the servie has been "up"
     * (running)
     *
     * @throws RemoteException If communication errors occur
     */
    long getUpTime() throws RemoteException;

    /**
     * Get the Uuid of the ServiceBeanInstantiator the ServiceBean is being
     * hosted by
     *
     * @return The Uuid of the ServiceBeanInstantiator the ServiceBean is being
     * hosted by
     *  
     * @throws RemoteException If communication errors occur
     */
    Uuid getServiceBeanInstantiatorUuid() throws RemoteException;
}
