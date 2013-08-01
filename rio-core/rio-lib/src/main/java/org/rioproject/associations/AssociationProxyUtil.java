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

/**
 * Utility for working with a dynamic proxies generated for associations
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unchecked")
public class AssociationProxyUtil {

    /**
     * Get the associated service proxy from the
     * {@link ServiceSelectionStrategy}
     *
     * @param proxy The association proxy
     *
     * @return The service proxy as determined by the
     * {@link ServiceSelectionStrategy} that manages the collection of
     * associated services. If there are no services a null will be returned.
     * If the supplied proxy is not an <tt>instanceof</tt>
     * {@link org.rioproject.associations.AssociationProxy} (in other words not
     * a generated proxy), this method will return the supplied proxy..
     */
    public static <T> T getService(T proxy) {
        T narrowed = proxy;
        if(proxy instanceof AssociationProxy) {
            narrowed = ((AssociationProxy<T>)proxy).getServiceSelectionStrategy().getService();
        }
        return narrowed;
    }

    /**
     * Regenerate the associated service proxy
     *
     * @param proxy The association proxy
     *
     * @return A new association service proxy based on information obtained from the provided proxy. The supplied
     * proxy will be terminated once the regenerated proxy has been produced.
     * <p>If the supplied proxy is not an <tt>instanceof</tt>
     * {@link org.rioproject.associations.AssociationProxy} (in other words not
     * a generated proxy), this method will return the supplied proxy.
     *
     * @throws ClassNotFoundException If the interfaces cannot be loaded
     * @throws IllegalAccessException if the proxy class cannot be created
     * @throws InstantiationException if the proxy class cannot be created
     */
    public static <T> T regenProxy(T proxy) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        T newProxy = null;
        if(proxy instanceof AssociationProxy) {
            AssociationProxy<T> aProxy = (AssociationProxy)proxy;
            Association<T> association = aProxy.getAssociation();
            String proxyClass = association.getAssociationDescriptor().getProxyClass();
            /*
             * Check for null proxyFactoryClass. If null,
             * AssociationProxySupport is default
             */
            proxyClass = (proxyClass==null? AssociationProxySupport.class.getName() : proxyClass);
            String strategyClass = aProxy.getServiceSelectionStrategy().getClass().getName();
            newProxy = (T) AssociationProxyFactory.createProxy(proxyClass,
                                                               strategyClass,
                                                               association,
                                                               aProxy.getClass().getClassLoader());
            aProxy.terminate();
        }
        return newProxy==null?proxy:newProxy;
    }

    /**
     * Get the first associated service proxy
     *
     * @param proxy The association proxy
     *
     * @return The first service proxy in the collection of associated
     * services. If there are no services a null will be returned. If the
     * supplied proxy is not an <tt>instanceof</tt>
     * {@link org.rioproject.associations.AssociationProxy} (in other words not
     * a generated proxy), this method will return the supplied proxy..
     */
    public static <T> T getFirst(T proxy) {
        T narrowed = proxy;
        if(proxy instanceof AssociationProxy) {
            Association<T> association =
                ((AssociationProxy<T>)proxy).getAssociation();
            narrowed = association.getService();
        }
        return narrowed;
    }

    /**
     * Get the {@link Association} for the association proxy
     *
     * @param proxy The association proxy
     * 
     * @return The {@link Association} for the association
     * proxy. If the provided proxy is not an <tt>instanceof</tt>
     * {@link org.rioproject.associations.AssociationProxy} (in other words not
     * a generated proxy), this method will return null.
     */
    public static <T> Association<T> getAssociation(T proxy) {
        Association<T> association = null;
        if(proxy instanceof AssociationProxy) {
            association = ((AssociationProxy<T>)proxy).getAssociation();
        }
        return association;
    }
}
