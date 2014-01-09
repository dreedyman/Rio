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
package org.rioproject.impl.servicebean;

import com.sun.jini.admin.DestroyAdmin;
import com.sun.jini.start.LifeCycle;
import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.discovery.LookupLocator;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.admin.ServiceBeanControlException;
import org.rioproject.util.MulticastStatus;
import org.rioproject.config.Constants;
import org.rioproject.impl.container.DiscardManager;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.impl.container.ServiceAdvertiser;
import org.rioproject.deploy.SystemRequirements;
import org.rioproject.loader.CommonClassLoader;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.log.LoggerConfig;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.impl.client.DiscoveryManagementPool;
import org.rioproject.impl.client.LookupCachePool;
import org.rioproject.impl.service.Destroyer;
import org.rioproject.rmi.RegistryUtil;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The ServiceBeanActivation class is a utility that provides bootstrap support
 * for a ServiceBean
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class ServiceBeanActivation {
    static final String COMPONENT = ServiceBeanActivation.class.getPackage().getName();
    public static final String QOS_COMPONENT = Constants.BASE_COMPONENT+".system";
    public static final String BOOT_COMPONENT = Constants.BASE_COMPONENT+".boot";
    public static final String BOOT_CONFIG_COMPONENT = BOOT_COMPONENT+".configComponent";
    static LifeCycleManager sbLifeCycleManager;
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);
    static final String BOOT_COOKIE = "boot-cookie";

    /**
     * Create a ServiceBeanContext from the Configuration element.
     *
     * @param configComponent The configuration component name of the
     * ServiceBean to use when obtaining configuration attributes. Must not be null.
     * @param defaultServiceName The default name of the service. Must not be null.
     * @param configArgs Configuration for the ServiceBean. Must not be null.
     * @param loader The ClassLoader which loaded the ServiceBean. Must not be null.
     *
     * @return A ServiceBeanContext. A new ServiceBeanContext is created each time
     *
     * @throws ConfigurationException If the Configuration cannot be created
     * @throws IOException If the hostname cannot be accessed
     * @throws IllegalArgumentException If any of the arguments are null.
     */
    public static ServiceBeanContext getServiceBeanContext(final String configComponent,
                                                           final String defaultServiceName,
                                                           final String[] configArgs,
                                                           final ClassLoader loader) throws ConfigurationException, IOException {
        if(configArgs == null)
            throw new IllegalArgumentException("configArgs is null");
        logger.debug("Creating configuration for {}", configComponent);
        Configuration config = ConfigurationProvider.getInstance(configArgs, loader);
        logger.debug("Created configuration for {}", configComponent);
        return getServiceBeanContext(configComponent, defaultServiceName, configArgs, config, loader);

    }

    /**
     * Create a ServiceBeanContext from the Configuration element. This method is used if the
     * {@code Configuration} object has already been created.
     *
     * @param configComponent The configuration component name of the
     * ServiceBean to use when obtaining configuration attributes. Must not be null.
     * @param defaultServiceName The default name of the service. Must not be null.
     * @param configArgs Configuration arguments for the ServiceBean. This is used to create a
     * {@code ServiceBeanConfig} object, and the {@code configArgs} are those used to create the
     * {@code Configuration} argument following. Must not be null.
     * @param config Configuration for the ServiceBean. Must not be null.
     * @param loader The ClassLoader which loaded the ServiceBean. Must not be null.
     *
     * @return A ServiceBeanContext. A new ServiceBeanContext is created each time
     *
     * @throws ConfigurationException If the Configuration cannot be created
     * @throws IOException If the hostname cannot be accessed
     * @throws IllegalArgumentException If any of the arguments are null.
     */
    public static ServiceBeanContext getServiceBeanContext(final String configComponent,
                                                           final String defaultServiceName,
                                                           final String[] configArgs,
                                                           final Configuration config,
                                                           final ClassLoader loader) throws ConfigurationException,
                                                                                      IOException {
        logger.debug("Entering getServiceBeanContext");
        if(configComponent == null)
            throw new IllegalArgumentException("configComponent is null");
        if(defaultServiceName == null)
            throw new IllegalArgumentException("defaultServiceName is null");
        if(configArgs == null)
            throw new IllegalArgumentException("configArgs is null");
        if(config == null)
            throw new IllegalArgumentException("config is null");
        if(loader == null)
            throw new IllegalArgumentException("loader is null");

        Map<String, Object> configParms = new HashMap<String, Object>();

        RegistryUtil.checkRegistry();
        
        configParms.putAll(readServiceBeanConfig(configComponent, defaultServiceName, config));

        String jmxName = (String)config.getEntry(configComponent, "jmxName", String.class, null);
        if(jmxName!=null)
            configParms.put(ServiceBeanConfig.JMX_NAME, jmxName);

        ServiceBeanConfig sbConfig = new ServiceBeanConfig(configParms, configArgs);
        sbConfig.addInitParameter(BOOT_CONFIG_COMPONENT, configComponent);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if(cl instanceof ServiceClassLoader) {
            Properties p = new Properties();
            p.setProperty("opStringName", sbConfig.getOperationalStringName());
            p.setProperty("serviceName", sbConfig.getName());
            ((ServiceClassLoader)cl).addMetaData(p);
        }

        /* Add a boot-cookie, indicating that the JSB was booted by this utility */
        sbConfig.addInitParameter("org.rioproject.boot", BOOT_COOKIE);
        ServiceLevelAgreements sla = new ServiceLevelAgreements();
        /* Get the export codebase from the ClassLoader which loaded us. The
         * export codebase will be returned by the getURLs() method since
         * getURLs() is overridden to return configured export codebase
         */
        URL[] urls = ((URLClassLoader)loader).getURLs();
        if(urls.length==0)
            throw new RuntimeException("Unknown Export Codebase");

        ClassBundle exportBundle;
        String exportCodebase = urls[0].toExternalForm();
        if(!exportCodebase.startsWith("artifact:")) {
            if(exportCodebase.contains(".jar")) {
                int index = exportCodebase.lastIndexOf('/');
                if(index != -1)
                    exportCodebase = exportCodebase.substring(0, index+1);
            } else {
                throw new RuntimeException("Cannot determine export codebase from "+exportCodebase);
            }
            exportBundle = new ClassBundle("");
            exportBundle.setCodebase(exportCodebase);
            for (URL url : urls) {
                String jar = url.getFile();
                int index = jar.lastIndexOf('/');
                if (index != -1)
                    jar = jar.substring(1);
                exportBundle.addJAR(jar);
            }
        } else {
            exportBundle = new ClassBundle("");
            exportBundle.setArtifact(exportCodebase);
        }

        /* Default system threshold is the number of available processors.
         * Since the system threshold is the summation of all depletion oriented
         * resources, total utilization is =
         * (num_available_processors * cpu_utilization *  mem_utilization * diskspace_utilization) */
        double defaultSystemThreshold = Runtime.getRuntime().availableProcessors();
        double systemThreshold = (Double)config.getEntry(QOS_COMPONENT,
                                                         "systemThreshold",
                                                         double.class,
                                                         defaultSystemThreshold);
        sla.getSystemRequirements().addSystemThreshold(SystemRequirements.SYSTEM,
                                                       new ThresholdValues(0.0, systemThreshold));

        ServiceElement sElem = new ServiceElement(ServiceElement.ProvisionType.EXTERNAL,
                                                  sbConfig,
                                                  sla,
                                                  new ClassBundle[]{exportBundle});
        if(sbLifeCycleManager == null)
            sbLifeCycleManager = new LifeCycleManager();

        checkUtilityConfiguration(config);
        ComputeResource computeResource = new ComputeResource(config);

        Uuid serviceID = UuidFactory.generate();
        DefaultServiceBeanManager serviceBeanManager = new DefaultServiceBeanManager(sElem,
                                               computeResource.getHostName(),
                                               computeResource.getAddress().getHostAddress(),
                                               serviceID);
        serviceBeanManager.setDiscardManager(sbLifeCycleManager);
        serviceBeanManager.setServiceID(serviceID);
        DefaultServiceBeanContext serviceBeanContext = new DefaultServiceBeanContext(sElem, serviceBeanManager, computeResource, null); /* Shared Configuration */
        serviceBeanContext.setConfiguration(config);
        serviceBeanContext.setConfigurationFiles(configArgs);
        logger.debug("Leaving getServiceBeanContext");
        return (serviceBeanContext);
    }

    /**
     * Get ServiceBean config params, using the package name of the ServiceBean
     * class as the component
     *
     * @param configComponent The component name used to access entries in
     * the configuration
     * @param defaultServiceName The service's default name
     * @param config The Configuration to use
     * 
     * @return  A Map of name,value pairs specifying configuration
     * attributes
     *
     * @throws ConfigurationException if there are errors reading the configuration 1
     * @throws IllegalArgumentException If any of the arguments are null.
     */
    static Map<String, Object> readServiceBeanConfig(final String configComponent,
                                                     final String defaultServiceName,
                                                     final Configuration config)
    throws ConfigurationException {
        if(configComponent == null)
            throw new IllegalArgumentException("configComponent is null");
        if(defaultServiceName == null)
            throw new IllegalArgumentException("defaultServiceName is null");
        if(config == null)
            throw new IllegalArgumentException("config is null");

        Map<String, Object> configParms = new HashMap<String, Object>();

        /* Set the ClassLoader for the DiscoveryManagementPool to be the
         * CommonCLassLoader, not the current context ClassLoader */
        ClassLoader cCL = Thread.currentThread().getContextClassLoader();
        CommonClassLoader common = CommonClassLoader.getInstance();
        Thread.currentThread().setContextClassLoader(common);
        try {
            /* Set the Configuration for the DiscoveryManagementPool */
            DiscoveryManagementPool.getInstance().setConfiguration(config);
            LookupCachePool.getInstance().setConfiguration(config);
        } finally {
            Thread.currentThread().setContextClassLoader(cCL);
        }

        /* Set the config component */
        configParms.put(ServiceBeanConfig.COMPONENT, configComponent);
        
        /* Get the ServiceBean name */
        String serviceName = (String)config.getEntry(configComponent, "serviceName", String.class, defaultServiceName);
        configParms.put(ServiceBeanConfig.NAME, serviceName);
        /* Get the comment for the ServiceBean */
        String serviceComment = (String)config.getEntry(configComponent, "serviceComment", String.class, null);
        if(serviceComment != null)
            configParms.put(ServiceBeanConfig.COMMENT, serviceComment);
        /* Get the log OperationalString name */
        String opStringName = (String)config.getEntry(configComponent, "opStringName", String.class, Constants.CORE_OPSTRING);
        configParms.put(ServiceBeanConfig.OPSTRING, opStringName);        
        /* Get group values, default to all groups */
        String[] groups = (String[])config.getEntry(configComponent,
                                                    "initialLookupGroups",
                                                    String[].class,
                                                    new String[]{"all"});
        configParms.put(ServiceBeanConfig.GROUPS, groups);
        /* Get LookupLocator values */
        LookupLocator[] lookupLocators =
            (LookupLocator[])config.getEntry(configComponent, "initialLookupLocators", LookupLocator[].class, null);
        /* Get LoggerConfig values */
        LoggerConfig[] logConfigs =
            (LoggerConfig[])config.getEntry(configComponent, "loggerConfigs", LoggerConfig[].class, null);
        if(logConfigs != null) {
            for (LoggerConfig logConfig : logConfigs) {
                logConfig.getLogger();
            }
        }
        if(lookupLocators != null)
            configParms.put(ServiceBeanConfig.LOCATORS, lookupLocators);
        return (configParms);
    }

    /**
     * Check for multicast verification and setting up a shutdown hook
     *
     * @param config Configuration to use
     *
     * @throws IOException If the multicast check is performed and it fails
     * @throws ConfigurationException if there are problems reading the configuration
     * @throws IllegalArgumentException If the config argument is null.
     */
    static void checkUtilityConfiguration(final Configuration config) throws IOException, ConfigurationException {
        if(config == null)
            throw new IllegalArgumentException("configArgs are null");

        Boolean multiCheck = (Boolean)config.getEntry(BOOT_COMPONENT, "verifyMulticast", Boolean.class, false);
        if(logger.isDebugEnabled())
            logger.debug("Verify multicast [{}]", multiCheck);
        if(multiCheck) {
            if(logger.isInfoEnabled())
                logger.info("Verifying multicast, wait up to 10 seconds ... ");
            MulticastStatus.checkMulticast(1000 * 10);
            if(logger.isInfoEnabled())
                logger.info("Multicast responds okay");
        }
        /* Check for the creation of a ShutdownHook */
        Boolean createShutdownHook = (Boolean)config.getEntry(BOOT_COMPONENT, "createShutdownHook", Boolean.class, true);
        if(logger.isDebugEnabled())
            logger.debug("Create ShutdownHook [{}]", createShutdownHook);
        if(createShutdownHook) {
            boolean okay = false;
            if(sbLifeCycleManager != null) {
                if(!sbLifeCycleManager.isShutDownHookRegistered()) {
                    okay = true;
                }
            } else {
                okay = true;
            }
            if(okay) {
                ShutdownHook shutdownHook = new ShutdownHook();
                shutdownHook.setDaemon(true);
                Runtime.getRuntime().addShutdownHook(shutdownHook);
                if(sbLifeCycleManager != null) {
                    sbLifeCycleManager.setShutDownHookRegistered();
                }
            }
        }
    }

    /**
     * Class that provides basic functionality to enable bootstrapping
     */
    public static class LifeCycleManager implements DiscardManager, LifeCycle {
        boolean terminated = false;
        boolean shutdownHookRegistered = false;
        final Map<DestroyAdmin, Subject>registrationMap = new HashMap<DestroyAdmin, Subject>();

        /*
         * Register the ServiceBean to the LifeCycleManager and advertise it
         */
        public void register(final Object sbProxy, final ServiceBeanContext context) throws ServiceBeanControlException {
            try {
                if(sbProxy == null)
                    throw new IllegalArgumentException("sbProxy is null");
                if(context == null)
                    throw new IllegalArgumentException("context is null");
                ServiceAdvertiser.advertise(sbProxy, context, false);

                try {
                    ProxyPreparer prep = (ProxyPreparer)context.getConfiguration().getEntry(BOOT_COMPONENT,
                                                                                            "adminProxyPreparer",
                                                                                            BasicProxyPreparer.class,
                                                                                            new BasicProxyPreparer());
                    Object preparedPProxy = prep.prepareProxy(sbProxy);
                    if(preparedPProxy instanceof Administrable) {
                        Administrable admin = (Administrable)preparedPProxy;
                        Object adminObject = admin.getAdmin();
                        if(adminObject instanceof DestroyAdmin) {
                            Subject subject = null;
                            if(context instanceof DefaultServiceBeanContext)
                                subject = ((DefaultServiceBeanContext)context).getSubject();
                            registrationMap.put((DestroyAdmin)adminObject, subject);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch(Throwable t) {
                String name = (context == null ? "unknown name" : context.getServiceElement().getName());
                throw new ServiceBeanControlException("ServiceBean ["+ name+"] advertise failed", t);
            }
        }

        /**
         * A flag to indicate that a shutdown hook has already been registered
         * by this utility
         */
        void setShutDownHookRegistered() {
            shutdownHookRegistered = true;
        }

        /*
         * Return whether a shutdown hook has already been registered
         * by this utility
         */
        boolean isShutDownHookRegistered() {
            return(shutdownHookRegistered);
        }

        /**
         * Shutdown the utility and terminate the ServiceBean if needed
         */
        public void terminate() {
            if(!terminated) {
                for (Map.Entry<DestroyAdmin, Subject> mapEntry :
                    registrationMap.entrySet()) {
                    final DestroyAdmin dAdmin = mapEntry.getKey();
                    final Subject subject = mapEntry.getValue();
                    if (subject != null) {
                        try {
                            Subject.doAsPrivileged(subject,
                                                   new PrivilegedExceptionAction<Void>() {
                                                       public Void run() throws Exception {
                                                           try {
                                                               dAdmin.destroy();
                                                           } catch (Exception e) {
                                                               //e.printStackTrace();
                                                           }
                                                           return (null);
                                                       }
                                                   },
                                                   null);
                        } catch (PrivilegedActionException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            dAdmin.destroy();
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }

                }
            }
        }

        /**
         * @see org.rioproject.impl.container.DiscardManager#discard
         */
        public void discard() {
            if(terminated)
                return;
            terminated = true;
            new Destroyer(null, null);
        }

        /**
         * @see com.sun.jini.start.LifeCycle#unregister
         */
        public boolean unregister(final Object impl) {
            if(terminated)
                return (true);
            terminate();
            new Destroyer(null, null);
            return (true);
        }
    }

    /**
     * ShutdownHook for the ServiceBean
     */
    static class ShutdownHook extends Thread {
        ShutdownHook() {
            super("ShutdownHook");
        }

        public void run() {
            try {
                if(sbLifeCycleManager != null) {
                    sbLifeCycleManager.terminate();
                }
            } catch(Throwable t) {
                logger.error("Terminating ServiceBean", t);
            }
        }
    }
}
