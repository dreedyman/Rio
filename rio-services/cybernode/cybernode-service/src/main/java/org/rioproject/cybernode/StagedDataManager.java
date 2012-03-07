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

import org.rioproject.opstring.ServiceElement;
import org.rioproject.core.provision.SystemRequirements;
import org.rioproject.core.provision.DownloadRecord;
import org.rioproject.core.provision.StagedSoftware;
import org.rioproject.core.provision.StagedData;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.OperatingSystemType;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.jsb.JSBContext;
import org.rioproject.resources.util.DownloadManager;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.exec.Util;

import java.util.*;
import java.util.logging.Logger;
import java.io.File;

/**
 * A utility to assist in downloading and installing data for a service.
 *
 * @author Dennis Reedy
 */
public class StagedDataManager {
    /** Collection of PlatformCapability instances that have been installed
     * by the JSBDelegate */
    private final Collection<PlatformCapability> installedPlatformCapabilities = new ArrayList<PlatformCapability>();
    private final List<DownloadRecord> dlRecords = new ArrayList<DownloadRecord>();
    private final Map<StagedData, DownloadRecord[]> downloadedArtifacts = new HashMap<StagedData, DownloadRecord[]>();
    private ServiceElement sElem;
    private ComputeResource computeResource;
    /** Logger */
    static Logger logger = Logger.getLogger("org.rioproject.cybernode");

    /**
     * Create a StagedDataManager

     * @param computeResource The compute resource
     */
    public StagedDataManager(ComputeResource computeResource) {
        if(computeResource==null)
            throw new IllegalArgumentException("ComputeResource is null");
        this.computeResource = computeResource;
    }

    /**
     * @param sElem The service element information
     */
    public void setServiceElement(ServiceElement sElem) {
        if(sElem==null)
            throw new IllegalArgumentException("ServiceElement is null");
        this.sElem = sElem;
    }

    /**
     * Remove any installed PlatformCapability components
     *
     * @return An array of PlatformCapability instances that were removed. If
     * none were removed, return a zero-length array
     */
    public PlatformCapability[] removeInstalledPlatformCapabilities() {
        return removeInstalledPlatformCapabilities(true);
    }

    /**
     * Remove  installed PlatformCapability components
     *
     * @param force Whether to remove the PlatformCapability, regardless of
     * whether the remove on destroy attribute of the StatedData element is true
     *
     * @return An array of PlatformCapability instances that were removed. If
     * none were removed, return a zero-length array
     */
    public PlatformCapability[] removeInstalledPlatformCapabilities(boolean force) {
        List<PlatformCapability> removed = new ArrayList<PlatformCapability>();
        for (PlatformCapability pCap : installedPlatformCapabilities) {
            StagedSoftware[] software = pCap.getStagedSoftware();
            for(StagedSoftware sw : software) {
                if(force && pCap.getUsageCount()==0) {
                    if(computeResource.removePlatformCapability(pCap, true))
                        removed.add(pCap);
                } else {
                    if (sw.removeOnDestroy() && pCap.getUsageCount()==0) {
                        if(computeResource.removePlatformCapability(pCap, true))
                            removed.add(pCap);
                    }
                }
            }
        }

        return removed.toArray(new PlatformCapability[removed.size()]);
    }

    /**
     * Remove any installed StagedData
     */
    public void removeStagedData() {
        for(Map.Entry<StagedData, DownloadRecord[]> entry : downloadedArtifacts.entrySet()) {
            StagedData data = entry.getKey();
            if(data.removeOnDestroy()) {
                DownloadRecord[] records = entry.getValue();
                for(DownloadRecord record : records) {
                    DownloadManager.remove(record);
                }
            }
        }
    }

    /**
     * Get any {@link org.rioproject.system.capability.PlatformCapability}
     * instances that were installed
     *
     * @return A collection of
     * {@link org.rioproject.system.capability.PlatformCapability} instances
     * that were installed. If there were none installed, return a zero-length
     * collection
     */
    public Collection<PlatformCapability> getInstalledPlatformCapabilities() {
        return installedPlatformCapabilities;
    }

    /**
     * Get all DownloadRecords
     *
     * @return An array of {@link org.rioproject.core.provision.DownloadRecord}
     * instances that represent the download of a
     * {@link org.rioproject.core.provision.StagedData} instance. If nothing
     * was downloaded return a zero-length array
     */
    public DownloadRecord[] getDownloadRecords() {
        return dlRecords.toArray(new DownloadRecord[dlRecords.size()]);
    }

    /**
     * Get the map of {@link org.rioproject.core.provision.StagedData} keys and
     * {@link org.rioproject.core.provision.DownloadRecord} values for each
     * downloaded item.
     *
     * @return A map of {@link org.rioproject.core.provision.StagedData} keys and
     * {@link org.rioproject.core.provision.DownloadRecord} values for each
     * downloaded item. If nothing was downloaded, return an empty map
     */
    public Map<StagedData, DownloadRecord[]> getDownloadedArtifacts() {
        return downloadedArtifacts;
    }

