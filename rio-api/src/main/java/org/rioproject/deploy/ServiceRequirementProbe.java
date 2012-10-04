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

import org.rioproject.system.ResourceCapability;

import java.io.Serializable;

/**
 * The {@code ServiceRequirementProbe} is used to determine whether a
 * {@link ServiceBeanInstantiator} meets specific requirements for service instantiation.
 *
 * <p>The {@code ServiceRequirementProbe} extends the matching semantics between
 * {@code PlatformCapability} objects to {@code SystemComponent} objects. The big difference
 * is the {@code ServiceRequirementProbe} is sent to the {@code ServiceBeanInstantiator},
 * where it is executed. All methods invocation on a {@code ServiceRequirementProbe} are performed
 * locally, at the {@code ServiceBeanInstantiator}.
 * </p>
 *
 * @author Dennis Reedy
 */
public interface ServiceRequirementProbe extends Serializable {
    /**
     * Determine if the probe's requirements can be met.
     *
     * @return If the probe determines that the {@code ServiceBeanInstantiator} supports
     * the requirements determined by the probe, {@code false} otherwise.
     */
    boolean supports();

    /**
     * Set the {@code ResourceCapability} that the {@code ServiceRequirementProbe}
     * may use to assist in the determination that the {@code ServiceBeanInstantiator} meets
     * requirements of the probe. This method will always be called <u>before</u> the {@code supports}
     * method invocation.
     *
     * @param resourceCapability The {@code ResourceCapability}, provides the utilization,
     * platform and measured resource capabilities for the compute resource the
     * {@code ServiceBeanInstantiator} is running on. The {@code resourceCapability} will
     * never be {@code null}.
     */
    void setResourceCapability(ResourceCapability resourceCapability);
}
