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
package org.rioproject.resources.client;

import com.sun.jini.lookup.entry.LookupAttributes;
import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscovery;
import net.jini.id.Uuid;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.*;
import net.jini.lookup.entry.Name;
import org.rioproject.cybernode.ServiceBeanContainer;
import org.rioproject.cybernode.ServiceBeanContainerListener;
import org.rioproject.cybernode.ServiceBeanDelegate;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.OpStringFilter;
import org.rioproject.resources.servicecore.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The LookupCachePool class provides the support to get an existing 
 * LookupCache from a pool of created LookupCache instances. Criteria for 
 * determining LookupCache matching is based on ServiceTemplate matching
 *
 * @author Dennis Reedy
 */
public class LookupCachePool {
    private final List<SDMWrapper> pool = new ArrayList<SDMWrapper>();
    private ServiceBeanContainerListener containerListener;
    private static final String COMPONENT = LookupCachePool.class.getName();
    private static final Logger logger = LoggerFactory.getLogger(COMPONENT);
    private static Configuration config;
    private static LookupCachePool singleton = new LookupCachePool();

    private LookupCachePool() {
        logger.debug("Create new LookupCachePool");
    }

    /**
     * Get the singleton instance of the LookupCachePool
     *
     * @return An instance of the LookupCachePool
     */
    public static synchronized LookupCachePool getInstance() {
        return(singleton);
    }

    /**
     * Set the {@link org.rioproject.cybernode.ServiceBeanContainer}
     *
     * @param container The <tt><ServiceBeanContainer/tt> used for local
     * discovery
     */
    public void setServiceBeanContainer(final ServiceBeanContainer container) {
        if(container !=null && containerListener==null) {
            containerListener = new ContainerListener(container);
        }
    }

    /**
     * Set the Configuration property
     * 
     * @param conf The Configuration to use when creating 
     * ServiceDiscoveryManager instances
     */
    public void setConfiguration(final Configuration conf) {
        config = conf;
        logger.debug("Set configuration for LookupCachePool {}", config);
    }
    
    /**
     * This method will return an instance of LookupCache based on matching the
     * DiscoveryManagement instance and ServiceTemplate provided as 
     * criteria. If there is an existing LookupCache instance created by a 
     * ServiceDiscoveryManager instance this utility has created that matches the 
     * supplied criteria, that instance will be returned. 
     * 
     * <p>If the LookupCache can not be found due to not being able to match discovery
     * criteria to a known ServiceDiscoveryManager instance a new 
     * ServiceDiscoveryManager instance will be created, then a LookupCache instance 
     * created and returned. 
     * 
     * <p>If a ServiceDiscoveryManager can be matched, but not a LookupCache, a new
     * LookupCache will be created using the matched ServiceDiscoveryManager
     * 
     * @param dMgr A DiscoveryManager instance created by the DiscoveryManagementPool
     * @param template ServiceTemplate to match
     * @return A ServiceDiscoveryManager object based on the provided parameters or 
     * null if the DiscoveryManagement instance was not created by the 
     * DiscoveryManagementPool
     *
     * @throws IOException If discovery management cannot be created
     */
    public LookupCache getLookupCache(final DiscoveryManagement dMgr, final ServiceTemplate template) throws IOException {
        if(!(dMgr instanceof DiscoveryManagementPool.SharedDiscoveryManager)) {
            logger.warn("The DiscoveryManagement instance passed was not created by the {}, returning null",
                        DiscoveryManagementPool.class.getName());
            return(null);
        }
        DiscoveryManagementPool.SharedDiscoveryManager sharedDM = (DiscoveryManagementPool.SharedDiscoveryManager)dMgr;
        return(getLookupCache(sharedDM.getSharedName(), sharedDM.getGroups(), sharedDM.getLocators(), template));
    }
    
