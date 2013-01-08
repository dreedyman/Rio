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

import net.jini.id.Uuid;
import org.rioproject.admin.ServiceAdmin;
import org.rioproject.system.ComputeResourceAdmin;
import org.rioproject.system.ComputeResourceUtilization;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * CybernodeAdmin defines methods used to administer a Cybernode
 *
 * @author Dennis Reedy
 */
public interface CybernodeAdmin extends ServiceAdmin, ComputeResourceAdmin {
    /**
     * Get the upper limit of services that this Cybernode can instantiate
     * 
     * @return The upper limit of services
     *
     * @throws RemoteException If communication errors happen
     */
    Integer getServiceLimit() throws RemoteException;

    /**
     * Set the upper limit of services that this Cybernode can instantiate
     * 
     * @param count Integer indicating the upper limit of services
     *
     * @throws RemoteException If communication errors happen
     */
    void setServiceLimit(Integer count) throws RemoteException;

    /**
     * Get the number of services that this Cybernode has instantiated
     * 
     * @return The number of services the Cybernode has instantiated
     *
     * @throws RemoteException If communication errors happen
     */
    Integer getServiceCount() throws RemoteException;

    /**
     * Get whether the Cybernode supports persistent provisioning of
     * qualitative capabilities
     * 
     * @return True if the Cybernode supports persistent
     * provisioning of qualitative capabilities otherwise return false
     *
     * @throws RemoteException If communication errors happen
     */
    boolean getPersistentProvisioning() throws RemoteException;

    /**
     * Set whether the Cybernode supports persistent provisioning of
     * qualitative capabilities
     * 
     * @param support Set to true if the Cybernode supports persistent
     * provisioning of qualitative capabilities otherwise, set false
     *
     * @throws RemoteException If communication errors happen
     */
    void setPersistentProvisioning(boolean support)
        throws IOException;

    /**
     * Get the port that the Cybernode has started the RMI Registry on
     *
     * @return The port the Cybernode has started the RMI Registry on
     *
     * @throws RemoteException If communication errors happen
     */
    int getRegistryPort() throws RemoteException;

    /**
     * Get the {@link org.rioproject.system.ComputeResourceUtilization} for an
     * instantiated service
     *
     * @param serviceUuid The {@link net.jini.id.Uuid} of an instantiated service
     *
     * @return The ComputeResourceUtilization for the service identified by
     * the <tt>serviceUuid</tt>. If the identified service is contained within
     * the Cybernode, the returned ComputeResourceUtilization will be the same
     * as the value returned from
     * {@link org.rioproject.system.ComputeResourceAdmin#getComputeResourceUtilization()}.
     * <p>If the identified service has been executed in it's own process, the returned
     * value will represent the ComputeResourceUtilization for that process.
     * <p>If no instantiated service can be found, a null is returned.
     *
     * @throws RemoteException If communication errors occur
     */
    ComputeResourceUtilization getComputeResourceUtilization(Uuid serviceUuid) throws RemoteException;
}
