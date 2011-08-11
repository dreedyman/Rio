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
package org.rioproject.associations;

/**
 * The AssociationServiceListener interface specifies the semantics of a client
 * that when registered to an {@link Association} instance will receive
 * notifications of services being added or removed to that {@link Association}.
 *
 * @author Dennis Reedy
 */
public interface AssociationServiceListener<T> {
    /**
     * A service instance has been added to the <tt>Association</tt>
     *
     * @param service The added service
     */
    void serviceAdded(T service);

    /**
     * A service instance has been removed from the <tt>Association</tt>
     *
     * @param service The service that has been removed.
     */
    void serviceRemoved(T service);
}