    /**
     * Download any artifacts the service requires
     *
     * @return A map of {@link org.rioproject.core.provision.StagedData} keys and
     * {@link org.rioproject.core.provision.DownloadRecord} values for each
     * downloaded item. If nothing was downloaded, return an empty map
     *
     * @throws Exception If errors occur downloading or creating
     * {@link org.rioproject.system.capability.PlatformCapability} instances
     */
    public Map<StagedData, DownloadRecord[]> download() throws Exception {
        if(sElem==null)
            throw new IllegalStateException("ServiceElement has not been set");
        /* If there are provisionable capabilities, or data staging, perform
         * the stagedData/installation */
        Collection<SystemRequirements.SystemComponent> installableComponents =
            sElem.getProvisionablePlatformCapabilities();
        install(installableComponents);

        /* Verify missing components. If there are any, go get them */
        Collection<SystemRequirements.SystemComponent> missing = verifyPlatformCapabilities();
        if(missing.size()>0) {
            install(missing);
            logger.info("Missing requirements have been provisioned");
        }

        StagedData[] stagedData = sElem.getStagedData();
        for (StagedData data : stagedData) {
            DownloadRecord dlRec;
            if (data.getInstallRoot().startsWith(File.separator)) {
                DownloadManager dlManager = new DownloadManager(data);
                dlRec = dlManager.download();
                dlRecords.add(dlRec);
            } else {
                String provisionRoot = computeResource.getPersistentProvisioningRoot();
                DownloadManager dlManager = new DownloadManager(provisionRoot, data);
                dlRec = dlManager.download();
                dlRecords.add(dlRec);
            }
            if (data.getPerms() != null) {
                if (OperatingSystemType.isWindows()) {
                    logger.warning("Cannot apply permissions [" + data.getPerms() + "] to StagedData on Windows");
                } else {
                    File toChmod;
                    StringBuilder perms = new StringBuilder();
                    if (dlRec.unarchived()) {
                        toChmod = new File(dlRec.getPath());
                        perms.append("-R ");
                    } else {
                        toChmod = new File(FileUtils.makeFileName(dlRec.getPath(), dlRec.getName()));
                    }
                    perms.append(data.getPerms());
                    logger.info("Applying permissions [" + perms.toString() + "] to " +
                                "data staged at [" + FileUtils.getFilePath(toChmod) + "]");
                    Util.chmod(toChmod, perms.toString());
                }
            }
            downloadedArtifacts.put(data, new DownloadRecord[]{dlRec});
        }
        return downloadedArtifacts;
    }

    /**
     * Verify that required platform capabilities are present
     *
     * @return A collection of SystemRequirements.SystemComponent components
     * that are not found. If there are no missing components, return an empty
     * collection
     */
    public Collection<SystemRequirements.SystemComponent> verifyPlatformCapabilities() {
        if(sElem==null)
            throw new IllegalStateException("ServiceElement has not been set");
        PlatformCapability[] platformCapabilities = computeResource.getPlatformCapabilities();
        SystemRequirements.SystemComponent[] jsbRequirements =
            sElem.getServiceLevelAgreements().getSystemRequirements().getSystemComponents();
        ArrayList<SystemRequirements.SystemComponent> missing = new ArrayList<SystemRequirements.SystemComponent>();

        /*
         * If there are no PlatformCapability requirements we can return
         * successfully
         */
        if(jsbRequirements.length == 0)
            return missing;
        /*
         * Check each of our PlatformCapability objects for supportability
         */
        for (SystemRequirements.SystemComponent jsbRequirement : jsbRequirements) {
            boolean supported = false;
            /*
             * Iterate through all resource PlatformCapability objects and see
             * if any of them supports the current PlatformCapability. If none
             * are found, then we dont have a match
             */
            for (PlatformCapability pCap : platformCapabilities) {
                if (pCap.supports(jsbRequirement)) {
                    supported = true;
                    /* Make sure that if the platform capability is
                     * provisionable that it is not being removed */
                    if(pCap.getType()==PlatformCapability.PROVISIONABLE &&
                        computeResource.removalInProcess(pCap)) {
                        logger.warning("Service ["+sElem.getName()+"] has a " +
                                       "requirement ["+jsbRequirement+"] that " +
                                       "is being removed as part of a previous " +
                                       "instantiation. An attempt will be made " +
                                       "to install the software");
                        missing.add(jsbRequirement);
                        logger.info("Wait for the removal to finish...");
                        do {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        } while(computeResource.removalInProcess(pCap));
                    }
                    break;
                }
            }
            if (!supported) {
                StagedSoftware[] data = jsbRequirement.getStagedSoftware();
                if(data.length>0) {
                    missing.add(jsbRequirement);
                    logger.info("Service ["+sElem.getName()+"] has a " +
                                "missing requirement ["+jsbRequirement+"]. " +
                                "An attempt will be made to install it.");
                } else {
                    logger.severe("Service ["+sElem.getName()+"] has a " +
                                  "requirement for ["+jsbRequirement+"], " +
                                  "and it is not found on the Cybernode, not " +
                                  "is it downloadable. The capability may have " +
                                  "been administratively removed during the " +
                                  "instantiation of the service");
                }
            }
        }

        return missing;
    }

    /*
     * Install Platform capabilities
     */
    private void install(Collection<SystemRequirements.SystemComponent> toInstall)
        throws IllegalAccessException, InstantiationException,ClassNotFoundException {
        for (SystemRequirements.SystemComponent sysComp : toInstall) {
            String className = sysComp.getClassName();
            if (className == null) {
                Map<String, String> pCapMap =
                    computeResource.getPlatformCapabilityNameTable();
                className = pCapMap.get(sysComp.getName());
            }
            PlatformCapability pCap = JSBContext.createPlatformCapability(className,
                                                                          sysComp.getClasspath(),
                                                                          sysComp.getAttributes());
            installedPlatformCapabilities.add(pCap);
            StagedSoftware[] staged = sysComp.getStagedSoftware();
            for (StagedSoftware sw : staged) {
                DownloadRecord[] dlRecs = computeResource.provision(pCap, sw);
                //downloadedArtifacts.put(sw, dlRecs);
                Collections.addAll(dlRecords, dlRecs);
            }
        }
    }
}
