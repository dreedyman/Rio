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

/**
 * The ServiceStatementManager defines the semantics of reading/writing
 * {@link ServiceStatement} instances. This will allow
 * for different mechanisms (shared drives, other services which may manage the 
 * persistence, etc...) to be employed declaratively
 *
 * @author Dennis Reedy
 */
public interface ServiceStatementManager {
    /**
     * Get an array of ServiceStatement objects
     * 
     * @return An array of ServiceStatement objects. The
     * array will be empty if no records have been found
     */
    ServiceStatement[] get();

    /**
     * Get a ServiceStatement for a particular ServiceElement
     * 
     * @param sElem The ServiceElement
     * @return A ServiceStatement object. If no ServiceStatement is found for the 
     * ServiceElement a null is returned
     */
    ServiceStatement get(ServiceElement sElem);

    /**
     * Record a ServiceStatement
     * 
     * @param statement The ServiceStatement to write
     */
    void record(ServiceStatement statement);

    /**
     * Terminate and clean up any resources
     */
    void terminate();
}
