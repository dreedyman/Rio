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

import com.sun.jini.config.Config;
import com.sun.jini.start.AggregatePolicyProvider;
import com.sun.jini.start.LoaderSplitPolicyProvider;
import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import net.jini.io.MarshalledInstance;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.policy.DynamicPolicyProvider;
import net.jini.security.policy.PolicyFileProvider;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.loader.ClassAnnotator;
import org.rioproject.loader.CommonClassLoader;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.config.Constants;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanContextFactory;
import org.rioproject.core.jsb.ServiceBeanFactory;
import org.rioproject.core.jsb.ServiceBeanManager;
import org.rioproject.jsb.*;
import org.rioproject.log.LoggerConfig;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.rmi.ResolvingLoader;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.PlatformCapabilityLoader;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Policy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ServiceBeanLoader will load and create a ServiceBean.
 *
 * @author Dennis Reedy
 */
public class ServiceBeanLoader {
    /** Component name for loading configuration artifacts specifically related
     * to service creation */
    static final String CONFIG_COMPONENT = "service.load";
    /** A Logger */
    static Logger logger = Logger.getLogger(ServiceBeanLoader.class.getName());
    private static AggregatePolicyProvider globalPolicy = null;
    private static Policy initialGlobalPolicy = null;
    private static final Map<String, AtomicInteger> counterTable = new HashMap<String, AtomicInteger>();
    private static final List<ProvisionedResources> provisionedResources = new ArrayList<ProvisionedResources>();

    /**
     * Trivial class used as the return value by the 
     * <code>load</code> method. This class aggregates
     * the results of a service creation attempt: 
     * proxy and associated ServiceBeanContext. 
     */
    public static class Result {
        /** The service impl */
        private final Object impl;
        /** The proxy as a MarshalledInstance */
        private final MarshalledInstance mi;
        /** Associated <code>ServiceBeanContext</code> object */
        private final ServiceBeanContext context;
        /** Uuid of the service */
        private final Uuid serviceID;

        /**
         * Trivial constructor. Simply assigns each argument
         * to the appropriate field.
         *
         * @param c The ServiceBeanContext
         * @param o The resulting loaded implementation
         * @param m The proxy as a MarshalledInstance
         * @param s The id of the service
         */
        public Result(ServiceBeanContext c, Object o, MarshalledInstance m, Uuid s) {
            context = c;
            impl = o;
            mi = m;
            serviceID = s;
        }

        public Object getImpl() {
            return impl;
        }

        public MarshalledInstance getMarshalledInstance() {
            return mi;
        }

        public ServiceBeanContext getServiceBeanContext() {
            return context;
        }

        public Uuid getServiceID() {
            return serviceID;
        }
    }

    /**
     * Clean up resources
     * <br>
     * <li>
     * Remove ServiceClassLoader from global policy to prevent leaking
     * ServiceClassLoader instances
     * <li>Remove any downloaded jars
     * </ul>
     *
     * @param result The Result object to unload
     * @param elem The ServiceElement to use as a reference
     */
    public static void unload(Result result, ServiceElement elem) {
        unload(result.impl.getClass().getClassLoader(), elem);
    }

    /**
     * Clean up resources
     * <br>
     * <li>
     * Remove ServiceClassLoader from global policy to prevent leaking
     * ServiceClassLoader instances
     * <li>Remove any downloaded jars
     * </ul>
     *
     * @param loader The ClassLoader to unload
     * @param elem The ServiceElement to use as a reference
     */
    public static void unload(ClassLoader loader, ServiceElement elem) {
        if(globalPolicy!=null)
            globalPolicy.setPolicy(loader, null);
        cleanJars(elem);
        ResolvingLoader.release(loader);
    }

