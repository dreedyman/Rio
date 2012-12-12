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
package org.rioproject.bean.spring;

import org.rioproject.bean.BeanFactory;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanManager;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.jsb.JSBManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Instantiates a Plain Old Java Object (POJO) using Spring, providing support
 * for remote invocations and administration by adapting the POJO into the
 * framework using the Spring framework.
 *
 * @see org.rioproject.core.jsb.ServiceBeanFactory
 * @see org.rioproject.bean.BeanFactory
 *
 * @author Dennis Reedy
 */
public class SpringBeanFactory extends BeanFactory {
    /** Codebase token to be replaced by the service's actual token */
    static final String CODEBASE_TOK = "$codebase";
    /** Codebase token to be replaced by the service's actual token */
    static final String CLASSPATH_TOK = "$classpath";
    private boolean useCodebase;
    /** Component name for the logger */
    static final String COMPONENT = "org.rioproject.bean.spring";
    /** A Logger */
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Get the bean object
     */
    protected Object getBean(ServiceBeanContext context) throws Exception {
        String[] configs = (String[])context.getConfiguration().
                                             getEntry("spring",
                                                      "config",
                                                      String[].class,
                                                      new String[]{""});
        if(configs.length==0) {
            throw new ServiceBeanInstantiationException("No Spring service " +
                                                "configuration");
        }
        String codebase = context.getServiceElement().getExportBundles()[0].getCodebase();
        for(int i=0; i<configs.length; i++) {
            if(configs[i].contains(CODEBASE_TOK + "/")) {
                configs[i] = replace(configs[i], CODEBASE_TOK+"/", codebase);
                useCodebase = true;
                logger.debug("Loading application context [{}]", configs[i]);
            } else if (configs[i].contains(CLASSPATH_TOK + "/")) {
                configs[i] = replace(configs[i], CLASSPATH_TOK+"/", codebase);
                logger.debug("Loading application context [{}]", configs[i]);
            } else {
                configs[i] = replace(configs[i], CODEBASE_TOK, codebase);
                logger.debug("Loading application context [{}]", configs[i]);
            }
        }
        final Thread currentThread = Thread.currentThread();
        ClassLoader cCL =
            AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return (currentThread.getContextClassLoader());
                }
            });
        /*
         * Reflection is used here because Spring technology classes may be
         * loaded by a child classloader of the the classloader which loaded
         * this class. If this is the case then we will be facing
         * NoClassDefFoundError exceptions.
         */
        /*
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.setClassLoader(cCL);
        */

        Class ctxClass = Class.forName( "org.springframework.context.support.GenericApplicationContext", true, cCL);
        Object ctx = ctxClass.newInstance();

        Method ctx_setClassLoader = ctxClass.getMethod("setClassLoader", ClassLoader.class);
        ctx_setClassLoader.invoke(ctx, cCL);

        //XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        Class xmlReaderClass = Class.forName( "org.springframework.beans.factory.xml.XmlBeanDefinitionReader", true, cCL);
        Constructor[] cons = xmlReaderClass.getConstructors();
        Object xmlReader = null;
        for (Constructor con : cons) {
            Class[] types = con.getParameterTypes();
            if (types.length == 1 &&
                types[0].getName().contains("BeanDefinitionRegistry")) {
                xmlReader = con.newInstance(ctx);
                break;
            }
        }

        Method[] methods = xmlReaderClass.getMethods();
        Method xmlReader_loadBeanDefinitions = null;
        for (Method method : methods) {
            if (method.getName().equals("loadBeanDefinitions")) {
                Class[] types = method.getParameterTypes();
                for (Class type : types) {
                    if (type.getName().equals("org.springframework.core.io.Resource")) {
                        xmlReader_loadBeanDefinitions = method;
                        break;
                    }
                }
                if (xmlReader_loadBeanDefinitions != null)
                    break;
            }
        }

        String resourceClassName;
        if(useCodebase) {
            resourceClassName = "org.springframework.core.io.UrlResource";
        } else {
            resourceClassName = "org.springframework.core.io.ClassPathResource";
        }
        
        Class resourceClass = Class.forName(resourceClassName, true, cCL);
        Constructor resourceCons = resourceClass.getConstructor(String.class);

        for (String config : configs) {
            Object resource = resourceCons.newInstance(config);
            assert xmlReader_loadBeanDefinitions != null;
            xmlReader_loadBeanDefinitions.invoke(xmlReader, resource);
        }

        /*
        for(int i=0; i<configs.length; i++) {
            xmlReader.loadBeanDefinitions(new UrlResource(configs[i]));
        }
        ctx.refresh();
        */

        Method ctx_refresh = ctxClass.getMethod("refresh", (Class[])null);
        ctx_refresh.invoke(ctx, (Object[])null);

        ServiceBeanManager mgr = context.getServiceBeanManager();
        if(mgr instanceof JSBManager) {
            SpringDiscardManager sdm = new SpringDiscardManager(ctx, context.getServiceBeanManager().getDiscardManager());
            ((JSBManager)mgr).setDiscardManager(sdm);
        } else {
            logger.warn("Unable to set Spring DiscardManager, unrecognized ServiceBeanManager");
        }
        String beanName = context.getServiceElement().getName();
        Method ctx_getBean = ctxClass.getMethod("getBean", String.class);
        Object bean = ctx_getBean.invoke(ctx, beanName);
        return(bean);

        //return(ctx.getBean(context.getServiceElement().getName()));

    }

    /*
     * Regular Expression Search and Replace
     */
    String replace(String str, String pattern, String replace) {
        int s = 0;
        int e;
        StringBuilder result = new StringBuilder();

        while((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e+pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }
}
