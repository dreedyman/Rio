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

import org.rioproject.costmodel.ResourceCost;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.deploy.DownloadRecord;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.Statistics;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to calculate resource costs for a service.
 *
 * @author Dennis Reedy
 */
public class ServiceCostCalculator {
    private static String COMPONENT = "org.rioproject.cybernode.instrument";
    static Logger logger = Logger.getLogger(COMPONENT);
    ComputeResource computeResource;
    DownloadRecord[] downloadRecords;
    ServiceBeanContext context;
    double lastCPUUtilization;
    double lastMemoryUtilization;

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
     * Calculate ResourceCost instances for cpu, memory, matched platform
     * capabilities and software downloads
     *
     * @param duration The time between cost calculations
     *
     * @return An array of ResourceCost objects containing at least a
     * ResourceCost for cpu and memory.
     * <ul>
     * <li>Both cpu and memory resource costs are computed as the mean of the
     * calculated utilizations each respective
     * {@link org.rioproject.system.measurable.MeasurableCapability} has recorded
     * over the time period provided.
     * <li>For each matched {@link org.rioproject.system.capability.PlatformCapability}
     * class a ResourceCost is added.
     * <li>If there are DownloadRecord instances, a ResourceCost for
     * disk space use is computed using the size of the downloadS() after
     * extraction.
     * </ul>
     */
    public ResourceCost[] calculateCosts(long duration) {
        List<ResourceCost> costList = new ArrayList<ResourceCost>();
        long now = System.currentTimeMillis();
        long from = now-duration;
        double utilization;
        
        /* CPU utilization is the mean of the last calculated utilization as well
         * as the mean of values returned from the last calculation */
        MeasurableCapability mCap = computeResource.getMeasurableCapability(
            SystemWatchID.PROC_CPU);
        if(mCap!=null) {
            utilization = getMean(mCap, from, now, lastCPUUtilization);
            double cpuUtilization = (lastCPUUtilization==0?utilization:
                                     (lastCPUUtilization+utilization)/2);
            lastCPUUtilization = utilization;
            costList.add(mCap.calculateResourceCost(cpuUtilization, duration));
        }

        /* Memory utilization is calculated the same as CPU (above); the mean
         * of the last calculated utilization as well as the mean of values
         * returned from the last calculation */
        mCap = computeResource.getMeasurableCapability("Memory");
        if(mCap!=null) {
            utilization = getMean(mCap, from, now, lastMemoryUtilization);
            double memUnits = (lastMemoryUtilization==0?utilization:
                               (lastMemoryUtilization+utilization)/2);
            lastMemoryUtilization = utilization;
            costList.add(mCap.calculateResourceCost(memUnits, duration));

        }
        if(downloadRecords!=null) {
            for (DownloadRecord downloadRecord : downloadRecords) {
                int size = downloadRecord.getDownloadedSize();
                if (downloadRecord.unarchived())
                    size += downloadRecord.getExtractedSize();
                mCap = computeResource.getMeasurableCapability("DiskSpace");
                if (mCap != null)
                    costList.add(mCap.calculateResourceCost(
                        new Integer(size).doubleValue(), duration));
                else {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE,
                                   "DiskSpace capability not found, " +
                                   "cannot create ResourceCost");
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

    private double getMean(MeasurableCapability mCap,
                           long from,
                           long to,
                           double last) {
        if(mCap==null)
            return 0;
        double mean = 0;
        try {
            Calculable[] calcs = mCap.getWatchDataSource().getCalculable(from,
                                                                         to);
            Statistics stats = new Statistics();
            stats.setValues(calcs);
            if (last != 0)
                stats.addValue(last);
            mean = stats.mean();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return (Double.isNaN(mean)?0:mean);
    }

}
