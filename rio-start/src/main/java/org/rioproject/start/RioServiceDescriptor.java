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
package org.rioproject.start;

import com.sun.jini.config.Config;
import com.sun.jini.start.*;
import net.jini.config.Configuration;
import net.jini.export.ProxyAccessor;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.policy.DynamicPolicyProvider;
import net.jini.security.policy.PolicyFileProvider;
import org.rioproject.config.PlatformCapabilityConfig;
import org.rioproject.config.PlatformLoader;
import org.rioproject.loader.ClassAnnotator;
import org.rioproject.loader.CommonClassLoader;
import org.rioproject.loader.ServiceClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The RioServiceDescriptor class is a utility that conforms to the Jini&trade;
 * technology ServiceStarter framework, and will start a service using the
 * {@link org.rioproject.loader.CommonClassLoader} as a shared, non-activatable,
 * in-process service. Clients construct this object with the details of the 
 * service to be launched, then call <code>create</code> to launch the service in 
 * invoking object's VM.
 * <P>
 * This class provides separation of the import codebase (where the server
 * classes are loaded from) from the export codebase (where clients should load
 * classes from for stubs, etc.) as well as providing an independent security
 * policy file for each service object. This functionality allows multiple
 * service objects to be placed in the same VM, with each object maintaining a
 * distinct codebase and policy.
 * <P>
 * Services need to implement the following "non-activatable constructor":
 * <blockquote>
 * 
 * <pre>
 * &lt;impl&gt;(String[] args, LifeCycle lc)
 * </pre>
 * 
 * </blockquote>
 *
 * where,
 * <UL>
 * <LI>args - are the service configuration arguments
 * <LI>lc - is the hosting environment's {@link LifeCycle} reference.
 * </UL>
 *
 * @author Dennis Reedy
 */
public class RioServiceDescriptor implements ServiceDescriptor {
    static String COMPONENT = RioServiceDescriptor.class.getPackage().getName();
    static Logger logger = LoggerFactory.getLogger(COMPONENT);
    /**
     * The parameter types for the "activation constructor".
     */
    private static final Class[] actTypes = {String[].class, LifeCycle.class};
    private String codebase;
    private String policy;
    private String classpath;
    private String implClassName;
    private String[] serverConfigArgs;
    private LifeCycle lifeCycle;
    private static LifeCycle NoOpLifeCycle = new LifeCycle() { // default, no-op
                                                               // object
        public boolean unregister(Object impl) {
            return false;
        }
    };
    private static AggregatePolicyProvider globalPolicy = null;
    private static Policy initialGlobalPolicy = null;
    /**
     * Object returned by
     * {@link RioServiceDescriptor#create(net.jini.config.Configuration)
     * RioServiceDescriptor.create()}
     * method that returns the proxy and implementation references
     * for the created service.
     */
    public static class Created {
        /** The reference to the proxy of the created service */
        public final Object proxy;
        /** The reference to the implementation of the created service */
        public final Object impl;
        /** Constructs an instance of this class.
         * @param impl reference to the implementation of the created service
         * @param proxy reference to the proxy of the created service
         */
        public Created(Object impl, Object proxy) {
            this.proxy = proxy;
            this.impl = impl;
        }
    }

    /**
     * Create a RioServiceDescriptor, assigning given parameters to their associated,
     * internal fields.
     * 
     * @param codebase location where clients can download required
     * service-related classes (for example, stubs, proxies, etc.). Codebase
     * components must be separated by spaces in which each component is in
     * <code>URL</code> format.
     * @param policy server policy filename or URL
     * @param classpath location where server implementation classes can be
     * found. Classpath components must be separated by path separators.
     * @param implClassName name of server implementation class
     * @param serverConfigArgs service configuration arguments 
     * @param lifeCycle <code>LifeCycle</code> reference for hosting
     * environment
     */
    public RioServiceDescriptor(String codebase, 
                            String policy, 
                            String classpath,
                            String implClassName,
                            // Optional Args
                            LifeCycle lifeCycle,
                            String... serverConfigArgs) {
        if(codebase == null || policy == null || classpath == null || implClassName == null)
            throw new IllegalArgumentException("Codebase, policy, classpath, and implementation cannot be null");
        this.codebase = codebase;
        this.policy = policy;
        this.classpath = setClasspath(classpath);
        this.implClassName = implClassName;
        this.serverConfigArgs = serverConfigArgs;
        this.lifeCycle = (lifeCycle == null) ? NoOpLifeCycle : lifeCycle;
    }
    
