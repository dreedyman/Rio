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
package org.rioproject.impl.container;

import org.rioproject.servicebean.ServiceBeanContextFactory;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.servicebean.ServiceBeanManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.impl.servicebean.JSBContext;
import org.rioproject.impl.system.ComputeResource;
import net.jini.config.Configuration;

/**
 * A {@link org.rioproject.servicebean.ServiceBeanContextFactory} which creates
 * a {@link org.rioproject.impl.servicebean.JSBContext}
 *
 * @author Dennis Reedy
 */
public class ServiceContextFactory implements ServiceBeanContextFactory {
    /**
     * @see org.rioproject.servicebean.ServiceBeanContextFactory#create
     */
    public ServiceBeanContext create(final ServiceElement sElem,
                                     final ServiceBeanManager serviceBeanManager,
                                     final ComputeResource computeResource,
                                     /* Optional */
                                     final Configuration sharedConfig) {
        if(sElem == null)
            throw new IllegalArgumentException("sElem is null");
        if(serviceBeanManager == null)
            throw new IllegalArgumentException("serviceBeanManager is null");
        if(computeResource == null)
            throw new IllegalArgumentException("computeResource is null");
        return(new JSBContext(sElem, serviceBeanManager, computeResource, sharedConfig));
    }
}
