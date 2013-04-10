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

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.costmodel.ResourceCost;
import org.rioproject.deploy.DownloadRecord;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.MeasurableCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to calculate resource costs for a service.
 *
 * @author Dennis Reedy
 */
public class ServiceCostCalculator {
    static Logger logger = LoggerFactory.getLogger(ServiceCostCalculator.class);
    private ComputeResource computeResource;
    private DownloadRecord[] downloadRecords;
    private ServiceBeanContext context;

    /**
     * Set the ComputeResource
     *
     * @param computeResource The ComputeResource object, used to access
     * resource cost producers
     */
    public void setComputeResource(ComputeResource computeResource) {
        this.computeResource = computeResource;
    }
    
    /**
     * Set the ServiceBeanContext
     *
     * @param context The ServiceBeanContext. This is used to assist
     * in creating ResourceCost objects for any matched platform cCapability
     * declarations.
     */
    public void setServiceBeanContext(ServiceBeanContext context) {
        this.context = context;
    }

    /**
     * Set DownloadRecord instances
     * 
     * @param downloadRecords An array of DownloadRecord instances,
     * documenting that software bundles have been installed onto the compute
     * resource. Each DownloadRecord will be used to compute a
     * ResourceCost for disk space use.
     */
    public void setDownloadRecords(DownloadRecord[] downloadRecords) {
        this.downloadRecords = downloadRecords;
    }

    /**
     * Calculate ResourceCost instances for matched platform
     * capabilities and software downloads
     *
     * @param duration The time between cost calculations
     *
     * @return An array of ResourceCost objects.
     * <ul>
     * <li>For each matched {@link org.rioproject.system.capability.PlatformCapability}
     * class a ResourceCost is added.
     * <li>If there are DownloadRecord instances, a ResourceCost for
     * disk space use is computed using the size of the downloadS() after
     * extraction.
     * </ul>
     */
    public ResourceCost[] calculateCosts(long duration) {
        List<ResourceCost> costList = new ArrayList<ResourceCost>();
        if(downloadRecords!=null) {
            for (DownloadRecord downloadRecord : downloadRecords) {
                int size = downloadRecord.getDownloadedSize();
                if (downloadRecord.unarchived())
                    size += downloadRecord.getExtractedSize();
                MeasurableCapability mCap = computeResource.getMeasurableCapability("DiskSpace");
                if (mCap != null)
                    costList.add(mCap.calculateResourceCost(new Integer(size).doubleValue(), duration));
                else {
                    if (logger.isDebugEnabled()) {
                        logger.warn("DiskSpace capability not found, cannot create ResourceCost");
                    }
                }
            }
        }

        if(context!=null) {
            PlatformCapability[] pCaps =
                context.getComputeResourceManager().getMatchedPlatformCapabilities();
            for (PlatformCapability pCap : pCaps)
                costList.add(
                    pCap.calculateResourceCost((double) duration, duration));
        }
        return(costList.toArray(new ResourceCost[costList.size()]));
    }

}
