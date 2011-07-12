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

import org.rioproject.core.OperationalString;
import org.rioproject.core.Schedule;
import org.rioproject.monitor.DeployAdmin;
import org.rioproject.monitor.OpStringManager;
import org.rioproject.monitor.ServiceElementManager;

import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduled Task which will control deployment scheduling
 */
public class DeploymentTask extends TimerTask {
    private int repeats;
    private DeployAdmin deployAdmin;
    private OpStringManager opMgr;
    static Logger logger = Logger.getLogger(DeploymentTask.class.getName());

    public DeploymentTask(OpStringManager opMgr, DeployAdmin deployAdmin) {
        this.opMgr = opMgr;
        this.deployAdmin = deployAdmin;
    }

    /**
     * The action to be performed by this timer task.
     */
    public void run() {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Deploy [" + opMgr.getName() + "]");
        opMgr.addDeploymentDate(new Date(System.currentTimeMillis()));
        ServiceElementManager[] mgrs = opMgr.getServiceElementManagers();
        for (ServiceElementManager mgr : mgrs) {
            try {
                int alreadyRunning = mgr.startManager(null);
                if (alreadyRunning > 0) {
                    opMgr.doUpdateServiceElement(mgr.getServiceElement());
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Starting ServiceElementManager", e);
            }
        }
        opMgr.setDeploymentStatus(OperationalString.DEPLOYED);
        OperationalString loadedOpString = opMgr.doGetOperationalString();
        Schedule schedule = loadedOpString.getSchedule();
        repeats++;
        if (schedule.getRepeatCount() != Schedule.INDEFINITE &&
            repeats > schedule.getRepeatCount()) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Repeat count [" + schedule.getRepeatCount() + "] " +
                            "reached for OperationalString " +
                            "[" + loadedOpString.getName() + "], " +
                            "cancel DeploymentTask");
            cancel();
            UnDeploymentTask unDeploymentTask = new UnDeploymentTask(opMgr, true, deployAdmin);
            opMgr.addTask(unDeploymentTask);
            long scheduledUndeploy = schedule.getDuration() + System.currentTimeMillis();
            TaskTimer.getInstance().schedule(unDeploymentTask, new Date(scheduledUndeploy));
        } else {
            if (schedule.getDuration() != Schedule.INDEFINITE) {
                UnDeploymentTask unDeploymentTask = new UnDeploymentTask(opMgr, false, deployAdmin);
                long scheduledUndeploy = schedule.getDuration() + System.currentTimeMillis();
                TaskTimer.getInstance().schedule(unDeploymentTask, new Date(scheduledUndeploy));
            }
        }
    }

    public boolean cancel() {
        if (opMgr != null)
            opMgr.removeTask(this);
        return (super.cancel());
    }
}