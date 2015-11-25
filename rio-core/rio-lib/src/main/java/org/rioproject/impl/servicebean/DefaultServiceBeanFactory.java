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

import com.sun.jini.start.LifeCycle;
import com.sun.jini.start.ServiceProxyAccessor;
import net.jini.export.ProxyAccessor;
import org.rioproject.impl.bean.BeanFactory;
import org.rioproject.impl.config.ConfigHelper;
import org.rioproject.servicebean.ServiceBean;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.servicebean.ServiceBeanFactory;
import org.rioproject.impl.container.ServiceBeanLoader;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.opstring.ClassBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The DefaultServiceBeanFactory will load and create a ServiceBean.
 *
 * @author Dennis Reedy
 */
public class DefaultServiceBeanFactory implements ServiceBeanFactory {
    /** Component name logging */
    static String COMPONENT = "org.rioproject.impl.servicebean";
    /** A Logger */
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);
    /** Constructor types for a JSK service */
    private static final Class[] activationTypes = {String[].class,
                                                    LifeCycle.class};
    /** ServiceBean start method type */
    private static final Class[] startType = {ServiceBeanContext.class};

    /* (non-Javadoc)
     * @see org.rioproject.cybernode.ServiceBeanFactory#create(org.rioproject.core.servicebean.ServiceBeanContext)
     */
    public Created create(ServiceBeanContext context) throws ServiceBeanInstantiationException {

        final Thread currentThread = Thread.currentThread();
        final ClassLoader jsbCL = currentThread.getContextClassLoader();
        Created created;
        try {
            ClassBundle jsbBundle = context.getServiceElement().getComponentBundle();
            logger.trace("Loading class [{}], using ClassLoader: {}", jsbBundle.getClassName(), jsbCL.toString());
            Class<?> implClass = jsbCL.loadClass(jsbBundle.getClassName());
            if(useActivationConstructor(implClass) && !isServiceBean(implClass)) {
                Constructor constructor = implClass.getDeclaredConstructor(activationTypes);
                logger.trace("Obtained implementation constructor: {}", constructor);
                constructor.setAccessible(true);
                LifeCycle lifeCycle = (LifeCycle)context.getServiceBeanManager().getDiscardManager();
                String[] args = ConfigHelper.getConfigArgs(context.getServiceElement());
                Object impl = constructor.newInstance(args, lifeCycle);
                if(logger.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    for(String s : args) {
                        if(sb.length()>0)
                            sb.append("\n");
                        sb.append("\t").append(s);
                    }
                    logger.debug("{}/{} CONFIG ARGS: [{}]\n{}",
                                 context.getServiceElement().getOperationalStringName(),
                                 context.getServiceElement().getName(), args.length, sb);
                }
                Object proxy;
                logger.trace("Obtained implementation instance: {}", impl);
                if(impl instanceof ServiceProxyAccessor) {
                    proxy = ((ServiceProxyAccessor)impl).getServiceProxy();
                } else if(impl instanceof ProxyAccessor) {
                    proxy = ((ProxyAccessor)impl).getProxy();
                } else {
                    proxy = null; // just for insurance
                }
                created = new Created(impl, proxy);

            } else {
                if(isServiceBean(implClass)) {
                    logger.trace("Activating as ServiceBean");
                    Constructor constructor = implClass.getConstructor((Class[])null);
                    logger.trace("Obtained implementation constructor: {}", constructor);
                    Object impl = constructor.newInstance((Object[])null);
                    ServiceBean instance = (ServiceBean)impl;
                    logger.trace("Obtained implementation instance: {}", instance);
                    Object proxy = instance.start(context);
                    created = new Created(impl, proxy);
                } else {
                    BeanFactory beanFactory = new BeanFactory();
                    created = beanFactory.create(context);
                }
            }

        } catch(Throwable t) {
            logger.warn("Could not create service", t);
            ServiceBeanLoader.unload(jsbCL, context.getServiceElement());
            if(t instanceof ServiceBeanInstantiationException)
                throw (ServiceBeanInstantiationException)t;
            /* Get the cause if we have an InvocationTargetException, it will allow the thrown exception
             * to have a more meaningful cause. */
             if(t instanceof InvocationTargetException) {
                 t = t.getCause()==null? ((InvocationTargetException)t).getTargetException(): t.getCause();
            }
            throw new ServiceBeanInstantiationException("Service Instantiation Exception", t, true);
        }
        return created;
    }

    /*
     * Determine if a class has a declared constructor matching that required by
     * an activation constructor
     */
    static boolean useActivationConstructor(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor(activationTypes);
            return (true);
        } catch(NoSuchMethodException e) {
            return (false);
        }
    }

    /*
     * Determine if a class has method signatures as those found in the
     * ServiceBean
     */
    static boolean isServiceBean(Class<?> clazz) {
        try {
            clazz.getMethod("start", startType);
            return (true);
        } catch(NoSuchMethodException e) {
            return (false);
        }
    }
}
