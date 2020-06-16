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

import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;

import java.net.URL;
import java.rmi.RemoteException;

/**
 * DeployAdmin interface defines methods to deploy, undeploy, redeploy, query
 * and optionally remove pending deployment and redeployment requests.
 *
 * @author Dennis Reedy
 */
public interface DeployAdmin {
      
    /**
     * Deploy an OperationalString to the ProvisionMonitor. The
     * ProvisionMonitor will load the location and deploy the OperationalString
     *
     * If the OperationalString includes nested OperationalStrings, the nested 
     * OperationalStrings will be deployed as well. If nested OperationalString
     * items are already deployed, they will not be re-deployed. If the 
     * OperationalString specified by the input URL has already been deployed,
     * or is scheduled for deployment, then no part of that OperationalString
     * (or nested OperationalString instances) will be deployed
     * 
     * @param opStringLocation The location of the OperationalString to deploy
     * (may also be an OAR, or artifact that resolves to an OAR)
     * 
     * @return A {@code DeploymentResult}.
     *
     * @throws OperationalStringException If the OperationalString parsing
     * errors occur
     * @throws RemoteException If communication errors happen
     */
    DeploymentResult deploy(String opStringLocation) throws OperationalStringException, RemoteException;

    /**
     * Deploy an OperationalString to the ProvisionMonitor. The
     * ProvisionMonitor will load the location and deploy the OperationalString
     *
     * If the OperationalString includes nested OperationalStrings, the nested
     * OperationalStrings will be deployed as well. If nested OperationalString
     * items are already deployed, they will not be re-deployed. If the
     * OperationalString specified by the input URL has already been deployed,
     * or is scheduled for deployment, then no part of that OperationalString
     * (or nested OperationalString instances) will be deployed
     *
     * @param opStringLocation The location of the OperationalString to deploy
     * (may also be an OAR, or artifact that resolves to an OAR)
     * @param listener If not null, the ServiceProvisionListener will be
     * notified as each service is deployed
     *
     * @return A {@code DeploymentResult}.
     *
     * @throws OperationalStringException If OperationalString parsing
     * errors occur
     * @throws RemoteException If communication errors happen
     */
    DeploymentResult deploy(String opStringLocation, ServiceProvisionListener listener)
        throws OperationalStringException, RemoteException;


    /**
     * Deploy an OperationalString URL to the ProvisionMonitor. The
     * ProvisionMonitor will load the location and deploy the OperationalString
     *
     * If the OperationalString includes nested OperationalStrings, the nested
     * OperationalStrings will be deployed as well. If nested OperationalString
     * items are already deployed, they will not be re-deployed. If the
     * OperationalString specified by the input URL has already been deployed,
     * or is scheduled for deployment, then no part of that OperationalString
     * (or nested OperationalString instances) will be deployed
     *
     * @param opStringUrl The URL indicating the location of the
     * OperationalString to deploy
     *
     * @return A {@code DeploymentResult}.
     *
     * @throws OperationalStringException If the OperationalString parsing
     * errors occur
     * @throws RemoteException If communication errors happen
     */
    DeploymentResult deploy(URL opStringUrl) throws OperationalStringException, RemoteException;

    
    /**
     * Deploy an OperationalString URL to the ProvisionMonitor. The 
     * ProvisionMonitor will load the location and deploy the OperationalString
     *
     * If the OperationalString includes nested OperationalStrings, the nested 
     * OperationalStrings will be deployed as well. If nested OperationalString
     * items are already deployed, they will not be re-deployed. If the 
     * OperationalString specified by the input URL has already been deployed,
     * or is scheduled for deployment, then no part of that OperationalString
     * (or nested OperationalString instances) will be deployed
     * 
     * @param opStringUrl The URL indicating the location of the
     * OperationalString to deploy
     * @param listener If not null, the ServiceProvisionListener will be
     * notified as each service is deployed
     *
     * @return A {@code DeploymentResult}.
     *
     * @throws OperationalStringException If OperationalString parsing
     * errors occur
     * @throws RemoteException If communication errors happen
     */
    DeploymentResult deploy(URL opStringUrl, ServiceProvisionListener listener)
    throws OperationalStringException, RemoteException;
    
