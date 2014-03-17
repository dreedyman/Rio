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
package org.rioproject.impl.system;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import org.rioproject.config.Constants;
import org.rioproject.deploy.DownloadRecord;
import org.rioproject.deploy.StagedSoftware;
import org.rioproject.net.HostUtil;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.impl.util.DownloadManager;
import org.rioproject.impl.util.FileUtils;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.impl.system.capability.PlatformCapabilityWriter;
import org.rioproject.system.capability.platform.StorageCapability;
import org.rioproject.impl.system.measurable.MeasurableCapability;
import org.rioproject.impl.system.measurable.disk.DiskSpace;
import org.rioproject.impl.system.measurable.memory.Memory;
import org.rioproject.impl.system.measurable.memory.SystemMemory;
import org.rioproject.impl.util.StringUtil;
import org.rioproject.util.TimeUtil;
import org.rioproject.impl.watch.ThresholdListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The <code>ComputeResource</code> represents an abstract notion of a compute 
 * resource that offers computational resources. These computational resources correlate 
 * to qualitative and quantitative capabilities such as CPU, Memory, and others.
 *
 * @author Dennis Reedy
 */
public class ComputeResource {
    /** Name to use when getting Configuration values and to get the Logger */
    static final String COMPONENT = "org.rioproject.system";
    static Logger logger = LoggerFactory.getLogger(ComputeResource.class);
    /**
     * A description of the <code>ComputeResource</code> 
     */
    private String description;
    /**
     * The <code>InetAddress</code> of the compute resource the 
     * <code>ComputeResource</code> object was constructed on
     */
    private final InetAddress address;
    /**
     * The local host, used for host name
     */
    private final InetAddress localHost;
    /** 
     * The <code>platformCapabilities</code> defines a <code>Collection</code> of 
     * <code>PlatformCapabilities</code> which are specific types of mechanism(s) 
     * associated with this <code>ComputeResource</code>, and is used to define 
     * base platform capabilities and resources. 
     */
    private final List<PlatformCapability> platformCapabilities = new ArrayList<PlatformCapability>();
    /**
     * A collection of <tt>PlatformCapability</tt> instances being
     * provisioned/installed
     */
    private final List<PlatformCapability> platformCapabilityPending = new ArrayList<PlatformCapability>();
    /** 
     * The <code>measurables</code> defines a <code>Collection</code> of 
     * <code>MeasurableCapabilities</code> which are specific types of measurable 
     * mechanism(s) associated with this <code>ComputeResource</code>
     */
    private final List<MeasurableCapability> measurables = new ArrayList<MeasurableCapability>();
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
    private final Map<String, String> platformCapabilityNameTable = new HashMap<String, String>();
    private final List<PlatformCapability> removals = new ArrayList<PlatformCapability>();
    private SystemCapabilitiesLoader systemCapabilitiesLoader;

    public static final long DEFAULT_REPORT_INTERVAL=1000*60;
    private long reportInterval;
    private final List<ResourceCapabilityChangeListener> listeners = new ArrayList<ResourceCapabilityChangeListener>();
    private final ScheduledExecutorService resourceCapabilityChangeService = Executors.newScheduledThreadPool(1);
    private ScheduledFuture resourceCapabilityChangeNotifierFuture;

