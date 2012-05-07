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
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * This class provides an implementation of a LeaseListener which uses manages a
 * LinkedList of <code>ServiceResource</code> objects which reflect the
 * resources being leased. This class must be registered with the
 * <code>LandlordLessor</code>, and will be notified as resources are leased,
 * updated or removed <br>
 * 
 * @see LandlordLessor
 * @see ResourceLessor
 *
 * @author Dennis Reedy
 */
public class LeasedListManager implements LeaseListener {
    final LinkedList<ServiceResource> list = new LinkedList<ServiceResource>();

    /**
     * This method returns a snapshot of the ServiceResource objects in the
     * LinkedList of resources
     *
     * @return An array of ServiceResource objects
     */
    public ServiceResource[] getServiceResources() {
        ServiceResource[] svcResources ;
        synchronized(list) {
            svcResources = list.toArray(new ServiceResource[list.size()]);
        }
        return (svcResources);
    }

    /**
     * Removes a ServiceResource from the managed Collection of ServiceResource
     * elements
     *
     * @param resource The ServiceResource to remove
     */
    public void removeResource(ServiceResource resource) {
        synchronized(list) {
            if(resource != null) {
                list.remove(resource);
            }
        }
    }

    /**
     * This method will move the ServiceResource from wherever it is in the list
     * to the last element in the list
     *
     * @param sr The ServiceResource to move
     */
    public void putLast(ServiceResource sr) {
        synchronized(list) {
            list.remove(sr);
            list.addLast(sr);
        }
    }

    /**
     * Returns the next <code>ServiceResource</code> in the list of
     * <code>ServiceResource</code> elements that have been leased. The
     * behavior of this method is simply to remove the first element from the
     * LinkedList and append it to the end of the list.
     * <p>
     * Use of this method provides access to the list of
     * <code>ServiceResource</code> elements traversing in a forward
     * direction.
     * <p>
     * If there is only one element in the list, the that element will be
     * returned each time this method is called. <br>
     * 
     * @return ServiceResource
     * @throws NoSuchElementException if the iteration is empty
     */
    public ServiceResource getNext() throws NoSuchElementException {
        ServiceResource sr;
        if(list.isEmpty())
            throw new NoSuchElementException("Empty resource list");
        synchronized(list) {
            sr = list.removeFirst();
            if(sr != null)
                list.addLast(sr);
        }
        return (sr);
    }

    /**
     * Returns the previous <code>ServiceResource</code> in the list of
     * <code>ServiceResource</code> elements that have been leased. The
     * behavior of this method is simply to remove the last element from the
     * LinkedList and append it to the beginning of the list.
     * <p>
     * Use of this method provides access to the list of
     * <code>ServiceResource</code> elements traversing in a forward
     * direction.
     * <p>
     * If there is only one element in the list, the that element will be
     * returned each time this method is called. <br>
     * 
     * @return ServiceResource
     * @throws NoSuchElementException if the iteration is empty
     */
    public ServiceResource getPrevious() throws NoSuchElementException {
        ServiceResource sr;
        if(list.isEmpty())
            throw new NoSuchElementException("Empty resource list");
        synchronized(list) {
            sr = list.removeLast();
            if(sr != null)
                list.addFirst(sr);
        }
        return (sr);
    }

    /**
     * Notifies the manager of a lease expiration <br>
     * 
     * @param resource The resource associated with the expiration
     */
    public void expired(LeasedResource resource) {
        removed(resource);
    }

    /**
     * Notifies the manager of a lease removal <br>
     * 
     * @param resource The resource associated with the removal
     */
    public void removed(LeasedResource resource) {
        if(resource != null) {
            ServiceResource sr = (ServiceResource)resource;
            synchronized(list) {
                list.remove(sr);
            }
        }
    }

    /**
     * Notifies the manager of a new lease being created.
     * 
     * @param resource The resource associated with the new Lease.
     */
    public void register(LeasedResource resource) {
        synchronized(list) {
            list.add((ServiceResource)resource);
        }
    }

    /**
     * Notifies the manager of a lease being renewed.
     * 
     * @param resource The resource associated with the new Lease.
     */
    public void renewed(LeasedResource resource) {
        synchronized(list) {
            ServiceResource sr = (ServiceResource)resource;
            int index = list.indexOf(sr);
            if(index != -1)
                list.set(index, (ServiceResource)resource);
        }
    }
}
