/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import org.rioproject.boot.BootUtil;
import org.rioproject.core.provision.DownloadRecord;
import org.rioproject.core.provision.StagedSoftware;
import org.rioproject.resources.util.DownloadManager;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.PlatformCapabilityWriter;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.system.measurable.disk.DiskSpace;
import org.rioproject.system.measurable.memory.Memory;
import org.rioproject.system.measurable.memory.SystemMemory;
import org.rioproject.watch.ThresholdListener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>ComputeResource</code> represents an abstract notion of a compute 
 * resource that offers computational resources which can be measured using Quality 
 * of Service mechanisms which correlate to qualitative and quantitative capabilities 
 * such as CPU, Memory, and others
 *
 * @author Dennis Reedy
 */
public class ComputeResource extends Observable {
    /** Name to use when getting Configuration values and to get the Looger */
    static final String COMPONENT = "org.rioproject.system";
    static Logger logger = Logger.getLogger(COMPONENT);
    /** 
     * A description of the <code>ComputeResource</code> 
     */
    private String description;
    /**
     * The <code>InetAddress</code> of the compute resource the 
     * <code>ComputeResource</code> object was constructed on
     */
    private InetAddress address;
    /** 
     * The <code>platformCapabilities</code> defines a <code>Collection</code> of 
     * <code>PlatformCapabilities</code> which are specific types of mechanism(s) 
     * associated with this <code>ComputeResource</code>, and is used to define 
     * base platform capabilities and resources. 
     */
    private final List<PlatformCapability> platformCapabilities =
        new ArrayList<PlatformCapability>();
    /**
     * A collection of <tt>PlatformCapability</tt> instances being
     * provisioned/installed
     */
    private final List<PlatformCapability> platformCapabilityPending =
        new ArrayList<PlatformCapability>();
    /** 
     * The <code>measurables</code> defines a <code>Collection</code> of 
     * <code>MeasurableCapabilities</code> which are specific types of measurable 
     * mechanism(s) associated with this <code>ComputeResource</code>
     */
    private final Vector<MeasurableCapability> measurables = new Vector<MeasurableCapability>();
    /**
     * The CapabilityChannel is an inner class which becomes an Observer to 
     * MeasurableCapability components and ends up notifying the ComputeResource to 
     * update registered Observer instances if any MeasurableCapability components 
     * have changed their state within the interval prescribed for the broadcaster
     */
    private CapabilityChannel capabilityChannel = new CapabilityChannel();    
    /**
     * Flag determines whether persistent provisioning is supported
     */
    private boolean persistentProvisioning=true;
    /** 
     * The directory to provision PlatformCapability software. This is the root 
     * directory where PlatformCapability software may be installed
     */
    private String provisionRoot;
    /**
     * Configuration object
     */
    private Configuration config;
    /**
     * Flag indicated we are initializing 
     */
    private boolean initializing = false;
    /**
     * PlatformCapability name table
     */
    private Map<String, String> platformCapabilityNameTable;
    private final List<PlatformCapability> removals = new ArrayList<PlatformCapability>();
    private SystemCapabilitiesLoader systemCapabilitiesLoader;

    /**
     * Create a ComputeResource with a default (empty) configuration
     *
     * @throws ConfigurationException If there are problems accessing the
     * ComputeResource's configuration object
     * @throws UnknownHostException If the local host cannot be obtained
     */
    public ComputeResource() throws ConfigurationException,
                                    UnknownHostException {
        this(EmptyConfiguration.INSTANCE);
    }

