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

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscovery;
import net.jini.discovery.LookupDiscoveryManager;
import org.rioproject.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A strategy has been taken in an attempt to conserve resources in a multi-service 
 * JVM environment as it relates to DiscoveryManagement. The DiscoveryManagementPool 
 * class provides an approach that enables a caller to get an existing 
 * DiscoveryManagement instance from a pool of previously created 
 * DiscoveryManagement instances, instead of creating a new DiscoveryManagement 
 * instance, and going through the overhead of DiscoveryManagement creation and 
 * performing discovery each time a service is initialized.
 * <p>
 * The criteria for pooling of created DiscoveryManagement instances is to match 
 * groups, locators and a shared name. The default behavior for ServiceBeans is to 
 * use the OperationalString name as the shared name. Therefore all services 
 * instantiated in the same Cybernode, that are in the same OperationalString, 
 * which have the same DiscoveryManagement groups and locators, will share the 
 * same DiscoveryManagement instance.
 * <p>
 * Of course use of shared DiscoveryManagement instances must be used with care 
 * as changing the settings of a shared DiscoveryManagement instance may present 
 * problems for other users of that DiscoveryManagement instance.
 * <p>
 * As a deployer, if you want DiscoveryManagement sharing turned off for services, 
 * declaring the following will turn it off:
 * <br><br>
 * <tt>
 * &lt;ServiceBean Name="Does not share well" DiscoveryManagementPooling="no"&gt;
 * </tt>
 *
 * @author Dennis Reedy
 */
public class DiscoveryManagementPool {
    private final List<DiscoveryControl> pool = new ArrayList<DiscoveryControl>();
    private final static String COMPONENT = DiscoveryManagementPool.class.getName();
    private static Logger logger = LoggerFactory.getLogger(COMPONENT);
    private Configuration defaultConfig;
    private static DiscoveryManagementPool singleton = new DiscoveryManagementPool();

    private DiscoveryManagementPool() {
        if(logger.isDebugEnabled())
            logger.debug("Create new DiscoveryManagementPool");
    }

    /**
     * Get the singleton instance of the DiscoveryManagementPool
     *
     * @return The singleton DiscoveryManagementPool instance
     */
    public static synchronized DiscoveryManagementPool getInstance() {        
        return(singleton);
    }
    
    /**
     * Set the Configuration property
     * 
     * @param configuration The Configuration to use when creating
     * DiscoveryManagementPool instances
     */
    public void setConfiguration(Configuration configuration) {
        defaultConfig = configuration;
        if(logger.isDebugEnabled()) {
            if(defaultConfig==null)
                logger.debug("Set null configuration for DiscoveryManagementPool");
            else
                logger.debug("Set configuration for DiscoveryManagementPool {}", defaultConfig.toString());
        } 
    }

    /**
     * This method will return an instance of <code>DiscoveryManagement</code>
     * based on matching the shared name, groups and locators as criteria. If there
     * is an existing instance of <code>DiscoveryManagement</code> instantiated by 
     * this utility, that instance will be returned. Otherwise a new
     * <code>DiscoveryManagement</code> instance will be created and returned
     * 
     * <p>Note: Use of returned DiscoveryManagement
     * instances must be used with care as changing the settings of the returned
     * DiscoveryManagement instance may present problems for other users of
     * DiscoveryManagement instance and is not advised.
     * 
     * @param sharedName The name the DiscoveryManagement instances are shared across
     *
     * @return A DiscoveryManagement object suitable for lookup discovery
     * management using the following system properties:
     * <ul>
     * <li>org.rioproject.groups: a comma separated list of groups to use. If this
     * property is not found LookupDiscoveryGroups will be set to
     * LookupDiscovery.NO_GROUPS. Additionally, the value of "all" will be set
     * to LookupDiscovery.ALL_GROUPS
     * <li>org.rioproject.locators: a comma separated list of
     * LookupLocator formatted URLs
     *
     * @throws IOException If the DiscoveryManagement instance cannot be created
     */
    public DiscoveryManagement getDiscoveryManager(String sharedName) throws IOException {
        String[] groups = JiniClient.parseGroups(System.getProperty(Constants.GROUPS_PROPERTY_NAME));
        LookupLocator[] locators = JiniClient.parseLocators(System.getProperty(Constants.LOCATOR_PROPERTY_NAME));
        return(getDiscoveryManager(sharedName, groups, locators, null, defaultConfig));
    }

