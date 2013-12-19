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
package org.rioproject.tools.ui;

import net.jini.config.Configuration;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.LookupCache;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.impl.opstring.OAR;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.resolver.Artifact;
import org.rioproject.tools.ui.progresspanel.WaitingDialog;
import org.rioproject.tools.ui.serviceui.UndeployPanel;
import org.rioproject.tools.ui.util.*;
import org.rioproject.ui.Util;

import javax.swing.*;
import javax.swing.SwingWorker;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Displays deployments for a Provision Monitor
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorPanel extends JPanel {
    private final GraphView graphView;
    private final ImageIcon westIcon;
    private final ImageIcon westSelectedIcon;
    private final ImageIcon northIcon;
    private final ImageIcon northSelectedIcon;
    private File lastDir = null;
    private String lastArtifact = null;

    public ProvisionMonitorPanel(final ProvisionMonitor monitor,
                                 final LookupCache monitorCache,
                                 final JFrame frame,
                                 final ColorManager colorManager,
                                 final Map<String, ImageIcon> toolBarImageMap,
                                 final Configuration config,
                                 final int orientation) {
        super(new BorderLayout());
        graphView = new GraphView(frame, config, colorManager, monitor, orientation);
        JButton deploy = new JButton("Deploy ...");

        deploy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                OpStringAndOARFileChooser chooser = new OpStringAndOARFileChooser(frame, lastDir, lastArtifact);
                final String chosen = chooser.getName();
                if (chosen == null)
                    return;
                boolean isArtifact = false;
                try {
                    new Artifact(chosen);
                    isArtifact = true;
                    lastArtifact = chosen;
                } catch (Exception e) {
                    /* don't need to print stack trace here */
                }

                /* If deploying an artifact or the oar is http based, deploy */
                if (isArtifact || chosen.startsWith("http")) {
                    final ServiceItem item = monitorCache.lookup(null);
                    SwingDeployHelper.deploy(chosen, item, frame);
                } else {
                    File opStringFile = new File(chosen);
                    if (opStringFile.exists()) {
                        lastDir = chooser.getCurrentDirectory();
                        final ServiceItem item = monitorCache.lookup(null);
                        final OperationalString[] opstrings;
                        try {
                            if (opStringFile.getName().endsWith("oar")) {
                                OAR oar = new OAR(opStringFile);
                                opstrings = oar.loadOperationalStrings();
                                // TODO: embed webster and stream the OAR
                            } else {
                                opstrings = parseOperationalString(opStringFile);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Util.showError(e, frame, "Failure parsing " + opStringFile.getName());
                            return;
                        }
                        SwingDeployHelper.deploy(opstrings, item, frame, chosen);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                                      "The OperationalString file " + chosen + " does not exist",
                                                      "Deployment Failure",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        JButton undeploy = new JButton("Undeploy");
        undeploy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String[] names = graphView.getOpStringNames();
                JDialog dialog = new JDialog((JFrame)null, "Undeploy OperationalString", true);
                UndeployPanel u = new UndeployPanel(names, dialog);
                Container contentPane = dialog.getContentPane();
                contentPane.add(u, BorderLayout.CENTER);
                int width = 380;
                int height = 225;
                dialog.setSize(new Dimension(width, height));
                dialog.setLocationRelativeTo(frame);
                dialog.setVisible(true);
                final String name = u.getSelectedOpStringName();
                if(name==null)
                    return;
                final GraphNode node = graphView.getOpStringNode(name);
                final JDialog waitDialog = new WaitingDialog(frame, "Undeploying "+name+"...", 500);
                org.rioproject.tools.ui.util.SwingWorker worker = new org.rioproject.tools.ui.util.SwingWorker() {
                    public Object construct() {
                        try {
                            DeployAdmin dAdmin = (DeployAdmin)node.getProvisionMonitor().getAdmin();
                            dAdmin.undeploy(name);
                        } catch(OperationalStringException e) {
                            graphView.removeOpString(name);
                        } catch(Exception e) {
                            System.err.println("OUCH");
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    public void finished() {
                        waitDialog.dispose();
                    }
                };
                worker.start();
            }
        });

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorderPainted(false);

        ImageIcon refreshIcon = toolBarImageMap.get("refreshIcon");
        ImageIcon fitIcon = toolBarImageMap.get("fitIcon");
        westIcon = toolBarImageMap.get("westIcon");
        westSelectedIcon = toolBarImageMap.get("westSelectedIcon");
        northIcon = toolBarImageMap.get("northIcon");
        northSelectedIcon = toolBarImageMap.get("northSelectedIcon");
        /*
        JButton zoomOut = new JButton(zoomOutIcon);
        zoomOut.getAccessibleContext().setAccessibleName("zoom-out");
        zoomOut.setToolTipText("Zoom-out the display area");
        zoomOut.setPreferredSize(new Dimension(22, 22));
        zoomOut.setMaximumSize(new Dimension(22, 22));
        zoomOut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                graphView.zoom(0.98);
            }
        });
        JButton zoomIn = new JButton(zoomInIcon);
        zoomIn.getAccessibleContext().setAccessibleName("zoom-in");
        zoomIn.setToolTipText("Zoom-in the display area");
        zoomIn.setPreferredSize(new Dimension(22, 22));
        zoomIn.setMaximumSize(new Dimension(22, 22));
        zoomIn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                graphView.zoom(1.1);
            }
        });
        */
        final JButton fit = new JButton(fitIcon);
        fit.getAccessibleContext().setAccessibleName("fit display");
        fit.setToolTipText("Fit the graph into the display area");
        fit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                graphView.zoomToFit();
            }
        });
        final JButton refresh = new JButton(refreshIcon);
        refresh.getAccessibleContext().setAccessibleName("refresh display");
        refresh.setToolTipText("Refresh the display");
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            graphView.refresh();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final JButton west = new JButton((orientation==prefuse.Constants.ORIENT_LEFT_RIGHT? westSelectedIcon:westIcon));
        west.getAccessibleContext().setAccessibleName("root on left");
        west.setToolTipText("Root on the left");

        final JButton north = new JButton((orientation==prefuse.Constants.ORIENT_TOP_BOTTOM? northSelectedIcon:northIcon));
        west.getAccessibleContext().setAccessibleName("root at top");
        north.setToolTipText("Root at the top");

        west.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                graphView.setOrientation(prefuse.Constants.ORIENT_LEFT_RIGHT);
                west.setIcon(westSelectedIcon);
                north.setIcon(northIcon);
            }
        });

        north.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                graphView.setOrientation(prefuse.Constants.ORIENT_TOP_BOTTOM);
                north.setIcon(northSelectedIcon);
                west.setIcon(westIcon);
            }
        });
        //toolBar.add(zoomIn);
        toolBar.add(fit);
        toolBar.add(refresh);
        toolBar.add(west);
        toolBar.add(north);
        toolBar.setMinimumSize(new Dimension(8, 24));

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBorder(BorderFactory.createEtchedBorder());
        //controls.add(colorChooser);
        controls.add(toolBar);
        controls.add(deploy);
        controls.add(undeploy);

        Dimension dim = getSize();
        dim.height = dim.height- controls.getSize().height;
        graphView.setPreferredSize(dim);
        graphView.setSize(dim);
        //graphView.showProgressPanel();

        setBackground(Color.WHITE);
        add(controls, BorderLayout.NORTH);
        add(graphView, BorderLayout.CENTER);
        graphView.systemUp();
    }

    GraphView getGraphView() {
        return graphView;
    }

    private OperationalString[] parseOperationalString(File file) throws Exception {
        OpStringLoader loader = new OpStringLoader();
        return(loader.parseOperationalString(file));
    }
}
