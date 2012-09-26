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
package org.rioproject.core.jsb;

import org.rioproject.bean.BeanAdapter;
import org.rioproject.deploy.ServiceBeanInstantiationException;

/**
 * The ServiceBeanFactory defines the semantics to create a service instance. 
 * Implementations of this interface are responsible for the creation of a service
 * implementation and the proxy used to communicate to the created service.
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanFactory {
    /**
     * Class used as the return value by the <code>create</code> method. This class 
     * aggregates the results of a service creation attempt: 
     * proxy and associated implementation. 
     */
    public static class Created {
        /** The service impl */
        private final Object impl;
        /** The service proxy */
        private final Object proxy;
        /* The wrapping BeanAdapter */
        private final BeanAdapter beanAdapter;

        /**
         * Trivial constructor. Simply assigns each argument
         * to the appropriate field.
         *
         * @param o The service implementation
         * @param p The service proxy
         */
        public Created(final Object o, final Object p) {
            this(o, p, null);
        }

        /**
         * Trivial constructor. Simply assigns each argument
         * to the appropriate field.
         *
         * @param o The service implementation
         * @param p The service proxy
         * @param beanAdapter The {@code BeanAdapter}
         */
        public Created(final Object o, final Object p, final BeanAdapter beanAdapter) {
            impl = o;
            proxy = p;
            this.beanAdapter = beanAdapter;
        }

        public Object getImpl() {
            return impl;
        }

        public Object getProxy() {
            return proxy;
        }

        public BeanAdapter getBeanAdapter() {
            return beanAdapter;
        }

        @Override
        public String toString() {
            return "impl=" + impl +", proxy=" + proxy+", beanAdapter="+beanAdapter;
        }
    }
    
    /**
     * The create method is called to create the service implementation and to 
     * create the proxy to communicate with the service. There is no requirement
     * to create a class loader, this method will be called with the context
     * class loader set to the class loader that can be used to load the
     * service. If needed, the class loader can be obtained by access the
     * thread's current context class loader.
     * 
     * Additionally:
     * <ul>
     * <li>If the service has been configured with any shared resources, they
     * will have been loaded into the common class loader prior to the
     * invocation of this method.
     * <li>If any logger instances have been configured for the service,
     * specific loggers and handlers will have been configued prior to the
     * invocation of this method
     * </ul>
     * 
     * @param context The {@link ServiceBeanContext} describing the service to
     * create
     * 
     * @return The Created object
     * 
     * @throws org.rioproject.deploy.ServiceBeanInstantiationException If there are errors creating the
     * service bean
     */
    Created create(ServiceBeanContext context) throws ServiceBeanInstantiationException;
}
