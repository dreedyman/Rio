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

import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanFactory;

import java.lang.reflect.Constructor;
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
     * that will handle dynamic remoting and proxy delegation.
     *
     * @see org.rioproject.core.jsb.ServiceBeanFactory#create
     */
    public Created create(ServiceBeanContext context) throws ServiceBeanInstantiationException {
        Object bean;
        Object proxy;
        try {
            bean = getBean(context);
            BeanAdapter adapter = new BeanAdapter(bean);
            /* Invoke the start method */
            proxy = adapter.start(context);
        } catch(Exception e) {
            if(e instanceof ServiceBeanInstantiationException)
                throw (ServiceBeanInstantiationException)e;
            throw new ServiceBeanInstantiationException("Service Instantiation Exception", e, true);
        }
        return (new Created(bean, proxy));
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
        ClassBundle bundle = context.getServiceElement().getComponentBundle();
        ClassLoader beanCL = currentThread.getContextClassLoader();
        Class beanClass = beanCL.loadClass(bundle.getClassName());
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "Load service class: {0}", beanClass);
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "Activating as ServiceBean");
        Constructor constructor = beanClass.getConstructor((Class[])null);
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "Obtained implementation constructor: {0}", constructor);
        return(constructor.newInstance((Object[])null));
    }

}
