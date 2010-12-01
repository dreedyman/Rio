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
package org.rioproject.bean;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.boot.ClassAnnotator;
import org.rioproject.boot.ServiceClassLoader;
import org.rioproject.config.AggregateConfig;
import org.rioproject.core.ClassBundle;
import org.rioproject.core.JSBInstantiationException;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanFactory;

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Instantiates a Plain Old Java Object (POJO), providing support for
 * remote invocations and administration by adapting the POJO into the
 * framework using the {@link org.rioproject.bean.BeanAdapter} class
 *
 * @see org.rioproject.core.jsb.ServiceBeanFactory
 * @see org.rioproject.bean.BeanAdapter
 *
 * @author Dennis Reedy
 */
public class BeanFactory implements ServiceBeanFactory {
    /** Component name for the logger */
    static String COMPONENT = "org.rioproject.bean";
    /** A Logger */
    static final Logger logger = Logger.getLogger(COMPONENT);

    /**
     * Creates the bean and the {@link org.rioproject.bean.BeanAdapter}
     * which will handle dynamic remoting and proxy delegation.
     *
     * @see org.rioproject.core.jsb.ServiceBeanFactory#create
     */
    public Created create(ServiceBeanContext context)
    throws JSBInstantiationException {
        Object bean;
        Object proxy;
        try {
            String[] dlJars = getPlatformDLJars(context.getConfiguration());
            for(String dlJar : dlJars)
                verifyCodebaseContent(context, dlJar);
            bean = getBean(context);
            BeanAdapter adapter = new BeanAdapter(bean);
            /* Invoke the start method */
            proxy = adapter.start(context);
        } catch(Exception e) {
            if(e instanceof JSBInstantiationException)
                throw (JSBInstantiationException)e;
            throw new JSBInstantiationException("Service " +
                                                "Instantiation Exception",
                                                e);
        }
        return (new Created(bean, proxy));
    }

    /**
     *
     */
    /**
     * Get the jar names which should be part of the codebase.
     *
     * @param config The configuration to use hen loading the
     * org.rioproject.bean.platformDLJars property
     *
     * @return A String array of platform jars, defaulting to rio-dl.jar
     *
     * @throws ConfigurationException If there are exceptions accessing the
     * property
     */
    protected String[] getPlatformDLJars(Configuration config)
        throws ConfigurationException {
        if(config instanceof AggregateConfig) {
            config = ((AggregateConfig)config).getCommonConfiguration();
        }
        String[] configuredJars =
            (String[])config.getEntry(COMPONENT,
                                      "platformDLJars",
                                      String[].class,
                                      new String[]{"rio-dl.jar", "jsk-dl.jar"});

        return(configuredJars);
    }

    /*
     * Check the codebase to ensure correct download jars are declared.
     */
    private void verifyCodebaseContent(ServiceBeanContext context,
                                       String dlJar)
        throws MalformedURLException, JSBInstantiationException {
        /* Check if the bean has DL Jar defined in it's codebase */
        boolean dlFound = false;
        ClassBundle[] exports =
            context.getServiceElement().getExportBundles();
        for(ClassBundle export : exports) {
            String[] jars = export.getJARNames();
            for(String jar : jars) {
                if(jar.equals(dlJar)) {
                    dlFound = true;
                    break;
                }
            }
            if(dlFound)
                break;
        }
        if(!dlFound && exports.length>0) {
            final Thread currentThread = Thread.currentThread();
            ClassLoader beanCL = currentThread.getContextClassLoader();
            if(beanCL instanceof ServiceClassLoader) {
                ServiceClassLoader sCL = (ServiceClassLoader)beanCL;
                ClassAnnotator annotator = sCL.getClassAnnotator();
                String codebase = exports[0].getCodebase();
                if(codebase==null) {
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Unable to add "+dlJar+" to codebase for ["+
                                    context.getServiceElement().getName()+
                                    "] service, no codebase set");
                    return;
                }
                URL[] urls = annotator.getURLs();
                ArrayList<URL> list = new ArrayList<URL>();
                list.addAll(Arrays.asList(urls));
                list.add(new URL(codebase+dlJar));
                annotator.setAnnotationURLs(list.toArray(new URL[list.size()]));
                if(logger.isLoggable(Level.FINE))
                    logger.fine("Added "+dlJar+" to codebase for ["+
                                context.getServiceElement().getName()+
                                "] service");
            } else {
                throw new JSBInstantiationException("Service created "+
                                                    "with an unsupported "+
                                                    "classloader="+
                                                    beanCL.toString());
            }
        }
    }

    /**
     * Get the bean object.
     *
     * @param context The context used to obtain details on what to load
     *
     * @return The loaded bean
     *
     * @throws Exception If there are errors loading the bean
     */
    protected Object getBean(ServiceBeanContext context) throws Exception {
        final Thread currentThread = Thread.currentThread();
        ClassBundle bundle =
            context.getServiceElement().getComponentBundle();
        ClassLoader beanCL = currentThread.getContextClassLoader();
        Class beanClass = beanCL.loadClass(bundle.getClassName());
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST,
                       "Load service class: {0}",
                       beanClass);
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST,
                       "Activating as ServiceBean");
        Constructor constructor = beanClass.getConstructor((Class[])null);
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST,
                       "Obtained implementation constructor: {0}",
                       constructor);
        return(constructor.newInstance((Object[])null));
    }

}
