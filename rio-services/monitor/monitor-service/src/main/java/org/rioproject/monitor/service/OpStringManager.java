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
package org.rioproject.monitor.service;

import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.impl.opstring.OAR;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.RemoteRepository;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;

/**
 * Extends {@link org.rioproject.opstring.OperationalStringManager} and provides additional support for
 * working within the monitor
 */
public interface OpStringManager  {
    /**
     * Get the active property
     *
     * @return The active property
     */
    boolean isActive();

    /**
     * Get the name of the managed Operational String
     *
     * @return The name of the managed Operational String
     */
    String getName();

    /**
     * Set that the {@code OpStringManager} is managing the OperationalString
     *
     * @param newActive If {@code true}, managing
     */
    void setManaging(boolean newActive);

    /**
     * Get date(s) when the {@code OperationalString} was deployed
     *
     * @return An array of {@code Date}s when the {@code OperationalString} was deployed
     */
    Date[] getDeploymentDates();

    /**
     * Get any repositories used for the deployment of the {@code OperationalString} when loaded from
     * an {@code OAR}.
     *
     * @return An array of {@code RemoteRepository}s used for deployment. If the deployment was not done using an
     * {@code OAR}, this method will return an empty array.
     */
    RemoteRepository[] getRemoteRepositories();

    /**
     * Get the {@code OAR} used to deploy
     *
     * @return If the deployment used an {@code OAR}, return it. Otherwise return {@code null}.
     */
    OAR getOAR();

    /**
     * Determine of this OpStringManager has any parents
     *
     * @return If true, this OpStringManager is top-level (has no parents)
     */
    boolean isTopLevel();

    /**
     * Get the OperationalStringManager remote object
     *
     * @return The OperationalStringManager remote object created during a call to the Exporter
     */
    OperationalStringManager getProxy();

    /**
     * Get all ServiceElementManager instances
     *
     * @return All ServiceElementManager instances as an array
     */
    ServiceElementManager[] getServiceElementManagers();

    /**
     * Add a deployment Date
     *
     * @param date A Date indicating when an OperationalString gets deployed
     */
    void addDeploymentDate(Date date);

    /**
     * Add a nested OpStringManager
     *
     * @param nestedMgr The nested OpStringManager to add
     */
    void addNested(OpStringManager nestedMgr);

    /**
     * Add a parent for this OpStringManager. This OpStringManager will
     * now be a nested OpStringManager
     *
     * @param parent The parent for this OpStringManager.
     */
    void addParent(OpStringManager parent);

    /**
     * Get the number of parents the OpStringManager has
     *
     * @return The number of parents the OpStringManager has
     */
    int getParentCount();

    /**
     * Get the collection of parent for the OpStringManager
     *
     * @return A Collection od parents, if there are no parents return a zero-length collection.
     * A new collection is created each time.
     */
    Collection<OpStringManager> getParents();

    /**
     * Remove a parent from this OpStringManager.
     *
     * @param parent The parent to remove
     */
    void removeParent(OpStringManager parent);

    /**
     * Remove a nested OpStringManager
     *
     * @param nestedMgr The nested OpStringManager to remove
     */
    void removeNested(OpStringManager nestedMgr);

    /**
     * Add a TimerTask to the Collection of TimerTasks
     *
     * @param task The TimerTask to add
     */
    void addTask(TimerTask task);

    /**
     * Remove a TimerTask from Collection of scheduled TimerTask
     * instances
     *
     * @param task The TimerTask to remove
     */
    void removeTask(TimerTask task);

    /**
     * Set the active mode. If the new mode is not equal to the old mode,
     * iterate through the Collection of ServiceElementManager instances and
     * set their mode to be equal to the OpStringManager mode
     *
     * @param newActive the new mode
     */
    void setActive(boolean newActive);

    /**
     * Verify all services are being monitored by iterating through the
     * Collection of ServiceElementManager instances and invoking each
     * instance's verify() method
     *
     * @param listener A ServiceProvisionListener that will be notified of services if they are provisioned.
     */
    void verify(ServiceProvisionListener listener);

    /**
     * Get the OperationalString the OpStringManager is managing
     *
     * @return The OperationalString the OpStringManager is managing
     */
    OperationalString doGetOperationalString();