    /**
     * This method will return an instance of <code>DiscoveryManagement</code>
     * based on matching the shared name, groups and locators as criteria. If there
     * is an existing instance of <code>DiscoveryManagement</code> instantiated by
     * this utility, that instance will be returned. Otherwise a new
     * <code>DiscoveryManagement</code> instance will be created and returned
     *
     * <p>Note: Use of returned DiscoveryManagement
     * instances must be used with care as changing the settings of the returned
     * DiscoveryManagement instance may present problems for other users of
     * DiscoveryManagement instance and is not advised.
     *
     * @param sharedName The name the DiscoveryManagement instances are shared across
     * @param groups An array of String objects indicating the Jini Lookup
     * Service groups to discover
     * @param locators An array of LookupLocator objects indicating specific
     * Jini Lookup Service instances to discover
     * @return A DiscoveryManagement object suitable for lookup discovery
     * management based on provided parameters
     *
     * @throws IOException If the DiscoveryManagement instance cannot be created
     */
    public DiscoveryManagement getDiscoveryManager(String sharedName,
                                                   String[] groups,
                                                   LookupLocator[] locators) throws IOException {
        return(getDiscoveryManager(sharedName, groups, locators, null, defaultConfig));
    }

    /**
     * This method will return an instance of <code>DiscoveryManagement</code>
     * based on matching the shared name, groups and locators as criteria. If there
     * is an existing instance of <code>DiscoveryManagement</code> instantiated by 
     * this utility, that instance will be returned. Otherwise a new
     * <code>DiscoveryManagement</code> instance will be created and returned
     * 
     * <p>Note: Use of returned DiscoveryManagement
     * instances must be used with care as changing the settings of the returned
     * DiscoveryManagement instance may present problems for other users of
     * DiscoveryManagement instance and is not advised.
     *
     * @param sharedName The name the DiscoveryManagement instances are shared across
     * @param groups An array of String objects indicating the Jini Lookup Service 
     * groups to discover
     * @param locators An array of LookupLocator objects indicating specific Jini 
     * Lookup Service instances to discover
     * @param listener A instance of a DiscoveryListener which will be notified 
     * when targeted Jini Lookup Services are discovered or discarded. If this 
     * parameter is not null, then it will be added to either the created or existing 
     * <code>DiscoveryManagement</code> instance 
     * @param config If a DiscoveryManagement instance needs to be created, the 
     * Configuration to use when creating the DiscoveryManagement instance 
     *
     * @return  A DiscoveryManagement object suitable for lookup 
     * discovery management based on provided parameters
     *
     * @throws IOException If the DiscoveryManagement instance cannot be created
     */
    public DiscoveryManagement getDiscoveryManager(String sharedName,             
                                                   String[] groups, 
                                                   LookupLocator[] locators, 
                                                   DiscoveryListener listener,
                                                   Configuration config) throws IOException {
        
        LookupDiscoveryManager ldm  ;
        synchronized(this) {
            DiscoveryControl discoControl = getDiscoveryControl(sharedName);
            if(discoControl==null) {
                discoControl = new DiscoveryControl(sharedName);
                pool.add(discoControl);
                if(logger.isDebugEnabled()) 
                    logger.debug("Create new DiscoveryControl for [{}]", sharedName);
            } else {
                if(logger.isDebugEnabled()) 
                    logger.debug("DiscoveryControl obtained for [{}]", sharedName);
            }
            ldm = discoControl.getLookupDiscoveryManager(groups, locators);        
            if(ldm==null) {
                ldm = discoControl.createLookupDiscoveryManager(groups, locators, listener, config);
            } else {
                ((SharedDiscoveryManager)ldm).incrementRefCounter();
                if(listener!=null)
                    ldm.addDiscoveryListener(listener);
            }
        }
        return(ldm);
    }

    /**
     * For all DiscoveryManagement instances this utility has created, terminate 
     * them and set the singleton instance to null;
     */
    public void terminate() {
        for(DiscoveryControl dc : pool) {
            dc.terminate();
        }
        pool.clear();
        singleton = null;
    }
    
    /*
     * Get a DiscoveryControl instance
     */    
    private DiscoveryControl getDiscoveryControl(String name) {
        DiscoveryControl discoControl=null;
        synchronized(pool) {
            for(DiscoveryControl dc : pool) {
                if(dc.namesMatch(name)) {
                    discoControl = dc;
                    break;
                }
            }
        }
        return(discoControl);
    }           
    
    /**
     * Maintains a collection of LookupDiscoveryManager instances
     */
    public static class DiscoveryControl {
        String sharedName;
        final List<SharedDiscoveryManager> pool = Collections.synchronizedList(new ArrayList<SharedDiscoveryManager>());
        
        DiscoveryControl(String sharedName) {
            this.sharedName = sharedName;
        }

        /*
         * Determine if names match
         */
        boolean namesMatch(String name) {
            if(sharedName == null && name == null)
                return (true);
            if(sharedName == null)
                return (false);
            if(name == null)
                return (false);
            return(sharedName.equals(name));
        }
        
