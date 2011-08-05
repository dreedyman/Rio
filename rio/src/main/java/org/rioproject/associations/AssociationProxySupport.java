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

//import net.sf.cglib.proxy.*;
import net.jini.core.discovery.LookupLocator;
import org.rioproject.resources.util.ThrowableUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides support for an {@link AssociationProxy}
 *
 * @author Dennis Reedy
 */
public class AssociationProxySupport<T> implements AssociationProxy<T> {
    private ServiceSelectionStrategy<T> strategy;
    private final List<String> proxyMethods = new ArrayList<String>();
    Logger logger = Logger.getLogger(AssociationProxy.class.getName());
    private AtomicLong invocationCount = new AtomicLong();
    private boolean terminated;

    /**
     * Create an InvocationHandler
     *
     * @param association The Association to use
     *
     * @return An InvocationHandler for use with a dynamic JDK proxy. This
     * method returns a
     * {@link org.rioproject.associations.AssociationProxySupport.AssociationInvocationHandler}
     * instance
     */
    public InvocationHandler getInvocationHandler(final Association<T> association) {
        return new AssociationInvocationHandler(association, this);
    }

    /**
     * Create an array of {@link net.sf.cglib.proxy.Callback}
     *
     * @param association The Association to use
     *
     * @return An array of {@link net.sf.cglib.proxy.Callback} for use with a
     * generated CGLIB proxy. This method returns a
     * {@link org.rioproject.associations.AssociationProxySupport.LocalInterceptor} as
     * the first <tt>Callback, followed by the
     * {@link org.rioproject.associations.AssociationProxySupport.Interceptor} class.
     * This method can be overriden to return a different array of
     * <tt>Callback</tt> instances if required.
     */
   /* public Callback[] getCallbacks(final Association<T> association) {
        return new Callback[]{new LocalInterceptor(this),
                              new Interceptor(association)};
    }*/

    /**
     * Create a {@link net.sf.cglib.proxy.CallbackFilter}
     *
     * @param association The Association to use
     *
     * @return An array of {@link net.sf.cglib.proxy.CallbackFilter} for use
     * with a generated CGLIB proxy. This method returns a
     * {@link org.rioproject.associations.AssociationProxySupport.LocalCallbackFilter}.
     * This method can be overriden to return a different
     * <tt>CallbackFilter</tt> if required.
     */
    /*public CallbackFilter getCallbackFilter(Association<T> association) {
        return new LocalCallbackFilter(getClass().getMethods());
    }*/

    /**
     * Create a {@link net.sf.cglib.proxy.Dispatcher}
     *
     * @param association The Association to use
     *
     * @return A {@link net.sf.cglib.proxy.Dispatcher} for use with a generated
     * CGLIB proxy. This method returna a <tt>null</tt>. If the underlying proxy
     * requires a <tt>Dispatcher</tt>, this method should be overriden.
     */
    /*public Dispatcher getDispatcher(final Association<T> association) {
        return null;
    }*/

    public Association getAssociation() {
        return strategy.getAssociation();
    }

    /**
     * Get the {@link ServiceSelectionStrategy}
     *
     * @return The <tt>ServiceSelectionStrategy</tt>
     */
    public ServiceSelectionStrategy<T> getServiceSelectionStrategy() {
        return strategy;
    }

    /**
     * Set the strategy for selecting services
     *
     * @param strategy The {@link ServiceSelectionStrategy}. Must not be null.
     */
    public void setServiceSelectionStrategy(ServiceSelectionStrategy<T> strategy) {
        this.strategy = strategy;
    }

    /**
     * Notification that an Association has been discovered
     */
    public void discovered(Association<T> association, T service) {
        strategy.discovered(association, service);
    }

    /**
     * Notification that an Association has changed
     */
    public void changed(Association<T> association, T service) {
        strategy.changed(association, service);
    }

    /**
     * Notification that an Association is broken
     */
    public void broken(Association<T> association, T service) {
        strategy.broken(association, service);
    }

    /**
     * Clean up any resources allocated
     */
    public void terminate() {
        terminated = true;
        strategy.terminate();
    }

