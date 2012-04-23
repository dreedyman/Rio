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
package org.rioproject.associations;

import org.rioproject.associations.strategy.FailOver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The AssociationInjector is an AssociationListener implementation that provides 
 * support for setter-based dependency injection by calling setters on a target 
 * object method when it receives association events.
 *
 * For example, we have a bean called Foo and it requires JavaSpace. In order to
 * have the JavaSpace proxy set, a setter method is required :
 * 
 * <pre>
 * public class Foo  {
 *     JavaSpace myJavaSpace;
 * 
 *     ...
 *     public void setMyJavaSpace(JavaSpace space) {
 *         myJavaSpace = space; 
 *     }
 *     ...
 * }
 * </pre>
 *
 * Finally, in the OperationalString, you'll need to create an Association with a 
 * Property attribute which matches the setter.
 * <pre>
 * associations {
 * &nbsp;&nbsp;&nbsp;&nbsp;association type:"uses", name="JavaSpace", property="myJavaSpace"
 * }
 * </pre>
 *
 * @author Dennis Reedy
 */
public class AssociationInjector<T> implements AssociationListener<T> {
    private Object target;
    /**
     * The ClassLoader which will be used to provide the caller/client
     * with a properly annotated proxy for associated services
     */
    private ClassLoader callerCL;
    /**
     * Map of Associations and injection methods
     */
    private final Map<Association, String> injectedMap = new HashMap<Association, String>();
    /**
     * Map of Associations to their generated proxy
     */
    private final Map<Association, AssociationProxy<T>> proxyMap =
        new HashMap<Association, AssociationProxy<T>>();
    private String targetPropertyName;
    static final String COMPONENT = "org.rioproject.associations";
    private static final Logger logger = Logger.getLogger(COMPONENT);

    /**
     * Create an AssociationInjector
     * 
     * @param target The object that will have dependencies injected
     */
    public AssociationInjector(Object target) {
        setBackend(target);
    }

    /**
     * Set the object that will have dependencies injected
     *
     * @param target The object that will have dependencies injected
     */
    public void setBackend(Object target)  {
        this.target = target;
    }

    /**
     * Set The ClassLoader which will be used to load proxies
     *
     * @param callerCL The classloader for the target
     */
    public void setCallerClassLoader(ClassLoader callerCL) {
        this.callerCL = callerCL;
    }

    private synchronized ClassLoader getCallerClassLoader() {
        if(callerCL==null)
            callerCL = Thread.currentThread().getContextClassLoader();
        return callerCL;
    }

    /**
     * If any AssociationProxy instances were created, make sure they are
     * terminated
     */
    public void terminate() {
        if(logger.isLoggable(Level.FINEST))
            logger.finest("Terminating injector, proxyMap size="+proxyMap.size());
        for(Map.Entry<Association, AssociationProxy<T>> entry : proxyMap.entrySet()) {
            AssociationProxy<T> aProxy = entry.getValue();
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Terminating association proxy "+aProxy);
            aProxy.terminate();
        }
        this.target = null;
        this.callerCL = null;
        this.injectedMap.clear();
        this.proxyMap.clear();
    }

    /**
     * For each generated association proxy, get the corresponding
     * AssociationDescriptor and how many successful invocations were made to
     * all services in the association.
     *
     * @return A Map whose keys are AssociationDescriptor, and values are the
     * number of successful invocations made to all services in the association.
     * If there are no generated association proxy instances, a Map with no
     * entries is returned. A new Map is created each time this method is called.
     */
    public Map<AssociationDescriptor, Long> getInvocationMap() {
        Map<AssociationDescriptor, Long> map =
            new HashMap<AssociationDescriptor, Long>();
        for(Map.Entry<Association, AssociationProxy<T>> entry : proxyMap.entrySet()) {
            AssociationProxy<T> aProxy = entry.getValue();
            map.put(aProxy.getAssociation().getAssociationDescriptor(),
                    aProxy.getInvocationCount());
        }
        return map;
    }

    void setTargetPropertyName(String targetPropertyName) {
        this.targetPropertyName = targetPropertyName;
    }

    private String getTargetPropertyName(Association association) {
        if(targetPropertyName==null)
            return association.getAssociationDescriptor().getPropertyName();
        return targetPropertyName;
    }

