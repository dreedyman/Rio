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
import org.rioproject.resources.util.ThrowableUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class that assists in invoking bean lifecycle events, either using
 * annotations or declared method names
 *
 * @author Dennis Reedy
 */
public class BeanHelper {
    private static final Logger logger = Logger.getLogger("org.rioproject.bean");

    /**
     * Invoke a lifecycle method
     *
     * @param annClass The annotation class to use
     * @param methodName The lifecycle method name to invoke if the annotation
     * is not found
     * @param bean The bean to invoke methods on
     * @throws ServiceBeanInstantiationException of errors occur
     */
    public static void invokeLifeCycle(final Class<? extends Annotation> annClass,
                                       final String methodName,
                                       final Object bean) throws ServiceBeanInstantiationException {
        if(bean==null) {
            return;
        }
        /* First check if the annotation is declared */
        String methodNameToInvoke = methodName;
        Method m = getAnnotatedMethod(bean, annClass);
        if (m != null) {
            methodNameToInvoke = m.getName();
        }
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        ClassLoader beanCL = bean.getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(beanCL);
        Throwable abort = null;
        try {
            Method method = bean.getClass().getMethod(methodNameToInvoke, (Class[]) null);
            if (logger.isLoggable(Level.FINEST))
                logger.finest(String.format("Invoking method [%s] on [%s]", methodNameToInvoke, bean.getClass().getName()));

            method.invoke(bean, (Object[]) null);
        } catch (NoSuchMethodException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(String.format("Bean [%s] does not have lifecycle method %s() defined",
                                            bean.getClass().getName(), methodNameToInvoke));
            }
        } catch (IllegalAccessException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                           String.format("Bean [%s] %s() method security access", bean.getClass().getName(), methodNameToInvoke),
                           e);
            }
            abort = e;
        } catch (InvocationTargetException e) {
            abort = ThrowableUtil.getRootCause(e);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                           String.format("Bean [%s] %s() method invocation", bean.getClass().getName(), methodNameToInvoke),
                           abort);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
        if(abort!=null) {
            String message = String.format("Invoking Bean [%s] %s() method", bean.getClass().getName(), methodNameToInvoke);
            throw new ServiceBeanInstantiationException(message, abort, true);
        }
    }

    /**
     * Helper to invoke a method on the bean using reflection
     *
     * @param bean The bean to act on
     * @param annClass The annotation class to use
     * @param methodName The method name
     * @param classArgs Class arguments
     * @param objectArgs Object argument
     * @return The result of dispatching the method
     * @throws ServiceBeanInstantiationException If errors occur
     */
    public static Object invokeBeanMethod(final Object bean,
                                          final Class<? extends Annotation> annClass,
                                          final String methodName,
                                          final Class[] classArgs,
                                          final Object[] objectArgs) throws ServiceBeanInstantiationException {
         /* First check if the annotation is declared */
        Method m = getAnnotatedMethod(bean, annClass);
        String methodNameToInvoke = methodName;
        if (m != null) {
            methodNameToInvoke = m.getName();
        }
        Object result = null;
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        ClassLoader beanCL = bean.getClass().getClassLoader();
        Throwable abort = null;
        Thread.currentThread().setContextClassLoader(beanCL);
        try {
            Method method = bean.getClass().getMethod(methodNameToInvoke, classArgs);
            result = method.invoke(bean, objectArgs);
        } catch (NoSuchMethodException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(String.format("Bean [%s] does not have a %s() method defined",
                                            bean.getClass().getName(), methodNameToInvoke));
            }
        } catch (IllegalAccessException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                           String.format("Bean [%s] %s() method security access", bean.getClass().getName(), methodNameToInvoke),
                           e);
            }
            abort = e;
        } catch (InvocationTargetException e) {
            abort = ThrowableUtil.getRootCause(e);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                           String.format("Bean [%s] %s() method invocation", bean.getClass().getName(), methodNameToInvoke),
                           abort);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
        if(abort!=null) {
            String message = String.format("Invoking Bean [%s] %s() method", bean.getClass().getName(), methodNameToInvoke);
            throw new ServiceBeanInstantiationException(message,
                                                        ThrowableUtil.getRootCause(abort),
                                                        true);
        }
        return (result);
    }

    public static Class getMethodFirstParamType(final String methodName, final Object o) {
        Class firstParamType = null;
        Method[] methods = o.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                firstParamType = getMethodFirstParamType(method);
            }
        }
        return (firstParamType);
    }

    public static Class getMethodFirstParamType(final Method method) {
        Class firstParamType = null;
        Class[] types = method.getParameterTypes();
        if (types.length > 0) {
            firstParamType = types[0];

        }
        return (firstParamType);
    }

    public static Method getAnnotatedMethod(final Object bean, final Class<? extends Annotation> annClass) {
        Method m = null;
        if(bean!=null && annClass!=null) {
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                if(method.getAnnotation(annClass)!=null) {
                    m = method;
                    break;
                }                
            }
        }
        return (m);
    }
}
