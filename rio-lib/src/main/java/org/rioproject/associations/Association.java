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

import net.jini.core.lookup.ServiceItem;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Associations provide a mechanism to model and enforce uses and requires
 * associations between services in an
 * {@link org.rioproject.opstring.OperationalString}. Associations take 5 forms:
 * <ul>
 * <li><b><u>Uses </u> </b> <br>
 * A weak association relationship where if A uses B exists then, then B may be
 * present for A
 * <li><b><u>Requires </u> </b> <br>
 * A stronger association relationship where if A requires B exists then B must
 * be present for A
 * <li><b><u>Colocated</u> </b> <br>
 * An association which requires that A be colocated with B in the same
 * JVM. If B does not exist, or cannot be located, A shall not be created
 * without B
 * <li><b><u>Opposed</u> </b> <br>
 * An association which requires that A exist in a different JVM then B.
 * <li><b><u>Isolated</u> </b> <br>
 * An association which requires that A exist in a different machine then B.
 * </ul>
 * <p>
 * Associations are optional and may be declared as part of a service's
 * OperationalString declaration. An example :<br>
 * <div style="margin-left: 40px;"> <span style="font-family:
 * monospace;">associations {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; association
 * name:"JavaSpace", type:'requires', property:"space"<br>
 * &nbsp;&nbsp;&nbsp; }<br>
 * }
 * </div>
 *
 * @author Dennis Reedy
 */
public interface Association<T> extends Iterable<T> {
    /**
     * Association State enums indicate the following:
     * <ul>
     * <li>PENDING: Indicates that an associated service has not been discovered
     * <li>DISCOVERED: Indicates that an associated service has been discovered
     * <li>CHANGED: Indicates that an associated service has changed, that is an
     * end-point has changed, and the association now points a different service
     * <li>BROKEN: Indicates that the association is broken
     * </ul>
     */
    public enum State { PENDING, DISCOVERED, CHANGED, BROKEN }    


    /**
     * Get the AssociationType
     * 
     * @return The AssociationType
     */
    AssociationType getAssociationType();

    /**
     * Set the Association state
     * 
     * @param state The Association state
     */
    void setState(State state);

    /**
     * Get the Association state
     * 
     * @return The Association state
     */
    State getState();

    /**
     * Get the associated service's name
     * 
     * @return The associated service's name
     */
    String getName();

    /**
     * Get the associated service's OperationalString name
     * 
     * @return The associated service's OperationalString name
     */
    String getOperationalStringName();

    /**
     * Get the AssociationDescriptor
     * 
     * @return The {@link AssociationDescriptor} used to
     * create this Association
     */
    AssociationDescriptor getAssociationDescriptor();

    /**
     * Get the number of associated services
     *
     * @return The number of associated services
     */
    int getServiceCount();
    
    /**
     * Get the first service Object that can be used to communicate to the
     * associated service.
     * 
     * @return The first service  (proxy) Object in the collection of associated 
     * services. If there are no services a null will be returned.
     */
    T getService();


    /**
     * Get a future representing pending service association.
     *
     * @return a Future representing pending service association. A new Future
     * is created each time.
     *
     * @see AssociationProxy
     * @see ServiceSelectionStrategy
     */
    Future<T> getServiceFuture();

    /**
     * Get all Objects that can be used to communicate to all known associated
     * services.
     * 
     * @return Array of service Object instances that can be used to
     * communicate to all known associated service instances. A new collection
     * is allocated each time. If there are no services, an empty collection
     * will be returned
     */
    Collection<T> getServices();

    /**
     * Get the ServiceItem for the service thats first in the List of services.
     * 
     * @return The ServiceItem for an associated service. If there are no
     * services, a null will be returned
     */
    ServiceItem getServiceItem();
    
    /**
     * Get the ServiceItem for the associated service. The collection of
     * associated services will be searched and if the service proxy is equal
     * to a known associated service proxy, the
     * {@link net.jini.core.lookup.ServiceID} for that service will be returned
     *
     * @param service The proxy of an associated service
     *
     * @return The ServiceItem for an associated service. If the service is
     * unknown, a null will be returned
     */
    ServiceItem getServiceItem(T service);

    /**
     * Get ServiceItem instances for all known associated service instances.
     * 
     * @return Array of ServiceItem instances for all known associated
     * service instances. A new array is allocated each time. If there are no
     * services, an empty array will be returned
     */
    ServiceItem[] getServiceItems();

    /**
     * Get the next ServiceItem in the collection of associated services.   
     *
     * @return The next ServiceItem in the collection of associated services.
     * If there are services, a null will be returned. If the current
     * ServiceItem is the last in the collection, the first ServiceItem in the
     * collection will be returned
     */
    ServiceItem getNextServiceItem();

    /**
     * Add a service to the Association
     * 
     * @param item The ServiceItem for the service to add
     * @return True if added, false if the ServiceItem already exists
     */
    boolean addServiceItem(ServiceItem item);

    /**
     * Remove a service from the Association
     * 
     * @param service The proxy for the Service to remove
     * @return The ServiceItem of the removed service
     */
    ServiceItem removeService(T service);

    /**
     * Register an {@link AssociationServiceListener} for notification.
     *
     * @param al The AssociationServiceListener. If the AssociationServiceListener
     * is not null and not already registered it will be added to the collection of
     * AssociationServiceListeners
     */
    void registerAssociationServiceListener(AssociationServiceListener<T> al);

    /**
     * Remove a {@link AssociationServiceListener} for notification.
     *
     * @param al The AssociationServiceListener. If the AssociationServiceListener
     * is not null and registered, it will be removed from the collection of
     * AssociationServiceListeners
     */
    void removeAssociationServiceListener(AssociationServiceListener<T> al);

    void terminate();
}
