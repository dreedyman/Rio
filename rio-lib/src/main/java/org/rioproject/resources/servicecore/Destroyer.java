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

import org.rioproject.core.jsb.ServiceBeanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.activation.*;

/**
 * A Service Destroyer
 *
 * @author Dennis Reedy
 */
public class Destroyer extends Thread {
    private final ActivationID activationID;
    private final ServiceBeanContext context;
    private final static long DEFAULT_KILL_WAIT = 1000*2;
    private long killWait;
    private boolean force = false;
    private Logger logger = LoggerFactory.getLogger("org.rioproject.resources.servicecore");

    public Destroyer(ActivationID activationID, ServiceBeanContext context) {
        this(activationID, context, DEFAULT_KILL_WAIT, false);
    }

    public Destroyer(ActivationID activationID, ServiceBeanContext context, long killWait, boolean force) {
        super("Destroyer");
        this.activationID = activationID;
        this.context = context;
        this.force = force;
        this.killWait = killWait;
        start();
    }

    public void run() {
        if(activationID != null) {
            /* inactive will set current group ID to null */
            ActivationGroupID gid = ActivationGroup.currentGroupID();
            try {
                Activatable.inactive(activationID);
                Activatable.unregister(activationID);
            } catch(RemoteException e) {
                logger.error("Communicating to Activation System", e);
            } catch(ActivationException e) {
                logger.error("Communicating to Activation System", e);
            }
            if(!force && context == null) {
                logger.error("Cannot determine Activatable object's context. "+
                              "Unable to unregister the ActivationGroup or terminate the JVM");
                return;
            }
            if(force) {
                try {
                    if(logger.isDebugEnabled())
                        logger.debug("Unregister ActivationGroup");
                    if(gid != null)
                        ActivationGroup.getSystem().unregisterGroup(gid);
                    else {
                        if(logger.isDebugEnabled())
                            logger.debug("Unable to unregister ActivationGroup: groupID is null");
                    }
                } catch(RemoteException e) {
                    logger.warn("RemoteException unregistering Activation group");
                } catch(ActivationException e) {
                    logger.warn("ActivationException unregistering Activation group");
                }
                killVM(killWait);
            }
        } else {
            killVM(killWait);
        }
    }

    private void killVM(long wait) {
        try {
            if(logger.isDebugEnabled())
                logger.debug("Shutting down the JVM in {} seconds ...", (wait/1000));
            Thread.sleep(wait);
        } catch(InterruptedException e) {
            logger.warn("Interrupted while waiting");
        }
        if(logger.isDebugEnabled())
            logger.debug("JVM Shutting down");
        System.exit(0);
    }
}
