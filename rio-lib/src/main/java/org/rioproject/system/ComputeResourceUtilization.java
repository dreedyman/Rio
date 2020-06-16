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

import org.rioproject.system.measurable.cpu.CpuUtilization;
import org.rioproject.system.measurable.cpu.ProcessCpuUtilization;
import org.rioproject.system.measurable.disk.DiskSpaceUtilization;
import org.rioproject.system.measurable.memory.ProcessMemoryUtilization;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The ComputeResourceUtilization provides a mechanism to represent the
 * utilization of a ComputeResource. ComputeResource quantitative behavior is represented by
 * {@link MeasuredResource} objects. Each MeasuredResource contains a relative value which represents
 * the percentage of the resource being measured. This value is the relative utilization of the resource's
 * usage. The summation of a compute resource's utilization is represented by the utilization property.
 *
 * @author Dennis Reedy
 */
public class ComputeResourceUtilization implements Comparable, Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /**
     * Description of the ComputeResource
     */
    private String description;
    /**
     * The host name of the compute resource
     */
    private String hostName;
    /**
     * The IP Address of the compute resource
     */
    private String address;
    /**
     * The utilization of the compute resource, which is a summation of
     * {@link MeasuredResource} components, representing a
     * snapshot of the depletion-oriented resources of the compute resource
     */
    private Double utilization = Double.NaN;
    /**
     * Collection of measurable capability utilization values
     */
    private final List<MeasuredResource> mRes = new ArrayList<MeasuredResource>();

    /**
     * Construct a ComputeResourceUtilization
     * 
     * @param description A description for the resource
     * @param hostName The host name of the ComputeResource
     * @param address IP address of the ComputeResource
     * @param mRes Collection of MeasuredResources, corresponding to whats being
     * measured on the compute resource
     */
    public ComputeResourceUtilization(String description, 
                                      String hostName,
                                      String address,
                                      Collection<MeasuredResource> mRes) {
        if(description == null)
            throw new IllegalArgumentException("description is null");
        if(hostName == null)
            throw new IllegalArgumentException("hostName is null");
        if(address == null)
            throw new IllegalArgumentException("address is null");
        if(mRes == null)
            throw new IllegalArgumentException("measured resources is null");
        this.description = description;
        this.hostName = hostName;
        this.address = address;
        this.mRes.addAll(mRes);
    }

    /**
     * Construct a ComputeResourceUtilization
     *
     * @param description A description for the resource
     * @param hostName The host name of the ComputeResource
     * @param address IP address of the ComputeResource
     * @param utilization Composite utilization of the ComputeResource
     * @param mRes Collection of MeasuredResources, corresponding to whats being
     * measured on the compute resource
     */
    public ComputeResourceUtilization(String description,
                                      String hostName,
                                      String address,
                                      Double utilization,
                                      Collection<MeasuredResource> mRes) {
        this(description, hostName, address, mRes);
        this.utilization = utilization;
    }

    /**
     * Get the description
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the hostname
     *
     * @return The hostname
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Set the hostname
     *
     * @param hostName The hostname
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Get the address the compute resource is bound to
     *
     * @return The host address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Get the utilization value, a summation of the measured resources
     *
     * @return The utilization value 
     */
    public Double getUtilization() {
        if(Double.isNaN(utilization)) {
            for (MeasuredResource m : mRes) {
                utilization = utilization + m.getValue();
            }
            utilization = utilization/mRes.size();
        }
        return utilization;
    }

    /**
     * The Map of measurable capability utilization values
     *
     * @return The Map of measurable capability utilization values
     */
    public Collection<MeasuredResource> getMeasuredResources() {
        return mRes;
    }

    /**
     * Get the measured CPU utilization
     *
     * @return The latest
     * {@link org.rioproject.system.measurable.cpu.CpuUtilization} taken
     * from the compute resource. If not available, return null.
     */
    public CpuUtilization getCpuUtilization() {
        CpuUtilization cpu = null;
        for (MeasuredResource m : mRes) {
            if(m instanceof CpuUtilization) {
                cpu = (CpuUtilization)m;
                break;
            }
        }
        return cpu;
    }

    /**
     * Get the measured system memory utilization
     *
     * @return The latest
     * {@link org.rioproject.system.measurable.memory.SystemMemoryUtilization}
     * taken from the compute resource. If not available, return null.
     */
    public SystemMemoryUtilization getSystemMemoryUtilization() {
        SystemMemoryUtilization mem = null;
        for (MeasuredResource m : mRes) {
            if(m instanceof SystemMemoryUtilization) {
                mem = (SystemMemoryUtilization)m;
                break;
            }
        }
        return mem;
    }

    /**
     * Get the measured process memory utilization
     *
     * @return The latest
     * {@link org.rioproject.system.measurable.memory.ProcessMemoryUtilization}
     * taken from the compute resource. If not available, return null.
     */
    public ProcessMemoryUtilization getProcessMemoryUtilization() {
        ProcessMemoryUtilization mem = null;
        for (MeasuredResource m : mRes) {
            if(m instanceof ProcessMemoryUtilization) {
                mem = (ProcessMemoryUtilization)m;
                break;
            }
        }
        return mem;
    }

    /**
     * Get the measured process cpu utilization
     *
     * @return The latest
     * {@link org.rioproject.system.measurable.cpu.ProcessCpuUtilization}
     * taken from the compute resource. If not available, return null.
     */
    public ProcessCpuUtilization getProcessCpuUtilization() {
        ProcessCpuUtilization pCpu = null;
        for (MeasuredResource m : mRes) {
            if(m instanceof ProcessCpuUtilization) {
                pCpu = (ProcessCpuUtilization)m;
                break;
            }
        }
        return pCpu;
    }

    /**
     * Get the measured disk space utilization
     *
     * @return The latest
     * {@link org.rioproject.system.measurable.disk.DiskSpaceUtilization} taken
     * from the compute resource. If not available, return null.
     */
    public DiskSpaceUtilization getDiskSpaceUtilization() {
        DiskSpaceUtilization disk = null;
        for (MeasuredResource m : mRes) {
            if(m instanceof DiskSpaceUtilization) {
                disk = (DiskSpaceUtilization)m;
                break;
            }
        }
        return disk;
    }

    /**
     * Determine if the ComputeResourceUtilization contains MeasuredResource instances
     * that have values which fall outside of their threshold declaration
     *
     * @return True if all MeasuredResource instances are within their known
     * threshold range. If any of the MeasuredResource instances have a value which
     * falls outside of their threshold declaration, return false;
     */
    public boolean measuredResourcesWithinRange() {
        for (MeasuredResource measuredResource : mRes) {
            if (measuredResource.thresholdCrossed())
                return (false);
        }
        return(true);
    }

    /**
     * Compares this ComputeResourceUtilization object with another
     * ComputeResourceUtilization object for order using the computed
     * utilization of the ComputeResourceUtilization
     * 
     * @param o Object to compare to
     */
    public int compareTo(Object o) {
        // will throw a ClassCastException if the obj is not the right type
        ComputeResourceUtilization that = (ComputeResourceUtilization)o;
        // return -1 if I am less than the object being compared
        return (this.utilization.compareTo(that.utilization));
    }


    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ComputeResourceUtilization: description: ");
        builder.append(description).append(", hostName: ").append(hostName).append(", ");
        builder.append("address: ").append(address).append(", utilization: ").append(utilization).append(", ");
        builder.append("measured resources: ").append(mRes);
        return  builder.toString();
    }
}
