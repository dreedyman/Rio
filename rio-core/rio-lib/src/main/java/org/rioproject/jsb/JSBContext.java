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
package org.rioproject.jsb;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.associations.AssociationManagement;
import org.rioproject.associations.AssociationMgmt;
import org.rioproject.config.AggregateConfig;
import org.rioproject.config.ConfigHelper;
import org.rioproject.config.Constants;
import org.rioproject.core.jsb.ComputeResourceManager;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanManager;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventHandler;
import org.rioproject.loader.CommonClassLoader;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resources.client.DiscoveryManagementPool;
import org.rioproject.resources.client.JiniClient;
import org.rioproject.sla.SLA;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.software.SoftwareSupport;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.watch.WatchDataSourceRegistry;
import org.rioproject.watch.WatchRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * JSBContext implements the ServiceBeanContext interface
 *
 * @author Dennis Reedy
 */
public class JSBContext implements ServiceBeanContext, ComputeResourceManager {
    /** The ServiceElement */
    private ServiceElement sElem;
    /**
     * The ServiceBeanManager, providing a mechanism for the ServiceBean to
     * obtain a DiscardManager, and request it's ServiceElement be updated to
     * known OperationalStringManager instance(s)
     */
    private ServiceBeanManager serviceBeanManager;    
    /** Get the ServiceBean's join specifics */
    private DiscoveryManagement serviceDiscoMgmt;
    /** The ComputeResource this ServiceBean has been instantiated on */
    private ComputeResource computeResource;
    /**
     * The exportCodebase attribute identifies the codebase used to export
     * ServiceBean JARs
     */
    private String exportCodebase;
    /** List of PlatformCapability instances that were created */
    private final List<PlatformCapability> platformList = new ArrayList<PlatformCapability>();
    /** AssociationManagement for the ServiceBean */
    private AssociationManagement associationManagement;
    /** Shared Configuration for the ServiceBean to use*/
    private Configuration sharedConfig;
    /** WatchRegistry for storing watches and corresponding WatchDataSources */
    private WatchRegistry watchRegistry;
    /**
     * The eventTable associates an EventHandler to an EventDescriptor for the
     * ServiceProvider. Event registration requests for events this
     * ServiceProvider has advertised will consult the eventTable to determine
     * the correct EventHandler to use in order to return an event registration
     */
    private final Map<Long, EventHandler> eventTable = new HashMap<Long, EventHandler>();
    /** Collection of attributes */
    private final List<Entry> attrs = new ArrayList<Entry>();
    /** The service's configuration, created lazily */
    private Configuration  serviceBeanConfig;
    /** Optional configuration files */
    private String[] configurationFiles;
    /* The Subject used to authenticate the service */
    private Subject subject;
    /** Component name for logging and configuration property retrieval */
    private static final String COMPONENT="org.rioproject.jsb";
    /** A Logger instance for this component */
    private static Logger logger = LoggerFactory.getLogger(JSBContext.class.getName());

    /**
     * Create a JSBContext
     * 
     * @param sElem The ServiceElement
     * @param serviceBeanManager The ServiceBeanManager
     * @param computeResource The ComputeResource object representing
     * capabilities of the compute resource the service has been instantiated on
     * @param sharedConfig Configuration from the "platform" which will be used
     * as the shared configuration with an AggregateConfig
     */
    public JSBContext(final ServiceElement sElem,
                      final ServiceBeanManager serviceBeanManager,
                      final ComputeResource computeResource,
                      /* Optional */
                      final Configuration sharedConfig) {
        if(sElem == null)
            throw new IllegalArgumentException("sElem is null");
        if(serviceBeanManager == null)
            throw new IllegalArgumentException("serviceBeanManager is null");
        if(computeResource == null)
            throw new IllegalArgumentException("computeResource is null");
        this.sElem = sElem;
        this.serviceBeanManager = serviceBeanManager;
        this.computeResource = computeResource;
        this.sharedConfig = sharedConfig;
        ClassLoader cCL = Thread.currentThread().getContextClassLoader();
        if(cCL instanceof ServiceClassLoader) {
            ServiceClassLoader scl = (ServiceClassLoader)cCL;
            URL urls[] = scl.getURLs();
            if(urls!=null && urls.length>0) {
                if(urls[0].getProtocol().equals("artifact"))
                    exportCodebase = urls[0].toExternalForm();
                else
                    exportCodebase = urls[0].getProtocol()+"://"+urls[0].getHost()+":"+urls[0].getPort()+"/";
            }
        }
    }

