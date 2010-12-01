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

import org.rioproject.resources.servicecore.Service;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link java.lang.reflect.InvocationHandler} that delegates method
 * calls between the {@link org.rioproject.bean.BeanAdapter} and the bean
 *
 * @author Dennis Reedy
 */
public class BeanDelegator implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 1L;
    private Set<PackagedMethod> methodSet = new HashSet<PackagedMethod>();
    private Object service;
    private Object bean;
    private static Logger logger = Logger.getLogger("org.rioproject.bean");

    /*
     * Private constructor, instantiable only from the static factory
     */
    private BeanDelegator(Object service, Object bean) {
        this.service = service;
        this.bean = bean;
        Method[] methods = Service.class.getMethods();
        for(Method method : methods) {
            methodSet.add(new PackagedMethod(method));
        }
    }

    /**
     * Get an instance of the BeanDelegator using the ClassLoader of the
     * <tt>bean</tt> parameter to define the proxy class
     *
     * @param service The service, must not be <code>null</code>
     * @param bean The bean, must not be <code>null</code>
     * @param interfaces The interfaces to expose, must not be <code>null</code>
     *
     * @return An object suitable for use as a proxy. A new
     * proxy will be created each time
     *
     * @throws NullPointerException if any of the parameters is
     * <code>null</code>
     * @throws IllegalArgumentException If the interfaces parameter has a
     * zero length
     */
    public static Object getInstance(Object service,
                                     Object bean,
                                     Class[] interfaces) {
        return(getInstance(service,
                           bean,
                           interfaces,
                           bean.getClass().getClassLoader()));
    }

    /**
     * Get an instance of the BeanDelegator
     *
     * @param service The service, must not be <code>null</code>
     * @param bean The bean, must not be <code>null</code>
     * @param interfaces The interfaces to expose, must not be
     * <code>null</code>
     * @param loader The class loader to define the proxy class
     *
     * @return An object suitable for use as a proxy. A new proxy will be
     *         created each time
     *
     * @throws NullPointerException if any of the parameters is
     * <code>null</code>
     * @throws IllegalArgumentException If the interfaces parameter has a zero
     * length
     */
    public static Object getInstance(Object service,
                                     Object bean,
                                     Class[] interfaces,
                                     ClassLoader loader) {
        if(service == null)
            throw new NullPointerException("service is null");
        if(bean == null)
            throw new NullPointerException("bean is null");
        if(interfaces == null)
            throw new NullPointerException("interfaces is null");
        if(interfaces.length == 0)
            throw new IllegalArgumentException("interfaces must contain values");

        return (Proxy.newProxyInstance(loader,
                                       interfaces,
                                       new BeanDelegator(service,
                                                         bean)));
    }

    /**
     * Perform the invocation, directing the method request to the appropriate
     * implementation
     *
     * @see InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
        PackagedMethod template = null;
        try {
            template = new PackagedMethod(method);
            if(methodSet.contains(template)) {
                if(logger.isLoggable(Level.FINEST)) {
                    logger.finest("Method "+method.getName()+", " +
                                  "invocation found in ServiceBean using " +
                                  "template "+template);
                }
                return method.invoke(service, args);
            } else {
                if(logger.isLoggable(Level.FINEST)) {
                    logger.finest("Method "+method.getName()+", " +
                                  "invocation being performed on " +
                                  bean.getClass().getName()+", "+
                                  "no matching method found on service bean " +
                                  "using template "+template);
                }
                Class beanClass = bean.getClass();
                Method beanMethod =
                    beanClass.getMethod(method.getName(),
                                        method.getParameterTypes());
                return(beanMethod.invoke(bean, args));
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } finally {
            if(template!=null)
                template.clear();
        }
    }
}
