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

import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.opstring.Schedule;
import org.rioproject.resources.servicecore.Service;

import java.rmi.RemoteException;

/**
 * A Cybernode represents a compute resource as a service available through the
 * network. The Cybernode represents the capabilities of compute resource
 * through quantitative &amp; qualitative mechanisms. Cybernode instances
 * dynamically discover and enlist with dynamic provisioning agents, and provide
 * a lightweight container to instantiate dynamic services.
 *
 * @author Dennis Reedy
 */
public interface Cybernode extends ServiceBeanInstantiator, Service {
    @Deprecated
    Schedule getSchedule() throws RemoteException;

    /**
     * Have the Cybernode add itself as a resource which can be used to
     * instantiate dynamic application services.
     *
     * <p></p>If the Cybernode is already enlisted, this method will have
     * no effect
     *
     *
     * @throws IllegalArgumentException If the schedule parameter is <code>null</code>
     * @throws RemoteException If communication errors occur
     */
    void enlist() throws RemoteException;

    /**
     * @deprecated Use enlist() instead
     */
    @Deprecated
    void enlist(Schedule schedule) throws RemoteException;

    /**
     * Have the Cybernode remove itself as a resource which that can be used
     * to instantiate dynamic application services.
     *
     * <p></p>If the Cybernode is already released, this method will have
     * no effect
     *
     * @param terminateServices If this parameter is <code>true</code>, all
     * services which are being hosted by the Cybernode will be terminated as
     * a result of this method invocation
     *
     * @throws RemoteException If communication errors occur
     */
    void release(boolean terminateServices) throws RemoteException;

    /**
     * Get the enlisted state of the Cybernode
     *
     * @return True if the Cybernode can be used to instantiate dynamic
     * application services.
     *
     * @throws RemoteException If communication errors occur
     */
    boolean isEnlisted() throws RemoteException;
}
