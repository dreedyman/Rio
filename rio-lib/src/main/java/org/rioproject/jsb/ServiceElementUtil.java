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

import com.sun.jini.lookup.entry.LookupAttributes;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.discovery.DiscoveryGroupManagement;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationType;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.deploy.SystemComponent;
import org.rioproject.opstring.*;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.log.LoggerConfig;
import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAPolicyHandler;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.watch.ThreadDeadlockMonitor;
import org.rioproject.watch.WatchDescriptor;

import javax.management.MBeanServerConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ServiceElementUtil class provides static methods to assist in working
 * with a {@link org.rioproject.opstring.ServiceElement}
 *
 * @author Dennis Reedy
 */
public final class ServiceElementUtil {
    private static Logger logger = Logger.getLogger("org.rioproject.jsb.ServiceElementUtil");

    private ServiceElementUtil() {}

    public static String getLoggingName(ServiceBeanContext context) {
        return getLoggingName(context.getServiceElement());
    }

    public static String getLoggingName(ServiceElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append(element.getOperationalStringName()).append("/").append(element.getName());
        sb.append(":").append(element.getServiceBeanConfig().getInstanceID());
        return sb.toString();
    }

    /**
     * Check to see if the ThreadDeadlockMonitor has been declared as an SLA.
     * If not, add the default setup for the ThreadDeadlockMonitor
     *
     * @param sElem The ServiceElement to use
     * @param mbsc An optional MBeanServerConnection to set to the
     * ThreadDeadlockDetector
     *
     * @throws IllegalArgumentException if the ServiceElement parameter is null
     */
    public static void setThreadDeadlockDetector(ServiceElement sElem, MBeanServerConnection mbsc) {
        WatchDescriptor threadDeadlockDesc = getWatchDescriptor(sElem, ThreadDeadlockMonitor.ID);

        if(threadDeadlockDesc == null) {
            SLA sla = new SLA(ThreadDeadlockMonitor.ID, 0, 1);
            sla.setSlaPolicyHandler(SLAPolicyHandler.class.getName());
            threadDeadlockDesc = ThreadDeadlockMonitor.getWatchDescriptor();
            threadDeadlockDesc.setMBeanServerConnection(mbsc);
            sla.setWatchDescriptors(threadDeadlockDesc);
            sElem.getServiceLevelAgreements().addServiceSLA(sla);
        } else {
            threadDeadlockDesc.setMBeanServerConnection(mbsc);
        }
    }

    /**
     * Get the WatchDescriptor from an SLA for the given ID
     *
     * @param sElem The ServiceElement to use
     * @param name The WatchDescriptor name
     *
     * @return The matching WatchDescriptor or null if not found
     *
     * @throws IllegalArgumentException if either of the parameters are null
     */
    public static WatchDescriptor getWatchDescriptor(ServiceElement sElem, String name) {
        if(sElem == null)
            throw new IllegalArgumentException("ServiceElement cannot be null");
        if(name == null)
            throw new IllegalArgumentException("name cannot be null");
        WatchDescriptor wDesc = null;
        ServiceLevelAgreements slas = sElem.getServiceLevelAgreements();
        for(SLA sla : slas.getServiceSLAs()) {
            WatchDescriptor[] wDescs = sla.getWatchDescriptors();
            for(WatchDescriptor wd : wDescs) {
                if(wd.getName().equals(name)) {
                    wDesc = wd;
                    break;
                }
            }
        }
        return wDesc;
    }

    /**
     * Determine if the LoggerConfig attributes are different between
     * the two ServiceElement instances
     *
     * @param sElem1 ServiceElement to compare
     * @param sElem2 ServiceElement to compare
     * 
     * @return True if there is a difference between the LoggerConfig attributes, 
     * false otherwise
     */
    public static boolean hasDifferentLoggerConfig(ServiceElement sElem1, ServiceElement sElem2) {
        if(sElem1==null || sElem2==null)
            throw new IllegalArgumentException("parameters cannot be null");
        boolean different = false;
        Map map = sElem2.getServiceBeanConfig().getConfigurationParameters();
        LoggerConfig[] loggerConfigs2 = (LoggerConfig[])map.get(ServiceBeanConfig.LOGGER);
        map = sElem1.getServiceBeanConfig().getConfigurationParameters();
        LoggerConfig[] loggerConfigs1 = (LoggerConfig[])map.get(ServiceBeanConfig.LOGGER);

        if(loggerConfigs1!=null && loggerConfigs2!=null) {
            for (LoggerConfig aLoggerConfigs2 : loggerConfigs2) {
                if (LoggerConfig.isNewLogger(aLoggerConfigs2, loggerConfigs1)) {
                    different = true;
                    break;
                } else if (LoggerConfig.levelChanged(aLoggerConfigs2, loggerConfigs1)) {
                    different = true;
                    break;
                }
            }
        }
        return(different);
    }

