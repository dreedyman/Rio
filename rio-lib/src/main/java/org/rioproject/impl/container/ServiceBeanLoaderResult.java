/*
 * Copyright to the original author or authors.
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
package org.rioproject.impl.container;

import net.jini.id.Uuid;
import net.jini.io.MarshalledInstance;
import org.rioproject.impl.bean.BeanAdapter;
import org.rioproject.servicebean.ServiceBeanContext;

/**
 * Trivial class used as the return value by the <code>load</code> method. This class aggregates
 * the results of a service creation attempt: proxy and associated ServiceBeanContext.
 *
 * @author Dennis Reedy
 */
public class ServiceBeanLoaderResult {
    /**
     * The service impl
     */
    private final Object impl;
    /**
     * The proxy as a MarshalledInstance
     */
    private final MarshalledInstance mi;
    /**
     * Associated <code>ServiceBeanContext</code> object
     */
    private final ServiceBeanContext context;
    /**
     * Uuid of the service
     */
    private final Uuid serviceID;
    /* The wrapping BeanAdapter, may be null */
    private final BeanAdapter beanAdapter;

    /**
     * Trivial constructor. Simply assigns each argument
     * to the appropriate field.
     *
     * @param c The ServiceBeanContext
     * @param o The resulting loaded implementation
     * @param m The proxy as a MarshalledInstance
     * @param s The id of the service
     */
    public ServiceBeanLoaderResult(final ServiceBeanContext c,
                                   final Object o,
                                   final BeanAdapter beanAdapter,
                                   final MarshalledInstance m,
                                   final Uuid s) {
        context = c;
        impl = o;
        this.beanAdapter = beanAdapter;
        mi = m;
        serviceID = s;
    }

    public Object getImpl() {
        return impl;
    }

    public MarshalledInstance getMarshalledInstance() {
        return mi;
    }

    public ServiceBeanContext getServiceBeanContext() {
        return context;
    }

    public Uuid getServiceID() {
        return serviceID;
    }

    public BeanAdapter getBeanAdapter() {
        return beanAdapter;
    }
}
