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

import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.monitor.OpStringManager;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

/**
 * This class represents a scheduled redeployment request
 */
public class RedeploymentTask extends TimerTask {
    OpStringManager opMgr;
    ServiceBeanInstance instance;
    ServiceElement sElem;
    boolean clean = false;
    boolean sticky = false;
    ServiceProvisionListener listener;
    static Logger logger = LoggerFactory.getLogger(RedeploymentTask.class.getName());


    /**
     * Create a RedeploymentTask
     *
     * @param opMgr    The OpStringManager which scheduled the task
     * @param sElem    The ServiceElement
     * @param instance The ServiceBeanInstance
     * @param clean    Use the original configuration or the current instance's config
     * @param sticky   Use the same cybernode
     * @param listener A ServiceProvisionListener
     */
    public RedeploymentTask(OpStringManager opMgr,
                            ServiceElement sElem,
                            ServiceBeanInstance instance,
                            boolean clean,
                            boolean sticky,
                            ServiceProvisionListener listener) {
        this.opMgr = opMgr;
        this.instance = instance;
        this.sElem = sElem;
        this.clean = clean;
        this.sticky = sticky;
        this.listener = listener;
    }

    public ServiceElement getServiceElement() {
        return sElem;
    }

    public ServiceBeanInstance getInstance() {
        return instance;
    }

    public void run() {
        if (!opMgr.isActive()) {
            if (logger.isTraceEnabled()) {
                String name = "unknown";
                if (instance == null && sElem == null) {
                    name = opMgr.getName();
                } else {
                    if (sElem != null)
                        name = sElem.getName();
                    if (instance != null)
                        name = instance.getServiceBeanConfig().getName();
                }
                logger.trace("Redeployment request for " + "[" + name + "] " +
                              "cancelled, OpStringManager is not primary");
            }
            cancel();
            return;
        }

        try {
            if (instance == null && sElem == null) {
                opMgr.doRedeploy(clean, sticky, listener);
            } else {
                opMgr.doRedeploy(sElem, instance, clean, sticky, listener);
            }
        } catch (Exception e) {
            logger.warn("Executing Scheduled Redeployment", e);
        } finally {
            cancel();
        }
    }

    public boolean cancel() {
        if (opMgr != null)
            opMgr.removeTask(this);
        return (super.cancel());
    }
}