    /**
     * Update a ServiceElement
     *
     * @param sElem The ServiceElement to update
     *
     * @throws Exception if there are errors
     */
    void doUpdateServiceElement(ServiceElement sElem) throws Exception;

    /**
     * Add a ServiceElement
     *
     * @param sElem The ServiceElement to add
     * @param listener Provide a listener for notification
     *
     * @throws Exception if there are errors
     */
    void doAddServiceElement(ServiceElement sElem, ServiceProvisionListener listener) throws Exception;

    /**
     * Set the deployment status
     *
     * @param status Either OperationalString.UNDEPLOYED, OperationalString.SCHEDULED or OperationalString.DEPLOYED
     */
    void setDeploymentStatus(int status);

    /**
     * Undeploy the OperationalString
     *
     * @param killServices If true destroy the services being managed
     * @return Array of terminated OperationalStrings. If the
     *         managed OperationalString has no nested OperationalStrings, return
     *         just the OperationalString that was being managed. If nested
     *         OperationalStrings were also undeployed, return those as well. A
     *         new array is allocated each time.
     */
    OperationalString[] terminate(boolean killServices);

    /**
     * Update an OperationalString
     *
     * @param newOpString The updated opstring
     *
     * @return Map of any errors
     */
    Map<String, Throwable> doUpdateOperationalString(OperationalString newOpString);

    /**
     * Update a ServiceBeanInstance
     *
     * @param instance The service instance to update
     *
     * @return The ServiceElement that contains he updated details for the instance
     *
     * @throws org.rioproject.opstring.OperationalStringException if there are errors updating
     */
    ServiceElement doUpdateServiceBeanInstance(ServiceBeanInstance instance) throws OperationalStringException;

    /**
     * Remove a ServiceElement
     *
     * @param sElem The service to remove
     * @param destroy If true, the service will be destroyed
     *
     * @throws org.rioproject.opstring.OperationalStringException if there are errors during removal
     */
    void doRemoveServiceElement(ServiceElement sElem, boolean destroy)throws OperationalStringException;

    /**
     * Get the ServiceElementManager for a ServiceElement instance
     *
     * @param sElem The ServiceElement instance
     * @return The ServiceElementManager that is managing the ServiceElement.
     *         If no ServiceElementManager is found, null is returned
     */
    ServiceElementManager getServiceElementManager(ServiceElement sElem);

    /**
     * Redeploy the OperationalString
     *
     * @param clean Set to true if the redeployment should not include any previous state
     * @param sticky Set to true if the redeployment should be on the same machine
     * @param listener Provide a listener for notification
     * @throws org.rioproject.opstring.OperationalStringException if there are errors during redeployment
     */
    void doRedeploy(boolean clean, boolean sticky, ServiceProvisionListener listener) throws OperationalStringException;

    /**
     * Redeploy a service
     *
     * @param sElem The service to redeploy
     * @param instance The ServiceBeanInstance
     * @param clean Set to true if the redeployment should not include any previous state
     * @param sticky Set to true if the redeployment should be on the same machine
     * @param listener Provide a listener for notification
     *
     * @throws org.rioproject.opstring.OperationalStringException if there are errors during redeployment
     */
    void doRedeploy(ServiceElement sElem,
                    ServiceBeanInstance instance,
                    boolean clean,
                    boolean sticky,
                    ServiceProvisionListener listener) throws OperationalStringException;

    /**
     * Schedule a redeploy task
     *
     * @param delay If > 0, the delay
     * @param sElem The service to redeploy
     * @param instance The ServiceBeanInstance
     * @param clean Set to true if the redeployment should not include any previous state
     * @param sticky Set to true if the redeployment should be on the same machine
     * @param listener Provide a listener for notification
     *
     * @throws org.rioproject.opstring.OperationalStringException if there are errors during redeployment
     */
    void doScheduleRedeploymentTask(long delay,
                                    ServiceElement sElem,
                                    ServiceBeanInstance instance,
                                    boolean clean,
                                    boolean sticky,
                                    ServiceProvisionListener listener) throws OperationalStringException;

    /**
     * Determine if this OperationalString was deployed as stand-alone.
     *
     * @return {@code true} if the OperationalString was deployed without any nested or parent relationships
     */
    boolean isStandAlone();

    /**
     * Get the {@code OperationalStringManager}
     *
     * @return The {@code OperationalStringManager}
     */
    OperationalStringManager getOperationalStringManager();
}
