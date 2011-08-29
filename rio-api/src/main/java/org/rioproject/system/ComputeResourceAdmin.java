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

import net.jini.id.Uuid;
import org.rioproject.sla.SLA;
import org.rioproject.system.capability.PlatformCapability;

import java.rmi.RemoteException;

/**
 * The ComputeResourceAdmin provides an administrative interface for a
 * ComputeResource
 *
 * @author Dennis Reedy
 */
public interface ComputeResourceAdmin {
    /**
     * Set the SLA for a MeasurableCapability contained within the
     * ComputeResource
     * 
     * @param serviceLevelAgreement The SLA for the ComputeResource
     *
     * @return True if updated, false otherwise
     *
     * @throws RemoteException If communication errors occur
     */
    boolean setSLA(SLA serviceLevelAgreement) throws RemoteException;

    /**
     * Get the system SLAs which provide control information for the
     * MeasurableCapability components the ComputeResource contains
     * 
     * @return The system SLA for each MeasurableCapability contained
     * within the ComputeResource
     *
     * @throws RemoteException If communication errors occur
     */
    SLA[] getSLAs() throws RemoteException;

    /**
     * Get the PlatformCapability components for the ComputeResource
     * 
     * @return The PlatformCapability components for the ComputeResource
     *
     * @throws RemoteException If communication errors occur
     */
    PlatformCapability[] getPlatformCapabilties() throws RemoteException;

    /**
     * Get the MeasuredResource components for the ComputeResource
     * 
     * @return Array of MeasuredResource instances that correspond to 
     * MeasurableCapability components
     *
     * @throws RemoteException If communication errors occur
     */
    MeasuredResource[] getMeasuredResources() throws RemoteException;

    /**
     * Get the resource's utilization. ComputeResource quantitative behavior is
     * represented by MeasurableCapability objects. MeasurableCapability objects
     * produce a relative value which represents the percentage of the
     * capability being measured. This value is referred to as a
     * MeasurableCapability utilization. This method returns the aggregate of
     * all computed utilizations
     * 
     * @return The ComputeResource's utilization
     *
     * @throws RemoteException If communication errors occur
     */
    double getUtilization() throws RemoteException;

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

    /**
     * Get the reportInterval property which controls how often the ComputeResource
     * will inform registered Observers of a state change. A state change is
     * determined if any of the MeasurableCapability components contained within
     * this ComputeResource provide an update in the interval specified by the
     * reportInterval property
     *
     * @return The interval controlling when the ComputeResource reports change
     * of state
     *
     * @throws RemoteException If communication errors occur
     */
    long getReportInterval() throws RemoteException;

    /**
     * Set the reportInterval property which controls how often the ComputeResource
     * will inform registered Observers of a state change. A state change is
     * determined if any of the MeasurableCapability components contained within
     * this ComputeResource provide an update in the interval specified by the
     * reportInterval property.
     *
     * <p>Note: MeasurableCapability components contained within
     * the ComputeResource may have reportRates greater than then the
     * reportInterval. If this is the case, the ComputeResource will only report
     * state changes if the values reported by the contained
     * MeasurableCapability objects change.
     *
     * @param reportInterval The interval controlling when the ComputeResource
     * reports change of state to registered Observers
     *
     * @throws IllegalArgumentException if the reportInterval < 0
     * @throws RemoteException If communication errors occur
     */
    void setReportInterval(long reportInterval) throws RemoteException;

}