    /**
     * Create a RioServiceDescriptor. Equivalent to calling the other overloaded
     * constructor with <code>null</code> for the <code>LifeCycle</code>
     * reference.
     *
     * @param codebase location where clients can download required
     * service-related classes (for example, stubs, proxies, etc.). Codebase
     * components must be separated by spaces in which each component is in
     * <code>URL</code> format.
     * @param policy server policy filename or URL
     * @param classpath location where server implementation classes can be
     * found. Classpath components must be separated by path separators.
     * @param implClassName name of server implementation class
     * @param serverConfigArgs service configuration arguments
     */
    public RioServiceDescriptor(String codebase,
                                String policy,
                                String classpath,
                                String implClassName,
                                // Optional Args
                                String... serverConfigArgs) {
        this(codebase, policy, classpath, implClassName, null, serverConfigArgs);
    }
    
    /**
     * Codebase accessor method.
     * 
     * @return The codebase string associated with this service descriptor.
     */
    public String getCodebase() {
        return codebase;
    }

    /**
     * Policy accessor method.
     * 
     * @return The policy string associated with this service descriptor.
     */
    public String getPolicy() {
        return policy;
    }
        
    /**
     * <code>LifeCycle</code> accessor method.
     *
     * @return The <code>LifeCycle</code> object associated with 
     * this service descriptor.
     */
    @SuppressWarnings("unused")
    public LifeCycle getLifeCycle() {
        return lifeCycle;
    }
    
    /**
     * LifCycle accessor method.
     * 
     * @return The classpath string associated with this service descriptor.
     */
    public String getClasspath() {
        return classpath;
    }

    /**
     * Implementation class accessor method.
     * 
     * @return The implementation class string associated with this service
     * descriptor.
     */
    public String getImplClassName() {
        return implClassName;
    }

    /**
     * Service configuration arguments accessor method.
     * 
     * @return The service configuration arguments associated with this service
     * descriptor.
     */
    public String[] getServerConfigArgs() {
        return (serverConfigArgs != null)? serverConfigArgs.clone(): null;
    }    
    
    synchronized void ensureSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    /**
     * @see com.sun.jini.start.ServiceDescriptor#create
     */
    public Object create(Configuration config) throws Exception {
        ensureSecurityManager();
        Object proxy = null;

        /* Warn user of inaccessible codebase(s) */
        if(getCodebase().startsWith("http"))
            HTTPDStatus.httpdWarning(getCodebase());

        /* Set common JARs to the CommonClassLoader */
        String defaultDir = null;
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome==null) {
            logger.warn("RIO_HOME not defined, no default platformDir");
        } else {
            defaultDir = rioHome+ File.separator+"config"+File.separator+"platform";
        }

        PlatformLoader platformLoader = new PlatformLoader();
        List<URL> urlList = new ArrayList<URL>();
        PlatformCapabilityConfig[] caps = platformLoader.getDefaultPlatform(rioHome);
        for (PlatformCapabilityConfig cap : caps) {
            URL[] urls = cap.getClasspathURLs();
            urlList.addAll(Arrays.asList(urls));
        }

        String platformDir = (String)config.getEntry(COMPONENT, "platformDir", String.class, defaultDir);
        logger.debug("Platform directory set as {}", platformDir);
        caps = platformLoader.parsePlatform(platformDir);
        for (PlatformCapabilityConfig cap : caps) {
            if (cap.getCommon()) {
                URL[] urls = cap.getClasspathURLs();
                urlList.addAll(Arrays.asList(urls));
            }
        }

        URL[] commonJARs = urlList.toArray(new URL[urlList.size()]);

        /*
        if(commonJARs.length==0)
            throw new RuntimeException("No commonJARs have been defined");
        */
        CommonClassLoader commonCL = CommonClassLoader.getInstance();
        logger.debug("Created {}", commonCL.getClass().getName());
        if(logger.isTraceEnabled()) {
            StringBuilder buffer = new StringBuilder();
            for(int i=0; i<commonJARs.length; i++) {
                if(i>0)
                    buffer.append("\n");
                buffer.append(commonJARs[i].toExternalForm());
            }
            logger.trace("commonJARs=\n{}", buffer.toString());
        }
        commonCL.addCommonJARs(commonJARs);
        final Thread currentThread = Thread.currentThread();
        ClassLoader currentClassLoader = currentThread.getContextClassLoader();

        ClassAnnotator annotator = new ClassAnnotator(ClassLoaderUtil.getCodebaseURLs(getCodebase()));

        ServiceClassLoader serviceCL =
            new ServiceClassLoader(ServiceClassLoader.getURIs(ClassLoaderUtil.getClasspathURLs(getClasspath())),
                                   annotator,
                                   commonCL);
        logger.debug("Created {}", serviceCL);
        if(logger.isTraceEnabled())
            ClassLoaderUtil.displayClassLoaderTree(serviceCL);
        
