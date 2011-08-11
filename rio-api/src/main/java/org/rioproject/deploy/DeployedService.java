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
package org.rioproject.deploy;

import org.rioproject.opstring.ServiceElement;
import org.rioproject.system.ComputeResourceUtilization;

import java.io.Serializable;

/**
 * Provides information about a deployed service.
 */
public class DeployedService implements Serializable {
    static final long serialVersionUID = 1L;
    private ServiceElement serviceElement;
    private ServiceBeanInstance serviceBeanInstance;
    private ComputeResourceUtilization computeResourceUtilization;

    public DeployedService(ServiceElement serviceElement,
                           ServiceBeanInstance serviceBeanInstance,
                           ComputeResourceUtilization computeResourceUtilization) {
        this.serviceElement = serviceElement;
        this.serviceBeanInstance = serviceBeanInstance;
        this.computeResourceUtilization = computeResourceUtilization;
    }

    public ServiceElement getServiceElement() {
        return serviceElement;
    }

    public ServiceBeanInstance getServiceBeanInstance() {
        return serviceBeanInstance;
    }

    public ComputeResourceUtilization getComputeResourceUtilization() {
        return computeResourceUtilization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DeployedService that = (DeployedService) o;

        return !(serviceBeanInstance != null?
                !serviceBeanInstance.equals(that.serviceBeanInstance):
                that.serviceBeanInstance != null);

    }

    @Override
    public int hashCode() {
        int result = serviceElement != null ? serviceElement.hashCode() : 0;
        result = 31 * result +
                (serviceBeanInstance != null
                        ? serviceBeanInstance.hashCode()
                        : 0);
        result = 31 * result + (computeResourceUtilization != null
                ? computeResourceUtilization.hashCode()
                : 0);
        return result;
    }
}
