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

import org.rioproject.opstring.ServiceElement;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A ServiceProvisionListener waits for notification that a ServiceBean has been 
 * provisioned
 *
 * @author Dennis Reedy
 */
public interface ServiceProvisionListener extends Remote {
    /**
     * Notify listener that the Service described by the ServiceBeanInstance has
     * been provisioned successfully
     * 
     * @param serviceBeanInstance The ServiceBeanInstance
     *
     * @throws RemoteException If communication errors occur
     */
    void succeeded(ServiceBeanInstance serviceBeanInstance) throws RemoteException;

    /**
     * Notify listener that the Service described by the ServiceElement has
     * not been provision successfully
     * 
     * @param sElem The ServiceElement
     * @param resubmitted Whether the  Service described by the ServiceElement
     * has been resubmitted for provisioning
     *
     * @throws RemoteException If communication errors occur
     */
    void failed(ServiceElement sElem, boolean resubmitted) throws RemoteException;
}