    /**
     * Set the Subject used to authenticate the service
     *
     * @param subject The Subject 
     */
    public void setSubject(final Subject subject) {
        this.subject = subject;
    }

    /**
     * Get the Subject used to authenticate the service
     *
     * @return The Subject
     */
    public Subject getSubject() {
        return(subject);
    }
    
    /**
     * Set the ServiceElement for the ServiceBean
     * 
     * @param newElem The ServiceElement
     */
    public void setServiceElement(final ServiceElement newElem) {
        if(newElem == null)
            throw new IllegalArgumentException("sElem is null");
        boolean update = (sElem != null);
        sElem = newElem;        
        if(serviceBeanManager instanceof JSBManager)
            ((JSBManager)serviceBeanManager).setServiceElement(sElem);
        if(update && associationManagement!=null) {
            associationManagement.setServiceBeanContext(this);
        }
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getExportCodebase
     */
    public String getExportCodebase() {
        return (exportCodebase);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getConfiguration
     */
    public Configuration getConfiguration() throws ConfigurationException {
        if(serviceBeanConfig==null) {
            logger.debug("Getting configuration for {}/{}", sElem.getOperationalStringName(), sElem.getName());
            ClassLoader cCL = Thread.currentThread().getContextClassLoader();
            String[] args;
            try {
                args = ConfigHelper.getConfigArgs(sElem.getServiceBeanConfig().getConfigArgs(), cCL);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Creating configuration", e);
            } catch (IOException e) {
                throw new ConfigurationException("Creating configuration", e);
            }
            if(logger.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                for(String s : args) {
                    if(sb.length()>0)
                        sb.append("\n");
                    sb.append("\t").append(s);
                }
                logger.debug("{}/{} CONFIG ARGS: \n{}", sElem.getOperationalStringName(), sElem.getName(), sb);
            }
            if(sharedConfig!=null) {
                serviceBeanConfig =  new AggregateConfig(sharedConfig, args, cCL);
            } else {
                serviceBeanConfig = ConfigurationProvider.getInstance(args, cCL);
            }

            /* If a temporary file was created remove it */
            try {
                if(args.length==1 &&
                   (args[0].endsWith(".config") || args[0].endsWith(".groovy"))) {
                    File file = new File(args[0]);
                    File parent = file.getParentFile().getCanonicalFile();
                    File tmpDir = new File(System.getProperty("java.io.tmpdir")).getCanonicalFile();
                    if(parent.equals(tmpDir) &&
                       file.getName().startsWith("tmp")) {
                        if(file.delete() && logger.isTraceEnabled())
                            logger.trace("Deleted temporary configuration file {}", file.getName());

                    }
                }
                logger.debug("Configuration for {}/{} created", sElem.getOperationalStringName(), sElem.getName());
            } catch(IOException e) {
                logger.warn("Unable to get canonical file for the tmp directory, cannot remove generated config file(s)",
                            e);
            }
        }
        return(serviceBeanConfig);
    }

    public void setConfiguration(final Configuration serviceBeanConfig) {
        this.serviceBeanConfig = serviceBeanConfig;
    }

    void setConfigurationFiles(final String... configFiles) {
        configurationFiles = new String[configFiles.length];
        System.arraycopy(configFiles, 0, configurationFiles, 0, configFiles.length);
    }

    public String[] getConfigurationFiles() {
        return configurationFiles;
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getServiceBeanConfig
     */
    public ServiceBeanConfig getServiceBeanConfig() {
        return (sElem.getServiceBeanConfig());
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getServiceElement
     */
    public ServiceElement getServiceElement() {
        return (sElem);
    }

    /**
     * Set the ServiceBeanManager for the ServiceBean
     * 
     * @param serviceBeanManager The ServiceBeanManager
     */
     public void setServiceBeanManager(final ServiceBeanManager serviceBeanManager) {
        if(serviceBeanManager==null)
            throw new IllegalArgumentException("serviceBeanManager us null");
        this.serviceBeanManager = serviceBeanManager;
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getServiceBeanManager
     */
    public ServiceBeanManager getServiceBeanManager() {
        return (serviceBeanManager);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getComputeResourceManager
     */
    public ComputeResourceManager getComputeResourceManager() {
        return this;
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getInitParameter
     */
    public Object getInitParameter(final String name) {
        if(name==null)
            throw new IllegalArgumentException("name is null");
        return(sElem.getServiceBeanConfig().getInitParameters().get(name));
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getInitParameterNames
     */
    public Iterable<String> getInitParameterNames() {
        return (sElem.getServiceBeanConfig().getInitParameters().keySet());
    }

    /**
     * Get the name of the OperationalString
     *
     * @return The name of the OperationalString
     */
    public String getOperationalStringName() {
        return (sElem.getOperationalStringName());
    }
    
    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getDiscoveryManagement()
     */
    public DiscoveryManagement getDiscoveryManagement() throws IOException {
        if(serviceDiscoMgmt == null) {
            logger.trace("Create DiscoveryManagement for {}/{}", sElem.getOperationalStringName(), sElem.getName());
            DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance();

            String locatorString = System.getProperty(Constants.LOCATOR_PROPERTY_NAME);
            List<LookupLocator> locators = new ArrayList<LookupLocator>();
            if(sElem.getServiceBeanConfig().getLocators()!=null)
                locators.addAll(
                    Arrays.asList(sElem.getServiceBeanConfig().getLocators()));

            if(locatorString!=null) {
                LookupLocator[] systemLocators = JiniClient.parseLocators(locatorString);
                if(locators.isEmpty()) {
                    locators.addAll(Arrays.asList(systemLocators));
                } else {
                    List<LookupLocator> toAdd = new ArrayList<LookupLocator>();
                    for(LookupLocator lookLoc : systemLocators) {
                        boolean add = true;
                        for(LookupLocator ll : locators) {
                            if(ll.equals(lookLoc)) {
                                add = false;
                                break;
                            }
                        }
                        if(add)
                            toAdd.add(lookLoc);
                    }
                    for(LookupLocator ll : toAdd)
                        locators.add(ll);
                }
            }
            LookupLocator[] locatorsToUse = locators.isEmpty()?null:
                                            locators.toArray(new LookupLocator[locators.size()]);
            if(sElem.getDiscoveryManagementPooling()) { 
                serviceDiscoMgmt = discoPool.getDiscoveryManager(getOperationalStringName(),
                                                                 sElem.getServiceBeanConfig().getGroups(),
                                                                 locatorsToUse);
            } else {
                Configuration discoConfig = null;
                try {
                    discoConfig = getConfiguration();
                } catch(ConfigurationException e) {
                    logger.trace("Getting Configuration while creating DiscoveryManagement", e);
                }
                serviceDiscoMgmt = discoPool.getDiscoveryManager(serviceBeanManager.getServiceID().toString(),
                                                                 sElem.getServiceBeanConfig().getGroups(),
                                                                 locatorsToUse,
                                                                 null,
                                                                 discoConfig);
            }
            logger.trace("Created DiscoveryManagement for {}/{}", sElem.getOperationalStringName(), sElem.getName());
        }
        return (serviceDiscoMgmt);
    }
    
    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getAssociationManagement
     */
    public AssociationManagement getAssociationManagement() {  
        if(associationManagement==null) {
            AssociationManagement defaultAssociationManagement = new AssociationMgmt();
            try {
                associationManagement = (AssociationManagement)getConfiguration().getEntry(COMPONENT,
                                                                                           "associationManagement",
                                                                                           AssociationManagement.class,
                                                                                           defaultAssociationManagement);
            } catch(ConfigurationException e) {
                logger.warn("Creating AssociationManagement, will use default", e);
                associationManagement = defaultAssociationManagement;
            }
        }            
        return (associationManagement);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getWatchRegistry
     */
    public WatchRegistry getWatchRegistry() {
        if(watchRegistry==null) {
            try {
                Configuration config = getConfiguration();
                watchRegistry = (WatchRegistry)config.getEntry(COMPONENT,
                                                               "watchRegistry",
                                                               WatchRegistry.class,
                                                               new WatchDataSourceRegistry());
                watchRegistry.setServiceBeanContext(this);
            } catch(Exception e) {
                logger.warn("Getting watchRegistry", e);
            }
        }
        return(watchRegistry);
    }

    /**
     * The eventTable associates an EventHandler to an EventDescriptor for the
     * ServiceBean. Event registration requests for events this
     * ServiceBean has advertised will consult the eventTable to determine
     * the correct EventHandler to use in order to return an event registration
     *
     * @return The Map of event IDs to EventHandler instances
     */
    public Map<Long, EventHandler> getEventTable() {
        return(eventTable);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#registerEventHandler
     */
    public void registerEventHandler(final EventDescriptor descriptor, final EventHandler handler) {
        eventTable.put(descriptor.eventID, handler);
        addAttribute(descriptor);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#addAttribute
     */
    public void addAttribute(final Entry attribute) {
        if(attribute!=null)
            attrs.add(attribute);
    }

    /**
     * Get the attribute list
     *
     * @return The attribute list
     */
    public Collection<Entry> getAttributes() {
        return attrs;
    }

    /**
     * @see org.rioproject.core.jsb.ComputeResourceManager#getComputeResource
     */
    public ComputeResource getComputeResource() {
        return (computeResource);
    }

    /**
     * @see org.rioproject.core.jsb.ComputeResourceManager#getPlatformCapability
     */
    public PlatformCapability getPlatformCapability(final String name) {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        PlatformCapability pCap = computeResource.getPlatformCapability(name);
        return (pCap);
    }

    /**
     * @see org.rioproject.core.jsb.ComputeResourceManager#getMatchedPlatformCapabilities
     */
    public PlatformCapability[] getMatchedPlatformCapabilities() {
        PlatformCapability[] pCaps = computeResource.getPlatformCapabilities();
        return(ServiceElementUtil.getMatchedPlatformCapabilities(sElem, pCaps));
    }

    /**
     * Create a PlatformCapability
     *
     * @param className The fully qualified classname to instantiate, must not be null
     * @param mapping If not null, the attributes will be set to the instantiated
     * PlatformCapability
     *
     * @return A PlatformCapability object with attributes applied
     *
     * @throws ClassNotFoundException If the platform capability class cannot
     * be found
     * @throws IllegalAccessException If there is a security exception
     * @throws InstantiationException If the class cannot be created
     */
    public static PlatformCapability createPlatformCapability(final String className, final Map<String, Object> mapping)
    throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if(className == null)
            throw new IllegalArgumentException("className is null");
        PlatformCapability pCap;
        if(className.equals(SoftwareSupport.class.getSimpleName())) {
            pCap = new SoftwareSupport();
        } else if(className.equals(SoftwareSupport.class.getName())) {
            pCap = new SoftwareSupport();
        } else {
            CommonClassLoader cl = CommonClassLoader.getInstance();
            Class clazz = cl.loadClass(className);
            pCap = (PlatformCapability)clazz.newInstance();
        }
        if(mapping!=null)
            pCap.defineAll(mapping);
        return (pCap);
    }

    /**
     * Remove all PlatformCapability instances the ServiceBean added
     */
    public void removePlatformCapabilities() {
        for (PlatformCapability pCap : platformList) {
            computeResource.removePlatformCapability(pCap, true);
        }
        platformList.clear();
    }

    /**
     * @see org.rioproject.core.jsb.ComputeResourceManager#getMatchedMeasurableCapabilities
     */
    public MeasurableCapability[] getMatchedMeasurableCapabilities() {
        List<MeasurableCapability> list = new ArrayList<MeasurableCapability>();
        SLA[] slas = sElem.getServiceLevelAgreements().getServiceSLAs();
        MeasurableCapability[] mCaps = computeResource.getMeasurableCapabilities();
        /*
        * Check each of the MeasuredCapability objects for a match
        */
        for (SLA sla : slas) {
            for (MeasurableCapability mCap : mCaps) {
                if (mCap.getId().equals(sla.getIdentifier())) {
                    list.add(mCap);
                }
            }
        }
        return (list.toArray(new MeasurableCapability[list.size()]));
    }     
}