        String getSharedName() {
            return(sharedName);
        }
        
        /*
         * Add a LookupDiscoveryManager instance to the pool
         */
        void addLookupDiscoveryManager(SharedDiscoveryManager ldm) {
            if(pool!=null)
                pool.add(ldm);
        }
        
        /*
         * Remove a LookupDiscoveryManager instance from the pool
         */
        void removeLookupDiscoveryManager(SharedDiscoveryManager ldm) {
            if(pool!=null)
                pool.remove(ldm);
        }
     
        /*
         * Create a LookupDiscoveryManager instance
         */
        LookupDiscoveryManager createLookupDiscoveryManager(String[] groups,
                                                            LookupLocator[] locators,
                                                            DiscoveryListener listener,
                                                            Configuration config) throws IOException {
            if(logger.isDebugEnabled()) {
                StringBuilder buffer = new StringBuilder();
                if(groups==null) {
                    buffer.append("Create new SharedDiscoveryManager for ALL_GROUPS");
                } else {
                    buffer.append("Create new SharedDiscoveryManager for groups [");
                    for(int i=0; i<groups.length; i++) {
                        if(i>0)
                            buffer.append(", ");
                        if(groups[i].equals(""))
                            buffer.append("[public]");
                        else
                            buffer.append(groups[i]);
                    }
                    buffer.append("], ");
                }
                buffer.append("shared name [").append(sharedName).append("], ");
                if(config == null) {
                    buffer.append("using null config");
                    logger.debug(buffer.toString());
                } else {
                    buffer.append("using config {}");
                    logger.warn(buffer.toString(), config.toString());
                }
            }

            SharedDiscoveryManager ldm;
            Configuration configToUse = (config==null?EmptyConfiguration.INSTANCE:config);
            String className = SharedDiscoveryManager.class.getName();
            try {
                className = (String)configToUse.getEntry(COMPONENT,
                                                    "sharedDiscoveryManager",
                                                    String.class,
                                                    SharedDiscoveryManager.class.getName());
            } catch (ConfigurationException e) {
                logger.warn("Error obtaining the "+COMPONENT+".sharedDiscoveryManager property, defaulting to "+className, e);
            }

            try {
                ldm = createSharedDiscoveryManager(className, groups, locators, listener, configToUse);
            } catch (ConfigurationException e) {
                logger.warn("Creating SharedDiscoveryManager with Configuration", e);
                ldm = createSharedDiscoveryManager(className, groups, locators, listener);
            } catch (IOException e) {
                logger.warn("Could not create SharedDiscoveryManager", e);
                throw e;
            }
            if(ldm!=null)
                ldm.incrementRefCounter();
            addLookupDiscoveryManager(ldm);
            return(ldm);
        }

        private SharedDiscoveryManager createSharedDiscoveryManager(String classname,
                                                                    String[] groups,
                                                                    LookupLocator[] locators,
                                                                    DiscoveryListener listener)
        throws IOException {
            ClassLoader cL = getClass().getClassLoader();
            SharedDiscoveryManager ldm = null;
            Throwable thrown = null;
            try {
                Class ldmClass = cL.loadClass(classname);

                Constructor cons = ldmClass.getConstructor(DiscoveryControl.class,
                                                           String[].class,
                                                           LookupLocator[].class,
                                                           DiscoveryListener.class);
                ldm = (SharedDiscoveryManager)cons.newInstance(this,
                                                               groups,
                                                               locators,
                                                               listener);
            } catch (ClassNotFoundException e) {
                thrown = e;
            } catch (IllegalAccessException e) {
                thrown = e;
            } catch (NoSuchMethodException e) {
                thrown = e;
            } catch (InvocationTargetException e) {
                thrown = e;
            } catch (InstantiationException e) {
                thrown = e;
            }
            if(thrown!=null) {
                logger.warn("Could not create "+classname+", defaulting to "+SharedDiscoveryManager.class.getName(), thrown);
                ldm = new SharedDiscoveryManager(this, groups, locators, listener);
            }
            return ldm;
        }

