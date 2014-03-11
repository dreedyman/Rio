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
package org.rioproject.impl.container;

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
import org.rioproject.RioVersion;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.config.Constants;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.impl.servicebean.DefaultServiceBeanFactory;
import org.rioproject.impl.servicebean.DefaultServiceBeanManager;
import org.rioproject.impl.servicebean.ServiceBeanActivation;
import org.rioproject.impl.servicebean.ServiceElementUtil;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.impl.system.capability.PlatformCapabilityLoader;
import org.rioproject.loader.ClassAnnotator;
import org.rioproject.loader.CommonClassLoader;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.log.LoggerConfig;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.rmi.ResolvingLoader;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.servicebean.ServiceBeanContextFactory;
import org.rioproject.servicebean.ServiceBeanFactory;
import org.rioproject.servicebean.ServiceBeanManager;
import org.rioproject.system.capability.PlatformCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Policy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The ServiceBeanLoader will load and create a service.
 *
 * @author Dennis Reedy
 */
public class ServiceBeanLoader {
    /** Component name for loading configuration artifacts specifically related
     * to service creation */
    private static final String CONFIG_COMPONENT = "service.load";
    /** A Logger */
    private static Logger logger = LoggerFactory.getLogger(ServiceBeanLoader.class.getName());
    private final static AggregatePolicyProvider globalPolicy;
    private final static Policy initialGlobalPolicy;
    static {
        initialGlobalPolicy = Policy.getPolicy();
        globalPolicy = new AggregatePolicyProvider(initialGlobalPolicy);
        Policy.setPolicy(globalPolicy);
    }
    private static final Map<String, AtomicInteger> counterTable =
        Collections.synchronizedMap(new HashMap<String, AtomicInteger>());
    private static final List<ProvisionedResources> provisionedResources =
        Collections.synchronizedList(new ArrayList<ProvisionedResources>());
    private final static ExecutorService service = Executors.newCachedThreadPool();