    /*
    * Invokes the method on on the first available service in
    * the collection of associated services. If an invocation to the service
    * fails as a result of remote communication failure, the next available
    * service will be used.
    *
    * <p>Attempts to invoke an available service will continue until either the
    * invocation succeeds, or there are no more services available
    *
    * @param a The Association referencing a collection of associated services
    * @param method The mthod to invoke
    * @param args Method arguments
    *
    * @return The result of the method invocation
    *
    * @throws Throwable the exception to throw from the method invocation on
    * the associated service instance.
    */
    public Object doInvokeService(Association<T> a, Method method, Object[] args)
        throws Throwable {

        if(terminated)
            throw new IllegalStateException("The association proxy for " +
                                            formatAssociationService(a)+" "+
                                            "has been terminated, invocations " +
                                            "to the service through this " +
                                            "generated proxy are not possible " +
                                            "in it's current state. Make sure " +
                                            "all invoking threads are " +
                                            "terminated to resolve this issue");
        Object result = null;
        long stopTime = 0;
        while (!terminated) {
            T service = getServiceSelectionStrategy().getService();
            if(service==null) {
                AssociationDescriptor aDesc = a.getAssociationDescriptor();
                if(aDesc.getServiceDiscoveryTimeout()>0) {
                    stopTime = (stopTime==0?
                                System.currentTimeMillis()+
                                aDesc.getServiceDiscoveryTimeUnits().toMillis(
                                    aDesc.getServiceDiscoveryTimeout()): stopTime);
                    if(System.currentTimeMillis()<stopTime) {
                        if(logger.isLoggable(Level.FINEST)) {
                            logger.finest("The association proxy for " +
                                          formatAssociationService(a)+" is " +
                                          "not available. A service discovery " +
                                          "timeout of " +
                                          "["+aDesc.getServiceDiscoveryTimeout()+"], " +
                                          "has been configured, and the " +
                                          "computed stop time is: "+
                                          new Date(stopTime)+", " +
                                          "sleep for one second and re-evaluate");
                        }
                        Thread.sleep(1000);
                        continue;
                    } else {
                        String s = formatAssociationService(a);
                        throw new RemoteException("No services available for " +
                                                  "associated service " +
                                                  s+", "+
                                                  formatDiscoveryAttributes(a)+". "+
                                                  "A timeout of "+
                                                  aDesc.getServiceDiscoveryTimeout()+
                                                  " "+
                                                  aDesc.getServiceDiscoveryTimeUnits()+
                                                  " expired. Check network " +
                                                  "connections and ensure that " +
                                                  "the "+s+" service is deployed");
                    }
                } else {
                    String s = formatAssociationService(a);
                    throw new RemoteException("No services available for " +
                                              "service association " +
                                              s+", "+
                                              formatDiscoveryAttributes(a)+". " +
                                              "Check network " +
                                              "connections and ensure that "+
                                              "the ["+s+"] service is deployed. " +
                                              "You may also want to check the " +
                                              "service discovery timeout property, " +
                                              "it is set to " +
                                              "["+aDesc.getServiceDiscoveryTimeout()+"]. " +
                                              "Changing this value will allow " +
                                              "Rio to wait the specified " +
                                              "amount of time for a service to " +
                                              "become available.");                
                }
            }
            try {
                result = method.invoke(service, args);
                invocationCount.incrementAndGet();
                break;
            } catch (Throwable t) {
                if(!ThrowableUtil.isRetryable(t)) {
                    logger.log(Level.WARNING,
                               "Interceptor.intercept " +
                               "method [" + method.getName() + "] " +
                               "remove service ["+service+"]",
                               t);
                    if (service != null) {
                        if(a.removeService(service)!=null)
                            logger.warning("Service ["+service+"] removed");
                        else
                            logger.warning("Unable to remove service ["+service+"]");
                    }
                    terminated = true;
                } else {
                    throw t;
                }
            }
        }
        return result;
    }

    private String formatAssociationService(Association<T> a) {
        AssociationDescriptor aDesc = a.getAssociationDescriptor();
        StringBuffer sb = new StringBuffer();
        String[] names = aDesc.getInterfaceNames();
        sb.append("[");
        for(int i=0; i<names.length; i++) {
            if(i>0)
                sb.append(", ");
            sb.append(names[i]);
        }
        sb.append("]");
        if(!aDesc.getName().equals(AssociationDescriptor.NO_NAME)) {
            sb.append(", name [");
            sb.append(aDesc.getName());
            sb.append("]");
        }
        sb.append(", ");
        sb.append("strategy=[");
        sb.append(a.getAssociationDescriptor().getServiceSelectionStrategy());
        sb.append("]");
        return sb.toString();
    }

