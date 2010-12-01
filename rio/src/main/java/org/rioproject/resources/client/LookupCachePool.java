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
package org.rioproject.resources.client;

import com.sun.jini.config.Config;
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
import org.rioproject.core.ServiceBeanInstance;
import org.rioproject.core.provision.ServiceRecord;
import org.rioproject.cybernode.ServiceBeanContainer;
import org.rioproject.cybernode.ServiceBeanContainerListener;
import org.rioproject.cybernode.ServiceBeanDelegate;
import org.rioproject.opstring.OpStringFilter;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The LookupCachePool class provides the support to get an existing 
 * LookupCache from a pool of created LookupCache instances. Criteria for 
 * determining LookupCache matching is based on ServiceTemplate matching
 *
 * @author Dennis Reedy
 */
public class LookupCachePool {
    private final List<SDMWrapper> pool = new ArrayList<SDMWrapper>();
    private Timer taskTimer = new Timer(true);
    private static final long DEFAULT_CACHE_TIMEOUT=1000*10;
    private long cacheTimeout;
    private ServiceBeanContainerListener containerListener;
    private static final String COMPONENT = LookupCachePool.class.getName();
    private static final Logger logger = Logger.getLogger(COMPONENT);
    private static Configuration config;
    private static LookupCachePool singleton = new LookupCachePool();

