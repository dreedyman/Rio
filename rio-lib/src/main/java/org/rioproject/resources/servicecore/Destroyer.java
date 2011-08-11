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

import java.rmi.RemoteException;
import java.rmi.activation.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Service Destroyer
 *
 * @author Dennis Reedy
 */
public class Destroyer extends Thread {
    private ActivationID activationID;
    private ServiceBeanContext context;
    private final static long DEFAULT_KILL_WAIT = 1000*2;
    private long killWait;
    private boolean force = false;
    private Logger logger =
        Logger.getLogger("org.rioproject.resources.servicecore");

    public Destroyer(ActivationID activationID, ServiceBeanContext context) {
        this(activationID,
             context,
             DEFAULT_KILL_WAIT,
             false);
    }

    public Destroyer(ActivationID activationID,
                     ServiceBeanContext context,
                     boolean force) {
        this(activationID,
             context,
             DEFAULT_KILL_WAIT,
             force);
    }

    public Destroyer(ActivationID activationID,
                     ServiceBeanContext context,
                     long killWait) {
        this(activationID,
             context,
             killWait,
             false);
    }

    public Destroyer(ActivationID activationID, ServiceBeanContext context,
                     long killWait, boolean force) {
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
                logger.log(Level.SEVERE,
                           "Communicating to Activation System",
                           e);
            } catch(ActivationException e) {
                logger.log(Level.SEVERE,
                           "Communicating to Activation System",
                           e);
            }
            if(!force && context == null) {
                logger.severe("Cannot determine Activatable object's context. "+
                              "Unable to unregister the ActivationGroup or "+
                              "terminate the JVM");
                return;
            }
            if(force) {
                try {
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Unregister ActivationGroup");
                    if(gid != null)
                        ActivationGroup.getSystem().unregisterGroup(gid);
                    else {
                        if(logger.isLoggable(Level.FINE))
                            logger.fine("Unable to unregister ActivationGroup: "+
                                        "groupID is null");
                    }
                } catch(RemoteException e) {
                    logger.warning("RemoteException unregistering Activation group");
                } catch(ActivationException e) {
                    logger.warning(
                        "ActivationException unregistering Activation "+
                        "group");
                }
                killVM(killWait);
            }
        } else {
            killVM(killWait);
        }
    }

    private void killVM(long wait) {
        try {
            if(logger.isLoggable(Level.FINE))
                logger.fine("Shutting down the JVM in "
                            +(wait/1000)
                            +" seconds ...");
            Thread.sleep(wait);
        } catch(InterruptedException e) {
            logger.warning("Interrupted while waiting");
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine("JVM Shutting down");
        System.exit(0);
    }
}
