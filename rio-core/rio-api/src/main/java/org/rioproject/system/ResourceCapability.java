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

import org.rioproject.system.capability.PlatformCapability;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Represents the capabilities of a compute resource. The ResourceCapability
 * class provides the utilization, platform and measured resource capabilities for
 * a compute resource.
 *
 * @author Dennis Reedy
 */
public class ResourceCapability implements Comparable, Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** Platform capabilities of the ComputeResource */
    private PlatformCapability[] platformCapabilities;
    /** Measured capabilities of the ComputeResource */
    private ComputeResourceUtilization resourceUtilization;
    /** The IP Address of the ComputeResource */
    private String address;
    /** The hostname of the ComputeResource */
    private String hostName;
    /** Whether the ComputeResource supports persistent provisioning */
    private boolean persistentProvisioning;
    /** Used to get all MeasuredResource instances */
    public static final int ALL_MEASURED_RESOURCES=1;
    /** Used to get MeasuredResource instances which have a value outside
     * of declared thresholds */
    public static final int MEASURED_RESOURCES_BREACHED=2;         

    /**
     * Construct a ResourceCapability object
     * 
     * @param address The IP Address of the ComputeResource
     * @param hostName The hostname of the ComputeResource
     * @param persistentProvisioning Whether the ComputeResource supports
     * persistent provisioning
     * @param platformCapabilities An array of PlatformCapability objects for
     * the compute resource
     * @param resourceUtilization The ComputeResourceUtilization,
     * representing the measured attributes of the compute resource
     */
    public ResourceCapability(String address, 
                              String hostName,
                              boolean persistentProvisioning,
                              PlatformCapability[] platformCapabilities,
                              ComputeResourceUtilization resourceUtilization) {
        if(address == null)
            throw new IllegalArgumentException("address is null");
        if(hostName == null)
            throw new IllegalArgumentException("hostName is null");
        if(platformCapabilities == null)
            throw new IllegalArgumentException("platformCapabilities is null");
        if(resourceUtilization == null)
            throw new IllegalArgumentException("resourceUtilization is null");
        this.address = address;
        this.hostName = hostName;
        this.persistentProvisioning = persistentProvisioning;
        this.platformCapabilities = platformCapabilities;
        this.resourceUtilization = resourceUtilization;
    }

    /**
     * Get the address
     * 
     * @return The TCP/IP address the ResourceCapability represents
     */
    public String getAddress() {
        return (address);
    }

    /**
     * Get the hostname
     * 
     * @return The hostname the ResourceCapability represents
     */
    public String getHostName() {
        return (hostName);
    }

    /**
     * Get the computed resource utilization
     *
     * @return The computed utilization
     */
    public double getUtilization() {
        return resourceUtilization.getUtilization();
    }

    /**
     * Get whether the ComputeResource supports persistent provisioning
     * 
     * @return Return true if the ComputeResource supports persistent
     * provisioning, otherwise return false
     */
    public boolean supportsPersistentProvisioning() {
        return (persistentProvisioning);
    }
    
    /**
     * Get an array of PlatformCapability objects for the compute resource
     * 
     * @return PlatformCapability[] Array of PlatformCapability objects.
     */
    public PlatformCapability[] getPlatformCapabilities() {
        if(platformCapabilities == null)
            return (new PlatformCapability[0]);
        PlatformCapability[] pCaps =  new PlatformCapability[platformCapabilities.length];
        System.arraycopy(platformCapabilities, 0, pCaps, 0, platformCapabilities.length);
        return (pCaps);
    }   

    /**
     * Get an array of MeasuredResource objects for the compute resource. Each
     * MeasuredResource represents the current state of attributes of a
     * MeasurableCapability
     * 
     * @return MeasuredResource[] Array of MeasuredResource objects.
     */
    public MeasuredResource[] getMeasuredResources() {        
        return (getMeasuredResources(ALL_MEASURED_RESOURCES));
    }
    
    /**
     * Get an array of MeasuredResource objects for the compute resource. Each
     * MeasuredResource represents the current state of attributes of a
     * MeasurableCapability
     *
     * @param type Either ALL_MEASURED_RESOURCES or MEASURED_RESOURCES_BREACHED
     * @return MeasuredResource[] Array of MeasuredResource objects.
     */
    public MeasuredResource[] getMeasuredResources(int type) {
        if(type < ALL_MEASURED_RESOURCES || type > MEASURED_RESOURCES_BREACHED)
            throw new IllegalArgumentException("unknown type : "+type);
        Collection<MeasuredResource> measuredResources = resourceUtilization.getMeasuredResources();
        MeasuredResource[] mRes;
        if(type == ALL_MEASURED_RESOURCES) {
            mRes = measuredResources.toArray(new MeasuredResource[measuredResources.size()]);
        } else {
            ArrayList<MeasuredResource> list = new ArrayList<MeasuredResource>();
            for (MeasuredResource measuredResource : measuredResources) {
                if (measuredResource.thresholdCrossed())
                    list.add(measuredResource);
            }
            mRes = list.toArray(new MeasuredResource[list.size()]);
        }
        return (mRes);
    }

    /**
     * Determine if the ResourceCapability contains MeasuredResource instances
     * that have values which fall outside of their threshold declaration
     * 
     * @return True if all MeasuredResource instances are within their known 
     * threshold range. If any of the MeasuredResource instances have a value which
     * falls outside of their threshold declaration, return false;
     */
    public boolean measuredResourcesWithinRange() {
        return resourceUtilization.measuredResourcesWithinRange();        
    }    

    /**
     * Compares this ResourceCapability object with another ResourceCapability
     * object for order using the computed utilization of the ResourceCapability
     * 
     * @param o Object to compare to
     */
    public int compareTo(Object o) {
        // will throw a ClassCastException if the obj is not the right type
        ResourceCapability that = (ResourceCapability)o;
        // return -1 if I am less than the object being compared
        if(this.getUtilization() < that.getUtilization())
            return (-1);
        // return +1 if I am greater than the object being compared
        if(this.getUtilization() > that.getUtilization())
            return (1);
        // objects are equal
        return (0);
    }

    /**
     * Provide a String output
     */
    public String toString() {
        return "ResourceCapability: platformCapabilities=" +
               (platformCapabilities == null ? null : Arrays.asList(platformCapabilities)) +
               ", resourceUtilization=" +
               resourceUtilization +", address='" + address +
               ", hostName='" + hostName + '\'' +
               ", persistentProvisioning=" +
               persistentProvisioning +
               '}';
    }
}
