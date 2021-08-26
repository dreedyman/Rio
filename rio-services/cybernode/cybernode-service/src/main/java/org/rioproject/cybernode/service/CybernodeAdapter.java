/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.cybernode.service;

import org.rioproject.impl.container.ServiceBeanDelegate;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.system.ResourceCapability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Use as an interchange between the Cybernode and it's ServiceConsumer
 *
 * @author Dennis Reedy
 */
public class CybernodeAdapter {
    private final ServiceBeanInstantiator instantiator;
    private final CybernodeImpl impl;

    public CybernodeAdapter(final ServiceBeanInstantiator instantiator,
                            final CybernodeImpl impl) {
        if (impl == null) {
            throw new IllegalArgumentException("impl is null");
        }

        this.instantiator = instantiator;
        this.impl = impl;
    }

    public ServiceBeanInstantiator getInstantiator() {
        return instantiator;
    }

    public ComputeResource getComputeResource() {
        return impl.getComputeResource();
    }

    public ResourceCapability getResourceCapability() {
        return impl.getComputeResource().getResourceCapability();
    }

    public List<DeployedService> getDeployedServices() {
        List<DeployedService> list = new ArrayList<>();

        for (ServiceRecord record :
            impl.getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD)) {
            ServiceBeanDelegate delegate = impl.getServiceBeanContainer().getServiceBeanDelegate(record.getServiceID());

            if(delegate!=null) {
                DeployedService deployed = new DeployedService(delegate.getServiceElement(),
                                                               delegate.getServiceBeanInstance(),
                                                               delegate.getComputeResourceUtilization());
                list.add(deployed);
            }

        }
        return Collections.unmodifiableList(list);
    }
}
