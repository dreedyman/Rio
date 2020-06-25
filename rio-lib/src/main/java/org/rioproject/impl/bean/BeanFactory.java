/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.impl.bean;

import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.servicebean.ServiceBeanFactory;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.opstring.ClassBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Instantiates a Plain Old Java Object (POJO), providing support for
 * remote invocations and administration by adapting the POJO into the
 * framework using the {@link BeanAdapter} class
 *
 * @see org.rioproject.servicebean.ServiceBeanFactory
 * @see BeanAdapter
 *
 * @author Dennis Reedy
 */
public class BeanFactory implements ServiceBeanFactory {
    private static final Logger logger = LoggerFactory.getLogger(BeanFactory.class);

    /**
     * Creates the bean and the {@link BeanAdapter}
     * that will handle dynamic remoting and proxy delegation.
     *
     * @see org.rioproject.servicebean.ServiceBeanFactory#create
     */
    public Created create(final ServiceBeanContext context) throws ServiceBeanInstantiationException {
        Object bean;
        Object proxy;
        BeanAdapter adapter;
        try {
            bean = getBean(context);
            logger.trace("Obtained bean: {}", bean);
            adapter = new BeanAdapter(bean);
            /* Invoke the start method */
            proxy = adapter.start(context);
        } catch(Exception e) {
            logger.error("Failed creating bean", e);
            if(e instanceof ServiceBeanInstantiationException)
                throw (ServiceBeanInstantiationException)e;
            throw new ServiceBeanInstantiationException("Service Instantiation Exception", e, true);
        }
        return new Created(bean, proxy, adapter);
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
    protected Object getBean(final ServiceBeanContext context) throws Exception {
        logger.trace("Getting currentThread");
        final Thread currentThread = Thread.currentThread();
        logger.trace("Getting component bundle");
        ClassBundle bundle = context.getServiceElement().getComponentBundle();
        logger.trace("Getting bean ClassLoader");
        ClassLoader beanCL = currentThread.getContextClassLoader();
        logger.trace("Loading bean class: {}", bundle.getClassName());
        Class<?> beanClass = beanCL.loadClass(bundle.getClassName());
        logger.trace("Loaded service class: {}", beanClass);
        logger.trace("Activating as ServiceBean");
        Constructor constructor = beanClass.getConstructor((Class[])null);
        logger.trace("Obtained implementation constructor: {}", constructor);
        return constructor.newInstance((Object[])null);
    }

}