    /**
     * This method will return an instance of LookupCache based on matching the
     * shared name, shared discovery name, groups, locators and ServiceTemplate 
     * provided as criteria. If there is an existing LookupCache instance created 
     * by a ServiceDiscoveryManager instance this utility has created that matches 
     * the supplied criteria, that instance will be returned. 
     * 
     * <p>If a LookupCache can not be found due to not being able to match discovery
     * criteria, a new ServiceDiscoveryManager instance will be created using the 
     * provided discovery criteria, and a LookupCache instance created and returned. 
     * 
     * <p>If a ServiceDiscoveryManager can be matched, but not a LookupCache, a new
     * LookupCache will be created using the matched ServiceDiscoveryManager
     * 
     * @param sharedName The name the LookupCache instances are shared across
     * @param groups An array of String objects indicating the Jini Lookup
     * Service groups to discover
     * @param locators An array of LookupLocator objects indicating specific
     * Jini Lookup Service instances to discover
     * @param template ServiceTemplate to match
     * @return A ServiceDiscoveryManager object based on the provided parameters
     *
     * @throws IOException If discovery management cannot be created
     */
    public LookupCache getLookupCache(final String sharedName,
                                      final String[] groups,
                                      final LookupLocator[] locators,
                                      final ServiceTemplate template) throws IOException {
        if(template==null)
            throw new IllegalArgumentException("template is null");
        SDMWrapper sdmWrapper;
        try {
            sdmWrapper = getSDMWrapper(sharedName, groups, locators);
        } catch(ConfigurationException e) {
            throw new IOException("Configuration problem creating a SDMWrapper", e);
        }
        return(sdmWrapper.getLookupCache(template, true));
    }
    
    /**
     * For all ServiceDiscoveryManager instances this utility has created, terminate 
     * them and set the singleton instance to null;
     */
    public void terminate() {
        SDMWrapper[] sdms = getSDMWrappers();
        for(SDMWrapper sdmWrapper : sdms) {
            sdmWrapper.sdm.terminate();
        }
        pool.clear();
        singleton = null;        
    }

    private SDMWrapper[] getSDMWrappers() {
        SDMWrapper[] sdms;
        synchronized(pool) {
            sdms = pool.toArray(new SDMWrapper[pool.size()]);
        }
        return sdms;
    }

    /*
     * Get an SDMWrapper which matches the sharedName, discovery criteria, or 
     * create one if not found
     */
    private SDMWrapper getSDMWrapper(final String sharedName,
                                     final String[] groupsToMatch,
                                     final LookupLocator[] locatorsToMatch) throws IOException, ConfigurationException {
        SDMWrapper sdmWrapper = null;
        SDMWrapper[] sdms = getSDMWrappers();
        for(SDMWrapper wrapper : sdms) {
            if(wrapper.namesMatch(sharedName) &&
               wrapper.groupsMatch(groupsToMatch) &&
               wrapper.locatorsMatch(locatorsToMatch)) {
                sdmWrapper = wrapper;
                break;
            }
        }

        if(sdmWrapper==null) {            
            config = (config==null?EmptyConfiguration.INSTANCE:config);
            ServiceDiscoveryManager sdm = 
                new ServiceDiscoveryManager(DiscoveryManagementPool.getInstance().getDiscoveryManager(sharedName,
                                                                                                      groupsToMatch,
                                                                                                      locatorsToMatch),
                                            new LeaseRenewalManager(config),
                                            config);
                    
            sdmWrapper = new SDMWrapper(sharedName, sdm, groupsToMatch, locatorsToMatch);
            synchronized(pool) {
                pool.add(sdmWrapper);
            }
        }
        return(sdmWrapper);
    }
    
    /**
     * The SDMWrapper class provides a wrapper around a known ServiceDiscoveryManager,
     * the lookupCache instances that this utility has created using the
     * ServiceDiscoveryManager and checks to see if the referenced 
     * ServiceDiscoveryManager matches specified criteria, or if any known
     * LookupCache instances match criteria
     */
    class SDMWrapper {
        final String sharedName;
        final ServiceDiscoveryManager sdm;
        String[] groups;
        final LookupLocator[] locators;
        final Hashtable<ServiceTemplate, SharedLookupCache> cacheTable = new Hashtable<ServiceTemplate, SharedLookupCache>();
        
