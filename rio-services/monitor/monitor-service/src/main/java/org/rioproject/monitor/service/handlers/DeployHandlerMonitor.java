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
package org.rioproject.monitor.service.handlers;

import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.DeploymentResult;
import org.rioproject.monitor.service.OpStringManager;
import org.rioproject.monitor.service.OpStringManagerController;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Use DeployHandlers to provide hot deployment capability
 */
public class DeployHandlerMonitor {
    private DeployHandler[] deployHandlers;
    private ScheduledExecutorService deployExecutor;
    private long lastRecordedTime;
    private OpStringManagerController opStringMangerController;
    private DeployAdmin deployAdmin;
    static Logger logger = LoggerFactory.getLogger(DeployHandlerMonitor.class.getName());

    public DeployHandlerMonitor(DeployHandler[] deployHandlers,
                                long deployScan,
                                OpStringManagerController opStringMangerController,
                                DeployAdmin deployAdmin) {
        this.deployHandlers = deployHandlers;
        this.opStringMangerController = opStringMangerController;
        this.deployAdmin = deployAdmin;
        processDeployHandlers(null);
        lastRecordedTime = System.currentTimeMillis();
        deployExecutor = Executors.newSingleThreadScheduledExecutor();

        deployExecutor.scheduleAtFixedRate(new Runnable() {
                                                  public void run() {
                                                      processDeployHandlers(new Date(lastRecordedTime));
                                                      lastRecordedTime = System.currentTimeMillis();
                                                  }
                                              },
                                              0,
                                              deployScan,
                                              TimeUnit.MILLISECONDS);
    }

    public void terminate() {
        if (deployExecutor != null)
            deployExecutor.shutdownNow();
    }

    private void processDeployHandlers(Date from) {
        for (DeployHandler dHandler : deployHandlers) {
            List<OperationalString> opstrings = from == null ?
                                                dHandler.listOfOperationalStrings() :
                                                dHandler.listOfOperationalStrings(from);
            for (OperationalString opstring : opstrings) {
                String action = null;
                try {
                    Map<String, Throwable> errorMap;
                    if (opStringMangerController.getOpStringManager(opstring.getName())!=null) {
                        action = "update";
                        OpStringManager mgr = opStringMangerController.getOpStringManager(opstring.getName());
                        errorMap = mgr.doUpdateOperationalString(opstring);
                    } else {
                        action = "deploy";
                        DeploymentResult result = deployAdmin.deploy(opstring, null);
                        errorMap = result.getErrorMap();
                    }
                    if (!errorMap.isEmpty()) {
                        for (Map.Entry<String, Throwable> entry : errorMap.entrySet()) {
                            logger.warn("Deploying service [" + entry.getKey() + "] resulted in " +
                                        "the following exception",
                                        entry.getValue());
                        }
                    }
                } catch (OperationalStringException e) {
                    logger.warn("Unable to " + action + " [" + opstring.getName() + "], "+e.getMessage());
                } catch (RemoteException e) {
                    logger.warn("Unable to " + action + " [" + opstring.getName() + "]", e);
                }
            }
        }
    }
}