    /**
     * Create a ComputeResource
     * 
     * @param config The Configuration used to initialize the ComputeResource
     *
     * @throws ConfigurationException If there are problems accessing the
     * ComputeResource's configuration object
     * @throws UnknownHostException If the local host cannot be obtained
     * @throws IllegalArgumentException If the config is null
     */
    public ComputeResource(Configuration config)
        throws ConfigurationException, UnknownHostException {
        if(config==null)
            throw new IllegalArgumentException("config is null");

        String system = System.getProperty("os.name")+", "+
                        System.getProperty("os.arch")+", "+
                        System.getProperty("os.version");
        this.config = config;
        address = (InetAddress)config.getEntry(COMPONENT,
                                               "address",
                                               InetAddress.class,
                                               InetAddress.getLocalHost());
        String defaultDescription = address.getHostName()+" "+system;
        description = (String)config.getEntry(COMPONENT,
                                              "description",
                                              String.class,
                                              defaultDescription);
        long reportInterval =
        Config.getLongEntry(config,
                            COMPONENT,
                            "reportInterval",
                            CapabilityChannel.DEFAULT_REPORT_INTERVAL,
                            1000,
                            Long.MAX_VALUE);
        capabilityChannel.setReportInterval(reportInterval);
    }
    
    /**
     * Set the reportInterval property which controls how often the ComputeResource 
     * will inform registered Observers of a state change. A state change is 
     * determined if any of the MeasurableCapability components contained within 
     * this ComputeResource provide an update in the interval specified by the 
     * reportIntervalProperty
     *
     * @param reportInterval The interval controlling when the ComputeResource 
     * reports change of state to registered Observers
     */
    public void setReportInterval(long reportInterval) {
        capabilityChannel.setReportInterval(reportInterval);
    }

    /**
     * Get the reportInterval property which controls how often the ComputeResource 
     * will inform registered Observers of a state change. A state change is 
     * determined if any of the MeasurableCapability components contained within 
     * this ComputeResource provide an update in the interval specified by the 
     * reportInterval property
     *
     * @return The interval controlling when the ComputeResource reports change 
     * of state to registered Observers
     */
    public long getReportInterval() {
        return(capabilityChannel.getReportInterval());
    }

    /**
     * Get the description
     *
     * @return The description
     */
    public String getDescription() {
        return(description);
    }    
    
    /**
     * Get the InetAddress ComputeResource object has been configured to represent
     * 
     * @return The InetAddress ComputeResource object has been configured to 
     * represent
     */
    public InetAddress getAddress() {
        return(address);
    }

    /**
     * Add a <code>PlatformCapability</code> object. The addition of a 
     * <code>PlatformCapability</code> component causes the state of this object 
     * to change, triggering the notification of all registered 
     * <code>Observer</code> instances.
     *
     * <p>If there is a <code>PlatformCapability</code> component that is equal to 
     * the provided <code>PlatformCapability</code> (according to the 
     * <code>PlatformCapability.equals()</code> contract), the existing 
     * <code>PlatformCapability</code> object will be remain unchanged and
     * the provided <code>PlatformCapability</code> ignored
     *
     * @param pCap The PlatformCapability to add
     */
    public void addPlatformCapability(PlatformCapability pCap) {
        if(!hasPlatformCapability(pCap)) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Have PlatformCapability : "+
                              pCap.getClass().getName()+" "+
                              "load any system resources");
            pCap.loadResources();