    /**
     * Deploy an OperationalString to the ProvisionMonitor. The 
     * ProvisionMonitor will deploy the contents specified by the 
     * OperationalString object
     *
     * f the OperationalString includes nested OperationalStrings, the nested 
     * OperationalStrings will be deployed as well. If nested OperationalString
     * items are already deployed, they will not be re-deployed. If the 
     * OperationalString specified by the input URL has already been deployed,
     * or is scheduled for deployment, then no part of that OperationalString
     * (or nested OperationalString instances) will be deployed
     * 
     * @param opstring The OperationalString to deploy
     *
     * @return A {@code DeploymentResult}.
     *
     * @throws OperationalStringException If the OperationalString parsing
     * errors occur
     * @throws RemoteException If communication errors happen
     */
    DeploymentResult deploy(OperationalString opstring)
    throws OperationalStringException, RemoteException;
    
    /**
     * Deploy an OperationalString to the ProvisionMonitor. The 
     * ProvisionMonitor will deploy the contents specified by the 
     * OperationalString object
     *
     * f the OperationalString includes nested OperationalStrings, the nested 
     * OperationalStrings will be deployed as well. If nested OperationalString
     * items are already deployed, they will not be re-deployed. If the 
     * OperationalString specified by the input URL has already been deployed,
     * or is scheduled for deployment, then no part of that OperationalString
     * (or nested OperationalString instances) will be deployed
     * 
     * @param opstring The OperationalString to deploy
     * @param listener If not null, the ServiceProvisionListener will be
     * notified as each service is deployed
     *
     * @return A {@code DeploymentResult}.
     *
     * @throws OperationalStringException if the the entire deployment fails
     * @throws RemoteException If communication errors happen
     */
    DeploymentResult deploy(OperationalString opstring, ServiceProvisionListener listener)
    throws OperationalStringException, RemoteException;

    /**
     * Undeploy an OperationalString deployed by the
     * ProvisionMonitor. The ProvisionMonitor will search as follows:
     * <ol>
     * <li>First for the
     * OperationalString by it's name, and if found, remove the
     * OperationalString and any nested OperationalStrings that are included
     * by the OperatinalString.
     * <li>If the name is not found, the ProvisionMonitor will
     * search for the matching OAR, by looking at directory names under the
     * configured <tt>installDir</tt> property. If a matching directory name
     * is found, the corresponding OAR
     * file will be loaded, loading the configured OperationalString,
     * and undeploying as indicated in step 1.
     * </ol> 
     *
     * As a result of undeploying the
     * OperationalString(s), all services that have a provision type of
     * {@link org.rioproject.opstring.ServiceElement.ProvisionType#DYNAMIC} or
     * {@link org.rioproject.opstring.ServiceElement.ProvisionType#FIXED}
     * will be terminated. If any deployment request or redeployment requests
     * are pending for the OperationalString(s) being undeployed, these
     * requests will be cancelled
     * 
     * @param name The name of the OperationalString or directory to
     * undeploy
     *
     * @return Returns true if the OperationalString has been undeployed
     *
     * @throws OperationalStringException If the OperationalString does not
     * exist
     * @throws RemoteException If communication errors occur
     */
    boolean undeploy(String name) throws OperationalStringException,
                                         RemoteException;
    
    /**
     * Determine if this ProvisionMonitor has deployed the 
     * OperationalString as determined by the name
     * 
     * @param opStringName The name of the OperationalString
     * 
     * @return If found return true, otherwise false
     *
     * @throws RemoteException If communication errors happen
     */
    boolean hasDeployed(String opStringName) throws RemoteException;

    /**
     * This will retrieve an array of OperationalStringManager instances that
     * are managing deployed OperationalString instances. If an
     * OperationalString includes nested OperationalStrings, those
     * OperationalStringManager instances that are nested will be returned as
     * part of this array.
     * 
     * @return Array of OperationalStringManager instances that are managing
     * deployed OperationalString instances. Only OperationalStringManager
     * instances created on the local ProvisionMonitor will be returned
     *
     * @throws RemoteException If communication errors happen
     */
    OperationalStringManager[] getOperationalStringManagers()
    throws RemoteException;
    
    /**
     * Retrieve the managing OperationalStringManager for an OperationalString
     * 
     * @param name The name of the OperationalString
     * 
     * @return The OperationalStringManager instance that is managing a deployed 
     * OperationalString. The OperationalStringManager returned may be running
     * in a remote ProvisionMonitor
     *
     * @throws OperationalStringException If an OperationalStringManager can not
     * be found to the OperationalString
     * @throws RemoteException If communication errors happen
     */
    OperationalStringManager getOperationalStringManager(String name) 
        throws OperationalStringException, RemoteException;

 }
