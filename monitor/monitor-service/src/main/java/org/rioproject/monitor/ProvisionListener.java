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
package org.rioproject.monitor;

import org.rioproject.deploy.ServiceBeanInstance;

/**
 * A ProvisionListener waits for notification that a Service has been provisioned
 *
 * @author Dennis Reedy
 */
public interface ProvisionListener  {

    /**
     * Notify listener that the Service described by the ServiceBeanInstance has
     * been provision successfully
     * 
     * @param jsbInstance The ServiceBeanInstance
     * @param resource The InstantiatorResource that instantiated the service
     */
    void serviceProvisioned(ServiceBeanInstance jsbInstance,
                            InstantiatorResource resource);

    /**
     * Notify listener that the Service described by the ServiceBeanInstance is
     * uninstantiable, allowing the listener to release any pending resources
     * associated with the provisioning
     *
     * @param request The ProvisionRequest that resulted in failure
     */
    void uninstantiable(ProvisionRequest request);
}


