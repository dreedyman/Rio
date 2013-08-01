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
package org.rioproject.resources.servicecore;

import com.sun.jini.landlord.LeasedResource;

/**
 * This interface defines the mechanism through which an entity receives
 * notification that the ResourceLessor has determined that a lease has failed
 * to renew itself and has been removed as a LeasedResource, or that an entity
 * has cancelled it lease and has been removed as a LeasedResource. It is the
 * responsibility of the entity to register with the ResourceLessor. The object
 * that implements this interface should define the actions to take upon receipt
 * of such notifications. Note that prior to sending the event, the
 * ResourceLessor will remove the LeasedResource
 *
 * @author Dennis Reedy
 */
public interface LeaseListener {
    /**
     * Notifies the manager of a new lease being created.
     * 
     * @param resource The resource associated with the new Lease.
     */
    void register(LeasedResource resource);

    /**
     * Notifies the manager of a lease expiration <br>
     * 
     * @param resource The resource associated with the expiration
     */
    void expired(LeasedResource resource);

    /**
     * Notifies the manager of a lease removal <br>
     * 
     * @param resource The resource associated with the removal
     */
    void removed(LeasedResource resource);

    /**
     * Notifies the manager of a lease renewel <br>
     * 
     * @param resource The resource associated with the renewal
     */
    void renewed(LeasedResource resource);
}