            boolean stateChange;
            synchronized(platformCapabilities) {
                stateChange = platformCapabilities.add(pCap);
            }
            if(stateChange)
                stateChange();
        }
    }
    
    /**
     * Provision a {@link org.rioproject.core.provision.StagedSoftware} for a
     * {@link org.rioproject.system.capability.PlatformCapability} object. The
     * provisioning of {@link org.rioproject.core.provision.StagedSoftware}
     * object for a{@link org.rioproject.system.capability.PlatformCapability}
     * component causes the state of this object to change, triggering the
     * notification of all registered {@link java.util.Observer} instances.
     *
     * <p>If there is a <tt>PlatformCapability</tt> component that is equal to
     * the provided <tt>PlatformCapability</tt> (according to the
     * <tt>PlatformCapability.equals()</tt> contract), the existing
     * <tt>PlatformCapability</tt> object will be changed. If the
     * <tt>PlatformCapability</tt> object does not exist, it will be added
     *
     * @param pCap The PlatformCapability
     * @param stagedSoftware StagedSoftware to provision for the
     * PlatformCapability
     *
     * @return Array of DownloadRecord instances which
     * represent any software downloaded to add the PlatformCapability. If no 
     * software was downloaded to add the PlatformCapability, return an empty array
     */
    public DownloadRecord[] provision(PlatformCapability pCap,
                                      StagedSoftware stagedSoftware) {
        boolean pendingInstallation;
        synchronized(platformCapabilityPending) {
            pendingInstallation = platformCapabilityPending.contains(pCap);
        }
        if(!pendingInstallation) {
            synchronized(platformCapabilityPending) {
                platformCapabilityPending.add(pCap);
            }
        } else {
            do {
                synchronized(platformCapabilityPending) {
                    pendingInstallation = platformCapabilityPending.contains(pCap);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            } while(pendingInstallation);
        }

        if(hasPlatformCapability(pCap)) {
            return new DownloadRecord[0];
        }

        synchronized(platformCapabilities) {
            platformCapabilities.add(pCap);
        }

        try {
            if(stagedSoftware !=null) {
                DownloadManager slm = new DownloadManager(provisionRoot,
                                                          stagedSoftware);
                DownloadRecord record = null;
                try {
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("Provisioning StagedSoftware for "+
                                      "PlatformCapability : "+
                                      pCap.getClass().getName());
                    record = slm.download();
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest(record.toString());
                    pCap.addStagedSoftware(stagedSoftware);
                    pCap.addDownloadRecord(record);
                    DownloadRecord postInstallRecord = slm.postInstall();
                    if(postInstallRecord!=null)
                        pCap.addDownloadRecord(postInstallRecord);

                    pCap.setPath(record.unarchived()?
                                 record.getExtractedPath():record.getPath());
                    if(stagedSoftware.getUseAsClasspathResource()) {
                        String[] classpath;
                        if(pCap.getPath().endsWith(".jar") ||
                           pCap.getPath().endsWith(".zip")) {
                            classpath = BootUtil.toArray(pCap.getPath());
                        } else {
                            String cp = pCap.getPath();
                            File f = new File(cp);
                            if(!f.isDirectory())
                                cp = FileUtils.getFilePath(f.getParentFile());
                            classpath = new String[]{cp};
                        }
                        pCap.setClassPath(classpath);
                    }
                    if(!stagedSoftware.removeOnDestroy()) {
                        String configFileLocation =
                            systemCapabilitiesLoader.getPlatformConfigurationDirectory(config);
                        if(configFileLocation==null) {
                            logger.warning("Unable to write PlatformConfiguration " +
                                           "["+pCap.getName()+"] configuration, " +
                                           "unknown platform configuration " +
                                           "directory. The RIO_HOME environment " +
                                           "variable must be set");
                        } else {
                            PlatformCapabilityWriter pCapWriter = new PlatformCapabilityWriter();
                            String fileName =
                                pCapWriter.write(pCap, configFileLocation);
                            pCap.setConfigurationFile(fileName);
                            logger.info("Wrote PlatformCapability ["+pCap.getName()+"] " +
                                        "configuration to "+fileName);
                        }
                    }

                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("Have PlatformCapability : "+
                                      pCap.getClass().getName()+" "+
                                      "load any system resources");
                    pCap.loadResources();
                    stateChange();

                } catch(IOException e) {
                    if(record!=null)
                        slm.remove();
                    logger.log(Level.WARNING,
                               "Provisioning StagedSoftware for "+
                               "PlatformCapability : "+
                               pCap.getClass().getName(),
                               e);
                }
            }
        } finally {
            synchronized(platformCapabilityPending) {
                platformCapabilityPending.remove(pCap);
            }
        }
        return(pCap.getDownloadRecords());
    }

    /**
     * Determine if the PlatformCapability exists in this ComputeResource
     *
     * @param capability The PlatformCapability to check
     *
     * @return True if exists, false otherwise
     */
    public boolean hasPlatformCapability(PlatformCapability capability) {
        boolean contains;
        synchronized(platformCapabilities) {
            contains = platformCapabilities.contains(capability);
        }
        return(contains);
    }

    /**
     * Update a <code>PlatformCapability</code> object. Updating a 
     * <code>PlatformCapability</code> component has the following semantic:
     *
     * <p>If the <code>PlatformCapability</code> object <code>Class</code> exists 
     * in the collection of <code>PlatformCapability</code> components, the 
     * existing <code>PlatformCapability</code> will be updated with the new 
     * <code>PlatformCapability</code> mappings. If the 
     * <code>PlatformCapability</code> object <code>Class</code> cannot be found,
     * it will be added to the collection.
     *
     * <p>Updating a <code>PlatformCapability</code> component causes the state of 
     * this object to change, triggering the notification of all registered 
     * <code>Observer</code> instances
     *
     * @param capability The PlatformCapability to add
     *
     * @throws Exception If there are errors updating the PlatformCapability
     */
    public void updatePlatformCapability(PlatformCapability capability) 
    throws Exception {
        removePlatformCapability(capability, false);
        addPlatformCapability(capability);
    }

    /**
     * Remove a <code>PlatformCapability</code> object. The removal of a 
     * <code>PlatformCapability</code> component causes the state of this object to 
     * change, triggering the notification of all registered <code>Observer</code> 
     * instances
     *
     * @param pCap The PlatformCapability to remove
     * @param clean If this value is true and if the PlatformCapability has a
     * DownloadRecord defined (was provisioned during the time this Cybernode
     * was running) then remove the PlatformCapability from the system.  
     *
     * @return True if removed, false otherwise
     */
    public boolean removePlatformCapability(PlatformCapability pCap, boolean clean) {
        boolean removed = false;
        synchronized(removals) {
            removals.add(pCap);
        }
        try {
            if(clean) {
                DownloadRecord[] downloadRecords = pCap.getDownloadRecords();
                StringBuffer buff = new StringBuffer();
                if(downloadRecords.length > 0 && logger.isLoggable(Level.INFO))
                    buff.append(
                        "Removing StagedSoftware for PlatformCapability: [")
                        .append(pCap.getName())
                        .append("]");
                for (DownloadRecord downloadRecord : downloadRecords) {
                    buff.append("\n\tRemoved ")
                        .append(DownloadManager.remove(downloadRecord));
                }
                if(logger.isLoggable(Level.INFO))
                    logger.info(buff.toString());
                if(pCap.getConfigurationFile()!=null) {
                    File configFile = new File(pCap.getConfigurationFile());
                    if(configFile.exists()) {
                        if(configFile.delete())                
                            logger.info("Removed PlatformCapability " +
                                        "["+pCap.getName()+"] " +
                                        "configuration file: "+
                                        FileUtils.getFilePath(configFile));
                    }
                }
            }

            synchronized(platformCapabilities) {
                removed = platformCapabilities.remove(pCap);
            }
            if(removed) {
                stateChange();
            }
        } finally {
            removals.remove(pCap);
        }
        return(removed);
    }

    /**
     * Determine if a {@link org.rioproject.system.capability.PlatformCapability}
     * is being removed from the system. This will occur for provisioned
     * <tt>PlatformCapability</tt> instances that are removed as a result of
     * service termination.
     *
     * @param pCap The <tt>PlatformCapability</tt> to check
     *
     * @return True if the <tt>PlatformCapability</tt> is currently in the
     * process of being removed
     */
    public boolean removalInProcess(PlatformCapability pCap) {
        boolean beingRemoved;
        synchronized(removals) {
            beingRemoved = removals.contains(pCap);
        }
        return beingRemoved;
    }

    /**
     * Add a <code>MeasurableCapability</code> object. The addition of a 
     * <code>MeasurableCapability</code> component causes the state of this object 
     * to change, triggering the notification of all registered <code>Observer</code> 
     * instances. 
     *
     * <p>If there is a <code>MeasurableCapability</code> component that is equal 
     * to the provided <code>MeasurableCapability</code> (according to the 
     * <code>MeasurableCapability.equals()</code> contract), the existing 
     * <code>MeasurableCapability</code> object will be remain unchanged and
     * the provided <code>MeasurableCapability</code> ignored
     *
     * @param capability The MeasurableCapability to add
     *
     * @return True if added, false otherwise
     */
    public boolean addMeasurableCapability(MeasurableCapability capability) {
        boolean added = false;
        if(measurables.contains(capability))
            return(false);
        if(measurables.add(capability)) {
            added = true;
            capabilityChannel.subscribe(capability);
            stateChange();
        }
        return(added);
    }

    /**
     * Determine if the MeasurableCapability exists in this ComputeResource
     *
     * @param capability The MeasurableCapability to check
     *
     * @return True if the MeasurableCapability exists, false otherwise
     */
    public boolean hasMeasurableCapability(MeasurableCapability capability) {
        return(measurables.contains(capability));
    }

    /**
     * Update a <code>MeasurableCapability</code> object. Updating a 
     * <code>MeasurableCapability</code> component causes the state of this object 
     * to change, triggering the notification of all registered 
     * <code>Observer</code> instances. 
     *
     * <p>If there is a <code>MeasurableCapability</code> object that is equal to 
     * the provided <code>MeasurableCapability</code> (according to the 
     * <code>MeasurableCapability.equals()</code> contract), the existing 
     * <code>MeasurableCapability</code> object will be replaced by the
     * provided <code>MeasurableCapability</code> object
     *
     * @param capability The MeasurableCapability to update
     */
    public void updateMeasurableCapability(MeasurableCapability capability) {
        if(measurables.contains(capability)) {
            measurables.remove(capability);
            capabilityChannel.unsubscribe(capability);
        }
        addMeasurableCapability(capability);
    }

    /**
     * Remove a <code>MeasurableCapability</code> object. The removal of a 
     * <code>MeasurableCapability</code> component causes the state of this object 
     * to change, triggering the notification of all registered 
     * <code>Observer</code> instances
     *
     * @param capability The MeasurableCapability to remove
     *
     * @return True if removed, false otherwise
     */
    public boolean removeMeasurableCapability(MeasurableCapability capability) {
        boolean removed = false;
        if(measurables.remove(capability)) {
            removed = true;
            capabilityChannel.unsubscribe(capability);
            stateChange();
        }
        return(removed);
    }

    /**
     * Notify all registered Observers the ComputeResource has changed. The argument 
     * provided on the Observable.notifyObservers() invocation will be the 
     * ResourceCapability component, used to communicate the resource utilization, 
     * platform and measurable capabilities attributes of the ComputeResource
     */
    public void stateChange() {
        if(initializing)
            return;
        setChanged();
        notifyObservers(getResourceCapability());
    }

    /**
     * A helper method which will add a <code>ThresholdListener</code> to all 
     * contained <code>MeasurableCapability</code> components. An alternate 
     * mechanism is to obtain all <code>MeasurableCapability</code> components 
     * contained by the <code>ComputeResource</code> and add the listener to each 
     * component
     *
     * @param listener The ThresholdListener to add
     */
    public void addThresholdListener(ThresholdListener listener) {
        MeasurableCapability[] mCaps = getMeasurableCapabilities();
        for (MeasurableCapability mCap : mCaps)
            mCap.addThresholdListener(listener);
    }

    /**
     * A helper method which will remove a <code>ThresholdListener</code> from all 
     * contained <code>MeasurableCapability</code> components. An alternate mechanism 
     * is to obtain all <code>MeasurableCapability</code> components contained by 
     * the <code>ComputeResource</code> and remove the listener from each component
     *
     * @param listener The ThresholdListener
     */
    public void removeThresholdListener(ThresholdListener listener) {
        MeasurableCapability[] mCaps = getMeasurableCapabilities();
        for (MeasurableCapability mCap : mCaps)
            mCap.removeThresholdListener(listener);
    }

    /**
     * Return an array of <code>PlatformCapability</code> objects. This method will 
     * create a new array of <code>PlatformCapability</code> objects each time it 
     * is invoked. If there are no <code>PlatformCapability</code> objects contained 
     * within the <code>platformCapabilities</code> Collection, a zero-length array 
     * will be returned.
     *
     * @return Array of PlatformCapability objects 
     */
    public PlatformCapability[] getPlatformCapabilities() {
        PlatformCapability[] pCaps;
        synchronized(platformCapabilities) {
            pCaps = platformCapabilities.toArray(
                new PlatformCapability[platformCapabilities.size()]);
        }
        return pCaps;
    }

    /**
     * Get a PlatformCapability
     * 
     * @param name The name of the PlatformCapability
     * 
     * @return The first PlatformCapability that matches the  name. If no
     * PlatformCapability matches the name, return null
     *
     * @see org.rioproject.system.capability.PlatformCapability#NAME
     */
    public PlatformCapability getPlatformCapability(String name) {
        PlatformCapability[] pCaps = getPlatformCapabilities();
        for (PlatformCapability pCap : pCaps) {
            if (pCap.getName().equals(name))
                return (pCap);
        }
        return(null);
    }

    /**
     * Get the PlatformCapability name table
     *
     * @return A Map of PlatformCapability names to PlatformCapability
     * classnames. A new Map is created each time
     */
    public Map<String, String> getPlatformCapabilityNameTable() {
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(platformCapabilityNameTable);
        return map;
    }

    /**
     * Get a MeasurableCapability
     * 
     * @param description The description of the MeasurableCapability
     * 
     * @return The first MeasurableCapability that matches the description. If no 
     * MeasurableCapability matches the description, return null
     */
    public MeasurableCapability getMeasurableCapability(String description) {
        MeasurableCapability[] mCaps = getMeasurableCapabilities();
        for (MeasurableCapability mCap : mCaps) {
            if (mCap.getId().equals(description))
                return (mCap);
        }
        return(null);
    }

    /**
     * Return an array of <code>MeasurableCapability</code> objects. This method 
     * will create a new array of <code>MeasurableCapability</code> objects each 
     * time it is invoked. If there are no <code>MeasurableCapability</code> 
     * objects contained within the <code>measurables</code> Collection, a 
     * zero-length array will be returned.
     *
     * @return Array of MeasurableCapability objects 
     */
    public MeasurableCapability[] getMeasurableCapabilities() {
        return(measurables.toArray(new MeasurableCapability[measurables.size()]));
    }

    /**
     * Get the MeasuredResource components for the ComputeResource
     * 
     * @return Array of MeasuredResource instances that correspond to 
     * MeasurableCapability components
     */
    public MeasuredResource[] getMeasuredResources() {
        MeasurableCapability[] mCaps;
        synchronized(measurables) {
            mCaps = getMeasurableCapabilities();
        }
        MeasuredResource[] measured = new MeasuredResource[mCaps.length];
        for(int i=0; i<mCaps.length; i++)
            measured[i] = mCaps[i].getMeasuredResource();
        return(measured);
    }

    /**
     * Get the resource utilization as computed by a summation of the 
     * MeasurableCapability components
     * 
     * @return The aggregate utilization of the ComputeResource
     */
    public double getUtilization() {
        return getUtilization(getMeasuredResources());
    }

    /*
     * Get the resource utilization as computed by a summation of the
     * MeasurableCapability components
     *
     * @return The aggregate utilization of the ComputeResource
     */
    private double getUtilization(MeasuredResource[] mResources) {
        double utilization=0;
        for (MeasuredResource mRes : mResources) {
            utilization = utilization + mRes.getValue();
        }
        if(mResources.length>0)
            utilization = utilization/mResources.length;
        return(utilization);
    }

    /**
     * Set the persistentProvisioning property
     * 
     * @param  persistentProvisioning True if the ComputeResource supports 
     * persistent provisioning, false otherise
     */
    public void setPersistentProvisioning(boolean persistentProvisioning) {
        boolean changed=false;
        if(this.persistentProvisioning!=persistentProvisioning)
            changed=true;
        this.persistentProvisioning = persistentProvisioning;
        if(changed)
            stateChange();
    }

    /**
     * Get the persistentProvisioning property
     * 
     * @return True if the ComputeResource supports persistent provisioning, 
     * false otherwise
     */
    public boolean getPersistentProvisioning() {
        return(persistentProvisioning);
    }

    /**
     * Get the location for the persistent provisioning of PlatformCapability 
     * instances
     * 
     * @return The root directory for the persistent provisioning of 
     * PlatformCapability instances
     */
    public String getPersistentProvisioningRoot() {
        return(provisionRoot);
    }

    /**
     * Set the location for the persistent provisioning of PlatformCapability 
     * instances
     * 
     * @param provisionRoot The root directory for the persistent provisioning of 
     * PlatformCapability instances
     */
    public void setPersistentProvisioningRoot(String provisionRoot) {
        if(provisionRoot!=null)
            this.provisionRoot = provisionRoot;
    }

    /**
     * Return the <code>ResourceCapability</code>object that will be used to 
     * communicate the platform and measurable capabilities attributes of the 
     * ComputeResource
     *
     * @return The ResourceCapability object for the ComputeResource
     */
    public ResourceCapability getResourceCapability() {
        return(new ResourceCapability(address.getHostAddress(), 
                                      address.getHostName(),
                                      getPersistentProvisioning(),
                                      getPlatformCapabilities(), 
                                      getComputeResourceUtilization()));
    }

    /**
     * Get the ComputeResourceUtilization
     *
     * @return A ComputeResourceUtilization object representing the utilization
     * of the ComputeResource
     */
    public ComputeResourceUtilization getComputeResourceUtilization() {
        MeasuredResource[] mrs = getMeasuredResources();
        ComputeResourceUtilization computeResourceUtilization =
            new ComputeResourceUtilization(getDescription(),
                                           getAddress().getHostName(),
                                           getAddress().getHostAddress(),
                                           getUtilization(mrs),
                                           Arrays.asList(mrs));
        return(computeResourceUtilization);
    }

    /**
     * Boot the ComputeResource, loading all PlatformCapability and
     * MeasurableCapability instances
     */
    public void boot() {
        if(config==null)
            throw new IllegalArgumentException("config is null, cannot boot");
        initializing = true;        
        try {
            systemCapabilitiesLoader =
                (SystemCapabilitiesLoader)config.getEntry(COMPONENT, 
                                                          "systemCapabilitiesLoader", 
                                                          SystemCapabilitiesLoader.class, 
                                                          new SystemCapabilities()); 
            /* Get the PlatformCapability instances */
            PlatformCapability[] pCaps =
                systemCapabilitiesLoader.getPlatformCapabilities(config);
            PlatformCapability storage = null;
            PlatformCapability memory = null;
            for (PlatformCapability pCap : pCaps) {
                if (pCap.getClass().getName().equals(
                    SystemCapabilities.STORAGE))
                    storage = pCap;
                if (pCap.getClass().getName().equals(
                    SystemCapabilities.MEMORY))
                    memory = pCap;
                addPlatformCapability(pCap);
            }

            platformCapabilityNameTable =
                systemCapabilitiesLoader.getPlatformCapabilityNameTable();
            
            /* Initialize the CapabilityChannel */
            capabilityChannel.init();
            
            /* Get the MeasurableCapability instances */
            MeasurableCapability[] mCaps = 
                systemCapabilitiesLoader.getMeasurableCapabilities(config);
            for (MeasurableCapability mCap : mCaps) {
                if (mCap instanceof DiskSpace) {
                    if (storage != null)
                        mCap.getObservable().addObserver((Observer) storage);
                }
                if (mCap instanceof Memory && !(mCap instanceof SystemMemory)) {
                    if (memory != null)
                        mCap.getObservable().addObserver((Observer) memory);
                }
                addMeasurableCapability(mCap);
                mCap.start();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Booting ComputeResource", e);
        } finally {
            initializing = false;
        }                            
    }

    /**
     * Inform all <code>MeasurableCapability</code> instances to stop measuring
     */
    public void shutdown() {
        for (MeasurableCapability measurable : measurables) {
            measurable.stop();
        }
        capabilityChannel.terminate();
    }    

    /**
     * The CapabilityChannel is an inner class that becomes an Observer to
     * MeasurableCapability components and ends up notifying the ComputeResource 
     * to update registered Observer instances if any MeasurableCapability 
     * components have changed their state within the interval prescribed for the 
     * broadcaster
     */
    public class CapabilityChannel implements Observer, Runnable {
        public static final long DEFAULT_REPORT_INTERVAL=1000*60;
        long reportInterval;
        boolean hasChanged;
        Thread channelThread;
        boolean run=true;
        long resetTime = 0;

        public CapabilityChannel() {
            reportInterval = DEFAULT_REPORT_INTERVAL;            
        }
        
        void init() {
            if(channelThread==null) {
                channelThread = new Thread(this, "CapabililityChannel");
                channelThread.setDaemon(true);
                channelThread.start();
            } else {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("CapabilityChannel already initialized");
            }
        }

        /**
         * Stop the CapabilityChannel channelThread
         */
        public void terminate() {
            run = false;
            if(channelThread!=null)
                channelThread.interrupt();
        }

        /**
         * Set the update interval for channelThread notification
         *
         * @param reportInterval The update interval for channelThread notification
         */
        public void setReportInterval(long reportInterval) {
            if(this.reportInterval!=reportInterval) {
                if(channelThread!=null) {
                    channelThread.interrupt();
                    channelThread = null;
                    resetTime = System.currentTimeMillis()+reportInterval;
                }
                this.reportInterval = reportInterval;
                init();
            }
        }

        /**
         * Get the update interval for channelThread notification
         *
         * @return The update interval for channelThread notification
         */
        public long getReportInterval() {
            return(reportInterval);
        }

        /**
         * Subscribe to a MeasurableCapability 
         *
         * @param capability A MeasurableCapability 
         */
        public void subscribe(MeasurableCapability capability) {
            capability.getObservable().addObserver(this);
        }

        /**
         * Unsubscribe from a MeasurableCapability 
         *
         * @param capability A MeasurableCapability 
         */
        public void unsubscribe(MeasurableCapability capability) {
            capability.getObservable().deleteObserver(this);
        }

        /**
         * Notification that concrete implementation(s) of the 
         * <code>MeasurableCapability<code> have changed their state. As these 
         * notifications arise they will cause a chained reaction to occur, that is 
         * all  listeners (Observers) of the <code>ComputeResource</code> object 
         * will be notified as well
         */
        public void update(Observable o, Object arg) {
            setHasChanged(true);
        }

        /**
         * Thread that will update the ComputeResource to notify Observer instances
         * if any MeasurableCapability have updated their state in the interval 
         * provided. If a change has been made, inform the ComputeResource and reset 
         * the hasChanged attribute
         */
        public void run() {
            while(run) {
                try {
                    /* resetTime will be set if the channel's report interval
                     * has been reset. This will avoid an unecessary update */
                    if(resetTime<=System.currentTimeMillis()) {
                        if(getHasChanged()) {
                            stateChange();
                            setHasChanged(false);
                        }
                    }
                    Thread.sleep(reportInterval);                    
                } catch(InterruptedException ignore) {
                    if(!run)
                        break;
                }
            }
        }

        /**
         * Set whether a change has occured from a MeasurableCapability
         *
         * @param hasChanged True if a change has occured, false otherwise. False 
         * indicates a clear, that is a reset
         */
        private synchronized void setHasChanged(boolean hasChanged) {
            this.hasChanged = hasChanged;
        }

        /**
         * Get whether a change has occured from a MeasurableCapability
         *
         * @return True if a change has occured, false otherwise
         */
        private synchronized boolean getHasChanged() {
            return(hasChanged);
        }
    }
}