        SDMWrapper(final String sharedName, final ServiceDiscoveryManager sdm, final String[] groups, final LookupLocator[] locators) {
            this.sharedName = sharedName;
            this.sdm = sdm;
            if(groups!=null) {
                this.groups = new String[groups.length];
                System.arraycopy(groups, 0, this.groups, 0, this.groups.length);
            }
            this.locators = new LookupLocator[locators.length];
            System.arraycopy(locators, 0, this.locators, 0, this.locators.length);
        }
        
        void removeCache(final SharedLookupCache lCache) {
            ServiceTemplate templateToMatch = lCache.getServiceTemplate();
            for(Enumeration<ServiceTemplate> en=cacheTable.keys(); en.hasMoreElements();) {
                ServiceTemplate template = en.nextElement();
                if(templatesMatch(template, templateToMatch)) {
                    cacheTable.remove(templateToMatch);
                    break;
                }
            }
            logger.trace("removeCache(), cacheTable.size()=={}",cacheTable.size());
            if(cacheTable.size()==0) {
                try {
                    sdm.terminate();
                } catch (IllegalStateException e) {
                    logger.trace("Terminating SDM", e);
                }
                synchronized(pool) {
                    pool.remove(this);
                }
            }
        }
        
        /*
         * See if the sharedNames match
         * 
         * @param name
         *
         * @return true if they do, false otherwise
         */
        boolean namesMatch(final String name) {
            if(sharedName==null && name==null)
                return(true);
            if(sharedName==null)
                return(false);
            if(name == null)
                return (false);
            return(sharedName.equals(name));
        }               
                
        /**
         * See if the groups provided match the groups for the referenced SDM
         * 
         * @param groupsToMatch The groups
         * 
         * @return true if they do, false otherwise
         */
        boolean groupsMatch(final String[] groupsToMatch) {
            /* If both are set to ALL_GROUPS we have a match */
            if(groupsToMatch == LookupDiscovery.ALL_GROUPS && 
               groups == LookupDiscovery.ALL_GROUPS) {
                return(true);
            }
            /* If one or the other is set to ALL_GROUPS return false */
            if(groupsToMatch == LookupDiscovery.ALL_GROUPS || 
               groups == LookupDiscovery.ALL_GROUPS)
                return(false);
            
            /* If both have the same "set", check for equivalence */
            if(groupsToMatch.length == groups.length) {
                int matches=0;
                for(int i=0; i<groupsToMatch.length; i++) {
                    if(groupsToMatch[i].equals(groups[i]))
                        matches++;
                }
                if(matches==groupsToMatch.length) {
                    return(true);
                }
            }
            return(false);
        }        
        
        /**
         * See if the locators provided match the locators for the referenced SDM
         * 
         * @param locatorsToMatch  The locators
         * 
         * @return true if they do. false otherwise
         */
        boolean locatorsMatch(final LookupLocator[] locatorsToMatch) {
            boolean matched=false;
            if(locatorsToMatch == null && locators == null)
                return(true);
            if(locatorsToMatch == null && locators.length==0)
                return(true);
            if(locatorsToMatch!=null && locators!=null) {
                if(locatorsToMatch.length == locators.length) {
                    int matches=0;
                    for(int i=0; i<locatorsToMatch.length; i++) {
                        if(locatorsToMatch[i].equals(locators[i]))
                            matches++;
                    }
                    if(matches==locatorsToMatch.length)
                        matched=true;
                }
            }
            return(matched);
        }
                
