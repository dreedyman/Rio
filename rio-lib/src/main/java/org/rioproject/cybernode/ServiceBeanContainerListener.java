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

import org.rioproject.deploy.ServiceRecord;

/**
 * Client interface allowing notification of service instantiations and discard
 * lifecycle events
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanContainerListener  {
    /**
     * Notify all ServiceBeanContainerListener implementations with a 
     * ServiceRecord signifying that a service has been instantiated
     *
     * @param record The ServiceRecord for the newly instantiated service
     */
    void serviceInstantiated(ServiceRecord record);

    /**
     * Notify all ServiceBeanContainerListener implementations with a 
     * ServiceRecord signifying that a service has been discarded
     *
     * @param record The ServiceRecord for the discarded service
     */
    void serviceDiscarded(ServiceRecord record);
}


