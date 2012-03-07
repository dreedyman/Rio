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
package org.rioproject.cybernode;

import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.system.ComputeResource;
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
    private ServiceBeanInstantiator instantiator;
    private CybernodeImpl impl;
    private ComputeResource computeResource;

    public CybernodeAdapter(ServiceBeanInstantiator instantiator,
                            CybernodeImpl impl,
                            ComputeResource computeResource) {
        if (instantiator == null)
            throw new IllegalArgumentException("instantiator is null");
        if (impl == null)
            throw new IllegalArgumentException("impl is null");
        if (computeResource == null)
            throw new IllegalArgumentException("computeResource is null");
        this.instantiator = instantiator;
        this.impl = impl;
        this.computeResource = computeResource;
    }

    public ServiceBeanInstantiator getInstantiator() {
        return instantiator;
    }

    public ComputeResource getComputeResource() {
        return computeResource;
    }

    public ResourceCapability getResourceCapability() {
        return computeResource.getResourceCapability();
    }

    public List<DeployedService> getDeployedServices() {
        List<DeployedService> list = new ArrayList<DeployedService>();

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