        private SharedDiscoveryManager createSharedDiscoveryManager(String classname,
                                                                    String[] groups,
                                                                    LookupLocator[] locators,
                                                                    DiscoveryListener listener,
                                                                    Configuration config)
        throws IOException, ConfigurationException {
            ClassLoader cL = Thread.currentThread().getContextClassLoader();
            SharedDiscoveryManager ldm = null;
            Throwable thrown = null;
            try {
                Class ldmClass = cL.loadClass(classname);

                Constructor cons = ldmClass.getConstructor(DiscoveryControl.class,
                                                           String[].class,
                                                           LookupLocator[].class,
                                                           DiscoveryListener.class,
                                                           Configuration.class);
                ldm = (SharedDiscoveryManager)cons.newInstance(this, groups, locators, listener, config);
            } catch (ClassNotFoundException e) {
                thrown = e;
            } catch (IllegalAccessException e) {
                thrown = e;
            } catch (NoSuchMethodException e) {
                thrown = e;
            } catch (InvocationTargetException e) {
                thrown = e;
            } catch (InstantiationException e) {
                thrown = e;
            }
            if(thrown!=null) {
                logger.warn("Could not create "+classname+", defaulting to "+SharedDiscoveryManager.class.getName(), thrown);
                ldm = new SharedDiscoveryManager(this, groups, locators, listener, config);
            }
            return ldm;
        }
        
        /**
         * Terminate all LookupDiscoveryManager instances
         */
        void terminate() {
            SharedDiscoveryManager[] dms;
            synchronized(pool) {
                dms = pool.toArray(new SharedDiscoveryManager[pool.size()]);                
            }
            for(DiscoveryManagement dm : dms) {
                dm.terminate();
            }
            pool.clear();
        }
        
        /*
         * Get a LookupDiscoveryManager instances based on the groups and locators
         * If a match cannot be found return null
         */
        LookupDiscoveryManager getLookupDiscoveryManager(String[] groupsToMatch, LookupLocator[] locatorsToMatch) {
            SharedDiscoveryManager[] dms;
            synchronized(pool) {
                dms = pool.toArray(new SharedDiscoveryManager[pool.size()]);
            }
            for(LookupDiscoveryManager ldm : dms) {
                String[] groups = ldm.getGroups();
                /* If both are set to ALL_GROUPS we have a match */
                if(groupsToMatch == LookupDiscovery.ALL_GROUPS && groups == LookupDiscovery.ALL_GROUPS) {
                    if(matchLocators(locatorsToMatch, ldm)) {
                        return(ldm);
                    }
                }
                /* If one or the other is set to ALL_GROUPS keep looking */
                if(groupsToMatch == LookupDiscovery.ALL_GROUPS || groups == LookupDiscovery.ALL_GROUPS)
                    continue;
                /* If both have the same "set", check for equivalence */
                if(groupsToMatch.length == groups.length) {
                    int matches=0;
                    for(int i=0; i<groupsToMatch.length; i++) {
                        if(groupsToMatch[i].equals(groups[i]))
                            matches++;
                    }
                    if(matches==groupsToMatch.length) {
                        if(matchLocators(locatorsToMatch, ldm)) {
                            return(ldm);
                        }
                    }
                }
            }
            return(null);
        }
        
        /*
         * Determine if the locators match
         */
        boolean matchLocators(LookupLocator[] locatorsToMatch, LookupDiscoveryManager ldm) {
            boolean matched=false;
            LookupLocator[] locators = ldm.getLocators();
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
    }

    /**
     * The SharedDiscoveryManager extends LookupDiscoveryManager and maintains a
     * reference counter for how many clients are sharing the instance. The reference
     * counter is used to determine if the LookupDiscoveryManager should be 
     * terminated. The reference counter is incremented each time this instance is
     * shared, and decremented each time the terminate method is called. If the 
     * reference counter goes to zero upon termination the LookupDiscoveryManager 
     * will be terminated
     */
    public static class SharedDiscoveryManager extends LookupDiscoveryManager {
        private AtomicInteger refCounter = new AtomicInteger();
        private DiscoveryControl discoControl;
        
        public SharedDiscoveryManager(DiscoveryControl discoControl,
                                      String[] groups,
                                      LookupLocator[] locators,
                                      DiscoveryListener listener) throws IOException {
            super(groups, locators, listener);
            this.discoControl = discoControl;
        }
        
        public SharedDiscoveryManager(DiscoveryControl discoControl,
                                      String[] groups,  
                                      LookupLocator[] locators,
                                      DiscoveryListener listener,
                                      Configuration config) throws IOException, ConfigurationException {
            super(groups, locators, listener, config);
            this.discoControl = discoControl;
        }

        /*
         * Get the sharedName property
         */
        public String getSharedName() {
            return(discoControl.getSharedName());
        }
               
        /*
         * Increment the references
         */
        synchronized void incrementRefCounter() {
            refCounter.incrementAndGet();
        }

        
        /**
         * Override parent's terminate method. Only call 
         * LookupDiscoveryManager.terminate() if there are no clients or users.
         */
        public void terminate() {            
            synchronized(this) {
                int counter = refCounter.decrementAndGet();
                if(counter==0) {
                    super.terminate();
                    discoControl.removeLookupDiscoveryManager(this);
                }
            }
        }
    }           
}