    private LookupCachePool() {
        if(logger.isLoggable(Level.FINE))
            logger.fine("Create new LookupCachePool");        
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
    public void setServiceBeanContainer(ServiceBeanContainer container) {
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
    public void setConfiguration(Configuration conf) {
        config = conf;
        if(logger.isLoggable(Level.FINE)) {
            if(config==null)
                logger.fine("Set null configuration for LookupCachePool");
            else
                logger.log(Level.FINE,
                           "Set configuration for LookupCachePool {0}",
                           new Object[] {config});
        } 
        try {
            cacheTimeout = Config.getLongEntry(conf, 
                                               COMPONENT, 
                                               "cacheTimeout", 
                                               DEFAULT_CACHE_TIMEOUT, 
                                               1000*5, 
                                               Long.MAX_VALUE);
                                               
        } catch(Exception e) {
            logger.log(Level.WARNING, "getting cacheTimeout", e);
            cacheTimeout = DEFAULT_CACHE_TIMEOUT;
        }        
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
     * @throws ConfigurationException If the Jini configuration cannot be used
     */
    public LookupCache getLookupCache(DiscoveryManagement dMgr,
                                      ServiceTemplate template) 
    throws IOException, ConfigurationException {
        if(!(dMgr instanceof DiscoveryManagementPool.SharedDiscoveryManager)) {
            logger.warning("The DiscoveryManagement instance pass was not " +
                           "created by the "+
                           DiscoveryManagementPool.class.getName()+", " +
                           "returning null");
            return(null);
        }
        DiscoveryManagementPool.SharedDiscoveryManager sharedDM = 
            (DiscoveryManagementPool.SharedDiscoveryManager)dMgr;        
        return(getLookupCache(sharedDM.getSharedName(),
                              sharedDM.getGroups(), 
                              sharedDM.getLocators(), 
                              template));
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
     * @throws IOException If discovery mangement cannot be created
     * @throws ConfigurationException If the Jini configuration cannot be used
     */
    public LookupCache getLookupCache(String sharedName, 
                                      String[] groups,
                                      LookupLocator[] locators,
                                      ServiceTemplate template) 
    throws IOException, ConfigurationException {
        
        if(template==null)
            throw new NullPointerException("template is null");
        SDMWrapper sdmWrapper = getSDMWrapper(sharedName,  
                                              groups, 
                                              locators);        
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
        if(taskTimer!=null)
            taskTimer.cancel();
        taskTimer = null;
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
    private SDMWrapper getSDMWrapper(String sharedName,
                                     String[] groupsToMatch, 
                                     LookupLocator[] locatorsToMatch) 
    throws IOException, ConfigurationException {
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
                new ServiceDiscoveryManager(
                        DiscoveryManagementPool.getInstance().
                                                getDiscoveryManager(sharedName,
                                                                    groupsToMatch, 
                                                                    locatorsToMatch),
                        new LeaseRenewalManager(config), 
                        config);
                    
            sdmWrapper = new SDMWrapper(sharedName, 
                                        sdm, 
                                        groupsToMatch, 
                                        locatorsToMatch);
            synchronized(pool) {
                pool.add(sdmWrapper);
            }
        }
        return(sdmWrapper);
    }    
    
    /*
     * Schedule a CacheTimeout to chec for an unues SharedLookupCache
     */
    void scheduleCacheTimeout(SharedLookupCache cache) {
        long now = System.currentTimeMillis();
        taskTimer.schedule(new CacheTimeout(cache), new Date(now+cacheTimeout));
    }
    
    /**
     * The SDMWrapper class provides a wrapper around a known ServiceDiscoveryManager,
     * the lookupCache instances that this utility has created using the
     * ServiceDiscoveryManager and checks to see if the referenced 
     * ServiceDsicoveryManager matches specified criteria, or if any known 
     * LookupCache instances match criteria
     */
    class SDMWrapper {
        String sharedName;
        ServiceDiscoveryManager sdm;
        String[] groups;
        LookupLocator[] locators;
        Hashtable<ServiceTemplate, SharedLookupCache> cacheTable =
            new Hashtable<ServiceTemplate, SharedLookupCache>();
        
        SDMWrapper(String sharedName,
                   ServiceDiscoveryManager sdm, 
                   String[] groups, 
                   LookupLocator[] locators) {
            this.sharedName = sharedName;
            this.sdm = sdm;
            if(groups!=null) {
                this.groups = new String[groups.length];
                System.arraycopy(groups, 0, this.groups, 0, this.groups.length);
            }
            this.locators = new LookupLocator[locators.length];
            System.arraycopy(locators, 0, this.locators, 0, this.locators.length);
        }
        
        void removeCache(SharedLookupCache lCache) {            
            ServiceTemplate templateToMatch = lCache.getServiceTemplate();
            for(Enumeration<ServiceTemplate> en=cacheTable.keys(); en.hasMoreElements();) {
                ServiceTemplate template = en.nextElement();
                if(templatesMatch(template, templateToMatch)) {
                    cacheTable.remove(templateToMatch);
                    break;
                }
            }
            if(logger.isLoggable(Level.FINEST))
                logger.finest("removeCache(), "+
                              "cacheTable.size()=="+cacheTable.size());
            if(cacheTable.size()==0) {
                try {
                    sdm.terminate();
                } catch (IllegalStateException e) {
                    if(logger.isLoggable(Level.FINEST))
                        logger.log(Level.FINEST, 
                                   "Terminating SDM", 
                                   e);
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
        boolean namesMatch(String name) {
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
        boolean groupsMatch(String[] groupsToMatch) {
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
        boolean locatorsMatch(LookupLocator[] locatorsToMatch) {
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
        SharedLookupCache getLookupCache(ServiceTemplate templateToMatch,
                                         boolean create)
        throws IOException {
            SharedLookupCache lCache = null;
            for(Enumeration en=cacheTable.keys(); en.hasMoreElements();) {
                ServiceTemplate template = (ServiceTemplate)en.nextElement();
                if(templatesMatch(template, templateToMatch)) {
                    lCache = cacheTable.get(template);
                    break;
                }
            }
            if(lCache==null && create) {
                ServiceItemFilter filter = (sharedName==null?null:
                                            new OpStringFilter(sharedName));
                LookupCache lc = 
                    sdm.createLookupCache(templateToMatch, 
                                          filter,
                                          null);
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
        boolean templatesMatch(ServiceTemplate st1, ServiceTemplate st2) {                        
            if(attributesMatch(st1.attributeSetTemplates, 
                               st2.attributeSetTemplates) &&                
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
    boolean attributesMatch(Entry[] attr1, Entry[] attr2) {
        if(attr1==null && attr2==null)
            return(true);
        if(attr1==null || attr2==null)
            return(false);
        return(LookupAttributes.equal(attr1, attr2));
    }
    
    /*
     * Check if service ID match
     */
    boolean serviceIDsMatch(ServiceID sid1, ServiceID sid2) {
        if(sid1==null && sid2==null)
            return(true);
        if(sid1==null || sid2==null)
            return(false);
        return(sid1.equals(sid2));
    }
    
    /*
     * Check if service types match
     */
    boolean serviceTypesMatch(Class[] types1, Class[] types2) {
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
        private LookupCache lCache;
        private ServiceTemplate template;
        private int refCounter;
        private SDMWrapper sdmWrapper;
        private boolean terminated = false;
        private ServiceItemFilter filter;
        private final List<ServiceDiscoveryListener> localListeners =
            new ArrayList<ServiceDiscoveryListener>();
        
        public SharedLookupCache(LookupCache lCache, 
                                 ServiceTemplate template, 
                                 SDMWrapper sdmWrapper) {
            this.lCache = lCache;
            this.template = template;
            this.sdmWrapper = sdmWrapper;
        }                

        void setServiceItemFilter(ServiceItemFilter filter) {
            this.filter = filter;
        }

        /*
         * Get the ServiceTemplate
         */
        private ServiceTemplate getServiceTemplate() {
            return(template);
        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#lookup(net.jini.lookup.ServiceItemFilter)
         */
        public ServiceItem lookup(ServiceItemFilter filter) {
            return(lCache.lookup(filter));
        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#lookup(net.jini.lookup.ServiceItemFilter, int)
         */
        public ServiceItem[] lookup(ServiceItemFilter filter, int maxMatches) {
            return(lCache.lookup(filter, maxMatches));
        }

        /* (non-Javadoc)
         * @see net.jini.lookup.ServiceDiscoveryManager#lookup(net.jini.core.lookup.ServiceTemplate, int, net.jini.lookup.ServiceItemFilter)
         */
        public ServiceItem[] lookupRemote(ServiceItemFilter filter, int maxMatches) {
            return sdmWrapper.sdm.lookup(template, maxMatches, filter);
        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#addListener(net.jini.lookup.ServiceDiscoveryListener)
         */
        public void addListener(ServiceDiscoveryListener listener) {
            synchronized(this) {
                refCounter++;
                if(logger.isLoggable(Level.FINEST))
                    logger.finest("LookupCache refCounter : "+refCounter);
            }
            synchronized(localListeners) {
                localListeners.add(listener);
            }
            lCache.addListener(listener);
        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#removeListener(net.jini.lookup.ServiceDiscoveryListener)
         */
        public void removeListener(ServiceDiscoveryListener listener) {
            synchronized(this) {
                if(!terminated) {
                    localListeners.remove(listener);
                    lCache.removeListener(listener);
                    refCounter--;
                }
                if(logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST,
                               "LCache={0} refCounter={1}",
                               new Object[] {lCache.toString(),
                                             Integer.toString(refCounter)});
                if(refCounter==0) {
                    scheduleCacheTimeout(this);
                }
            }
        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#discard(java.lang.Object)
         */
        public void discard(Object o) {
            if(logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST,
                               "Discard "+o.getClass().getName()+" " +
                               "from LookupCache");
            lCache.discard(o);

        }

        /* (non-Javadoc)
         * @see net.jini.lookup.LookupCache#terminate()
         */
        public void terminate() {
            if(refCounter==0) {
                if(logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST,
                               "LCache={0} terminated",
                               new Object[] {lCache.toString()});
                synchronized(this) {
                    terminated = true;
                    lCache.terminate();
                }
                sdmWrapper.removeCache(this);
            }
        }

        void notifyOnLocalAdd(ServiceItem item) {
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
    
    /**
     * Scheduled task to check for a SharedLookupCache that is not being used.
     */
    static class CacheTimeout extends TimerTask {
        SharedLookupCache cache;
        
        CacheTimeout(SharedLookupCache cache) {
            this.cache = cache;
        }

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        public void run() {
            cache.terminate();
        }
        
    }

    class ContainerListener implements ServiceBeanContainerListener {
        ServiceBeanContainer container;
        private final List<ServiceRecord> notified = new ArrayList<ServiceRecord>();

        ContainerListener(ServiceBeanContainer container) {
            this.container = container;
            container.addListener(this);
        }

        public void serviceInstantiated(ServiceRecord r) {
            try {
                for(ServiceRecord record : container.getServiceRecords()) {
                    ServiceTemplate templateToMatch = JiniClient.getServiceTemplate(record.getServiceElement());
                    ServiceBeanDelegate delegate = container.getServiceBeanDelegate(record.getServiceID());
                    ServiceBeanInstance instance = delegate.getServiceBeanInstance();
                    SDMWrapper[] sdms = getSDMWrappers();
                    for(SDMWrapper sdm : sdms) {
                        SharedLookupCache lCache =
                            sdm.getLookupCache(templateToMatch, false);
                        if(lCache!=null) {
                            boolean alreadyNotified;
                            synchronized(notified) {
                                alreadyNotified = notified.contains(record);
                            }
                            if(!alreadyNotified) {
                                lCache.notifyOnLocalAdd(makeServiceItem(instance));
                                synchronized(notified) {
                                    notified.add(record);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void serviceDiscarded(ServiceRecord record) {
            synchronized(notified) {
                notified.remove(record);
            }
        }

        private ServiceItem makeServiceItem(ServiceBeanInstance instance)
            throws IOException, ClassNotFoundException {
            Uuid uuid = instance.getServiceBeanID();
            ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(),
                                                uuid.getLeastSignificantBits());
            Entry[] attrs = null;
            Object service = instance.getService();
            if(service instanceof Administrable) {
                try {
                    Object admin = ((Administrable)service).getAdmin();
                    if(admin instanceof JoinAdmin) {
                        attrs = ((JoinAdmin)admin).getLookupAttributes();
                    }
                } catch(RemoteException e) {
                    logger.log(Level.WARNING,
                               "Getting attributes from " +
                               "["+instance.getServiceBeanConfig().getName()+"]",
                               e);
                }
            }
            return(new ServiceItem(serviceID, service, attrs));
        }
    }
}