    @SuppressWarnings("unchecked")
    private void inject(Association association, T service) {
        String propertyName = getTargetPropertyName(association);
        if(propertyName==null) {
            if(logger.isLoggable(Level.FINEST))
                logger.finer("Association ["+association.toString()+"], "+
                             "does not have a declared propertyName, "+
                             "injection aborted");
            return;
        }

        Method method = getInjectionMethod(propertyName);
        if (method != null) {
            synchronized(injectedMap) {
                if(injectedMap.containsKey(association)) {
                    AssociationProxy<T> associationProxy = proxyMap.get(association);
                    if(associationProxy!=null)
                        associationProxy.discovered(association, service);
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Association ["+association.getName()+"] "+
                                    "has already been injected to " +
                                    "["+method+"]");
                    return;
                }
                String proxyClass =
                    association.getAssociationDescriptor().getProxyClass();
                /*
                 * Check for null proxyFactoryClass. If null,
                 * AssociationProxySupport is default
                 */
                proxyClass = (proxyClass==null?
                              AssociationProxySupport.class.getName() : proxyClass);

                String strategyClass =
                     association.getAssociationDescriptor().getServiceSelectionStrategy();
                /*
                 * Check for null strategyClass. If null, FailOver is default
                 */
                strategyClass = (strategyClass==null?
                                 FailOver.class.getName() : strategyClass);
                try {
                    AssociationProxy associationProxy =
                        (AssociationProxy)AssociationProxyFactory.createProxy(
                            proxyClass, strategyClass, association, getCallerClassLoader());
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Association "+
                                    "["+association.getName()+"], " +
                                    "is DISCOVERED, inject dependency");
                    }
                    method.invoke(target, getInjectionArg(method,
                                                          association,
                                                          (T)associationProxy));
                    if (logger.isLoggable(Level.FINE)) {
                        String targetClass=target==null?"null":target.getClass().getName();
                        logger.fine("Association "+
                                    "["+association.getName()+"], " +
                                    "is INJECTED using method " +
                                    "["+method.getName()+"] " +
                                    "on target class ["+targetClass+"]");
                    }
                    proxyMap.put(association, associationProxy);
                    //injected.add(method.toString());
                    injectedMap.put(association, method.toString());

                } catch (Throwable t) {
                    String svc = "eager injection";
                    if(service!=null)
                        svc = service.getClass().getName();
                    String nested = (t.getCause() == null ? "" :"\nCAUSE:");
                    logger.log(Level.WARNING,
                               "Injecting Association " +
                               "["+association.getName()+"], " +
                               "Service [" + svc + "]" + nested,
                               (t.getCause() == null ? t : t.getCause()));
                }
            }
        } else {
            logger.log(Level.WARNING,
                       "Association ["+association.getName()+"], "+
                       "with declared propertyName ["+propertyName+"] "+
                       "not found on target object "+
                       "["+target.getClass().getName()+"], Check method name " +
                       "matches setXXX and that the setter has a single " +
                       "parameter");
        }
    }

    private Method getInjectionMethod(String propertyName) {
        Method method=null;
        StringBuilder sb = new StringBuilder();
        sb.append("\nDeclared property name: [").append(propertyName).append("]\n");
        for (Method m : target.getClass().getMethods()) {
            String mName = m.getName();
            if(mName.startsWith("set")) {
                mName = mName.substring(3);
                mName = Character.toLowerCase(mName.charAt(0))+mName.substring(1);
                String mods = Modifier.toString(m.getModifiers());
                sb.append("\tproperty=").append(mName).append(" modifiers: [").append(mods).append("], ").append(m).append("\n");
                if(mName.equals(propertyName) && mods.contains("public")) {
                    method = m;
                    break;
                }
            }
        }
        sb.append("Selected method: ").append(method);
        if(logger.isLoggable(Level.FINE))
            logger.fine(sb.toString());
        return(method);
    }

    private Object getInjectionArg(Method method,
                                   Association association,
                                   T service) {
        Object arg;
        Type[] types = method.getParameterTypes();
        if(types.length!=1)
            throw new IllegalArgumentException("method ["+method.getName()+"], " +
                                               "expected [1] type, " +
                                               "found ["+types.length+"] " +
                                               "types ["+ Arrays.toString(types)+"]");
        if(Iterable.class.isAssignableFrom((Class<?>)types[0])) {
            arg = association;
        } else {
            /* Assume that the arg is the injection of the service */
            arg = service;
        }
        return(arg);
    }

    public void injectEmpty(Association<T> association) {
        inject(association, null);
    }

    /**
     * @see AssociationListener#discovered(Association, Object)
     */
    public void discovered(Association<T> association, T service) {
        inject(association, service);
    }

    /*
     * If AssociationProxy is not null, delegate to AssociationProxy
     */
    public void changed(Association<T> association, T service) {
        AssociationProxy<T> aProxy = proxyMap.get(association);
        if(aProxy!=null)
            aProxy.changed(association, service);
    }

    /*
     * If AssociationProxy is not null, delegate to AssociationProxy
     */
    public void broken(Association<T> association, T service) {
        AssociationProxy<T> aProxy = proxyMap.get(association);
        if(aProxy!=null)
            aProxy.broken(association, service);
    }

}
