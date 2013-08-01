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

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanFactory;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.opstring.ClassBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

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
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Creates the bean and the {@link org.rioproject.bean.BeanAdapter}
     * that will handle dynamic remoting and proxy delegation.
     *
     * @see org.rioproject.core.jsb.ServiceBeanFactory#create
     */
    public Created create(final ServiceBeanContext context) throws ServiceBeanInstantiationException {
        Object bean;
        Object proxy;
        BeanAdapter adapter;
        try {
            bean = getBean(context);
            adapter = new BeanAdapter(bean);
            /* Invoke the start method */
            proxy = adapter.start(context);
        } catch(Exception e) {
            if(e instanceof ServiceBeanInstantiationException)
                throw (ServiceBeanInstantiationException)e;
            throw new ServiceBeanInstantiationException("Service Instantiation Exception", e, true);
        }
        return (new Created(bean, proxy, adapter));
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
        final Thread currentThread = Thread.currentThread();
        ClassBundle bundle = context.getServiceElement().getComponentBundle();
        ClassLoader beanCL = currentThread.getContextClassLoader();
        Class<?> beanClass = beanCL.loadClass(bundle.getClassName());
        logger.trace("Load service class: {}", beanClass);
        logger.trace("Activating as ServiceBean");
        Constructor constructor = beanClass.getConstructor((Class[])null);
        logger.trace("Obtained implementation constructor: {}", constructor);
        return(constructor.newInstance((Object[])null));
    }

}
