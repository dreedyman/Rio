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

import org.rioproject.deploy.ServiceRecord;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines a listener for forked service bean lifecycle notifications
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanExecListener extends Remote {
    /**
     * Notify all ServiceBeanContainerListener implementations with a
     * ServiceRecord signifying that a service has been instantiated
     *
     * @param record The ServiceRecord for the newly instantiated service
     *
     * @throws RemoteException if for some reason communication errors occur.
     */
    void serviceInstantiated(ServiceRecord record) throws RemoteException;

    /**
     * Notify all ServiceBeanContainerListener implementations with a
     * ServiceRecord signifying that a service has been discarded
     *
     * @param record The ServiceRecord for the discarded service
     *
     * @throws RemoteException if for some reason communication errors occur.
     */
    void serviceDiscarded(ServiceRecord record) throws RemoteException;
}
