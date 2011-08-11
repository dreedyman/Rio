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

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.cybernode.ServiceBeanContainer;

import java.util.List;

/**
 * AssociationManagement provides the necessary support to manage Associations a
 * ServiceBean has to other services. Associations provide a mechanism to model
 * and enforce uses and requires associations between services in an
 * OperationalString. Associations are optional and may be declared as part of a
 * ServiceBean element's specification. Associations take 2 forms:
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
 * Associations are created during the design/modelling phase and enforced
 * during runtime The AssociationManager will discover and monitor services
 * described in each Association. An Association will either be discovered,
 * changed or broken. Each state transition an Association goes through will
 * result in the notification of any AssociationListener objects which have
 * registered to this AssociationManagement instance. If the Association is of
 * type <code>AssociationType.REQUIRES</code> and the Association state
 * becomes <code>Association.BROKEN</code> <b>and there are no services </b>
 * that match the Association type that have a state of
 * <code>Association.DISCOVERED</code>, then the AssociationManagement object
 * will unadvertise the ServiceBean using the provided ServiceBeanControl
 * interface. Conversely, if the Association is of type
 * <code>AssociationType.REQUIRES</code> and a service is discovered which
 * matches the service type(s), then the Association state becomes
 * <code>Association.DISCOVERED</code>. If the ServiceBean is not advertised,
 * the AssociationManager will advertise the ServiceBean
 *
 * @author Dennis Reedy
 */
public interface AssociationManagement {
    /**
     * Set the ServiceBeanControl object for the ServiceBean
     * 
     * @param control The ServiceBeanControl object for the ServiceBean
     */
    void setServiceBeanControl(ServiceBeanControl control);

    /**
     * Set the ServiceBeanContext object for the ServiceBean
     * 
     * @param context The ServiceBeanContest object for the ServiceBean
     */
    void setServiceBeanContext(ServiceBeanContext context);    

    /**
     * Set the ServiceBeanContainer object
     *
     * @param container The ServiceBeanContainer that the ServiceBean is
     * running in
     */
    void setServiceBeanContainer(ServiceBeanContainer container);

    /**
     * Register {@link AssociationListener}s. Each listener object will receive
     * notifications of Association state changes. Once a listener is 
     * registered, it will be notified of all Association references discovered to 
     * date, and will be notified as Associations are discovered, changed or broken.
     * If the parameter value duplicates (using equals) another element in the set 
     * of listeners, no action is taken. If the parameter value is null, an 
     * IllegalArgumentException is  thrown
     * 
     * @param listeners the {@link AssociationListener}s
     */
    void register(AssociationListener... listeners);

    /**
     * Remove {@link AssociationListener}s
     * 
     * @param listeners the {@link AssociationListener}s
     */
    void remove(AssociationListener... listeners);

    /**
     * Terminate AssociationManagement, cleaning up all connections
     */
    void terminate();

    /**
     * Get the first matching Association.
     *
     * @param serviceType The service type to match, must not be null
     * @param serviceName String name of the associated service. If null will
     * be ignored
     * @param opStringName String name of the OperationalString. If null will
     * be ignored
     *
     * @return The first Association instance that matches the provided
     * criteria. If there no matching associations, a null will be returned.
     *
     * @throws IllegalArgumentException if the serviceType is null
     */
    <T> Association<T> getAssociation(Class<T> serviceType, String serviceName, String opStringName);

    /**
     * Add an association to the managed set of associations. If the
     * association already exists, it will not be added.
     *
     * @param descriptor The AssociationDescriptor
     *
     * @return An Association object.
     *
     * @throws IllegalArgumentException if the AssociationDescriptor is null.
     *
     * @see AssociationProxy
     * @see ServiceSelectionStrategy
     */
    <T> Association<T> addAssociationDescriptor(AssociationDescriptor descriptor);

    /**
     * Add associations to the managed set of associations. If any of the
     * associations already exists, it will not be added.
     *
     * @param descriptors AssociationDescriptor instances
     *
     * @return An unmodifiable List of Association objects corresponding to the
     * submitted descriptors. A new list is created each time.
     *
     * @throws IllegalArgumentException if the AssociationDescriptor is null.
     *
     * @see AssociationProxy
     * @see ServiceSelectionStrategy
     */
    List<Association<?>> addAssociationDescriptors(AssociationDescriptor... descriptors);

    /**
     * Get all managed associations.
     *
     * @return An unmodifiable List of Association objects. A new list is created
     * each time.
     */
    List<Association<?>> getAssociations();
    
    /**
     * Set unadvertiseOnBroken
     * 
     * @param unadvertiseOnBroken If true, and the service has an Association
     * with a type of AssociationType.REQUIRES and the Association is broken,
     * the AssociationManagement object will unadvertise the ServiceBean using
     * the ServiceBean instance's ServiceBeanControl object. If false, the
     * AssociationManagement object will not unadvertise the ServiceBean
     */
    void setUnadvertiseOnBroken(boolean unadvertiseOnBroken);
}
