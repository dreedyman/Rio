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


import java.lang.reflect.InvocationHandler;

/**
 * Methods that dynamic proxies created for associated services must implement
 * in order to interface with association management.
 *
 * @author Dennis Reedy
 */
public interface AssociationProxy<T> extends AssociationListener<T> {
    /**
     * Get the association
     *
     * @return The Association
     */
    Association<T> getAssociation();
    
    /**
     * Create an InvocationHandler
     *
     * @param association The Association to use
     *
     * @return An InvocationHandler for use with a dynamic JDK proxy.
     */
    InvocationHandler getInvocationHandler(final Association<T> association);

    /**
     * Set the strategy for selecting services
     *
     * @param strategy The {@link ServiceSelectionStrategy}. Must not be null.
     */
    void setServiceSelectionStrategy(ServiceSelectionStrategy<T> strategy);

    /**
     * Get the strategy for selecting services
     *
     * @return The {@link ServiceSelectionStrategy}. 
     */
    ServiceSelectionStrategy<T> getServiceSelectionStrategy();

    /**
     * Set the classes the proxy will support
     *
     * @param classes Array of interface classes
     */
    void setProxyInterfaces(Class[] classes);

    /**
     * Get the number of times the associated service(s) were invoked using this
     * proxy
     *
     * @return The number of time the associated services were invoked using
     * this proxy. The returned count will represent the total number of
     * invocations across all associated services, and include only successful
     * invocation attempts (attempts that did not result in an exception).
     */
    long getInvocationCount();

    /**
     * Clean up any resources allocated
     */
    void terminate();
}
