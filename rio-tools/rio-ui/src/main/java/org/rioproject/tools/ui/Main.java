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

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.NoSuchEntryException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.LookupDiscovery;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.id.Uuid;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryManager;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.opstring.*;
import org.rioproject.resolver.Artifact;
import org.rioproject.resources.client.JiniClient;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.rioproject.tools.discovery.RecordingDiscoveryListener;
import org.rioproject.tools.ui.discovery.GroupSelector;
import org.rioproject.ui.GlassPaneContainer;
import org.rioproject.ui.Util;
import org.rioproject.resources.util.SecurityPolicyLoader;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.system.ComputeResourceAdmin;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.tools.ui.prefs.PreferencesDialog;
import org.rioproject.tools.ui.progresspanel.WaitingDialog;
import org.rioproject.tools.ui.serviceui.ServiceAdminManager;
import org.rioproject.tools.ui.serviceui.UndeployPanel;
import org.rioproject.tools.ui.treetable.CybernodeNode;
import org.rioproject.tools.ui.util.SwingDeployHelper;
import org.rioproject.tools.ui.util.SwingWorker;
import org.rioproject.tools.webster.InternalWebster;
import org.rioproject.url.artifact.ArtifactURLStreamHandlerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Displays deployed system status.
 *
 * @author Dennis Reedy
 */
public class Main extends JFrame {
    final static long startTime = System.currentTimeMillis();
    JSplitPane splitPane;
    File lastDir = new File(System.getProperty("user.dir"));
    Configuration config;
    /** A DiscoveryListener that will record lookup service discovery/discard times */
	RecordingDiscoveryListener recordingListener;
	/** LookupDiscovery for discovering all groups */
	LookupDiscovery lookupDiscovery;
    static JiniClient jiniClient;
    ServiceDiscoveryManager sdm;
    CybernodeUtilizationPanel cup;
    /** A task to control ComputeResourceUtilization refreshes */
    ComputeResourceUtilizationTask cruTask;
    /** Scheduler for Cybernode utilization gathering */
    ScheduledExecutorService scheduler;
    private ExecutorService service;
    LookupCache monitorCache;
    JButton deploy;
    final GraphView graphView;
    JPanel controls;
    final Main frame;
    ColorManager colorManager;
    UtilizationColumnManager utilizationColumnManager;
    ImageIcon westIcon;
    ImageIcon westSelectedIcon;
    ImageIcon northIcon;
    ImageIcon northSelectedIcon;
    int cybernodeRefreshRate;
    UtilitiesPanel utilities;
    String lastArtifact = null;
    BasicEventConsumer clientEventConsumer;
    ProvisionClientEventConsumer provisionClientEventConsumer;

