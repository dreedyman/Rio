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
package org.rioproject.tools.ui.util;

import net.jini.core.lookup.ServiceItem;
import org.rioproject.opstring.OperationalString;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.ui.Util;
import org.rioproject.tools.ui.progresspanel.WaitingDialog;

import javax.swing.*;

/**
 * Utility that uses a SwingWorker to deploy operational strings and artifacts.
 */
public class SwingDeployHelper {

    /**
     * Deploy operational strings
     *
     * @param opstrings The operational strings to deploy
     * @param item The Provision Monitor's ServiceItem
     * @param frame The UI's frame, used as the JOptionPane's parent if a confirm dialog needs to be shown
     * @param deployName The name of the selected operational string
     */
    public static void deploy(final OperationalString[] opstrings,
                              final ServiceItem item,
                              final JFrame frame,
                              final String deployName) {
        StringBuilder opstringNames = new StringBuilder();
        for(OperationalString opString : opstrings) {
            if(opstringNames.length()>0)
                opstringNames.append(", ");
            opstringNames.append(opString.getName());
        }
        final JDialog dialog = new WaitingDialog(frame,
                                                 "Deploying "+opstringNames.toString()+"...",
                                                 500);
        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                try {
                    ProvisionMonitor monitor = (ProvisionMonitor) item.service;
                    DeployAdmin dAdmin = (DeployAdmin) monitor.getAdmin();
                    for (OperationalString opString : opstrings) {
                        if (dAdmin.hasDeployed(opString.getName())) {
                            int result = JOptionPane.showConfirmDialog(frame,
                                                                       "The [" + opString.getName() + "] " +
                                                                       "is already deployed, " +
                                                                       "update the deployment?",
                                                                       "Update Deployed Application",
                                                                       JOptionPane.YES_NO_OPTION);
                            if (result == JOptionPane.YES_OPTION) {
                                dAdmin.getOperationalStringManager(opString.getName()).update(opString);
                            }
                        } else {
                            dAdmin.deploy(opString);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Util.showError(e, frame, "Failure trying to deploy " + deployName);
                }
                return null;
            }

            @Override
            public void finished() {
                dialog.dispose();
            }
        };
        worker.start();
    }

    /**
     * Deploy operational strings
     *
     * @param artifact The artifact to deploy
     * @param item The Provision Monitor's ServiceItem
     * @param frame The UI's frame, used as the JOptionPane's parent if a confirm dialog or an error needs to be shown
     */
    public static void deploy(final String artifact,
                              final ServiceItem item,
                              final JFrame frame) {
        final JDialog dialog = new WaitingDialog(frame, "Deploying "+artifact+"...", 500);

        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                try {
                    ProvisionMonitor monitor = (ProvisionMonitor) item.service;
                    DeployAdmin dAdmin = (DeployAdmin) monitor.getAdmin();
                    dAdmin.deploy(artifact);
                } catch (Exception e) {
                    e.printStackTrace();
                    Throwable cause = e.getCause();
                    if(cause != null) {
                        Throwable nested = cause.getCause();
                        Util.showError((nested==null?cause:nested),
                                       frame,
                                       "Failure trying to deploy artifact" + artifact);
                    } else {
                        Util.showError(e, frame, "Failure trying to deploy artifact" + artifact);
                    }
                }
                return null;
            }

            @Override
            public void finished() {
                dialog.dispose();
            }
        };
        worker.start();
    }
}