        /**
         * Get a LookupCache from the cacheTable for the provided ServiceTemplate.
         * 
         * @param templateToMatch The template
         * @param create If true and a a LookupCache does not exist, create one
         * 
         * @return A SharedLookupCache for the ServiceTemplate
         *
         * @throws IOException If a LookupCache cannot be created
         */
        SharedLookupCache getLookupCache(final ServiceTemplate templateToMatch, final boolean create) throws IOException {
            SharedLookupCache lCache = null;
            for(Enumeration en=cacheTable.keys(); en.hasMoreElements();) {
                ServiceTemplate template = (ServiceTemplate)en.nextElement();
                if(templatesMatch(template, templateToMatch)) {
                    lCache = cacheTable.get(template);
                    break;
                }
            }
            if(lCache==null && create) {
                ServiceItemFilter filter = (sharedName==null?null: new OpStringFilter(sharedName));
                LookupCache lc = sdm.createLookupCache(templateToMatch, filter, null);
                lCache = new SharedLookupCache(lc, templateToMatch, this);
                lCache.setServiceItemFilter(filter);
                cacheTable.put(templateToMatch, lCache);
            }            
            return(lCache);
        }
        
        /**
         * Determine of the ServiceTemplate instances match each other
         *
         * @param st1 ServiceTemplate instance 1
         * @param st2 ServiceTemplate instance 2
         *
         * @return true if they match
         */
        boolean templatesMatch(final ServiceTemplate st1, final ServiceTemplate st2) {
            if(attributesMatch(st1.attributeSetTemplates, st2.attributeSetTemplates) &&
               serviceIDsMatch(st1.serviceID, st2.serviceID) &&
               serviceTypesMatch(st1.serviceTypes, st2.serviceTypes)) {
                return(true);
            }                                      
            return(false);
        }
    }
        
    /*
     * Check if attributes match
     */
    boolean attributesMatch(final Entry[] attr1, final Entry[] attr2) {
        if(attr1==null && attr2==null)
            return(true);
        if(attr1==null || attr2==null)
            return(false);
        return(LookupAttributes.equal(attr1, attr2));
    }
    
    /*
     * Check if service ID match
     */
    boolean serviceIDsMatch(final ServiceID sid1, final ServiceID sid2) {
        if(sid1==null && sid2==null)
            return(true);
        if(sid1==null || sid2==null)
            return(false);
        return(sid1.equals(sid2));
    }
    
    /*
     * Check if service types match
     */
    boolean serviceTypesMatch(final Class[] types1, final Class[] types2) {
        if(types1==null && types2==null)
            return(true);
        if(types1==null || types2==null)
            return(false);
        if(types1.length == types2.length) {            
            int matches = 0;
            for (Class c1 : types1) {
                for (Class c2 : types2) {
                    if (c1.getName().equals(c2.getName()))
                        matches++;
                }
            }
            if(matches==types1.length)
                return(true);
        }
        return(false);
    }    
    
    /**
     * The SharedLookupCache implements a LookupCache and delegates all method
     * invocations to it's LookupDiscoveryManager, and maintains a
     * reference counter for how many clients are sharing the instance. The reference
     * counter is used to determine if the LookupDiscoveryManager should be 
     * terminated. The reference counter is increments each time this instance is 
     * shared, and decremented each time the terminate method is called. If the 
     * reference counter goes to zero upon termination the LookupDiscoveryManager 
     * will be terminated
     */
    public class SharedLookupCache implements LookupCache {
        private final LookupCache lCache;
        private final ServiceTemplate template;
        private final AtomicInteger refCounter = new AtomicInteger();
        private final SDMWrapper sdmWrapper;
        private boolean terminated = false;
        private ServiceItemFilter filter;
        private final List<ServiceDiscoveryListener> localListeners = new ArrayList<ServiceDiscoveryListener>();
        
        public SharedLookupCache(final LookupCache lCache,
                                 final ServiceTemplate template,
                                 final SDMWrapper sdmWrapper) {
            this.lCache = lCache;
            this.template = template;
            this.sdmWrapper = sdmWrapper;
        }                