    private String formatDiscoveryAttributes(Association<T> a) {
        StringBuffer sb = new StringBuffer();
        sb.append("Using discovery attributes: ");
        String[] groups = a.getAssociationDescriptor().getGroups();
        sb.append("groups=[");
        int i=0;
        for(String s : groups) {
            if(i>0)
                sb.append(", ");
            sb.append(s);
            i++;
        }
        sb.append("]");
        LookupLocator[] locators = a.getAssociationDescriptor().getLocators();
        if(locators!=null) {
            sb.append(" ");
            sb.append("locators=[");
            i=0;
            for(LookupLocator l : locators) {
                if(i>0)
                    sb.append(", ");
                sb.append(l.toString());
                i++;
            }
            sb.append("] ");
        }
        return sb.toString();
    }

    /*
     * A method interceptor that operates on the service returned by the
     * {@link ServiceSelectionStrategy}. If an invocation to the service
     * fails as a result of remote communication failure, the next service
     * returned by the {@link ServiceSelectionStrategy}will be used.
     *
     * Attempts to invoke an available service will continue until either the
     * invocation succeeds, or there are no more services available
     */
    /*public class Interceptor implements MethodInterceptor {
        Association<T> association;

        public Interceptor(Association<T> association) {
            this.association = association;
        }

        @SuppressWarnings("unchecked")
        public Object intercept(Object proxy,
                                Method method,
                                Object[] args,
                                MethodProxy methodProxy) throws Throwable {
            return doInvokeService(association, method, args);
        }
    }*/

    /*
     * A method interceptor that operates on the local class
     */
    /*public class LocalInterceptor implements MethodInterceptor {
        Object localRef;

        public LocalInterceptor(Object localRef) {
            this.localRef = localRef;
        }

        public Object intercept(Object proxy,
                                Method method,
                                Object[] args,
                                MethodProxy methodProxy) throws Throwable {
            return method.invoke(localRef, args);
        }
    }*/

    /*
     * A CallbackFilter for local operations
     */
    /*public class LocalCallbackFilter implements CallbackFilter {
        public static final int LOCAL = 0;
        public static final int DEFAULT = 1;
        final List<String> localMethodNames = new ArrayList<String>();

        public LocalCallbackFilter(Method[] methods) {
            for (Method m : methods) {
                localMethodNames.add(m.getName());
            }
        }

        *//**
         * Specify which callback to use for the method being invoked.
         *
         * @param method the method being invoked.
         * @return the callback index in the callback array for this method
         *//*
        public int accept(Method method) {
            int callback = DEFAULT;
            String name = method.getName();
            if (localMethodNames.contains(name))
                callback = LOCAL;
            return callback;
        }

        protected List<String> getLocalMethodNames() {
            return localMethodNames;
        }
    }*/

    /*
     * An InvocationHandler that operates on the service returned by the
     * {@link ServiceSelectionStrategy}. If an invocation to the service
     * fails as a result of remote communication failure, the next service
     * returned by the {@link ServiceSelectionStrategy}will be used.
     *
     * Attempts to invoke an available service will continue until either the
     * invocation succeeds, or there are no more services available
     */
    class AssociationInvocationHandler implements InvocationHandler {
        Association<T> association;
        Object localRef;

        AssociationInvocationHandler(Association<T> association, Object localRef) {
            this.association = association;
            this.localRef = localRef;
        }

        public Object invoke(Object object,
                             Method method,
                             Object[] args) throws Throwable {
            Object result;

            if (!isProxyMethod(method)) {
                if (logger.isLoggable(Level.FINEST))
                    logger.finest(
                        "Invoking local method [" + method.toString() + "]");
                result = method.invoke(localRef, args);
            } else {
                result = doInvokeService(association, method, args);
            }
            return result;
        }
    }

    /**
     * Check if the methods is found in the generated proxy
     *
     * @param method The Method to check
     * @return true if the method is local to the proxy
     */
    protected boolean isProxyMethod(Method method) {
        return proxyMethods.contains(method.toString());
    }

    /**
     * Get the super-class when creating a concrete CGLIB proxy
     *
     * @return The super class to use when creating a concrete CGLIB proxy. If
     *         interfaces are being used, return null (as this default
     *         implementation does).
     */
    public Class getSuperClass() {
        return null;
    }

    public void setProxyInterfaces(Class[] classes) {
        for (Class clazz : classes) {
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                proxyMethods.add(m.toString());
            }
        }
    }

    public long getInvocationCount() {
        return invocationCount.get();
    }
}