    /**
     * Create a ComputeResource with a default (empty) configuration
     *
     * @throws ConfigurationException If there are problems accessing the
     * ComputeResource's configuration object
     * @throws UnknownHostException If the local host cannot be obtained
     */
    public ComputeResource() throws ConfigurationException, UnknownHostException {
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
    public ComputeResource(Configuration config) throws ConfigurationException, UnknownHostException {
        if(config==null)
            throw new IllegalArgumentException("config is null");

        String system = System.getProperty("os.name")+", "+
                        System.getProperty("os.arch")+", "+
                        System.getProperty("os.version");
        this.config = config;
        address = HostUtil.getInetAddressFromProperty(Constants.RMI_HOST_ADDRESS);
        localHost = InetAddress.getLocalHost();

        String defaultDescription = address.getHostName()+" "+system;
        description = (String)config.getEntry(COMPONENT, "description", String.class, defaultDescription);
        reportInterval = (Long)config.getEntry(COMPONENT, "reportInterval", Long.class, DEFAULT_REPORT_INTERVAL);
        if(reportInterval<1000)
            throw new ConfigurationException("The reportInterval must not be less then 1000 milliseconds.");
        scheduleResourceCapabilityReporting();
    }
    
    /**
     * Set the reportInterval property which controls how often the {@code ComputeResource} 
     * will inform registered {@link ResourceCapabilityChangeListener}s with an update to
     * the {@link org.rioproject.system.ResourceCapability}.
     *
     * @param reportInterval The interval controlling when the ComputeResource 
     * reports change of state to registered Observers
     */
    public void setReportInterval(long reportInterval) {
        if(this.reportInterval!=reportInterval) {
            this.reportInterval = reportInterval;
            if(!resourceCapabilityChangeNotifierFuture.isDone())
                resourceCapabilityChangeNotifierFuture.cancel(true);
            logger.debug("Set ResourceCapability reportInterval to {}", TimeUtil.format(reportInterval));
            scheduleResourceCapabilityReporting();
        }
    }

    private void scheduleResourceCapabilityReporting() {
        resourceCapabilityChangeNotifierFuture =
            resourceCapabilityChangeService.scheduleAtFixedRate(new ResourceCapabilityChangeNotifier(),
                                                                reportInterval,
                                                                reportInterval,
                                                                TimeUnit.MILLISECONDS);
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
        return reportInterval;
    }

    /**
     * Get the description
     *
     * @return The description
     */
    private String getDescription() {
        return(description);
    }    
    
    /**
     * Get the InetAddress ComputeResource object has been configured to represent
     * 
     * @return The InetAddress ComputeResource object has been configured to represent
     */
    public InetAddress getAddress() {
        return(address);
    }

    /**
     * Get the host name.
     *
     * @return The host name.
     */
    public String getHostName() {
        return localHost.getHostName();
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
            logger.trace("Have PlatformCapability : {} load any system resources", pCap.getClass().getName());

            boolean stateChange;
            synchronized(platformCapabilities) {
                stateChange = platformCapabilities.add(pCap);
            }
            if(stateChange)
                stateChange();
        }
    }
    
    /**
     * Provision a {@link org.rioproject.deploy.StagedSoftware} for a
     * {@link org.rioproject.system.capability.PlatformCapability} object. The
     * provisioning of {@link org.rioproject.deploy.StagedSoftware}
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
    public DownloadRecord[] provision(PlatformCapability pCap, StagedSoftware stagedSoftware) {
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
                DownloadManager downloadMgr = new DownloadManager(provisionRoot, stagedSoftware);
                DownloadRecord record = null;
                try {
                    logger.trace("Provisioning StagedSoftware for PlatformCapability : {}", pCap.getClass().getName());
                    record = downloadMgr.download();
                    if(record!=null) {
                        logger.trace(record.toString());
                        pCap.addDownloadRecord(record);
                        pCap.setPath(record.unarchived()?
                                     record.getExtractedPath():record.getPath());
                    }
                    pCap.addStagedSoftware(stagedSoftware);
                    DownloadRecord postInstallRecord = downloadMgr.postInstall();
                    if(postInstallRecord!=null)
                        pCap.addDownloadRecord(postInstallRecord);

                    if(stagedSoftware.getUseAsClasspathResource()) {
                        String[] classpath;
                        if(pCap.getPath().endsWith(".jar") || pCap.getPath().endsWith(".zip")) {
                            classpath = StringUtil.toArray(pCap.getPath());
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
                        String configFileLocation = systemCapabilitiesLoader.getPlatformConfigurationDirectory(config);
                        if(configFileLocation==null) {
                            logger.warn("Unable to write PlatformConfiguration [{}] configuration, " +
                                        "unknown platform configuration directory. The RIO_HOME environment " +
                                        "variable must be set", pCap.getName());
                        } else {
                            PlatformCapabilityWriter pCapWriter = new PlatformCapabilityWriter();
                            String fileName = pCapWriter.write(pCap, configFileLocation);
                            pCap.setConfigurationFile(fileName);
                            logger.info("Wrote PlatformCapability [{}] configuration to {}", pCap.getName(), fileName);
                        }
                    }
                    logger.trace("Have PlatformCapability : {} load any system resources", pCap.getClass().getName());
                    stateChange();

                } catch(IOException e) {
                    if(record!=null)
                        downloadMgr.remove();
                    logger.warn("Provisioning StagedSoftware for PlatformCapability : {}", pCap.getClass().getName(), e);
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
                StringBuilder buff = new StringBuilder();
                buff.append("Removing StagedSoftware for PlatformCapability: [")
                    .append(pCap.getName())
                    .append("]");
                for (DownloadRecord downloadRecord : downloadRecords) {
                    buff.append("\n\tRemoved ").append(DownloadManager.remove(downloadRecord));
                }
                logger.info(buff.toString());
                if(pCap.getConfigurationFile()!=null) {
                    File configFile = new File(pCap.getConfigurationFile());
                    if(configFile.exists()) {
                        if(configFile.delete())
                            logger.info("Removed PlatformCapability [{}] configuration file: {}",
                                        pCap.getName(), FileUtils.getFilePath(configFile));
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
     * @return True if the <tt>PlatformCapability</tt> is currently in the process of being removed
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
            stateChange();
        }
        return(added);
    }
    
    public void addListener(ResourceCapabilityChangeListener listener) {
        if(listener==null)
            return;
        synchronized (listeners) {
            if(!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(ResourceCapabilityChangeListener listener) {
        if(listener==null)
            return;
        synchronized (listeners) {           
            listeners.remove(listener);
        }
    }

    /**
     * Notify all registered Observers the ComputeResource has changed. The argument 
     * provided on the Observable.notifyObservers() invocation will be the 
     * ResourceCapability component, used to communicate the resource utilization, 
     * platform and measurable capabilities attributes of the ComputeResource
     */
    private void stateChange() {
        if(initializing)
            return;
        logger.trace("Notify listeners of a state change");
        notifyResourceCapabilityChangeListeners();
    }