    /**
     * Determine if there are different ServiceUI declarations in the 
     * ServiceElement instance
     * 
     * @param serviceUIs Array of attributes
     * @param sElem2 ServiceElement to compare
     * @param codebase The codebase to use
     * 
     * @return True if there are differences. false if the same
     */
    public static boolean hasDifferentServiceUIs(Entry[] serviceUIs, ServiceElement sElem2, String codebase) {
        if(serviceUIs==null || sElem2==null || codebase==null) {
            StringBuilder sb = new StringBuilder();
            if(serviceUIs==null)
                sb.append("serviceUIs");
            if(sElem2==null) {
                if(sb.length()>0)
                    sb.append(", ");
                sb.append("sElem2");
            }
            if(codebase==null) {
                if(sb.length()>0)
                    sb.append(", ");
                sb.append("codebase");
            }
            throw new IllegalArgumentException("The provided parameters " +
                                               "("+sb.toString()+") were passed " +
                                               "in with null values, they cannot be " +
                                               "null");
        }
        try {
            Configuration config2 =
                ConfigurationProvider.getInstance(sElem2.getServiceBeanConfig().getConfigArgs());
            String className = sElem2.getComponentBundle().getClassName();
            String serviceBeanComponent = className.substring(0, className.lastIndexOf("."));
            /* Get any configured ServiceUIs */
            Entry[] serviceUIs1 = (Entry[])config2.getEntry(serviceBeanComponent, 
                                                            "serviceUIs", 
                                                            Entry[].class,
                                                            new Entry[0],
                                                            codebase);            
            return(!LookupAttributes.equal(serviceUIs, serviceUIs1));
        } catch (ConfigurationException e) {
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, 
                           "Getting ServiceUIs from ServiceElement",
                           e);
        }
        return(false);
    }
    
    /**
     * Determine if the Discovery groups are different between
     * the two ServiceElement instances
     *
     * @param sElem1 ServiceElement to compare
     * @param sElem2 ServiceElement to compare
     *
     * @return True if there is a difference between the Discovery groups, 
     * false otherwise
     */
    public static boolean hasDifferentGroups(ServiceElement sElem1, ServiceElement sElem2) {
        if(sElem1==null || sElem2==null)
            throw new IllegalArgumentException("parameters cannot be null");
        boolean different = false;
        String[] groups1 = sElem1.getServiceBeanConfig().getGroups();
        String[] groups2 = sElem2.getServiceBeanConfig().getGroups();
        if (groups1 == DiscoveryGroupManagement.ALL_GROUPS && groups2 == DiscoveryGroupManagement.ALL_GROUPS)
            return false;
        else if (groups1 == null && groups2 == null)
            return false;
        else if (groups1 == null || groups2 == null)
            return true;
        else if (groups1.length != groups2.length) {
            different = true;
        } else {
            for (String aGroups1 : groups1) {
                boolean matched = false;
                for (String aGroups2 : groups2) {
                    if (aGroups1.equals(aGroups2)) {
                        matched = true;
                        break;
                    }
                }
                if (logger.isLoggable(Level.FINEST)) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("[");
                    builder.append(sElem1.getName()).append(":").append(sElem1.getServiceBeanConfig().getInstanceID());
                    builder.append("] groups [").append(aGroups1).append("] matched in groups2 ?").append(matched);
                    logger.finest(builder.toString());
                }
                if (!matched) {
                    different = true;
                    break;
                }
            }
        }
        return(different);
    }
    
    /**
     * Determine if the LookupLocators are different between
     * the two ServiceElement instances
     *
     * @param sElem1 ServiceElement to compare
     * @param sElem2 ServiceElement to compare
     *
     * @return True if there is a difference between the LookupLocators, 
     * false otherwise
     */
    public static boolean hasDifferentLocators(ServiceElement sElem1, ServiceElement sElem2) {
        if(sElem1==null || sElem2==null)
            throw new IllegalArgumentException("parameters cannot be null");
        boolean different = false;
        LookupLocator[] locs1 = sElem1.getServiceBeanConfig().getLocators();
        LookupLocator[] locs2 = sElem2.getServiceBeanConfig().getLocators();
        if(locs1 == null && locs2 == null)
            return(false);
        if(locs1 == null || locs2 == null)
            return (true);
        if(locs1.length != locs2.length) {
            different = true;
        } else {
            for (LookupLocator aLocs1 : locs1) {
                boolean matched = false;
                for (LookupLocator aLocs2 : locs2) {
                    if (aLocs1.equals(aLocs2)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    different = true;
                    break;
                }
            }
        }
        return(different);
    }
    
    /**
     * Set the instanceID, optionally making a copy of the ServiceElement
     * 
     * @param sElem The ServiceElement to use
     * @param copy If true, make a copy of the ServiceElement before assigning
     * the instanceID
     * @param id The instanceID to assign
     * 
     * @return A ServiceElement with it's instanceID set to the value provided
     */
    public static ServiceElement prepareInstanceID(ServiceElement sElem,  boolean copy, long id) {
        ServiceElement elem = sElem;
        if(copy)
            elem = copyServiceElement(sElem);
        
        /* set the instance id */
        Map<String, Object> parms = elem.getServiceBeanConfig().getConfigurationParameters();
        parms.put(ServiceBeanConfig.INSTANCE_ID, id);
        ServiceBeanConfig sbConfig = new ServiceBeanConfig(parms, elem.getServiceBeanConfig().getConfigArgs());
        /* Get the initialization parameters and add them */
        Map<String, Object> initParms = elem.getServiceBeanConfig().getInitParameters();
        for (Map.Entry<String, Object> e : initParms.entrySet()) {
            sbConfig.addInitParameter(e.getKey(), e.getValue());
        }
        elem.setServiceBeanConfig(sbConfig);        
        return(elem);
    }
    
    /**
     * Make a copy of the ServiceElement and set the instance ID
     *
     * @param sElem ServiceElement to use
     * @param id the id to set
     *
     * @return A new ServiceElement
     */
    public static ServiceElement prepareInstanceID(ServiceElement sElem, long id) {
        return(prepareInstanceID(sElem, true, id));
    }
        
    /**
     * Get the next instance ID
     *
     * @param lArray An array of ids
     *
     * @return The next instanceID
     */
    public static synchronized long getNextID(long[] lArray) {
        long nextID = 1;
        if(lArray.length>0) {
            Arrays.sort(lArray);
            for(int i=0,x=1; i<lArray.length; i++,x++) {
                if(lArray[i]!=x) {
                    nextID = x;
                    break;
                } else {
                    nextID=x+1;
                }
            }
        }
        return(nextID);
    }
    
    /**
     * Make a copy of the ServiceElement
     *
     * @param sElem The ServiceElement to copy
     * 
     * @return A new ServiceElement
     */
    public static ServiceElement copyServiceElement(ServiceElement sElem) {
        ServiceBeanConfig oldSBC = sElem.getServiceBeanConfig();
        ServiceBeanConfig sbc =
            new ServiceBeanConfig(oldSBC.getConfigurationParameters(),
                                  oldSBC.getConfigArgs());
        for(Map.Entry<String, Object> entry : oldSBC.getInitParameters().entrySet()) {
            sbc.addInitParameter(entry.getKey(), entry.getValue());
        }
        ServiceElement elem = new ServiceElement(sElem.getProvisionType(),
                                                 sbc,
                                                 sElem.getServiceLevelAgreements(),
                                                 sElem.getExportBundles(),
                                                 sElem.getFaultDetectionHandlerBundle(),
                                                 sElem.getComponentBundle());
        elem.setPlanned(sElem.getPlanned());
        elem.setCluster(sElem.getCluster());
        elem.setMaxPerMachine(sElem.getMaxPerMachine());
        elem.setMatchOnName(sElem.getMatchOnName());
        elem.setMachineBoundary(sElem.getMachineBoundary());
        elem.setAutoAdvertise(sElem.getAutoAdvertise());
        elem.setDiscoveryManagementPooling(sElem.getDiscoveryManagementPooling());
        elem.setAssociationDescriptors(sElem.getAssociationDescriptors());
        elem.setExecDescriptor(sElem.getExecDescriptor());
        elem.setStagedData(sElem.getStagedData());
        elem.setFork(sElem.forkService());
        List<RemoteRepository> rr = new ArrayList<RemoteRepository>();
        rr.addAll(Arrays.asList(sElem.getRemoteRepositories()));
        elem.setRemoteRepositories(rr);
        elem.setRuleMaps(sElem.getRuleMaps());
        return(elem);
    }

    /**
     * Create a dynamic ServiceElement
     *
     * @param serviceName The name of the service. Must not be null.
     * @param implClassName The implementation classname. Must not be null.
     * @param interfaceClassNames An iterable collection of interface classnames
     * the service exports, must not be null.
     * @param implJars An iterable collection of jars used to instantiate the
     * service, must not be null.
     * @param downloadJars An iterable collection of jars used as the codebase
     * of the service; for the jars
     * @param codebase The location the resources, both the implementation
     * and codebase jars, can be accessed from
     * @param group Name of the groups to use for discovery & join.
     * @return A ServiceElement
     *
     * @throws Exception If the ServiceElement cannot be created.
     */
    public static ServiceElement create(String serviceName,
                                        String implClassName,
                                        Iterable<String> interfaceClassNames,
                                        Iterable<String> implJars,
                                        Iterable<String> downloadJars,
                                        String codebase,
                                        String... group) throws Exception {
        if(serviceName==null)
            throw new IllegalArgumentException("serviceName cannot be null");
        if(implClassName==null)
            throw new IllegalArgumentException("implClassName cannot be null");
        if(interfaceClassNames==null)
            throw new IllegalArgumentException("interfaceClassNames " +
                                               "cannot be null");
        if(implJars==null)
            throw new IllegalArgumentException("implJars cannot be null");

        ParsedService svc = new ParsedService();
        List<ClassBundle> bundles = new ArrayList<ClassBundle>();
        for(String iFace : interfaceClassNames) {
            ClassBundle c = new ClassBundle(iFace);
            for(String jar : downloadJars) {
                c.addJAR(jar);
            }
            if(codebase!=null)
                c.setCodebase(codebase);
            bundles.add(c);
        }
        svc.setInterfaceBundles(bundles.toArray(new ClassBundle[bundles.size()]));
        ClassBundle c = new ClassBundle(implClassName);
        for(String jar : implJars) {
            c.addJAR(jar);
            if(codebase!=null)
                c.setCodebase(codebase);
        }
        svc.setComponentBundle(c);
        svc.setName(serviceName);
        int  maintain = 1;
        svc.setMaintain(Integer.toString(maintain));
        svc.setDiscoveryManagementPooling("yes");
        if(group!=null)
            svc.setGroups(group, true);
        svc.setMatchOnName(true);
        svc.setProvisionType(ParsedService.DYNAMIC);

        return OpStringLoader.makeServiceElement(svc, new HashMap<String, Map<String, AssociationDescriptor[]>>());
    }

    /**
     * Add a name,value pair to the ServiceBeanConfig
     *
     * @param sbc The current ServiceBeanConfig to set
     * @param key The key to set
     * @param value The value for the key
     *
     * @return A ServiceBeanConfig with the added properties
     */
    public static ServiceBeanConfig addConfigParameter(ServiceBeanConfig sbc,
                                                       String key,
                                                       Integer value) {
        if(sbc == null || key == null || value == null)
            throw new IllegalArgumentException("parameters cannot be null");
        Map<String, Object> configParms = sbc.getConfigurationParameters();
        configParms.put(key, value);
        ServiceBeanConfig newConfig =
            new ServiceBeanConfig(configParms,
                                  sbc.getConfigArgs());
        Map<String, Object> initParms = sbc.getInitParameters();

        for (Map.Entry<String, Object> e : initParms.entrySet()) {
            newConfig.addInitParameter(e.getKey(), e.getValue());
        }
        return (newConfig);
    }

    /**
     * Determines if the name, interfaces and opStringName equate to
     * attributes found in the provided ServiceElement
     *
     * @param sElem The ServiceElement to test, must not be null
     * @param name The name to check, may be null
     * @param interfaces The ames of the interfaces the service advertises,
     * must not be null
     * @param opStringName The name of the operationalString, may be null
     * @return If the attributes can be found in the ServiceElement, return
     * true
     */
    public static boolean matchesServiceElement(ServiceElement sElem,
                                                String name,
                                                String[] interfaces,
                                                String opStringName) {
        if(sElem==null)
            throw new IllegalArgumentException("sElem is null");
        if(interfaces == null)
            throw new IllegalArgumentException("interfaces is null");
        boolean found = false;
        ClassBundle[] exports = sElem.getExportBundles();
        for (ClassBundle export : exports) {
            boolean matched = false;
            for (String anInterface : interfaces) {
                if (export.getClassName().equals(anInterface))
                    matched = true;
            }
            if (matched) {
                found = true;
                break;
            }
        }

        if(found) {
            boolean attrsMatch = true;
            if(opStringName!=null) {
                if(!sElem.getOperationalStringName().equals(opStringName)) {
                    attrsMatch = false;
                }
            }
            if(attrsMatch) {
                if(name!=null) {
                    if(!name.equals(sElem.getName()))
                        attrsMatch = false;
                }
            }
            return(attrsMatch);
        }
        return(false);
    }

    /**
     * Get the PlatformCapability instances that match declared operational
     * requirements
     *
     * @param sElem The ServiceElement to use
     * @param pCaps Array of PlatformCapability objects
     * objects
     * @return Array of matching
     * {@link org.rioproject.system.capability.PlatformCapability} objects. If there
     * are no matching PlatformCapability instances, a zero-length array is
     * returned. A new array is allocated each time. 
     *
     */
    public static PlatformCapability[] getMatchedPlatformCapabilities(ServiceElement sElem,
                                                                      PlatformCapability[] pCaps) {
        ServiceLevelAgreements slas = sElem.getServiceLevelAgreements();
        SystemComponent[] requirements = slas.getSystemRequirements().getSystemComponents();
        List<PlatformCapability> list = new ArrayList<PlatformCapability>();
        if(requirements.length >= 0) {
            /*
             * Iterate through all resource PlatformCapability objects to
             * determine which ones support our declared requirements
             */
            for (SystemComponent requirement : requirements) {
                for (PlatformCapability pCap : pCaps) {
                    if (pCap.supports(requirement)) {
                        list.add(pCap);
                    }
                }
            }
        }
        return (list.toArray(new PlatformCapability[list.size()]));
    }

    /**
     * Get the AssociationDescriptors from the ServiceElement that match the
     * {@link org.rioproject.associations.AssociationType} type
     *
     * @param elem The ServiceElement
     * @param type The AssociationType type
     * @return An array of AssociationDescriptor instances that match the
     * AssociationType type. If there are no matching AssociationDescriptors,
     * return an empty array. A new array is allocated each time
     */
    public static AssociationDescriptor[] getAssociationDescriptors(ServiceElement elem, AssociationType type) {
        AssociationDescriptor[] aDescs = elem.getAssociationDescriptors();
        List<AssociationDescriptor> list = new ArrayList<AssociationDescriptor>();
        for (AssociationDescriptor aDesc : aDescs) {
            if (aDesc.getAssociationType() == type) {
                list.add(aDesc);
            }
        }
        return(list.toArray(new AssociationDescriptor[list.size()]));
    }

    /**
     * Format discovery settings
     *
     * @param sbConfig The ServiceElement to use
     *
     * @return a String with discovery settings formatted
     */
    public static String formatDiscoverySettings(ServiceBeanConfig sbConfig) {
        if(sbConfig==null)
            return "";
        Map<String, Object> conf = sbConfig.getConfigurationParameters();
        StringBuilder buff = new StringBuilder();
        String[] groups = (String[])conf.get(ServiceBeanConfig.GROUPS);
        buff.append("Groups: ");
        if(groups == null || groups.length == 0)
            buff.append("<none>");
        if(groups != null && groups.length > 0) {
            if(groups.length == 1 && groups[0].equals("all")) {
                buff.append("all");
            } else {
                for(int i = 0; i < groups.length; i++) {
                    if(i>0)
                        buff.append(", ");
                    buff.append(groups[i]);
                }
            }
        }
        buff.append("\n");
        buff.append("Locators: ");
        LookupLocator[] locs = (LookupLocator[])conf.get(ServiceBeanConfig.LOCATORS);
        if(locs == null || locs.length == 0)
            buff.append("<none>");
        if(locs != null && locs.length > 0) {
            for(int i=0; i<locs.length; i++) {
                if(i>0)
                    buff.append(", ");
                buff.append("jini://").append(locs[i].getHost()).append(":").append(locs[i].getPort());
            }
        }
        return buff.toString();
    }
}
