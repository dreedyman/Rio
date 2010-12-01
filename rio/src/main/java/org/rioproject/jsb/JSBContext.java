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
package org.rioproject.jsb;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.associations.AssociationManagement;
import org.rioproject.associations.AssociationMgmt;
import org.rioproject.boot.CommonClassLoader;
import org.rioproject.boot.ServiceClassLoader;
import org.rioproject.config.AggregateConfig;
import org.rioproject.config.ConfigHelper;
import org.rioproject.config.Constants;
import org.rioproject.core.ServiceBeanConfig;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.jsb.ComponentLoader;
import org.rioproject.core.jsb.ComputeResourceManager;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanManager;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventHandler;
import org.rioproject.resources.client.DiscoveryManagementPool;
import org.rioproject.resources.client.JiniClient;
import org.rioproject.sla.SLA;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.watch.WatchDataSourceRegistry;
import org.rioproject.watch.WatchRegistry;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private List<PlatformCapability> platformList = new ArrayList<PlatformCapability>();
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
    private final Map<Long, EventHandler> eventTable =
        new HashMap<Long, EventHandler>();
    /** Collection of attributes */
    private final List<Entry> attrs = new ArrayList<Entry>();
    /** The service's configuration, created lazily */
    private Configuration  serviceBeanConfig;
    /* The Subject used to authenticate the service */
    private Subject subject;
    /** Component name for logging and configuration property retrieval */
    private static final String COMPONENT="org.rioproject.jsb";
    /** A Logger instance for this component */
    private static Logger logger = Logger.getLogger(COMPONENT);    

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
    public JSBContext(ServiceElement sElem,
                      ServiceBeanManager serviceBeanManager, 
                      ComputeResource computeResource,
                      /* Optional */
                      Configuration sharedConfig) {
        if(sElem == null)
            throw new IllegalArgumentException("sElem is null");
        if(serviceBeanManager == null)
            throw new IllegalArgumentException("serviceBeanManager is null");
        if(computeResource == null)
            throw new IllegalArgumentException("computeResource is null");
        setServiceElement(sElem);
        this.serviceBeanManager = serviceBeanManager;
        this.computeResource = computeResource;
        this.sharedConfig = sharedConfig;
        ClassLoader cCL = Thread.currentThread().getContextClassLoader();
        if(cCL instanceof ServiceClassLoader) {
            ServiceClassLoader scl = (ServiceClassLoader)cCL;
            URL urls[] = scl.getURLs();
            if(urls!=null && urls.length>0) {
                exportCodebase = urls[0].getProtocol()+"://"+urls[0].getHost()+":"+urls[0].getPort()+"/";
            }
        }
    }

    /**
     * Get the shared config
     * 
     * @return The shared Configuration
     */
    public Configuration getSharedConfiguration() {
        return(sharedConfig);
    }

    /**
     * Set the Subject used to authenticate the service
     *
     * @param subject The Subject 
     */
    public void setSubject(Subject subject) {
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
    public void setServiceElement(ServiceElement newElem) {
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
     * @see org.rioproject.core.jsb.ServiceBeanContext#getComponentLoader
     */
    public ComponentLoader getComponentLoader() {        
        return (CommonClassLoader.getInstance());
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
        //Configuration  serviceBeanConfig;
        if(serviceBeanConfig==null) {
            ClassLoader cCL = Thread.currentThread().getContextClassLoader();
            String[] args;
            try {
                args = ConfigHelper.getConfigArgs(
                    sElem.getServiceBeanConfig().getConfigArgs(), cCL);
            } catch (IOException e) {
                throw new ConfigurationException("Creating configuration", e);
            }
            if(logger.isLoggable(Level.FINER)) {
                StringBuffer sb = new StringBuffer();
                for(String s : args) {
                    if(sb.length()>0)
                        sb.append("\n");
                    sb.append("\t"+s);
                }
                logger.finer("===> CONFIG ARGS: \n"+sb);
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
                    File tmpDir =
                        new File(System.getProperty("java.io.tmpdir")).getCanonicalFile();
                    if(parent.equals(tmpDir) &&
                       file.getName().startsWith("tmp")) {
                        if(file.delete() && logger.isLoggable(Level.FINEST))
                            logger.finest("Deleted temporary configuration file "+
                                          file.getName());

                    }
                }
            } catch(IOException e) {
                logger.log(Level.WARNING,
                           "Unable to get canonical file for the tmp " +
                           "directory, cannot remove generated config file(s)",
                           e);
            }
        }
        return(serviceBeanConfig);
    }

    void setConfiguration(Configuration serviceBeanConfig) {
        this.serviceBeanConfig = serviceBeanConfig;
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
     public void setServiceBeanManager(ServiceBeanManager serviceBeanManager) {
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
    public Object getInitParameter(String name) {
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
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Create DiscoveryManagement for "+sElem.getName());
            
            DiscoveryManagementPool discoPool = 
                DiscoveryManagementPool.getInstance();

            String locatorString = System.getProperty(Constants.LOCATOR_PROPERTY_NAME);
            List<LookupLocator> locators = new ArrayList<LookupLocator>();
            if(sElem.getServiceBeanConfig().getLocators()!=null)
                locators.addAll(
                    Arrays.asList(sElem.getServiceBeanConfig().getLocators()));

            if(locatorString!=null) {
                LookupLocator[] systemLocators = JiniClient.parseLocators(locatorString);
                if(locators.size()==0) {
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
            LookupLocator[] locatorsToUse = locators.size()==0?null:
                                            locators.toArray(new LookupLocator[locators.size()]);
            if(sElem.getDiscoveryManagementPooling()) { 
                serviceDiscoMgmt = discoPool.getDiscoveryManager(
                                       getOperationalStringName(),
                                       sElem.getServiceBeanConfig().getGroups(),
                                       locatorsToUse);
            } else {
                Configuration discoConfig = null;
                try {
                    discoConfig = getConfiguration();
                } catch(ConfigurationException e) {
                    if(logger.isLoggable(Level.FINEST))
                        logger.log(Level.FINEST,
                                   "Getting Configuration while "+
                                   "creating DiscoveryManagement",
                                   e);
                }
                serviceDiscoMgmt = discoPool.getDiscoveryManager(
                                        serviceBeanManager.getServiceID().toString(),
                                        sElem.getServiceBeanConfig().getGroups(),
                                        locatorsToUse,
                                        null,
                                        discoConfig);
            }
        }
        return (serviceDiscoMgmt);
    }
    
    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#getAssociationManagement
     */
    public AssociationManagement getAssociationManagement() {  
        if(associationManagement==null) {
            AssociationManagement defaultAssocMgmt = new AssociationMgmt();
            try {
                associationManagement = 
                    (AssociationManagement)getConfiguration().getEntry(
                                                        COMPONENT, 
                                                        "associationManagement", 
                                                        AssociationManagement.class, 
                                                        defaultAssocMgmt);
            } catch(ConfigurationException e) {
                logger.log(Level.WARNING, 
                           "Creating AssociationManagement, will use default", 
                           e);
                associationManagement = defaultAssocMgmt;
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
                watchRegistry =
                    (WatchRegistry)config.getEntry(
                        COMPONENT,
                        "watchRegistry",
                        WatchRegistry.class,
                        new WatchDataSourceRegistry());
                watchRegistry.setServiceBeanContext(this);
            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "Getting watchRegistry",
                           e);
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
    public void registerEventHandler(EventDescriptor descriptor,
                                     EventHandler handler) {
        eventTable.put(descriptor.eventID, handler);
        addAttribute(descriptor);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanContext#addAttribute
     */
    public void addAttribute(Entry attribute) {
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
    public PlatformCapability getPlatformCapability(String name) {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        PlatformCapability pCap =
            computeResource.getPlatformCapability(name);
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
     * @see org.rioproject.core.jsb.ComputeResourceManager#addPlatformCapability
     */
    public PlatformCapability addPlatformCapability(String className,
                                                    URL location,
                                                    Map<String, Object> mapping) {
        if(className == null)
            throw new IllegalArgumentException("className is null");
        PlatformCapability pCap = null;
        try {
            pCap = createPlatformCapability(className,
                                            (location==null?null:new URL[] {location}),
                                            mapping);
            computeResource.addPlatformCapability(pCap);
            platformList.add(pCap);
        } catch(Throwable t) {
            Throwable cause = t;
            if(t.getCause()!=null)
                cause = t.getCause();
            logger.log(Level.WARNING, "Adding PlatformCapability", cause);
        }
        return (pCap);
    }
    
    /**
     * Create a PlatformCapability
     * 
     * @param className The fully qualified classname to instantiate, must not be null
     * @param classPath Array of <code>URL</code> locations defining the classpath to
     * load the PlatformCapability. The common classloader will be checked first to 
     * determine if the class can be loaded, if it cannot, and the classpath parameter 
     * contains URL values, the classPath location(s) will be added to the common
     * classloader
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
    public static PlatformCapability createPlatformCapability(String className,
                                                              URL[] classPath,
                                                              Map<String, Object> mapping) 
    throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if(className == null)
            throw new IllegalArgumentException("className is null");        
        PlatformCapability pCap;
        CommonClassLoader cl = CommonClassLoader.getInstance();
        if(!cl.testComponentExistence(className)) {
            if(classPath!=null && classPath.length!=0) {
                cl.addComponent(className, classPath);
            } else {
                throw new IllegalArgumentException("Cannot load PlatformCapability "+
                                                  "{"+className+"}, unknown "+
                                                  "location");
            }                    
        }        
        Class clazz = cl.loadClass(className);
        pCap = (PlatformCapability)clazz.newInstance();
        if(mapping!=null)
            pCap.defineAll(mapping);            
        return (pCap);
    }

    /**
     * @see org.rioproject.core.jsb.ComputeResourceManager#removePlatformCapability
     */
    public boolean removePlatformCapability(PlatformCapability pCap) {
        if(pCap == null)
            throw new IllegalArgumentException("pCap is null");
        boolean removed = false;
        if(platformList.contains(pCap)) {
            removed =
                computeResource.removePlatformCapability(pCap, true);
            platformList.remove(pCap);
        }
        return (removed);
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
        MeasurableCapability[] mCaps =
            computeResource.getMeasurableCapabilities();
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