    /*
     * Remove downloaded jars
     */
    private static void cleanJars(ServiceElement elem) {
        List<ProvisionedResources> toRemove = new ArrayList<ProvisionedResources>();
        ProvisionedResources[] copy;
        synchronized(provisionedResources) {
            copy = provisionedResources.toArray(
                new ProvisionedResources[provisionedResources.size()]);
        }
        for(ProvisionedResources pr : copy) {
            if(elem.getComponentBundle()!=null &&
               elem.getComponentBundle().getArtifact()!=null)
            if(pr.getArtifact()!=null &&
               pr.getArtifact().equals(elem.getComponentBundle().getArtifact())) {
                toRemove.add(pr);
            }
        }
        for(ProvisionedResources pr : toRemove) {
            synchronized(counterTable) {
                AtomicInteger count = counterTable.get(pr.getArtifact());
                if(count!=null) {
                    int using = count.decrementAndGet();
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("Number of ["+pr.getArtifact()+"] artifacts still active="+using);
                    if(using==0) {
                        if(pr.getArtifact().contains("SNAPSHOT")) {
                            System.out.println("["+elem.getName()+"], artifact="+pr.getArtifact()+", " +
                                               "has the following jars");
                            for(URL u : pr.getJars()) {
                                System.out.println("\t"+u.toExternalForm());
                            }
                        }

                        //File parent = dir.getParentFile();
                        //FileUtils.remove(dir);
                        //if(parent.exists() && parent.list().length==0)
                        //    FileUtils.remove(parent);
                        synchronized(provisionedResources) {
                            provisionedResources.remove(pr);
                        }
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("Remove cached artifact ["+pr.getArtifact()+"] "+pr);
                    } else {
                        counterTable.put(pr.getArtifact(), count);
                    }
                }
            }
        }
    }

