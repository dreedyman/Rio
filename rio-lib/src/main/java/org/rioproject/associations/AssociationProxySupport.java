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

import net.jini.core.discovery.LookupLocator;
import org.rioproject.associations.strategy.FailOver;
import org.rioproject.resources.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides support for an {@link AssociationProxy}
 *
 * @author Dennis Reedy
 */
public class AssociationProxySupport<T> implements AssociationProxy<T> {
    private ServiceSelectionStrategy<T> strategy;
    private final List<String> proxyMethods = new ArrayList<String>();
    Logger logger = LoggerFactory.getLogger(AssociationProxySupport.class);
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


    public Association<T> getAssociation() {
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
        logger.trace("Adding service for {}", association.getName());
        strategy.serviceAdded(service);
    }

    /**
     * Notification that an Association has changed
     */
    public void changed(Association<T> association, T service) {
        strategy.serviceRemoved(service);
    }

    /**
     * Notification that an Association is broken
     */
    public void broken(Association<T> association, T service) {
        strategy.serviceRemoved(service);
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
    * @param method The method to invoke
    * @param args Method arguments
    *
    * @return The result of the method invocation
    *
    * @throws Throwable the exception to throw from the method invocation on
    * the associated service instance.
    */
    public Object doInvokeService(Association<T> a, Method method, Object[] args) throws Throwable {
        if(terminated)
            throw new IllegalStateException("The association proxy for "+formatAssociationService(a)+" "+
                                            "has been terminated, invocations to the service through this " +
                                            "generated proxy are not possible in it's current state. Make sure " +
                                            "all invoking threads are terminated to resolve this issue");
        Object result = null;
        long stopTime = 0;
        while (!terminated) {
            T service = getServiceSelectionStrategy().getService();
            if(service==null) {
                AssociationDescriptor aDesc = a.getAssociationDescriptor();
                if(aDesc.getServiceDiscoveryTimeout()>0) {
                    stopTime = (stopTime==0?
                                System.currentTimeMillis()+
                                aDesc.getServiceDiscoveryTimeUnits().toMillis(aDesc.getServiceDiscoveryTimeout()): stopTime);
                    if(System.currentTimeMillis()<stopTime) {
                        if(logger.isTraceEnabled()) {
                            logger.trace("The association proxy for {} is not available. A service discovery timeout of " +
                                         "[{} {}], has been configured, and the computed stop time is: {}, sleep for one " +
                                         "second and re-evaluate",
                                         formatAssociationService(a),
                                         aDesc.getServiceDiscoveryTimeout(),
                                         aDesc.getServiceDiscoveryTimeUnits().name(),
                                         new Date(stopTime));
                        }
                        Thread.sleep(1000);
                        continue;
                    } else {
                        String s = formatAssociationService(a);
                        throw new RemoteException("No services available for associated service " +
                                                  s+", "+formatDiscoveryAttributes(a)+". "+
                                                  "A timeout of "+aDesc.getServiceDiscoveryTimeout()+" "+
                                                  aDesc.getServiceDiscoveryTimeUnits()+" expired. Check network " +
                                                  "connections and ensure that the "+s+" service is deployed");
                    }
                } else {
                    String s = formatAssociationService(a);
                    throw new RemoteException("No services available for service association " +
                                              s+", "+formatDiscoveryAttributes(a)+". Check network " +
                                              "connections and ensure that the ["+s+"] service is deployed. " +
                                              "You may also want to check the service discovery timeout property, " +
                                              "it is set to ["+aDesc.getServiceDiscoveryTimeout()+"]. Changing this " +
                                              "value will allow Rio to wait the specified amount of time for a service " +
                                              "to become available.");
                }
            }
            try {
                result = method.invoke(service, args);
                invocationCount.incrementAndGet();
                break;
            } catch (Throwable t) {
                if(!ThrowableUtil.isRetryable(t)) {
                    logger.warn("Failed to invoke method [{}], remove  service [{}]", method.getName(), service.toString(), t);
                    getServiceSelectionStrategy().serviceRemoved(service);
                    if(a.removeService(service)!=null) {
                        logger.warn("Service [{}] removed, have [{}] services", service.toString(), a.getServiceCount());
                    } else {
                        logger.warn("Unable to remove service [{}], have [{}] services", service.toString(), a.getServiceCount());
                        //terminated = true;
                    }
                } else {
                    if(t instanceof InvocationTargetException) {
                        t = t.getCause()==null? ((InvocationTargetException)t).getTargetException(): t.getCause();
                    }
                    throw t;
                }
            }
        }
        return result;
    }

    private String formatAssociationService(Association<T> a) {
        AssociationDescriptor aDesc = a.getAssociationDescriptor();
        StringBuilder sb = new StringBuilder();
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
        String strategy = a.getAssociationDescriptor().getServiceSelectionStrategy();
        strategy = (strategy==null? FailOver.class.getName() : strategy);
        sb.append(strategy);
        sb.append("]");
        return sb.toString();
    }

    private String formatDiscoveryAttributes(Association<T> a) {
        StringBuilder sb = new StringBuilder();
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

        public Object invoke(Object object, Method method, Object[] args) throws Throwable {
            Object result;

            if (!isProxyMethod(method)) {
                if(logger.isTraceEnabled())
                    logger.trace("Invoking local method [{}]", method.toString());
                result = method.invoke(localRef, args);
            } else {
                result = doInvokeService(association, method, args);
            }
            return result;
        }
    }

    /**
     * Check if the method is found in the generated proxy
     *
     * @param method The Method to check
     * @return true if the method is local to the proxy
     */
    public boolean isProxyMethod(Method method) {
        return proxyMethods.contains(method.toString());
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
