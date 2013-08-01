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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for dynamic proxies to manage a collection of associated services.
 *
 * @author Dennis Reedy
 */
public class AssociationProxyFactory {

    /**
     * Create a proxy for associated services
     *
     * @param proxyClassName The classname of the proxy to create. This class must
     * be an instance of {@link AssociationProxy}
     * @param strategyClassName The classname of the
     * {@link ServiceSelectionStrategy} to create. If this parameter is null,
     * the AssociationProxy instance will not have a
     * <tt>ServiceSelectionStrategy</tt> set
     * @param association The association for the proxy to use
     * @param loader The class loader to create the proxy with
     * @return A generated proxy
     *
     * @throws ClassNotFoundException If the interfaces cannot be loaded
     * @throws IllegalAccessException if the proxy class cannot be created
     * @throws InstantiationException if the proxy class cannot be created
     */
    @SuppressWarnings("unchecked")
    public static Object createProxy(String proxyClassName,
                                     String strategyClassName,
                                     Association association,
                                     ClassLoader loader)
        throws ClassNotFoundException, IllegalAccessException,InstantiationException {

        if(proxyClassName==null)
            throw new IllegalArgumentException("proxyClassName is null");
        if(loader==null)
            throw new IllegalArgumentException("classloader is null");
        if(association==null)
            throw new IllegalArgumentException("association is null");        
        Object proxy;

        final ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);

            Class aProxyClass = loader.loadClass(proxyClassName);
            AssociationProxyType aProxyType =
                (AssociationProxyType) aProxyClass.getAnnotation(AssociationProxyType.class);
            boolean createJdkProxy;
            if(aProxyType!=null) {
                createJdkProxy = aProxyType.type().equals(AssociationProxyType.Type.JDK);
            } else {
                createJdkProxy =
                    association.getAssociationDescriptor().getProxyType().equals(AssociationDescriptor.JDK_PROXY);
            }
            AssociationProxy aProxy = (AssociationProxy)aProxyClass.newInstance();

            List<Class> list = loadAssociatedInterfaces(association.getAssociationDescriptor(), loader);
            aProxy.setProxyInterfaces(list.toArray(new Class[list.size()]));
            list.add(AssociationProxy.class);

            if(strategyClassName!=null) {
                ServiceSelectionStrategy strategy =
                    (ServiceSelectionStrategy)loader.loadClass(strategyClassName).newInstance();
                strategy.setAssociation(association);
                aProxy.setServiceSelectionStrategy(strategy);
            }

            Class[] interfaces = list.toArray(new Class[list.size()]);
            if (createJdkProxy) {
                proxy = Proxy.newProxyInstance(loader, interfaces, aProxy.getInvocationHandler(association));
            } else {
                throw new IllegalArgumentException("CGLIB no longer supported");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
        return proxy;
    }

    /*
     * Load interfaces from an AssociationDescriptor
     */
    private static List<Class> loadAssociatedInterfaces(AssociationDescriptor a, ClassLoader cl)
        throws ClassNotFoundException {
        List<Class> l = new ArrayList<Class>();
        String[] iNames = a.getInterfaceNames();
        for(String iName : iNames) {
            l.add(Class.forName(iName, false, cl));
        }
        return (l);
    }
}