    private void notifyResourceCapabilityChangeListeners() {
        ResourceCapabilityChangeListener[] changeListeners;
        synchronized (listeners) {
            changeListeners = listeners.toArray(new ResourceCapabilityChangeListener[listeners.size()]);
        }
        ResourceCapability resourceCapability = getResourceCapability();
        for(ResourceCapabilityChangeListener l : changeListeners) {
            l.update(resourceCapability);
        }
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
            pCaps = platformCapabilities.toArray(new PlatformCapability[platformCapabilities.size()]);
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
     * persistent provisioning, false otherwise
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
            PlatformCapability[] pCaps = systemCapabilitiesLoader.getPlatformCapabilities(config);
            StorageCapability storage = null;
            org.rioproject.system.capability.platform.Memory memory = null;
            org.rioproject.system.capability.platform.SystemMemory systemMemory = null;
            for (PlatformCapability pCap : pCaps) {
                if (pCap instanceof StorageCapability) {
                    storage = (StorageCapability) pCap;
                }
                if (pCap instanceof org.rioproject.system.capability.platform.Memory) {
                    memory = (org.rioproject.system.capability.platform.Memory) pCap;
                }
                if (pCap instanceof org.rioproject.system.capability.platform.SystemMemory) {
                    systemMemory = (org.rioproject.system.capability.platform.SystemMemory) pCap;
                }
                addPlatformCapability(pCap);
            }

            platformCapabilityNameTable.putAll(systemCapabilitiesLoader.getPlatformCapabilityNameTable());

            /* Get the MeasurableCapability instances */
            MeasurableCapability[] mCaps = systemCapabilitiesLoader.getMeasurableCapabilities(config);
            for (MeasurableCapability mCap : mCaps) {
                if (mCap instanceof DiskSpace) {
                    if (storage != null) {
                        mCap.addWatchDataReplicator(storage);
                    }
                }
                if (mCap instanceof Memory) {
                    if(mCap instanceof SystemMemory) {
                        if(systemMemory!=null) {
                            mCap.addWatchDataReplicator(systemMemory);
                        }
                    } else {
                        if (memory != null) {
                            mCap.addWatchDataReplicator(memory);
                        }
                    }
                }
                addMeasurableCapability(mCap);
                mCap.start();
            }
        } catch (Exception e) {
            logger.warn("Booting ComputeResource", e);
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
        resourceCapabilityChangeService.shutdown();
    }

    class ResourceCapabilityChangeNotifier implements Runnable {
        public void run() {
            notifyResourceCapabilityChangeListeners();
        }
    }
}