        currentThread.setContextClassLoader(serviceCL);
        /* Get the ProxyPreparer */
        ProxyPreparer servicePreparer = (ProxyPreparer)Config.getNonNullEntry(config,
                                                                              COMPONENT,
                                                                              "servicePreparer",
                                                                              ProxyPreparer.class,
                                                                              new BasicProxyPreparer());
        synchronized(RioServiceDescriptor.class) {
            /* supplant global policy 1st time through */
            if(globalPolicy == null) {
                initialGlobalPolicy = Policy.getPolicy();
                globalPolicy = new AggregatePolicyProvider(initialGlobalPolicy);
                Policy.setPolicy(globalPolicy);
                logger.trace("Global policy set: {}", globalPolicy.toString());
            }                        
            DynamicPolicyProvider service_policy = new DynamicPolicyProvider(new PolicyFileProvider(getPolicy()));
            LoaderSplitPolicyProvider splitServicePolicy =
                new LoaderSplitPolicyProvider(serviceCL, service_policy, new DynamicPolicyProvider(initialGlobalPolicy));
            /*
             * Grant "this" code enough permission to do its work under the
             * service policy, which takes effect (below) after the context
             * loader is (re)set.
             */
            splitServicePolicy.grant(RioServiceDescriptor.class,
                                     null, /* Principal[] */
                                     new Permission[]{new AllPermission()});
            globalPolicy.setPolicy(serviceCL, splitServicePolicy);
        }
        Object impl;
        try {
            Class<?> implClass;
            implClass = Class.forName(getImplClassName(), false, serviceCL);
            logger.trace("Attempting to get implementation constructor");
            Constructor constructor = implClass.getDeclaredConstructor(actTypes);
            logger.trace("Obtained implementation constructor: {}", constructor.toString());
            constructor.setAccessible(true);
            impl = constructor.newInstance(getServerConfigArgs(), lifeCycle);
            logger.trace("Obtained implementation instance: {}", impl.toString());
            if(impl instanceof ServiceProxyAccessor) {
                proxy = ((ServiceProxyAccessor)impl).getServiceProxy();
            } else if(impl instanceof ProxyAccessor) {
                proxy = ((ProxyAccessor)impl).getProxy();
            } else {
                proxy = null; // just for insurance
            }
            if(proxy != null) {
                proxy = servicePreparer.prepareProxy(proxy);
            }
            logger.trace("Proxy:  {}", proxy==null?"<NULL>":proxy.toString());
            currentThread.setContextClassLoader(currentClassLoader);
        } catch(InvocationTargetException e) {
            Throwable t = e.getCause()==null? e.getTargetException(): e.getCause();
            if(t!=null && t instanceof Exception)
                throw (Exception)t;
            throw e;

        } finally {
            currentThread.setContextClassLoader(currentClassLoader);
        }
        return(new Created(impl, proxy));
    }

    /*
     * Iterate through the classpath, for each jar see if there is a ClassPath
     * manifest setting. If there is, append the settings to the classpath
     */
    private String setClasspath(String cp) {
        logger.debug("Create classpath from [{}]", cp);
        StringBuilder buff = new StringBuilder();
        for(String s : toArray(cp, "," + File.pathSeparator)) {
            if(buff.length()>0 && !buff.toString().endsWith(File.pathSeparator))
                buff.append(File.pathSeparator);
            buff.append(s);
            File f = new File(s);
            try {
                String jarPath = f.getParentFile().getCanonicalPath();
                if(!jarPath.endsWith(File.separator)) {
                    jarPath = jarPath+File.separator;
                }
                logger.debug("Creating jar file path from [{}]", f.getCanonicalPath());
                JarFile jar = new JarFile(f);
                Manifest man = jar.getManifest();
                if (man == null) {
                    buff.append(File.pathSeparator);
                    continue;
                }
                Attributes attributes = man.getMainAttributes();
                if (attributes == null) {
                    buff.append(File.pathSeparator);
                    continue;
                }
                String values = (String)attributes.get(new Attributes.Name("Class-Path"));
                if(values!=null) {
                    for(String v : toArray(values, " ," + File.pathSeparator)) {
                        buff.append(File.pathSeparator);
                        String name = jarPath+v;
                        File add = new File(name);
                        buff.append(add.getCanonicalPath());
                    }
                } else {
                    buff.append(File.pathSeparator);
                }
            } catch (IOException e) {
                logger.warn("While trying to create classpath", e);
            }
        }
        logger.debug("Classpath created [{}]", buff.toString());
        return buff.toString();
    }

    private String[] toArray(String arg, String delim) {
        StringTokenizer tok = new StringTokenizer(arg, delim);
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return array;
    }

    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("RioServiceDescriptor codebase: ").append(codebase).append(", ");
        toStringBuilder.append("policy: ").append(policy).append(", ");
        toStringBuilder.append("classpath: ").append(classpath).append(", ");
        toStringBuilder.append("implClassName: ").append(implClassName).append(", ");
        toStringBuilder.append("serverConfigArgs: ").append((serverConfigArgs == null ? null :Arrays.asList(serverConfigArgs))).append(", ");
        toStringBuilder.append("lifeCycle: ").append(lifeCycle);
        return toStringBuilder.toString();
    }
}
