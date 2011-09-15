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
package org.rioproject.cybernode;

import org.rioproject.jsb.ServiceBeanAdapterMBean;

import java.io.IOException;

/**
 * Provides a standard MBean to use when administering the Cybernode using JMX
 *
 * @author Ming Fang
 * @author Dennis Reedy
 */
public interface CybernodeImplMBean extends ServiceBeanAdapterMBean {
    /**
     * Get the upper limit of services that this Cybernode can instantiate
     *
     * @return The upper limit of services
     */
    Integer getServiceLimit();

    /**
     * Set the upper limit of services that this Cybernode can instantiate
     *
     * @param limit The upper limit of services
     */
    void setServiceLimit(Integer limit);

    /**
     * Get the number of services that this Cybernode has instantiated
     *
     * @return The number of services the Cybernode has instantiated
     */
    Integer getServiceCount();

    /**
     * Get the resource's utilization. This method returns the aggregate of
     * all measured system utilization values
     *
     * @return The aggregate utilization as a relative value
     */
    double getUtilization();

    /**
     * Get whether the Cybernode supports persistent provisioning of
     * qualitative capabilities
     *
     * @return True if the Cybernode supports persistent provisioning of
     *         qualitative capabilities otherwise return false
     */
    boolean getPersistentProvisioning();

    /**
     * Set whether the Cybernode supports persistent provisioning of
     * qualitative capabilities
     *
     * @param provisionEnabled If the Cybernode supports persistent
     * provisioning of qualitative capabilities
     */
    void setPersistentProvisioning(boolean provisionEnabled) throws IOException;

    /**
     * Get the enlisted state of the Cybernode
     *
     * @return True if the Cybernode can be used to instantiate dynamic
     * application services.
     */
    boolean isEnlisted();
}
