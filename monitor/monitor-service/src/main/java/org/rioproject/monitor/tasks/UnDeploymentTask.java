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
package org.rioproject.monitor.tasks;

import org.rioproject.deploy.DeployAdmin;
import org.rioproject.opstring.OperationalString;
import org.rioproject.monitor.OpStringManager;
import org.rioproject.monitor.ServiceElementManager;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduled Task which will undeploy an OperationalString
 */
public class UnDeploymentTask extends TimerTask {
    private OpStringManager opMgr;
    private boolean undeploy;
    DeployAdmin deployAdmin;
    static Logger logger = Logger.getLogger(UnDeploymentTask.class.getName());

    UnDeploymentTask(OpStringManager opMgr, boolean undeploy, DeployAdmin deployAdmin) {
        this.opMgr = opMgr;
        this.undeploy = undeploy;
        this.deployAdmin = deployAdmin;
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Create new UnDeploymentTask for [" + opMgr.getName() + "]");
    }

    /**
     * The action to be performed by this timer task.
     */
    public void run() {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Undeploy [" + opMgr.getName() + "]");
        if (undeploy) {
            opMgr.setDeploymentStatus(OperationalString.UNDEPLOYED);
            try {
                if (opMgr.isActive())
                    deployAdmin.undeploy(opMgr.getName());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Undeploying OperationalString", e);
            }
        } else {
            ServiceElementManager[] mgrs = opMgr.getServiceElementManagers();
            for (ServiceElementManager mgr : mgrs)
                mgr.stopManager(true);
            opMgr.setDeploymentStatus(OperationalString.SCHEDULED);
        }
    }

    public boolean cancel() {
        if (opMgr != null)
            opMgr.removeTask(this);
        return (super.cancel());
    }
}
