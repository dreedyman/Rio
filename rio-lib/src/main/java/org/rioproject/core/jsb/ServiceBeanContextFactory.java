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

import org.rioproject.opstring.ServiceElement;
import org.rioproject.system.ComputeResource;
import net.jini.config.Configuration;

/**
 * Defines the semantics for a class to produce a
 * {@link org.rioproject.core.jsb.ServiceBeanContext}
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanContextFactory {
    /**
     * Create an instance of a {@link org.rioproject.core.jsb.ServiceBeanContext}
     *
     * @param sElem The ServiceElement
     * @param serviceBeanManager The ServiceBeanManager
     * @param computeResource The ComputeResource object representing
     * capabilities of the compute resource the service has been instantiated on
     * @param sharedConfig Configuration from the "platform" which will be used
     * as the shared configuration
     *
     * @return A ServiceBeanContext instance. A new ServiceBeanContext will
     * be returned each time this method is invoked
     *
     * @throws IllegalArgumentException if the sElem, serviceBeanManager or
     * computeResource parameters are null
     */
    ServiceBeanContext create(ServiceElement sElem,
                              ServiceBeanManager serviceBeanManager,
                              ComputeResource computeResource,
                              /* Optional */
                              Configuration sharedConfig);
}