    /**
     * The load method is invoked to load and instantiate a ServiceBean.
     *
     * @param sElem The ServiceElement
     * @param serviceID Uuid for the service
     * @param jsbManager The ServiceBeanManager
     * @param container The ServiceBeanContainer
     * 
     * @return A Result object with attributes to access the instantiated
     * service
     * 
     * @throws org.rioproject.deploy.ServiceBeanInstantiationException If errors occur while creating the
     * service bean
     */
    public static Result load(ServiceElement sElem,
                              Uuid serviceID,
                              ServiceBeanManager jsbManager,
                              ServiceBeanContainer container) throws ServiceBeanInstantiationException {
        Object proxy;
        MarshalledInstance mi = null;
        Object impl = null;
        ServiceBeanContext context;
        CommonClassLoader commonCL = CommonClassLoader.getInstance();
        ComputeResource computeResource = container.getComputeResource();

        synchronized(ServiceBeanLoader.class) {
            /* supplant global policy 1st time through */
            if(globalPolicy == null) {
                initialGlobalPolicy = Policy.getPolicy();
                globalPolicy = new AggregatePolicyProvider(initialGlobalPolicy);
                Policy.setPolicy(globalPolicy);
                if(logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "Global policy set: {0}", globalPolicy);
            }
        }

        /*
         * Provision service jars
         */
        URL[] exports;
        URL[] implJARs;
        try {
            Resolver resolver = ResolverHelper.getResolver();
            boolean install = computeResource.getPersistentProvisioning();
            Map<String, ProvisionedResources> serviceResources = provisionService(sElem, resolver, install);
            ProvisionedResources dlPR = serviceResources.get("dl");
            ProvisionedResources implPR = serviceResources.get("impl");
            if(dlPR.getJars().length==0 && dlPR.getArtifact()!=null) {
                String convertedArtifact = dlPR.getArtifact().replaceAll(":", "/");
                // TODO: if the repositories is default maven central, still need to add?
                exports = new URL[]{new URL("artifact:"+convertedArtifact+dlPR.getRepositories())};
            } else {
                exports = dlPR.getJars();
            }
            implJARs = implPR.getJars();
        } catch(Exception e) {
            throw new ServiceBeanInstantiationException("Unable to provision JARs for service ["+sElem.getName()+"]", e);
        }

        /*
         * Load any system components prior to the loading of the Service
         */
        if(sElem.getComponentBundle()!=null) {
            try {
                Map<String, URL[]> sharedResources = sElem.getComponentBundle().getSharedComponents();

                if(sharedResources.size()>0) {
                    for (Map.Entry<String, URL[]>entry : sharedResources.entrySet()) {
                        String name = entry.getKey();
                        URL[] urls =  entry.getValue();
                        if (urls != null)
                            commonCL.addComponent(name, urls);
                    }
                }
            } catch(MalformedURLException e) {
                throw new ServiceBeanInstantiationException("Unable to load SharedComponents for ["+sElem.getName()+"]",
                                                    e);
            }
        }

        final Thread currentThread = Thread.currentThread();
        ClassLoader currentClassLoader = currentThread.getContextClassLoader();
        try {
            ClassBundle jsbBundle = sElem.getComponentBundle();
            List<URL> urlList = new ArrayList<URL>();
            /*
            URL[] implJARs;
            if(jsbBundle!=null && jsbBundle.getCodebase()!=null)
                implJARs = jsbBundle.getJARs();
            else
                implJARs = new URL[0];
            */
            urlList.addAll(Arrays.asList(implJARs));

            /* Get matched PlatformCapability jars to load */
            PlatformCapability[] pCaps = computeResource.getPlatformCapabilities();
            PlatformCapability[] matched = ServiceElementUtil.getMatchedPlatformCapabilities(sElem, pCaps);
            for (PlatformCapability pCap : matched) {
                URL[] urls = PlatformCapabilityLoader.getLoadableClassPath(pCap);
                for(URL url : urls) {
                    if(!urlList.contains(url))
                        urlList.add(url);
                }
            }

            URL[] classpath = urlList.toArray(new URL[urlList.size()]);
            Properties metaData = new Properties();
            metaData.setProperty("opStringName", sElem.getOperationalStringName());
            metaData.setProperty("serviceName", sElem.getName());

            ServiceClassLoader jsbCL = new ServiceClassLoader(ServiceClassLoader.getURIs(classpath),
                                                              new ClassAnnotator(exports),
                                                              commonCL,
                                                              metaData);

            /*
            ServiceClassLoader jsbCL =
                new ServiceClassLoader(classpath, annotator, commonCL);
                */
            currentThread.setContextClassLoader(jsbCL);
            
            if(logger.isLoggable(Level.INFO)) {
                StringBuilder buffer = new StringBuilder();
                if(implJARs.length==0) {
                    buffer.append("<empty>");
                } else {
                    for(int i=0; i<implJARs.length; i++) {
                        if(i>0)
                            buffer.append(", ");
                        buffer.append(implJARs[i].toExternalForm());
                    }
                }
                String className = (jsbBundle==null?"<not defined>": jsbBundle.getClassName());
                logger.log(Level.INFO,
                           "Create ServiceClassLoader for {0}, classpath {1}, codebase {2}",
                           new Object[] { className, buffer.toString(), jsbCL.getClassAnnotation()});
                //ClassLoaderUtil.displayClassLoaderTree(jsbCL);
            }

            /* Get the servicePolicyFile from the environment. If the
             * property has not been set use the policy set for the VM */
            String servicePolicyFile = System.getProperty("rio.service.security.policy",
                                                          System.getProperty("java.security.policy"));
            if(servicePolicyFile!=null) {
                DynamicPolicyProvider service_policy =
                    new DynamicPolicyProvider(new PolicyFileProvider(servicePolicyFile));
                LoaderSplitPolicyProvider splitServicePolicy =
                    new LoaderSplitPolicyProvider(jsbCL,
                                                  service_policy,
                                                  new DynamicPolicyProvider(initialGlobalPolicy));
                /*
                 * Grant "this" code enough permission to do its work under the
                 * service policy, which takes effect (below) after the context
                 * loader is (re)set.
                 */
                splitServicePolicy.grant(ServiceBeanLoader.class,
                                         null, /* Principal[] */
                                         new Permission[]{new AllPermission()});
                globalPolicy.setPolicy(jsbCL, splitServicePolicy);
            }

            /* Reload the shared configuration using the service's classloader */
            //String[] configFiles = container.getSharedConfigurationFiles().toArray(new String[sharedConfigurationFiles.size()]);
            Configuration sharedConfiguration = container.getSharedConfiguration();

            /* Get the ServiceBeanContextFactory */
            ServiceBeanContextFactory serviceBeanContextFactory =
                (ServiceBeanContextFactory)Config.getNonNullEntry(sharedConfiguration,
                                                                  CONFIG_COMPONENT,
                                                                  "serviceBeanContextFactory",
                                                                  ServiceBeanContextFactory.class,
                                                                  new JSBContextFactory());
            /* Create the ServiceBeanContext */
            context = serviceBeanContextFactory.create(sElem,
                                                       jsbManager,
                                                       computeResource,
                                                       sharedConfiguration);
            /* Add a temporary startup value, used to check when issuing
             * lifecycle notification (RIO-141) */
            Map<String, Object> configParms = context.getServiceBeanConfig().getConfigurationParameters();
            configParms.put(Constants.STARTING, true);
            context.getServiceBeanConfig().setConfigurationParameters(configParms);
            
            /*
             * Initialize any configured Logger instances. If there are any
             * exceptions loading the configurations, log the appropriate
             * message and continue
             */
            Map map = context.getServiceBeanConfig().getConfigurationParameters();
            LoggerConfig[] loggerConfigs = (LoggerConfig[])map.get(ServiceBeanConfig.LOGGER);
            if(loggerConfigs != null) {
                for (LoggerConfig loggerConfig : loggerConfigs) {
                    try {
                        loggerConfig.getLogger();
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Loading LoggerConfig", t);
                    }
                }
            }
            
            /* Get the ServiceBeanFactory */
            ServiceBeanFactory serviceBeanFactory =
                (ServiceBeanFactory)Config.getNonNullEntry(context.getConfiguration(),
                                                           CONFIG_COMPONENT,
                                                           "serviceBeanFactory",
                                                           ServiceBeanFactory.class,
                                                           new JSBLoader());
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, 
                           "service = {0}, serviceBeanFactory = {1}",
                           new Object[]{sElem.getName(), serviceBeanFactory});            
            ServiceBeanFactory.Created created = serviceBeanFactory.create(context);
            impl = created.getImpl();
            
            if(context.getServiceElement().getComponentBundle()==null) {
                String compName = impl.getClass().getName();
                if(compName.indexOf(".")>0) {
                    int index = compName.lastIndexOf(".");
                    compName = compName.substring(0, index);
                }
                context.getServiceBeanConfig().addInitParameter(ServiceBeanActivation.BOOT_CONFIG_COMPONENT, compName);
            }

            /* Get the ProxyPreparer */
            ProxyPreparer servicePreparer = (ProxyPreparer)Config.getNonNullEntry(context.getConfiguration(),
                                                                                  CONFIG_COMPONENT,
                                                                                  "servicePreparer",
                                                                                  ProxyPreparer.class,
                                                                                  new BasicProxyPreparer());
            proxy = created.getProxy();
            if(proxy != null) {
                proxy = servicePreparer.prepareProxy(proxy);
            }
            
            /*
             * Set the MarshalledInstance into the ServiceBeanManager
             */
            mi = new MarshalledInstance(proxy);
            ((JSBManager)context.getServiceBeanManager()).
                setMarshalledInstance(mi);
            /*
             * The service may have created it's own serviceID
             */
            if(proxy instanceof ReferentUuid) {
                serviceID = ((ReferentUuid)proxy).getReferentUuid();
                ((JSBManager)context.getServiceBeanManager()).setServiceID(serviceID);
            }
            
            /*
             * If the proxy is Administrable and an instanceof 
             * ServiceBeanControl, set the ServiceBeanControl in the 
             * AssociationManagement object
             */
            if(proxy instanceof Administrable) {
                Object adminObject = ((Administrable)proxy).getAdmin();               
                if(adminObject instanceof ServiceBeanControl) {
                    context.getAssociationManagement().setServiceBeanControl((ServiceBeanControl)adminObject);
                }                    
            }
            
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Proxy =  {0}", proxy);
            //TODO - factor in code integrity for MO
            //proxy = (new MarshalledObject(proxy)).get();
            //currentThread.setContextClassLoader(currentClassLoader);
            
        } catch(Throwable t) {
            ServiceBeanInstantiationException e;
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Loading ServiceBean", t);
            if(t instanceof ServiceBeanInstantiationException)
                e = (ServiceBeanInstantiationException)t;
            else
                e = new ServiceBeanInstantiationException(t.getClass().getName()+ ": "+ t.getLocalizedMessage(), t);
            throw e;
        } finally {                
            currentThread.setContextClassLoader(currentClassLoader);
        }
        