        void setServiceItemFilter(final ServiceItemFilter filter) {
            this.filter = filter;
        }

        /*
         * Get the ServiceTemplate
         */
        private ServiceTemplate getServiceTemplate() {
            return(template);
        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#lookup(ServiceItemFilter)
         */
        public ServiceItem lookup(final ServiceItemFilter filter) {
            return(lCache.lookup(filter));
        }

        /* (non-Javadoc)
         * @see LookupCache#lookup(ServiceItemFilter, int)
         */
        public ServiceItem[] lookup(final ServiceItemFilter filter, final int maxMatches) {
            return(lCache.lookup(filter, maxMatches));
        }

        /* (non-Javadoc)
         * @see ServiceDiscoveryManager#lookup(ServiceTemplate, int, ServiceItemFilter)
         */
        public ServiceItem[] lookupRemote(final ServiceItemFilter filter, final int maxMatches) {
            return sdmWrapper.sdm.lookup(template, maxMatches, filter);
        }

        /* (non-Javadoc)
         * @see LookupCache#addListener(ServiceDiscoveryListener)
         */
        public synchronized void addListener(final ServiceDiscoveryListener listener) {
            refCounter.incrementAndGet();
            logger.trace("Added LookupCache Listener for template [{}], refCounter: {}",
                         getServiceTemplateAsString(), refCounter.get());
            synchronized(localListeners) {
                localListeners.add(listener);
            }
            lCache.addListener(listener);
        }

        /* (non-Javadoc)
         * @see LookupCache#removeListener(ServiceDiscoveryListener)
         */
        public synchronized void removeListener(final ServiceDiscoveryListener listener) {
            if(!terminated) {
                synchronized(localListeners) {
                    localListeners.remove(listener);
                }
                lCache.removeListener(listener);
                refCounter.decrementAndGet();
            }
            logger.trace("Removed LookupCache Listener for template [{}], refCounter: {}",
                         getServiceTemplateAsString(), refCounter.get());
            logger.trace("lCache={} refCounter={}", lCache.toString(), refCounter.get());
            if(refCounter.get()==0) {
                terminate();
            }
        }

        /* (non-Javadoc)
         * @see LookupCache#discard(Object)
         */
        public void discard(final Object o) {
            logger.trace("Discard {} from LookupCache", o.getClass().getName());
            try {
                lCache.discard(o);
            } catch(IllegalStateException e) {
                logger.warn("Could not discard {}, {}", o.getClass().getName(), e.getMessage());
            }

        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#terminate()
         */
        public synchronized void terminate() {
            if(refCounter.get()==0) {
                logger.trace("Terminating LookupCache for template [{}], refCounter: {}",
                             getServiceTemplateAsString(), refCounter.get());
                terminated = true;
                lCache.terminate();
                sdmWrapper.removeCache(this);
            }
        }

        String getServiceTemplateAsString() {
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            if(template.serviceTypes!=null) {
                for(Class c : template.serviceTypes) {
                    if(sb2.length()>0)
                        sb2.append(", ");
                    sb2.append(c.getName());
                }
            }
            sb1.append("types: [").append(sb2.toString()).append("] ");
            sb2.delete(0, sb2.length());
            if(template.attributeSetTemplates!=null) {
                for(Entry e : template.attributeSetTemplates) {
                    if(sb2.length()>0)
                        sb2.append(", ");
                    if(e instanceof Name)
                        sb2.append("Name: ").append(((Name)e).name);
                    else
                        sb2.append(e.getClass().getName());
                }
            }
            sb1.append("attributes: [").append(sb2.toString()).append("] ");
            return sb1.toString();

        }

        void notifyOnLocalAdd(final ServiceItem item) {
            boolean cleared = true;
            if(filter!=null) {
                cleared = filter.check(item);
            }
            if(cleared) {
                ServiceDiscoveryListener[] listeners;
                synchronized(localListeners) {
                    listeners = localListeners.toArray(new ServiceDiscoveryListener[localListeners.size()]);
                }
                for(ServiceDiscoveryListener l : listeners) {
                    l.serviceAdded(new ServiceDiscoveryEvent(this, null, item));
                }
            }
        }
    }

    class ContainerListener implements ServiceBeanContainerListener {
        private final ServiceBeanContainer container;
        private final List<ServiceRecord> notified = new ArrayList<ServiceRecord>();

        ContainerListener(final ServiceBeanContainer container) {
            this.container = container;
            container.addListener(this);
        }

        private ServiceBeanInstance getServiceBeanInstance(final ServiceRecord r) {
            ServiceBeanDelegate delegate = container.getServiceBeanDelegate(r.getServiceID());
            return delegate.getServiceBeanInstance();
        }

        List<Class<?>> getAllInterfaces(Class<?> classObject) {
            List<Class<?>> list = new ArrayList<Class<?>>();
            if(classObject.getName().equals(Object.class.getName()))
                return list;
            list.addAll(getAllInterfaces(classObject.getSuperclass()));
            Collections.addAll(list, classObject.getInterfaces());
            return list;
        }

        public void serviceInstantiated(final ServiceRecord record) {
            try {
                ServiceBeanInstance instance = getServiceBeanInstance(record);
                Class<?> proxyClass = instance.getService().getClass();
                Class<?> theInterfaceClass = null;
                for(Class<?> interfaceClass : getAllInterfaces(proxyClass)) {
                    for(ClassBundle cb : record.getServiceElement().getExportBundles()) {
                        if(interfaceClass.getName().equals(cb.getClassName())) {
                            theInterfaceClass = interfaceClass;
                            break;
                        }
                    }
                    if(theInterfaceClass!=null)
                        break;
                }
                if(theInterfaceClass==null) {
                    logger.warn("No matching interface class found for {}, defaulting to {}",
                                record.getServiceElement().getName(), Service.class.getName());
                    theInterfaceClass = Service.class;
                }
                logger.trace("[{}] selected: {}", record.getServiceElement().getName(), theInterfaceClass.getName());
                ServiceTemplate templateToMatch = JiniClient.getServiceTemplate(record.getServiceElement(),
                                                                                theInterfaceClass);
                SDMWrapper[] sdms = getSDMWrappers();
                for(SDMWrapper sdm : sdms) {
                    SharedLookupCache lCache = sdm.getLookupCache(templateToMatch, false);
                    if(lCache!=null) {
                        boolean alreadyNotified;
                        synchronized(notified) {
                            alreadyNotified = notified.contains(record);
                        }
                        if(!alreadyNotified) {
                            logger.trace("Notify listeners of local instantiation of {} {}",
                                         record.getServiceElement().getName(), theInterfaceClass.getName());
                            lCache.notifyOnLocalAdd(makeServiceItem(instance));
                            synchronized(notified) {
                                notified.add(record);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Unable to load service interface", e);
            }
        }

        public void serviceDiscarded(final ServiceRecord record) {
            synchronized(notified) {
                notified.remove(record);
            }
        }

        private ServiceItem makeServiceItem(final ServiceBeanInstance instance) throws IOException,
                                                                                       ClassNotFoundException {
            Uuid uuid = instance.getServiceBeanID();
            ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
            Entry[] attrs = null;
            Object service = instance.getService();
            if(service instanceof Administrable) {
                try {
                    Object admin = ((Administrable)service).getAdmin();
                    if(admin instanceof JoinAdmin) {
                        attrs = ((JoinAdmin)admin).getLookupAttributes();
                    }
                } catch(RemoteException e) {
                    logger.warn("Getting attributes from [{}]", instance.getServiceBeanConfig().getName(), e);
                }
            }
            return(new ServiceItem(serviceID, service, attrs));
        }
    }
}
