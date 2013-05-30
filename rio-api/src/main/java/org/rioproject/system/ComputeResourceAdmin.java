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
package org.rioproject.system;

import java.rmi.RemoteException;

/**
 * The ComputeResourceAdmin provides an administrative interface for a
 * ComputeResource
 *
 * @author Dennis Reedy
 */
public interface ComputeResourceAdmin {

    /**
     * Get the {@link org.rioproject.system.ResourceCapability} for the
     * compute resource
     *
     * @return The ResourceCapability for the compute resource.
     * The ResourceCapability class provides the utilization, platform and
     * measurable capabilities for a compute resource.
     *
     * @throws RemoteException If communication errors occur
     */
    ResourceCapability getResourceCapability() throws RemoteException;

    /**
     * Get the {@link org.rioproject.system.ComputeResourceUtilization} for the
     * compute resource
     *
     * @return The ComputeResourceUtilization for the compute resource. This
     * object represents a snapshot of the depletion-oriented resources on the
     * compute resource. A new ComputeResourceUtilization is created each time
     * this method is invoked
     *
     * @throws RemoteException If communication errors occur
     */
    ComputeResourceUtilization getComputeResourceUtilization() throws RemoteException;
}