        return(new Result(context, impl, mi, serviceID));
    }  

    static synchronized Map<String, ProvisionedResources> provisionService(ServiceElement elem,
                                                                           Resolver resolver,
                                                                           boolean supportsInstallation)
        throws MalformedURLException, ServiceBeanInstantiationException {

        Map<String, ProvisionedResources> map = new HashMap<String, ProvisionedResources>();
        URL[] implJARs;
        String implArtifact = (elem.getComponentBundle()!=null?
                               elem.getComponentBundle().getArtifact(): null);
        ProvisionedResources implPR = getProvisionedResources(implArtifact);

        if(implPR==null) {
            implPR = new ProvisionedResources(implArtifact);
            if(elem.getComponentBundle()!=null) {
                if(elem.getComponentBundle().getArtifact()!=null && resolver!=null) {
                    if(!supportsInstallation)
                        throw new ServiceBeanInstantiationException("Service ["+elem.getName()+"] " +
                                                            "cannot be instantiated, the Cybernode " +
                                                            "does not support persistent provisioning, and the " +
                                                            "service requires artifact resolution for "+implArtifact);
                    String[] jars = ResolverHelper.resolve(elem.getComponentBundle().getArtifact(),
                                                           resolver,
                                                           elem.getRemoteRepositories());
                    List<URL> urls = new ArrayList<URL>();
                    for(String jar: jars) {
                        if(jar!=null)
                            urls.add(new URL(jar));
                    }
                    implJARs = urls.toArray(new URL[urls.size()]);
                } else {
                    /* RIO-228 */
                    if(System.getProperty("StaticCybernode")!=null)
                        implJARs = new URL[0];
                    else
                        implJARs = elem.getComponentBundle().getJARs();
                }
            } else {
                implJARs = new URL[0];
            }

            implPR.setJars(implJARs);
        }

        String exportArtifact = null;
        for(ClassBundle cb : elem.getExportBundles()) {
            if(cb.getArtifact()!=null) {
                exportArtifact = cb.getArtifact();
                break;
            }
        }
        ProvisionedResources dlPR = getProvisionedResources(exportArtifact);
        //String localCodebase = System.getProperty(Constants.CODESERVER);
        if(dlPR==null) {
            dlPR = new ProvisionedResources(exportArtifact);
            //List<URL> exportURLs = new ArrayList<URL>();
            //if(exportArtifact!=null && resolver!=null && localCodebase!=null) {
            if(exportArtifact!=null && resolver!=null) {
            //if(exportArtifact!=null && resolver!=null) {
                for(ClassBundle cb : elem.getExportBundles()) {
                    /*String[] jars = */ResolverHelper.resolve(cb.getArtifact(),
                                                           resolver,
                                                           elem.getRemoteRepositories()/*,
                                                           localCodebase*/);
                    for(RemoteRepository r : elem.getRemoteRepositories())
                        dlPR.addRepositoryUrl(r.getUrl());
                    /*for(String jar: jars) {
                        if(jar!=null)
                            exportURLs.add(new URL(jar));
                    }*/
                }
                //dlPR.setJars(exportURLs.toArray(new URL[exportURLs.size()]));
            } else {
                if(System.getProperty("StaticCybernode")==null)
                    dlPR.setJars(elem.getExportURLs());
            }
        }

        /* RIO-228 */
        if(System.getProperty("StaticCybernode")==null) {
            /* Check the dlPR jars for requisite jar inclusion */

            // TODO instead of checking for requisite jars, should check for requisite artifact
            String[] requisiteExports = new String[]{"rio-api.jar", "jsk-dl.jar", "jmx-lookup.jar", "serviceui.jar"};
            //String libDLDir = new File(System.getProperty("RIO_HOME")+ File.separator+"lib-dl").toURI().toString();

            for(String export : requisiteExports) {
                boolean found = false;
                for(URL u : dlPR.getJars()) {
                    if(u.toExternalForm().endsWith(export)) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    /* We check if the impl has an artifact, not the dl. The
                     * dl may not declare an artifact. This accounts for the
                     * case where a service is just implemented as a pojo with
                     * no custom remote methods, and is just exported using Rio
                     * infrastructure support
                     * (through the org.rioproject.resources.servicecore.Service interface).
                     *
                     * If there is no declared artifact, we sail through since
                     * the ServiceElement would have been constructed with the
                     * default platform export jars as part of it's creation.
                     */
                    if(implPR.getArtifact()!=null) {
                        /*if(localCodebase!=null) {
                            if(!localCodebase.endsWith("/"))
                                localCodebase = localCodebase+"/";
                            dlPR.addJar(new URL(localCodebase+export));
                        }*/
                    }
                    /*if(implPR.getArtifact()!=null) {
                        if(!libDLDir.endsWith("/"))
                            libDLDir = libDLDir+"/";
                        dlPR.addJar(new URL(libDLDir+export));
                    }*/
                }
            }
        }

        if(logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Service [").append(elem.getName()).append("], ");
            if(implArtifact!=null)
                sb.append("impl artifact: [").append(implPR.getArtifact()).append("] ");
            sb.append("impl jars: ");
            sb.append(implPR.getJarsAsString());
            sb.append(", ");
            if(exportArtifact!=null)
                sb.append("export artifact: [").append(dlPR.getArtifact()).append("] ");
            sb.append("export jars: ");
                sb.append(dlPR.getJarsAsString());
            logger.fine(sb.toString());
        }

        map.put("impl", implPR);
        map.put("dl", dlPR);
        addProvisionedResources(map);
        registerArtifactCounts(map);
        return map;
    }

    private static ProvisionedResources getProvisionedResources(String artifact) {
        ProvisionedResources pr = null;
        if(artifact!=null && !artifact.contains("SNAPSHOT")) {
            synchronized(provisionedResources) {
                for(ProvisionedResources alreadyProvisioned : provisionedResources) {
                    if(alreadyProvisioned.getArtifact()!=null &&
                       alreadyProvisioned.getArtifact().equals(artifact)) {
                        pr = alreadyProvisioned;
                        break;
                    }
                }
            }
        }
        return pr;
    }

    private static void addProvisionedResources(Map<String, ProvisionedResources> map) {
        for(Map.Entry<String, ProvisionedResources> entry : map.entrySet()) {
            ProvisionedResources pr = entry.getValue();
            if(pr.getArtifact()!=null && !pr.getArtifact().contains("SNAPSHOT")) {
                synchronized(provisionedResources) {
                    if(!provisionedResources.contains(pr))
                        provisionedResources.add(pr);
                }
            }
        }
    }

    private static void registerArtifactCounts(Map<String, ProvisionedResources> map) {
        for(Map.Entry<String, ProvisionedResources> entry : map.entrySet()) {
            ProvisionedResources pr = entry.getValue();
            if(pr.getArtifact()!=null && !pr.getArtifact().contains("SNAPSHOT")) {
                synchronized(counterTable) {
                    AtomicInteger count = counterTable.get(pr.getArtifact());
                    if(count==null)
                        count = new AtomicInteger(1);
                    else
                        count.incrementAndGet();
                    counterTable.put(pr.getArtifact(), count);
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("Counter for ["+pr.getArtifact()+"] is " +
                                      "now "+count.get());
                }
            }
        }
    }
   
    private static class ProvisionedResources {
        List<URL> jarList = new ArrayList<URL>();
        String artifact;
        StringBuilder repositories = new StringBuilder();

        private ProvisionedResources(String artifact) {
            this.artifact = artifact;
        }

        void setJars(URL[] jars) {
            jarList.addAll(Arrays.asList(jars));
        }

        URL[] getJars() {
            return jarList.toArray(new URL[jarList.size()]);
        }

        String getArtifact() {
            return artifact;
        }

        String getJarsAsString() {
            return jarList.size() == 0 ? "<>" : jarList.toString();
        }

        void addRepositoryUrl(String u) {
            repositories.append(";").append(u);
        }

        String getRepositories() {
            return repositories.toString();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("jars=").append(
                jarList.size() == 0 ? "<>" : jarList.toString());
            sb.append('}');
            return sb.toString();
        }
    }    
}
