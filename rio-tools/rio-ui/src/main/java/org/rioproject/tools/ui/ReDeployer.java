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
package org.rioproject.tools.ui;

import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.ui.Util;

/**
 * Redeploy utilities
 *
 * @author Dennis Reedy
 */
class ReDeployer {

    /**
     * Redeploy an OperationalString
     *
     * @param monitor - The ProvisionMonitor instance to use
     * @param opStringName - The name of the OperationalString the services
     * are part of
     *
     * @throws Exception If there are errors redeploying
     */
    static void redeploy(ProvisionMonitor monitor,
                         String opStringName) throws Exception {
        OperationalStringManager mgr =
            Util.getOperationalStringManager(monitor, opStringName);
        mgr.redeploy(null, null, false, 0, null);
    }

    /**
     * Redeploy a ServiceElement
     *
     * @param monitor - The ProvisionMonitor instance to use
     * @param opStringName - The name of the OperationalString the services
     * are part of
     * @param sElem - The ServiceElement to be redeployed
     *
     * @throws Exception If there are errors redeploying
     */
    static void redeploy(ProvisionMonitor monitor,
                         String opStringName,
                         ServiceElement sElem) throws Exception {

        OperationalStringManager mgr =
            Util.getOperationalStringManager(monitor, opStringName);
        if(mgr!=null) {
            mgr.redeploy(sElem, null, false, 0, null);
        }
    }

    /**
     * Redeploy a service
     *
     * @param monitor - The ProvisionMonitor instance to use
     * @param opStringName - The name of the OperationalString the services
     * are part of
     * @param instance The ServiceBeanInstance
     *
     * @throws Exception If there are errors redeploying
     */
    static void redeploy(ProvisionMonitor monitor,
                         String opStringName,
                         ServiceBeanInstance instance) throws Exception {

        OperationalStringManager mgr =
            Util.getOperationalStringManager(monitor, opStringName);
        if(mgr!=null) {
            mgr.redeploy(null, instance, false, 0, null);
        }
    }

}

