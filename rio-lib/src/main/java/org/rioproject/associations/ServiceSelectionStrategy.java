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
package org.rioproject.associations;

/**
 * Provides a way to select a service for invocation across a collection of
 * associated services. Implementations of this interface provide a way to
 * separate service selection from the actual implementation of a dynamic
 * proxy.
 *
 * @author Dennis Reedy
 */
public interface ServiceSelectionStrategy<T> extends AssociationServiceListener<T> {
    /**
     * Set the association
     *
     * @param association The association
     */
    void setAssociation(Association<T> association);

    /**
     * Get the association set to the strategy
     *
     * @return The association
     */
    Association<T> getAssociation();

    /**
     * Select a service for invocation
     *
     * @return The proxy for a service
     */
    T getService();

    /**
     * Clean up any resources allocated
     */
    void terminate();
}