    public Main(Configuration config, final boolean exitOnClose, Properties startupProps) {
        this.config = config;
        String lastArtifactName = startupProps.getProperty(Constants.LAST_ARTIFACT);
        if(lastArtifactName!=null)
            lastArtifact = lastArtifactName;
        String lastDirname = startupProps.getProperty(Constants.LAST_DIRECTORY);
        if(lastDirname!=null)
            lastDir = new File(lastDirname);

        ServiceAdminManager.getInstance().setAdminFrameProperties(startupProps);
        colorManager = new ColorManager(startupProps);
        utilizationColumnManager = new UtilizationColumnManager(startupProps);
        int orientation = Integer.parseInt(startupProps.getProperty(Constants.GRAPH_ORIENTATION,
                                                                    Constants.GRAPH_ORIENTATION_WEST));
        graphView = new GraphView(this, config, colorManager, orientation);

        String defaultRefreshRate = Integer.toString(Constants.DEFAULT_CYBERNODE_REFRESH_RATE);
        String refreshRate = startupProps.getProperty(Constants.CYBERNODE_REFRESH_RATE, defaultRefreshRate);
        cybernodeRefreshRate = Integer.parseInt(refreshRate);

        deploy = new JButton("Deploy ...");
        deploy.setEnabled(false);
        try {
            String title = (String)config.getEntry(Constants.COMPONENT, "title", String.class, null);
            if(title!=null)
                setTitle(title);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        frame = this;
        deploy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                OpStringAndOARFileChooser chooser = new OpStringAndOARFileChooser(frame, lastDir, lastArtifact);
                final String chosen = chooser.getName();
                if(chosen==null)
                    return;
                boolean isArtifact = false;
                try {
                    new Artifact(chosen);
                    isArtifact = true;
                    lastArtifact = chosen;
                } catch(Exception e) {
                    /* don't need to print stack trace here */
                }

                /* If deploying an artifact or the oar is http based, deploy */
                if(isArtifact || chosen.startsWith("http")) {
                    final ServiceItem item = monitorCache.lookup(null);
                    SwingDeployHelper.deploy(chosen, item, frame);
                } else {
                    File opStringFile = new File(chosen);
                    if(opStringFile.exists()) {
                        lastDir = chooser.getCurrentDirectory();
                        final ServiceItem item = monitorCache.lookup(null);
                        final OperationalString[] opstrings;
                        try {
                            if(opStringFile.getName().endsWith("oar")) {
                                OAR oar = new OAR(opStringFile);
                                opstrings = oar.loadOperationalStrings();
                                // TODO: embed webster and stream the OAR
                            } else {
                                opstrings = parseOperationalString(opStringFile);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Util.showError(e, frame, "Failure parsing "+ opStringFile.getName());
                            return;
                        }
                        SwingDeployHelper.deploy(opstrings, item, frame, chosen);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                                      "The OperationalString file "+chosen+" does not exist",
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
                SwingWorker worker = new SwingWorker() {
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

        final JButton colorChooser = new JButton("Color ...");
        colorChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JColorChooser.showDialog(colorChooser, "Colors", colorChooser.getForeground());
            }
        });

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorderPainted(false);

        ImageIcon refreshIcon = Util.getImageIcon("org/rioproject/tools/ui/images/view-refresh.png");
        ImageIcon fitIcon = Util.getImageIcon("org/rioproject/tools/ui/images/view-fullscreen.png");
        westIcon = Util.getScaledImageIcon("org/rioproject/tools/ui/images/west.png", 22, 22);
        westSelectedIcon = Util.getScaledImageIcon("org/rioproject/tools/ui/images/west-selected.png", 22, 22);
        northIcon = Util.getScaledImageIcon("org/rioproject/tools/ui/images/north.png", 22, 22);
        northSelectedIcon = Util.getScaledImageIcon("org/rioproject/tools/ui/images/north-selected.png", 22, 22);
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
                graphView.refresh();
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
        //toolBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 24));
        toolBar.setMinimumSize(new Dimension(8, 24));

        controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBorder(BorderFactory.createEtchedBorder());
        //controls.add(colorChooser);
        controls.add(toolBar);
        controls.add(deploy);
        controls.add(undeploy);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.add(controls, BorderLayout.NORTH);        
        p.add(graphView, BorderLayout.CENTER);
        JTabbedPane topTabs = new JTabbedPane();
        topTabs.add("Deployments", new GlassPaneContainer(p));
        //topTabs.add("Event Feeds", new JPanel());
        //topTabs.add("Infrastructure", new JPanel());

        try {
            String bannerIcon = (String)config.getEntry(Constants.COMPONENT, "bannerIcon", String.class, null);
            if(bannerIcon!=null) {
                ImageIcon icon = Util.getImageIcon(bannerIcon);
                if(icon!=null) {
                    setIconImage(icon.getImage());
                    startupProps.setProperty("bannerIcon", bannerIcon);
                }
            }
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        cup = new CybernodeUtilizationPanel(new GraphViewAdapter(this),
                                            colorManager,
                                            utilizationColumnManager.getSelectedColumns(),
                                            startupProps);
        cup.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));

        utilities = new UtilitiesPanel(cup, colorManager, config, startupProps);

        JPanel utilitiesPanel = makeUtilitiesPanel(utilities);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);

        splitPane.setTopComponent(topTabs);
        splitPane.setBottomComponent(utilitiesPanel);
        splitPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        String s = startupProps.getProperty(Constants.FRAME_DIVIDER);
        int dividerLocation = (s==null?450:Integer.parseInt(s));

        splitPane.setDividerLocation(dividerLocation);
        splitPane.setDividerSize(8);

        setJMenuBar(createMenu());
        
        Container content = getContentPane();
        content.add(splitPane);
        Dimension dim = splitPane.getTopComponent().getSize();
        dim.height = dim.height-controls.getSize().height;
        graphView.setPreferredSize(dim);
        graphView.setSize(dim);
        graphView.showProgressPanel();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (exitOnClose)
                    terminateAndClose();
                else
                    terminate();
            }
        });
    }

    public UtilizationColumnManager getUtilizationColumnManager() {
        return utilizationColumnManager;
    }

    GraphView getGraphView() {
        return graphView;
    }

    Configuration getConfiguration() {
        return config;
    }

    private JPanel makeUtilitiesPanel(JComponent comp) {
        JPanel content = new JPanel(new BorderLayout());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(
            BorderFactory.createEmptyBorder(2, 4, 4, 2));

        content.add(bottom, BorderLayout.SOUTH);
        content.add(comp, BorderLayout.CENTER);
        content.add(new JLabel(""), BorderLayout.EAST);
        return content;
    }

    /**
     * Create a JMenuBar
     *
     * @return a JMenuBar
     */
    JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setLayout(new BoxLayout(menuBar, 0));
        JMenu fileMenu = null;
        if(!MacUIHelper.isMacOS()) {
            fileMenu = new JMenu("File");
            fileMenu.setMnemonic('F');
            JMenuItem preferencesMenuItem =
                fileMenu.add(new JMenuItem("Preferences"));
            preferencesMenuItem.setMnemonic('P');
            preferencesMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PreferencesDialog prefs = new PreferencesDialog(frame, graphView, cup);
                    prefs.setVisible(true);
                }
            });
            JMenuItem exitMenuItem = fileMenu.add(new JMenuItem("Exit"));
            exitMenuItem.setMnemonic('x');
            exitMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    terminateAndClose();
                }
            });
        }
        JMenu discoMenu = new JMenu("Discovery");
        JMenuItem groupSelector = discoMenu.add(new JMenuItem("Group Selector"));
        groupSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JDialog dialog = GroupSelector.getDialog(frame, sdm.getDiscoveryManager(), recordingListener);
                dialog.setVisible(true);
            }
        });
        JMenuItem addLocator = discoMenu.add(new JMenuItem("Add Locator..."));
        addLocator.setMnemonic('L');
        addLocator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addLocator();
            }
        });
        JMenuItem remLocator = discoMenu.add(new JMenuItem("Remove Locator..."));
        remLocator.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                   Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        remLocator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeLocator();
            }
        });
        discoMenu.addSeparator();
        //String[] gps = jiniClient.getRegistrarGroups();
        JMenuItem addGroup = discoMenu.add(new JMenuItem("Add Group..."));
        addGroup.setMnemonic('A');
        //if (gps == null)
        //    addGroup.setEnabled(false);
        addGroup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addGroup();
            }
        });
        JMenuItem remGroup = discoMenu.add(new JMenuItem("Remove Group..."));
        remGroup.setMnemonic('R');
        //if (gps == null)
        //    remGroup.setEnabled(false);
        remGroup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeGroup();
            }
        });

        JMenu helpMenu = null;
        if(!MacUIHelper.isMacOS()) {
            helpMenu = new JMenu("Help");
            helpMenu.setMnemonic('H');
            JMenuItem about = helpMenu.add(new JMenuItem("About..."));
            about.setMnemonic('A');
            about.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    new RioAboutBox(frame);
                }
            });
        } else {
			MacUIHelper.setUIHandler(this, graphView, cup);
		}
        if (fileMenu!=null)
            menuBar.add(fileMenu);
        menuBar.add(discoMenu);
        if (helpMenu!=null) {
            menuBar.add(Box.createGlue());
            menuBar.add(helpMenu);
        }
        return (menuBar);
    }

    /**
     * Add a Jini Locator to locate a Jini Lookup service
     */
    public void addLocator() {
        int port = 0;
        int portIndex;
        String input = JOptionPane
                .showInputDialog("Enter a Locator to Discover host[:port]");
        if (input != null) {
            portIndex = input.indexOf(":");
            if (portIndex == -1)
                port = 4160;
            if (input.startsWith("http://") || input.startsWith("jini://")) {
                String s = input.substring(0, 5);
                JOptionPane.showMessageDialog(this,
                                              "Remove the ["+s+ "] " +
                                              "and resubmit",
                                              "Locator Format Error",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                String host = (portIndex == -1 ? input : input.substring(0,
                        portIndex));
                if (portIndex != -1) {
                    boolean portError = false;
                    String errorReason = null;
                    String p = input.substring(portIndex + 1, input.length());
                    try {
                        port = Integer.parseInt(p);
                    } catch (Throwable t) {
                        portError = true;
                        errorReason = "Not a valid number";
                    }
                    if (port <= 0 || port >= 65536) {
                        portError = true;
                        errorReason = "port number out of range";
                    }
                    if (portError) {
                        JOptionPane.showMessageDialog(this,
                                                      "The provided port is " +
                                                      "invalid : "
                                                      + errorReason,
                                                      "Locator Port Error",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                jiniClient.addLocator(host, port);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                                              "Exception trying to add Locator ["
                                              + e.getClass().getName() + "]",
                                              "Locator Addition Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Remove a Jini Locator that is being used to locate a Jini Lookup service
     */
    public void removeLocator() {
        LookupLocator[] locators = jiniClient.getLocators();
        if (locators == null || locators.length == 0) {
            JOptionPane.showMessageDialog(this,
                                          "There are no locators to remove",
                                          "Zero Locator Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        LookupLocator selected =
            (LookupLocator) JOptionPane.showInputDialog(null,
                                                        "Select a Locator to Remove",
                                                        "Locator Removal Selector",
                                                        JOptionPane.INFORMATION_MESSAGE,
                                                        null,
                                                        locators,
                                                        locators[0]);
        if (selected == null)
            return;
        jiniClient.removeLocators(new LookupLocator[]{selected});
    }

    /**
     * Add a Jini Group to use to discover a Jini Lookup service
     */
    public void addGroup() {
        String group = JOptionPane.showInputDialog("Enter a Group to Discover");
        if (group != null) {
            String[] groups = jiniClient.getRegistrarGroups();
            if (groups != null && groups.length > 0) {
                for (String group1 : groups) {
                    if (group1.equals(group)) {
                        JOptionPane
                            .showMessageDialog(
                                this,
                                "The [" + group + "] group is already " +
                                "part of the discovery listener",
                                "Group Addition Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
            try {
                jiniClient.addRegistrarGroups(new String[]{group});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove a Jini Group that is being used to discover a Jini Lookup service
     */
    public void removeGroup() {
        String[] groups = jiniClient.getRegistrarGroups();
        if (groups == null || groups.length == 0) {
            JOptionPane.showMessageDialog(this,
                                          "There are no groups to remove",
                                          "Zero Group Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        String selected =
            (String)JOptionPane.showInputDialog(null,
                                                "Select a Group to Remove",
                                                "Group Removal Selector",
                                                JOptionPane.INFORMATION_MESSAGE,
                                                null,
                                                groups,
                                                groups[0]);
        if (selected == null)
            return;
        jiniClient.removeRegistrarGroups(new String[]{selected});
    }

    /**
     * Terminate the utility
     */
    public void terminate() {
        if(lookupDiscovery!=null)
            lookupDiscovery.terminate();
        if(sdm != null)
            sdm.terminate();
        if(jiniClient != null)
            jiniClient.terminate();
        if(scheduler!=null)
            scheduler.shutdownNow();
        if(service!=null)
            service.shutdownNow();
        if(utilities !=null)
            utilities.stopNotifications();

        Dimension dim = getSize();
        Point point = getLocation();
        int divider = splitPane.getDividerLocation();
        Properties props = new Properties();
        props.putAll(utilities.getOptions());
        
        props.put(Constants.FRAME_DIVIDER, Integer.toString(divider));
        props.put(Constants.FRAME_HEIGHT, Integer.toString(dim.height));
        props.put(Constants.FRAME_WIDTH, Integer.toString(dim.width));
        props.put(Constants.FRAME_X_POS, Double.toString(point.getX()));
        props.put(Constants.FRAME_Y_POS, Double.toString(point.getY()));

        props.put(Constants.TREE_TABLE_AUTO_EXPAND, Boolean.toString(cup.getExpandAll()));
        props.put(Constants.TREE_TABLE_AUTO_SORT, Boolean.toString(cup.getAutoSort()));
        props.put(Constants.TREE_TABLE_SORTED_COLUMN_NAME, cup.getSortedColumnName());

        if(lastArtifact!=null)
            props.put(Constants.LAST_ARTIFACT, lastArtifact);
        props.put(Constants.LAST_DIRECTORY, lastDir.getAbsolutePath());

        props.put(Constants.ALT_ROW_COLOR,
                  Integer.toString(colorManager.getAltRowColor().getRGB()));
        props.put(Constants.FAILURE_COLOR,
                  Integer.toString(colorManager.getFailureColor().getRGB()));
        props.put(Constants.OKAY_COLOR,
                  Integer.toString(colorManager.getOkayColor().getRGB()));
        props.put(Constants.WARNING_COLOR,
                  Integer.toString(colorManager.getWarningColor().getRGB()));

        dim = ServiceAdminManager.getInstance().getLastAdminFrameSize();
        if(dim!=null) {
            props.put(Constants.ADMIN_FRAME_HEIGHT, Integer.toString(dim.height));
            props.put(Constants.ADMIN_FRAME_WIDTH, Integer.toString(dim.width));
        }
        point = ServiceAdminManager.getInstance().getLastAdminFrameLocation();
        if(point!=null) {
            props.put(Constants.ADMIN_FRAME_X_POS, Double.toString(point.getX()));
            props.put(Constants.ADMIN_FRAME_Y_POS, Double.toString(point.getY()));
        }

        String lastAdminWindowLayout =
            ServiceAdminManager.getInstance().getLastAdminWindowLayout();
        props.put(Constants.ADMIN_FRAME_WINDOW_LAYOUT, lastAdminWindowLayout);

        props.put(Constants.GRAPH_ORIENTATION,
                  Integer.toString(graphView.getOrientation()));

        props.put(Constants.CYBERNODE_REFRESH_RATE,
                  Integer.toString(getCybernodeRefreshRate()));

        String[] cols = utilizationColumnManager.getSelectedColumns();
        for(int i=0; i<cols.length; i++) {
            props.put(cols[i], Integer.toString(i+1));
        }

        try {
            Util.saveProperties(props, Constants.UI_PROPS);
        } catch (IOException e) {
            e.printStackTrace();  
        }
    }

    /**
     * Terminate the utility
     */
    public void terminateAndClose() {
        terminate();
        System.exit(0);
    }

    private OperationalString[] parseOperationalString(File file) throws Exception {
        OpStringLoader loader = new OpStringLoader();
        return(loader.parseOperationalString(file));
    }

    public int getCybernodeRefreshRate() {
        return cybernodeRefreshRate;
    }

    public void setCybernodeRefreshRate(int rate) {
        if(rate!=cybernodeRefreshRate) {
            cybernodeRefreshRate = rate;
            scheduleComputeResourceUtilizationTask();
        }
    }

    void scheduleComputeResourceUtilizationTask() {
        if(scheduler!=null) {
            if(cruTask!=null)
                cruTask.cancel();
            cruTask = new ComputeResourceUtilizationTask();
            scheduler.scheduleAtFixedRate(cruTask,
                                          0,
                                          cybernodeRefreshRate,
                                          TimeUnit.SECONDS);
        } else {
            cruTask = new ComputeResourceUtilizationTask();
            long initialDelay = 2;
            scheduler =
                Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(cruTask,
                                          initialDelay,
                                          cybernodeRefreshRate,
                                          TimeUnit.SECONDS);
        }
    }

    void refreshCybernodeTable() {
        if(cruTask!=null) {
            if(cup.getCount()>0) {
                try {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    cruTask.run();
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        }
    }

    public void startDiscovery() throws Exception {
        lookupDiscovery = new LookupDiscovery(DiscoveryGroupManagement.ALL_GROUPS, config);
        recordingListener = new RecordingDiscoveryListener(lookupDiscovery);
        lookupDiscovery.addDiscoveryListener(recordingListener);

        int threadPoolSize = Config.getIntEntry(config,
                                                Constants.COMPONENT,
                                                Constants.THREAD_POOL_SIZE_KEY,
                                                Constants.DEFAULT_THREAD_POOL_SIZE,
                                                Constants.MIN_THREAD_POOL_SIZE,
                                                Constants.MAX_THREAD_POOL_SIZE);
        service = Executors.newCachedThreadPool();
        for(int i=0; i<threadPoolSize; i++) {
            service.submit(new ServiceItemFetcher());
        }

        scheduleComputeResourceUtilizationTask();
        
        String[] defaultGroups =
            JiniClient.parseGroups(System.getProperty(org.rioproject.config.Constants.GROUPS_PROPERTY_NAME, "all"));

        /* Get group values, default to all groups */
        String[] groups =
            (String[]) config.getEntry(Constants.COMPONENT, "initialLookupGroups", String[].class, defaultGroups);
        LookupLocator[] locators = null;
        try {
            LookupLocator[] defaultLocators =
                JiniClient.parseLocators(System.getProperty(org.rioproject.config.Constants.LOCATOR_PROPERTY_NAME));
            /* Get LookupLocator values */
            locators = (LookupLocator[]) config.getEntry(Constants.COMPONENT,
                                                         "initialLookupLocators",
                                                         LookupLocator[].class,
                                                         defaultLocators);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this,
                                          "<html>" +
                                          "The lookup locators provided in the<br>" +
                                          "configuration file or by using the<br>" +
                                          "<tt>locators</tt> option are formatted<br>" +
                                          "incorrectly.<br><br>" +
                                          "<p>The UI will continue initialization<br>" +
                                          "and ignore this setting. Check the<br>" +
                                          "log file for details.</html>",
                                          "Locators Incorrect",
                                          JOptionPane.WARNING_MESSAGE);
        }
        jiniClient = new JiniClient(new LookupDiscoveryManager(groups, locators, null, config));
        ServiceTemplate monitors = new ServiceTemplate(null, new Class[]{ProvisionMonitor.class}, null);
        ServiceTemplate cybernodes = new ServiceTemplate(null, new Class[]{Cybernode.class}, null);
        sdm = new ServiceDiscoveryManager(jiniClient.getDiscoveryManager(), new LeaseRenewalManager(), config);
        ServiceWatcher watcher = new ServiceWatcher();
        provisionClientEventConsumer = new ProvisionClientEventConsumer();
        clientEventConsumer = new BasicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(),
                                                     provisionClientEventConsumer,
                                                     config);
        monitorCache = sdm.createLookupCache(monitors, null, watcher);
        sdm.createLookupCache(cybernodes, null, watcher);
        utilities.setDiscoveryManagement(jiniClient.getDiscoveryManager());
    }

    void addProvisionMonitor(ServiceItem item) throws RemoteException, OperationalStringException {
        graphView.systemUp();
        deploy.setEnabled(true);
        clientEventConsumer.register(item);
        ProvisionMonitor monitor = (ProvisionMonitor) item.service;
        //failureEventConsumer.register(item);
        DeployAdmin da = (DeployAdmin)monitor.getAdmin();
        OperationalStringManager[] opStringMgrs = da.getOperationalStringManagers();
        if (opStringMgrs == null || opStringMgrs.length == 0) {
            return;
        }
        for (OperationalStringManager opStringMgr : opStringMgrs) {
            OperationalString ops = opStringMgr.getOperationalString();
            if(graphView.getOpStringNode(ops.getName())!=null)
                return;
            graphView.addOpString(monitor, ops);
            ServiceElement[] elems = ops.getServices();
            for(ServiceElement elem : elems) {
                ServiceBeanInstance[] instances = opStringMgr.getServiceBeanInstances(elem);
                for(ServiceBeanInstance instance : instances) {
                    GraphNode node =
                        graphView.serviceUp(elem, instance);
                    if(node!=null)
                        ServiceItemFetchQ.write(node);
                    else {
                        System.err.println("### Cant get GraphNode for ["+elem.getName()+"], " +
                                           "instance ["+instance.getServiceBeanConfig().getInstanceID()+"]");
                    }
                }
            }
            graphView.setOpStringState(ops.getName());
        }
    }

    /**
     * ServiceDiscoveryListener for Cybernodes and ProvisionMonitors
     */
    class ServiceWatcher extends ServiceDiscoveryAdapter {

        public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
            try {
                ServiceItem item = sdEvent.getPostEventServiceItem();
                utilities.addService(item);
                if(item.service instanceof ProvisionMonitor) {
                    graphView.systemUp();
                    deploy.setEnabled(true);
                    ProvisionMonitor monitor = (ProvisionMonitor) item.service;
                    clientEventConsumer.register(item);

                    //failureEventConsumer.register(item);
                    DeployAdmin da = (DeployAdmin)monitor.getAdmin();
                    OperationalStringManager[] opStringMgrs =
                        da.getOperationalStringManagers();
                    if (opStringMgrs == null || opStringMgrs.length == 0) {
                        return;
                    }
                    for (OperationalStringManager opStringMgr : opStringMgrs) {
                        OperationalString ops = opStringMgr.getOperationalString();
                        if(graphView.getOpStringNode(ops.getName())!=null)
                           return;
                        graphView.addOpString(monitor, ops);
                        ServiceElement[] elems = ops.getServices();
                        for(ServiceElement elem : elems) {
                            ServiceBeanInstance[] instances =
                                opStringMgr.getServiceBeanInstances(elem);
                            for(ServiceBeanInstance instance : instances) {
                                GraphNode node =
                                    graphView.serviceUp(elem, instance);
                                if(node!=null)
                                    ServiceItemFetchQ.write(node);
                                else {
                                    System.err.println("### Cant get GraphNode " +
                                                       "for ["+elem.getName()+"], " +
                                                       "instance ["+instance.getServiceBeanConfig().getInstanceID()+"]");
                                }
                            }
                        }
                        graphView.setOpStringState(ops.getName());
                    }
                }
                if(item.service instanceof Cybernode) {
                    Cybernode c = (Cybernode)item.service;
                    CybernodeAdmin cAdmin;
                    try {
                        cAdmin = (CybernodeAdmin)c.getAdmin();
                        cup.addCybernode(
                            new CybernodeNode(
                                item,
                                cAdmin,
                                getComputeResourceUtilization(cAdmin, item)));
                        cruTask.addCybernode(item);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public void serviceRemoved(ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPreEventServiceItem();
            if(item.service instanceof ProvisionMonitor) {
                monitorCache.discard(item.service);
                if(monitorCache.lookup(null, Integer.MAX_VALUE).length==0) {
                    deploy.setEnabled(false);
                    graphView.removeAllOpStrings();
                    graphView.systemDown();
                }
            }

            if(item.service instanceof Cybernode) {
                cup.removeCybernode((Cybernode)item.service);
            }
        }
    }

    ComputeResourceUtilization getComputeResourceUtilization(
        ComputeResourceAdmin crAdmin, ServiceItem item) throws Throwable {
        ComputeResourceUtilization cru=null;
        try {
            cru = crAdmin.getComputeResourceUtilization();
        } catch(Throwable e) {
            if(ThrowableUtil.isRetryable(e)) {
                /* Rio 3.2 does not have the getComputeResourceUtilization method */
                for(Entry entry : item.attributeSets) {
                    if(entry instanceof ComputeResourceUtilization) {
                        cru = (ComputeResourceUtilization)entry;
                        break;
                    }
                }
            }
            else
                throw e;
        }
        return(cru);
    }

    /**
     * Handler for ProvisionMonitorEvent utilities
     */
    class ProvisionClientEventConsumer implements RemoteServiceEventListener {
        final List<ProvisionMonitorEvent> eventQ = new LinkedList<ProvisionMonitorEvent>();

        public void notify(RemoteServiceEvent event) {
            ProvisionMonitorEvent pme = (ProvisionMonitorEvent) event;
            handleEvent(pme);
        }

        void handleEvent(ProvisionMonitorEvent pme) {
            ProvisionMonitorEvent.Action action = pme.getAction();
            ProvisionMonitor monitor = (ProvisionMonitor)pme.getSource();
            switch (action) {
                case SERVICE_ELEMENT_UPDATED:
                case SERVICE_BEAN_INCREMENTED:
                    graphView.serviceIncrement(pme.getServiceElement());
                    processQueued(pme.getOperationalStringName());
                    break;
                case SERVICE_BEAN_DECREMENTED:
                    graphView.serviceDecrement(pme.getServiceElement(),
                                               pme.getServiceBeanInstance());
                    refreshCybernodes(pme);
                    break;
                case SERVICE_ELEMENT_ADDED:
                    if(pme.getServiceElement()==null) {
                        System.err.println("Unable to add ServiceElement, " +
                                           "ServiceElement property in " +
                                           "ProvisionMonitorEvent is null");
                        break;
                    }
                    graphView.addServiceElement(pme.getServiceElement(),
                                                monitor);
                    processQueued(pme.getOperationalStringName());
                    break;
                case SERVICE_ELEMENT_REMOVED:
                    if(pme.getServiceElement()==null) {
                        System.err.println("Unable to remove ServiceElement, " +
                                           "ServiceElement property in " +
                                           "ProvisionMonitorEvent is null");
                        break;
                    }
                    graphView.removeServiceElement(pme.getServiceElement());
                    break;
                case OPSTRING_UPDATED:
                    graphView.updateOpString(monitor, pme.getOperationalString());
                    graphView.setOpStringState(pme.getOperationalString().getName());
                    break;
                case OPSTRING_DEPLOYED:
                    graphView.addOpString(monitor, pme.getOperationalString());
                    processQueued(pme.getOperationalStringName());
                    graphView.setOpStringState(pme.getOperationalStringName());
                    break;
                case OPSTRING_UNDEPLOYED:
                    graphView.removeOpString(pme.getOperationalStringName());
                    break;
                case SERVICE_PROVISIONED:
                case EXTERNAL_SERVICE_DISCOVERED:
                    handleServiceProvisioned(pme);
                    graphView.setOpStringState(pme.getOperationalStringName());
                    refreshCybernodes(pme);
                    break;
                case SERVICE_FAILED:
                case SERVICE_TERMINATED:
                    graphView.serviceDown(pme.getServiceElement(),
                                          pme.getServiceBeanInstance());
                    graphView.setOpStringState(pme.getOperationalStringName());
                    refreshCybernodes(pme);
                    break;
            }
        }

        void handleServiceProvisioned(ProvisionMonitorEvent pme) {
            if(graphView.getOpStringNode(pme.getOperationalStringName())==null) {
                eventQ.add(pme);
            } else {
                GraphNode node = null;
                try {
                    node = graphView.serviceUp(pme.getServiceElement(),
                                               pme.getServiceBeanInstance());
                    if(node==null)
                        eventQ.add(pme);
                } finally {
                    ServiceItemFetchQ.write(node);
                }
            }
        }

        void processQueued(String opStringName) {
            ProvisionMonitorEvent[] events = getEvents(opStringName);
            for(ProvisionMonitorEvent e : events) {
                handleServiceProvisioned(e);
            }
        }

        ProvisionMonitorEvent[] getEvents(String opStringName) {
            ProvisionMonitorEvent[] events;
            List<ProvisionMonitorEvent> list = new LinkedList<ProvisionMonitorEvent>();
            synchronized(eventQ) {
                events = eventQ.toArray(new ProvisionMonitorEvent[eventQ.size()]);
            }
            for(ProvisionMonitorEvent event : events) {
                if(event.getOperationalStringName().equals(opStringName)) {
                    synchronized(eventQ) {
                        eventQ.remove(event);
                    }
                    list.add(event);
                }
            }
            return(list.toArray(new ProvisionMonitorEvent[list.size()]));
        }

        void refreshCybernodes(ProvisionMonitorEvent pme) {
			if (pme == null)
				System.err.println("refreshCybernodes(): Null ProvisionMonitorEvent!!");
			else if (pme.getServiceBeanInstance() == null) {
                System.err.println("refreshCybernodes(): Can't fetch ServiceBeanInstance from ProvisionMonitorEvent");

            } else
            	cup.updateCybernodesAt(pme.getServiceBeanInstance().getHostAddress());
        }
    }

    /**
     * Update the compute resource utilization with a scheduled task
     */
    class ComputeResourceUtilizationTask implements Runnable {
        final Map<ServiceItem, CybernodeAdmin> adminTable = new HashMap<ServiceItem, CybernodeAdmin>();
        final List<ServiceItem> removals = new ArrayList<ServiceItem>();
        private boolean cancelled = false;

        void addCybernode(ServiceItem item) {
            try {
                Cybernode proxy =  (Cybernode)item.service;
                CybernodeAdmin cAdmin = (CybernodeAdmin)proxy.getAdmin();
                synchronized(adminTable) {
                    adminTable.put(item, cAdmin);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        void cancel() {
            cancelled = true;
        }

        public void run() {
            if(cancelled)
                return;
            Set<Map.Entry<ServiceItem,CybernodeAdmin>> tableSet;

            synchronized(adminTable) {
                tableSet = adminTable.entrySet();
            }
            for (Map.Entry<ServiceItem, CybernodeAdmin> mapEntry : tableSet) {
                ServiceItem item = mapEntry.getKey();
                Cybernode cybernode = (Cybernode)item.service;
                CybernodeAdmin cAdmin = mapEntry.getValue();
                try {
                    ComputeResourceUtilization cru =
                        getComputeResourceUtilization(cAdmin, item);
                    cup.update(new CybernodeNode(item, cAdmin, cru));
                } catch (Throwable e) {
                    if(!ThrowableUtil.isRetryable(e) &&
                       !(e instanceof NullPointerException)) {
                        removals.add(item);
                        cup.removeCybernode(cybernode);

                        System.err.println(e.getClass().getName()+":"+
                                           e.getMessage()+", " +
                                           "remove Cybernode from table");
                    }
                }
            }
            for(ServiceItem item : removals) {
                adminTable.remove(item);
            }
            removals.clear();
        }
    }

    /**
     * Fetch ServiceItems
     */
    class ServiceItemFetcher implements Runnable {
        public void run() {
            long id = Thread.currentThread().getId();
            while (true) {
                GraphNode node;
                try {
                    node = ServiceItemFetchQ.take();
                    if(node.getInstance()!=null) {
                        Uuid uuid = node.getInstance().getServiceBeanID();
                        ServiceID serviceID =
                            new ServiceID(uuid.getMostSignificantBits(),
                                          uuid.getLeastSignificantBits());
                        ServiceTemplate template = new ServiceTemplate(serviceID,
                                                                       null,
                                                                       null);
                        ServiceItem item = sdm.lookup(template, null, 1000);
                        if(item!=null) {
                            graphView.setGraphNodeServiceItem(node, item);
                        } else {
                            ServiceItemFetchQ.write(node);
                        }
                    } else {
                        ServiceItemFetchQ.write(node);
                    }
                } catch (InterruptedException e) {
                    System.err.println("ServiceVerificator ["+id+"] " +
                                      "InterruptedException, exiting");
                    break;
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    public static void redirect() {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null) {
            String location = Main.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
            /*
             * Strip the file: off
             */
            location = location.substring(5);
            /*
             * Strip the /lib/rio-ui.jar off
             */
            int ndx = location.lastIndexOf('/');
            location = location.substring(0, ndx);
            ndx = location.lastIndexOf('/');
            rioHome = location.substring(0, ndx);
            //rioHome = System.getProperty("user.home")+File.separator+".rio";
        }

        String logDirPath = System.getProperty(Constants.COMPONENT+".logDir",
                                               rioHome+File.separator+
                                               "logs");
        File logDir = new File(logDirPath);
        if(!logDir.exists())
            logDir.mkdirs();
        File rioUILog = new File(logDir, "rio-ui.log");
        if(rioUILog.exists()) {
            rioUILog.delete();
        }  
        System.out.println("Creating log file in "+rioUILog.getAbsolutePath());
        try {
            System.setErr(new PrintStream(new FileOutputStream(rioUILog)));
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        System.err.println("===============================================");
        System.err.println("Rio Monitor UI\n"+
                           "Log creation : "+new Date(startTime).toString()+"\n"+
                           "Operator : "+System.getProperty("user.name"));
        System.err.println("===============================================");
    }

    private static String generateGroovyOverride(long multicastAnnouncementInterval) throws IOException {
        String[] lines = new String[] {
            "import org.rioproject.config.Component\n",
            "@Component('net.jini.discovery.LookupDiscovery')\n",
            "class DiscoveryConfig {\n",
            "    long multicastAnnouncementInterval = "+multicastAnnouncementInterval+"\n",
            "}"
        };
        File tmp = File.createTempFile("tmp", ".groovy");
        tmp.deleteOnExit();
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(tmp));
            for (String line : lines) {
                out.write(line);
            }
        } finally {
            if (out != null)
                out.close();
        }
        return tmp.getCanonicalPath();
    }

    private static String generateJiniOverride(long multicastAnnouncementInterval) {
        String lookupComponent = "net.jini.discovery.LookupDiscovery";
        return lookupComponent+".multicastAnnouncementInterval="+multicastAnnouncementInterval;
    }

    public static void main(String[] args) {
        try {
            Main frame;
            final LinkedList<String> commandArgs = new LinkedList<String>();
            commandArgs.addAll(Arrays.asList(args));
            for(String arg : args) {
                if (arg.startsWith("groups")) {
                    String[] values = arg.split("=");
                    String groupsArg = values[1].trim();
                    String s = System.getProperty(org.rioproject.config.Constants.GROUPS_PROPERTY_NAME);
                    groupsArg = s!=null? s+","+groupsArg : groupsArg;
                    System.setProperty(org.rioproject.config.Constants.GROUPS_PROPERTY_NAME, groupsArg);
                    commandArgs.remove(arg);
                } else if (arg.startsWith("locators")) {
                    String[] values = arg.split("=");
                    String locatorsArg = values[1].trim();
                    String[] locators = locatorsArg.split(",");
                    StringBuilder sb = new StringBuilder();
                    int i=0;
                    for(String locator : locators) {
                        if(!locator.startsWith("jini://"))
                            locator = "jini://"+locator;
                        if(i>0)
                            sb.append(",");
                        sb.append(locator);
                        i++;
                    }
                    locatorsArg = sb.toString();
                    String s = System.getProperty(org.rioproject.config.Constants.LOCATOR_PROPERTY_NAME);
                    locatorsArg = s!=null? s+","+locatorsArg : locatorsArg;
                    System.setProperty(org.rioproject.config.Constants.LOCATOR_PROPERTY_NAME, locatorsArg);
                    commandArgs.remove(arg);
                }
            }

            /* Set the interval to wait for multicast announcements to
               5 seconds */
            long multicastAnnouncementInterval = 5000;
            String override;

            /* Add the interval to wait for multicast announcements as
               an override */
            if(commandArgs.size()==0) {
                override = generateGroovyOverride(multicastAnnouncementInterval);
            } else {
                if(commandArgs.get(0).endsWith(".groovy")) {
                    override = generateGroovyOverride(multicastAnnouncementInterval);
                } else {
                    override = generateJiniOverride(multicastAnnouncementInterval);
                }
            }
            commandArgs.add(override);
            args = commandArgs.toArray(new String[commandArgs.size()]);
                        
            final Configuration config = ConfigurationProvider.getInstance(args);
            final Properties props  = new Properties();
            try {
                props.putAll(Util.loadProperties(Constants.UI_PROPS));
            } catch(IOException e) {
                //
            }
            PrivilegedExceptionAction<Main> createViewer =
                new PrivilegedExceptionAction<Main>() {
                public Main run() throws Exception {
                    String securityPolicy = System.getProperty("java.security.policy");
                    if(securityPolicy == null) 
                        SecurityPolicyLoader.load(Main.class, "rio-ui.policy");
                    else
                        System.setSecurityManager(new RMISecurityManager());

                    /* Set properties for execution on a Mac client */
                    if(MacUIHelper.isMacOS()) {
                        MacUIHelper.setSystemProperties();                        
                    }

                    /* If the artifact URL has not been configured, set it up */
                    try {
                        new URL("artifact:foo");
                    } catch (MalformedURLException e) {
                        URL.setURLStreamHandlerFactory(new ArtifactURLStreamHandlerFactory());
                    }

                    InternalWebster.startWebster("sdm-dl.jar");
                    Main.redirect();
                    return new Main(config, true, props);
                }
            };

            try {
                LoginContext loginContext = (LoginContext) Config.getNonNullEntry(config,
                                                                                  Constants.COMPONENT,
                                                                                  "loginContext",
                                                                                  LoginContext.class);
                loginContext.login();
                frame = Subject.doAsPrivileged(loginContext.getSubject(), createViewer, null);
            } catch(NoSuchEntryException e) {
                frame = createViewer.run();
            }

            frame.startDiscovery();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();

            String s = props.getProperty(Constants.FRAME_WIDTH);
            int width = (s==null?700:Integer.parseInt(s));
            s = props.getProperty(Constants.FRAME_HEIGHT);
            int height = (s==null?690:Integer.parseInt(s));
            frame.setSize(new Dimension(width, height));

            s = props.getProperty(Constants.FRAME_X_POS);
            if(s!=null) {
                double xPos = Double.parseDouble(s);
                double yPos =
                    Double.parseDouble(props.getProperty(Constants.FRAME_Y_POS));
                frame.setLocation((int)xPos, (int)yPos);
            }
            frame.setVisible(true);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