    /**
     * Clean up resources
     * <br>
     * <li>
     * Remove ServiceClassLoader from global policy to prevent leaking
     * ServiceClassLoader instances
     * <li>Remove any downloaded jars
     * </ul>
     *
     * @param result The ServiceBeanLoaderResult object to unload
     * @param elem The ServiceElement to use as a reference
     */
    public static void unload(final ServiceBeanLoaderResult result, final ServiceElement elem) {
        unload(result.getImpl().getClass().getClassLoader(), elem);
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
    public static void unload(final ClassLoader loader, final ServiceElement elem) {
        if(globalPolicy!=null)
            globalPolicy.setPolicy(loader, null);
        checkAndMaybeCleanProvisionedResources(elem);
        service.submit(new Runnable() {
            public void run() {
                ResolvingLoader.release(loader);
            }
        });
    }

    @Override
    protected void finalize() throws Throwable {
        service.shutdownNow();
        super.finalize();
    }

    /*
     * Check and maybe remove provisioned resources collection
     */
    private static void checkAndMaybeCleanProvisionedResources(final ServiceElement elem) {
        List<ProvisionedResources> toRemove = new ArrayList<ProvisionedResources>();
        ProvisionedResources[] copy = provisionedResources.toArray(new ProvisionedResources[provisionedResources.size()]);
        for(ProvisionedResources pr : copy) {
            if(elem.getComponentBundle()!=null &&
               elem.getComponentBundle().getArtifact()!=null)
            if(pr.getArtifact()!=null &&
               pr.getArtifact().equals(elem.getComponentBundle().getArtifact())) {
                toRemove.add(pr);
            }
        }
        for(ProvisionedResources pr : toRemove) {
            AtomicInteger count = counterTable.get(pr.getArtifact());
            if(count!=null) {
                int using = count.decrementAndGet();
                if(logger.isTraceEnabled()) {
                    logger.trace("Number of [{}] artifacts still active={}", pr.getArtifact(), using);
                }
                if(using==0) {
                    provisionedResources.remove(pr);
                } else {
                    counterTable.put(pr.getArtifact(), count);
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
     * @return A ServiceBeanLoaderResult object with attributes to access the instantiated
     * service
     *
     * @throws org.rioproject.deploy.ServiceBeanInstantiationException If errors occur while creating the
     * service bean
     */
    public static ServiceBeanLoaderResult load(final ServiceElement sElem,
                                               final Uuid serviceID,
                                               final ServiceBeanManager jsbManager,
                                               final ServiceBeanContainer container) throws ServiceBeanInstantiationException {
        ServiceBeanFactory.Created created = null;
        MarshalledInstance marshalledProxy = null;
        ServiceBeanContext context;
        CommonClassLoader commonCL = CommonClassLoader.getInstance();
        ComputeResource computeResource = container.getComputeResource();

        /*
         * Provision service jars
         */
        URL[] exports = new URL[0];
        URL[] implJARs = new URL[0];
        if(System.getProperty("StaticCybernode")==null) {
            try {
                Resolver resolver = ResolverHelper.getResolver();
                boolean install = computeResource.getPersistentProvisioning();
                Map<String, ProvisionedResources> serviceResources = provisionService(sElem, resolver, install);
                ProvisionedResources dlPR = serviceResources.get("dl");
                ProvisionedResources implPR = serviceResources.get("impl");
                if(dlPR.getJars().length==0 && dlPR.getArtifact()!=null) {
                    String convertedArtifact = dlPR.getArtifact().replaceAll(":", "/");
                    String[] artifactParts = convertedArtifact.split(" ");
                    List<URL> exportURLList = new ArrayList<URL>();
                    for(String artifactPart : artifactParts) {
                        // TODO: if the repositories is default maven central, still need to add?
                        exportURLList.add(new URL("artifact:"+artifactPart+dlPR.getRepositories()));
                    }
                    exports = exportURLList.toArray(new URL[exportURLList.size()]);
                } else {
                    exports = dlPR.getJars();
                }
                implJARs = implPR.getJars();
            } catch(Exception e) {
                throw new ServiceBeanInstantiationException("Unable to provision JARs for " +
                                                            "service "+ ServiceLogUtil.logName(sElem), e);
            }
        }

        final Thread currentThread = Thread.currentThread();
        ClassLoader currentClassLoader = currentThread.getContextClassLoader();
        Uuid serviceIDToUse = serviceID;
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
            if(logger.isDebugEnabled()) {
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
                if(logger.isDebugEnabled()) {
                    logger.debug("Create ServiceClassLoader for {}, classpath {}, codebase {}",
                                 className, buffer.toString(), jsbCL.getClassAnnotation());
                }
            }

            /* Get the servicePolicyFile from the environment. If the
             * property has not been set use the policy set for the VM */
            String servicePolicyFile = System.getProperty("rio.service.security.policy",
                                                          System.getProperty("java.security.policy"));
            if(logger.isTraceEnabled()) {
                logger.trace("{} Service security policy file {}",
                             ServiceLogUtil.logName(sElem), servicePolicyFile);
            }
            if(servicePolicyFile!=null) {
                if(logger.isTraceEnabled()) {
                    logger.trace("Set {} service security to LoaderSplitPolicyProvider", ServiceLogUtil.logName(sElem));
                }
                DynamicPolicyProvider service_policy = new DynamicPolicyProvider(new PolicyFileProvider(servicePolicyFile));
                LoaderSplitPolicyProvider splitServicePolicy = new LoaderSplitPolicyProvider(jsbCL,
                                                                                             service_policy,
                                                                                             new DynamicPolicyProvider(initialGlobalPolicy));
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
                                                                  new ServiceContextFactory());
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
            LoggerConfig[] loggerConfigs = context.getServiceBeanConfig().getLoggerConfigs();
            if(loggerConfigs != null) {
                for (LoggerConfig loggerConfig : loggerConfigs) {
                    try {
                        loggerConfig.getLogger();
                    } catch (Throwable t) {
                        logger.warn("Loading LoggerConfig", t);
                    }
                }
            }
            
            /* Get the ServiceBeanFactory */
            ServiceBeanFactory serviceBeanFactory =
                (ServiceBeanFactory)Config.getNonNullEntry(context.getConfiguration(),
                                                           CONFIG_COMPONENT,
                                                           "serviceBeanFactory",
                                                           ServiceBeanFactory.class,
                                                           new DefaultServiceBeanFactory());
            if(logger.isTraceEnabled()) {
                logger.trace("service = {}, serviceBeanFactory = {}",
                             ServiceLogUtil.logName(sElem), serviceBeanFactory);
            }
            created = serviceBeanFactory.create(context);
            logger.trace("Created ServiceBeanFactory.Created {}", created);
            Object impl = created.getImpl();
            logger.trace("Obtained implementation {}", impl);
            
            if(context.getServiceElement().getComponentBundle()==null) {
                String compName = impl.getClass().getName();
                if(compName.indexOf(".")>0) {
                    int index = compName.lastIndexOf(".");
                    compName = compName.substring(0, index);
                }
                context.getServiceBeanConfig().addInitParameter(ServiceBeanActivation.BOOT_CONFIG_COMPONENT, compName);
            }

            /* Get the ProxyPreparer */
            if(logger.isTraceEnabled()) {
                logger.trace("Get the ProxyPreparer for {}", ServiceLogUtil.logName(sElem));
            }
            
            ProxyPreparer servicePreparer = (ProxyPreparer)Config.getNonNullEntry(context.getConfiguration(),
                                                                                  CONFIG_COMPONENT,
                                                                                  "servicePreparer",
                                                                                  ProxyPreparer.class,
                                                                                  new BasicProxyPreparer());
            if(logger.isTraceEnabled()) {
                logger.trace("Getting the proxy");
            }
            Object proxy = created.getProxy();
            if(logger.isTraceEnabled()) {
                logger.trace("Obtained the proxy %s", proxy);
            }
            if(proxy != null) {
                proxy = servicePreparer.prepareProxy(proxy);
            }
            if(logger.isTraceEnabled()) {
                logger.trace("Proxy {}, prepared? {}", proxy, (proxy==null?"not prepared, returned proxy was null": "yes"));
            }
            /*
             * Set the MarshalledInstance into the ServiceBeanManager
             */
            if(logger.isTraceEnabled()) {
                logger.trace("Set the MarshalledInstance into the ServiceBeanManager");
            }
            marshalledProxy = new MarshalledInstance(proxy);
            ((DefaultServiceBeanManager)context.getServiceBeanManager()).setMarshalledInstance(marshalledProxy);
            /*
             * The service may have created it's own serviceID
             */
            if(proxy instanceof ReferentUuid) {
                serviceIDToUse = ((ReferentUuid)proxy).getReferentUuid();
                if(logger.isDebugEnabled()) {
                    logger.debug("Service has provided Uuid: {}", serviceIDToUse);
                }
                ((DefaultServiceBeanManager)context.getServiceBeanManager()).setServiceID(serviceIDToUse);
            }
            
            /*
             * If the proxy is Administrable and an instanceof 
             * ServiceBeanControl, set the ServiceBeanControl in the 
             * DefaultAssociationManagement object
             */
            if(proxy instanceof Administrable) {
                Object adminObject = ((Administrable)proxy).getAdmin();               
                if(adminObject instanceof ServiceBeanControl) {
                    context.getAssociationManagement().setServiceBeanControl((ServiceBeanControl)adminObject);
                }                    
            }
            if(logger.isTraceEnabled()) {
                logger.trace("Proxy =  {}", proxy);
            }
        } catch(Exception e) {
            ServiceBeanInstantiationException sbe;
            if(logger.isTraceEnabled()) {
                logger.trace("Loading ServiceBean", e);
            }
            if(e instanceof ServiceBeanInstantiationException)
                sbe = (ServiceBeanInstantiationException)e;
            else
                sbe = new ServiceBeanInstantiationException(e.getClass().getName()+ ": "+ e.getLocalizedMessage(), e);
            throw sbe;
        } finally {                
            currentThread.setContextClassLoader(currentClassLoader);
        }
        return(new ServiceBeanLoaderResult(context, created.getImpl(), created.getBeanAdapter(), marshalledProxy, serviceIDToUse));
    }  

    static synchronized Map<String, ProvisionedResources> provisionService(final ServiceElement elem,
                                                                           final Resolver resolver,
                                                                           final boolean supportsInstallation)
        throws MalformedURLException, ServiceBeanInstantiationException {

        Map<String, ProvisionedResources> map = new HashMap<String, ProvisionedResources>();
        URL[] implJARs = null;
        String implArtifact = (elem.getComponentBundle()!=null?
                               elem.getComponentBundle().getArtifact(): null);
        ProvisionedResources implPR = getProvisionedResources(implArtifact);

        if(implPR==null) {
            implPR = new ProvisionedResources(implArtifact);
            if(elem.getComponentBundle()!=null) {
                if(System.getProperty("StaticCybernode")==null) {
                    if(elem.getComponentBundle().getArtifact()!=null && resolver!=null) {
                        if(!supportsInstallation)
                            throw new ServiceBeanInstantiationException("Service ["+elem.getName()+"] " +
                                                                        "cannot be instantiated, the Cybernode " +
                                                                        "does not support persistent provisioning, " +
                                                                        "and the service requires artifact resolution " +
                                                                        "for "+implArtifact);
                        String[] jars;
                        try {
                            StringBuilder builder = new StringBuilder();
                            for(RemoteRepository repository: elem.getRemoteRepositories()) {
                                if(builder.length()>0){
                                    builder.append(", ");
                                }
                                builder.append(repository.getUrl());
                            }
                            if(logger.isInfoEnabled()) {
                                logger.info("Resolve {} using these repositories [{}]",
                                            elem.getComponentBundle().getArtifact(), builder.toString());
                            }
                            jars = ResolverHelper.resolve(elem.getComponentBundle().getArtifact(),
                                                          resolver,
                                                          elem.getRemoteRepositories());
                        } catch (ResolverException e) {
                            Throwable thrown = e;
                            if(e.getCause()!=null)
                                thrown = e.getCause();
                            throw new ServiceBeanInstantiationException("Could not resolve implementation artifact", thrown);
                        }
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
                }
            } else {
                implJARs = new URL[0];
            }
            if(implJARs!=null)
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
        if(dlPR==null) {
            if(implArtifact!=null && exportArtifact==null) {
                exportArtifact = "org.rioproject:rio-api:"+ RioVersion.VERSION;
            }
            dlPR = new ProvisionedResources(exportArtifact);
            dlPR.setJars(elem.getExportURLs());
            for(RemoteRepository repository: elem.getRemoteRepositories()) {
                StringBuilder repositoryString = new StringBuilder();
                repositoryString.append(repository.getUrl());
                if(repository.getId()!=null) {
                    repositoryString.append("@");
                    repositoryString.append(repository.getId());
                }
                dlPR.addRepositoryUrl(repositoryString.toString());
            }
        }

        /*
         * If we are instantiating a service that does not use artifact deployment,
         * then we must check the dlPR jars for requisite jar inclusion
         */
        if(exportArtifact==null && implArtifact==null) {
            String localCodebase = System.getProperty(Constants.CODESERVER);
            /*File serviceUiJar = FileHelper.find(new File(rioHomeDir, "lib-dl"), "serviceui");
            File jskDlJar = FileHelper.find(new File(rioHomeDir, "lib-dl"), "serviceui");*/

            String[] requisiteExports = new String[]{"rio-dl-"+RioVersion.VERSION+".jar", "jsk-dl-2.2.2.jar"};
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
                    * (through the org.rioproject.service.Service
                    * interface).
                    *
                    * If there is no declared artifact, we sail through since
                    * the ServiceElement would have been constructed with the
                    * default platform export jars as part of it's creation.
                    */
                    if(implPR.getArtifact()!=null) {
                        if(localCodebase!=null) {
                            if(!localCodebase.endsWith("/"))
                                localCodebase = localCodebase+"/";
                            dlPR.addJar(new URL(localCodebase+export));
                        }
                    }
                }
            }
        }

        if(logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Service ").append(ServiceLogUtil.logName(elem)).append(" ");
            if(implArtifact!=null)
                sb.append("impl artifact: [").append(implPR.getArtifact()).append("] ");
            sb.append("impl jars: ");
            sb.append(implPR.getJarsAsString());
            sb.append(", ");
            if(dlPR.getArtifact()!=null)
                sb.append("export artifact: [").append(dlPR.getArtifact()).append("] ");
            sb.append("export jars: ");
            sb.append(dlPR.getJarsAsString());
            logger.debug(sb.toString());
        }

        map.put("impl", implPR);
        map.put("dl", dlPR);
        addProvisionedResources(map);
        registerArtifactCounts(map);
        return map;
    }

    private static ProvisionedResources getProvisionedResources(final String artifact) {
        ProvisionedResources pr = null;
        if(artifact!=null && !artifact.contains("SNAPSHOT")) {
            for(ProvisionedResources alreadyProvisioned : provisionedResources) {
                if(alreadyProvisioned.getArtifact()!=null &&
                   alreadyProvisioned.getArtifact().equals(artifact)) {
                    pr = alreadyProvisioned;
                    break;
                }
            }
        }
        return pr;
    }

    private static void addProvisionedResources(final Map<String, ProvisionedResources> map) {
        for(Map.Entry<String, ProvisionedResources> entry : map.entrySet()) {
            ProvisionedResources pr = entry.getValue();
            if(pr.getArtifact()!=null && !pr.getArtifact().contains("SNAPSHOT")) {
                if(!provisionedResources.contains(pr))
                    provisionedResources.add(pr);
            }
        }
    }

    private static void registerArtifactCounts(final Map<String, ProvisionedResources> map) {
        for(Map.Entry<String, ProvisionedResources> entry : map.entrySet()) {
            ProvisionedResources pr = entry.getValue();
            if(pr.getArtifact()!=null && !pr.getArtifact().contains("SNAPSHOT")) {
                AtomicInteger count = counterTable.get(pr.getArtifact());
                if(count==null)
                    count = new AtomicInteger(1);
                else
                    count.incrementAndGet();
                counterTable.put(pr.getArtifact(), count);
                if(logger.isTraceEnabled()) {
                    logger.trace("Counter for [{}] is now {}", pr.getArtifact(), count.get());
                }
            }
        }
    }
   
    private static class ProvisionedResources {
        List<URL> jarList = new ArrayList<URL>();
        String artifact;
        StringBuilder repositories = new StringBuilder();

        private ProvisionedResources(final String artifact) {
            this.artifact = artifact;
        }

        void setJars(final URL[] jars) {
            jarList.addAll(Arrays.asList(jars));
        }

        void addJar(final URL jar) {
            jarList.add(jar);
        }

        URL[] getJars() {
            return jarList.toArray(new URL[jarList.size()]);
        }

        String getArtifact() {
            return artifact;
        }

        String getJarsAsString() {
            return jarList.isEmpty() ? "<>" : jarList.toString();
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
                jarList.isEmpty() ? "<>" : jarList.toString());
            sb.append('}');
            return sb.toString();
        }
    }
}
