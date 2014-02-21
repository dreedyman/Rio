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
package org.rioproject.monitor.service;

import org.rioproject.deploy.DeploymentResult;
import org.rioproject.impl.servicebean.ServiceBeanAdapterMBean;

import javax.management.openmbean.TabularData;
import java.net.MalformedURLException;

/**
 * MBean interface for the ProvisionMonitor
 *
 * @author Ming Fang
 * @author Dennis Reedy
 */
public interface ProvisionMonitorImplMBean extends ServiceBeanAdapterMBean {
    /**
     * Deploy an OperationalString URL to the ProvisionMonitor. The
     * ProvisionMonitor will attempt to load the argument first from the
     * local file system as a {@link java.io.File} object, if the
     * <tt>File</tt> object exists, it will be used, otherwise the argument
     * will be used to create a {@link java.net.URL} object.
     *
     * If the OperationalString includes nested OperationalStrings, the nested
     * OperationalStrings will be deployed as well. If nested OperationalString
     * items are already deployed, they will not be re-deployed. If the
     * OperationalString specified by the input URL has already been deployed,
     * or is scheduled for deployment, then no part of that OperationalString
     * (or nested OperationalString instances) will be deployed
     *
     * @param opString The parameter indicating the location of the
     * OperationalString to deploy, may also be an artifact {groupId:artifactId:version}
     *
     * @return A {@code DeploymentResult}.
     *
     * @throws IllegalArgumentException if the opStringUrl is <code>null</code>
     * @throws MalformedURLException if the opStringUrl cannot be used to
     * create a <code>java.net.URL</code>
     */
    DeploymentResult deploy(String opString) throws MalformedURLException;

    /**
     * Undeploy and Remove an OperationalString deployed by the
     * ProvisionMonitor. The ProvisionMonitor will search for the
     * OperationalString by it's name and if found, remove the
     * OperationalString and any nested OperationalStrings that are included
     * by the OperationalString. As a result of undeploying the
     * OperationalString(s), all services that have a provision type of
     * ServiceProvisionManagement.DYNAMIC or ServiceProvisionManagement.FIXED
     * will be terminated. If any DeploymentRequest or RedeploymenRequests are
     * pending for the OperationalString(s) being undeployed, these requests
     * will be cancelled
     *
     * @param name The name of the OperationalString to remove
     *
     * @return Returns true if the OperationalString has been undeployed
     *
     * @throws IllegalArgumentException if the name is <code>null</code>
     */
    boolean undeploy(String name);

    /**
     * Get the current deployments for the ProvisionMonitor
     *
     * @return The current deployments in a tabular form, containing the
     * deployment name, the role of the Provisioner, and the time deployed
     */
    TabularData getDeployments();
}
