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
     * @throws Exception of errors occur
     */
    public static void invokeLifeCycle(Class<? extends Annotation> annClass,
                                       String methodName,
                                       Object bean) throws Exception {
        /* First check if the annotation is declared */
        Method m = getAnnotatedMethod(bean, annClass);
        if (m != null) {
            methodName = m.getName();
        }
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        ClassLoader beanCL = bean.getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(beanCL);
        try {
            Method method = bean.getClass().getMethod(methodName,
                                                      (Class[]) null);
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Invoking method ["+methodName+"] " +
                              "on ["+bean.getClass().getName()+"]");
            method.invoke(bean, (Object[]) null);
        } catch (NoSuchMethodException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Bean [" + bean.getClass().getName() + "] " +
                              "does not have lifecycle method "
                              + methodName + "() " +
                              "defined");
            }
        } catch (IllegalAccessException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                           "Bean [" + bean.getClass().getName() + "] " +
                           methodName + "() " +
                           "method security access",
                           e);
            }
        } catch (InvocationTargetException e) {
            throw new Exception(
                "Invoking Bean [" + bean.getClass().getName() + "] " +
                methodName + "() method",
                ThrowableUtil.getRootCause(e));
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
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
     * @throws Exception If errors occur
     */
    public static Object invokeBeanMethod(Object bean,
                                          Class<? extends Annotation> annClass,
                                          String methodName,
                                          Class[] classArgs,
                                          Object[] objectArgs) throws
                                                               Exception {
         /* First check if the annotation is declared */
        Method m = getAnnotatedMethod(bean, annClass);
        if (m != null) {            
            methodName = m.getName();
        }
        Object result = null;
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        ClassLoader beanCL = bean.getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(beanCL);
        try {
            Method method = bean.getClass().getMethod(methodName,
                                                      classArgs);
            result = method.invoke(bean, objectArgs);
        } catch (NoSuchMethodException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Bean [" + bean.getClass().getName() + "] " +
                              "does not have a " + methodName + "() " +
                              "method defined");
            }
        } catch (IllegalAccessException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                           "Bean [" + bean.getClass().getName() + "] " +
                           methodName + "() " +
                           "method security access",
                           e);
            }
        } catch (InvocationTargetException e) {
            throw new Exception(
                "Invoking Bean [" + bean.getClass().getName() + "] " +
                methodName + "() method",
                ThrowableUtil.getRootCause(e));
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
        return (result);
    }

    public static Class getMethodFirstParamType(String methodName, Object o) {
        Class firstParamType = null;
        Method[] methods = o.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                firstParamType = getMethodFirstParamType(method);
            }
        }
        return (firstParamType);
    }

    public static Class getMethodFirstParamType(Method method) {
        Class firstParamType = null;
        Class[] types = method.getParameterTypes();
        if (types.length > 0) {
            firstParamType = types[0];

        }
        return (firstParamType);
    }

    public static Method getAnnotatedMethod(Object bean,
                                             Class<? extends Annotation> annClass) {
        Method m = null;
        if(annClass!=null) {
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
