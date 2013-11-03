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

import java.io.Serializable;

/**
 * A {@code ServiceProbe} is dispatched to determine if a {@link ServiceBeanInstantiator} meets
 * operational requirements of a service.
 *
 * @author Dennis Reedy
 */
public interface ServiceProbe extends Serializable {

    /**
     * Determine if a {@code ServiceBeanInstantiator} meets the requirements of a service.
     *
     * @return {@code true} if {@code ServiceBeanInstantiator} meets the requirements of a service, {@code false}
     * otherwise
     */
    boolean meetsRequirements();
}
