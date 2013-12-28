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
import net.jini.lookup.entry.Host;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.entry.OperationalStringEntry;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.impl.client.JiniClient;
import org.rioproject.impl.client.ServiceDiscoveryAdapter;
import org.rioproject.impl.discovery.RecordingDiscoveryListener;
import org.rioproject.impl.event.BasicEventConsumer;
import org.rioproject.impl.util.ThrowableUtil;
import org.rioproject.install.Installer;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.system.ComputeResourceAdmin;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.tools.ui.browser.Browser;
import org.rioproject.tools.ui.cybernodeutilization.CybernodeUtilizationPanel;
import org.rioproject.tools.ui.discovery.GroupSelector;
import org.rioproject.tools.ui.multicast.MulticastMonitor;
import org.rioproject.tools.ui.prefs.PreferencesDialog;
import org.rioproject.tools.ui.servicenotification.RemoteEventTable;
import org.rioproject.tools.ui.serviceui.ServiceAdminManager;
import org.rioproject.tools.ui.util.TabLabel;
import org.rioproject.ui.GlassPaneContainer;
import org.rioproject.ui.Util;
import org.rioproject.url.artifact.ArtifactURLStreamHandlerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
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
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class Main extends JFrame {
    private final static long startTime = System.currentTimeMillis();
    private File lastDir = new File(System.getProperty("user.dir"));
    private final Configuration config;
    /** A DiscoveryListener that will record lookup service discovery/discard times */
    private RecordingDiscoveryListener recordingListener;
	/** LookupDiscovery for discovering all groups */
    private LookupDiscovery lookupDiscovery;
    private static JiniClient jiniClient;
    private ServiceDiscoveryManager sdm;
    private final CybernodeUtilizationPanel cup;
    /** A task to control ComputeResourceUtilization refreshes */
    private ComputeResourceUtilizationTask cruTask;
    /** Scheduler for Cybernode utilization gathering */
    private ScheduledExecutorService scheduler;
    private ExecutorService service;
    private LookupCache monitorCache;
    /*private JButton deploy;*/
    //private final GraphView graphView;
    private final Main frame;
    private final ColorManager colorManager;
    private final UtilizationColumnManager utilizationColumnManager;
    private int cybernodeRefreshRate;
    private String lastArtifact = null;
    private BasicEventConsumer clientEventConsumer;
    final String AS_SERVICE_UI = "as.service.ui";
    private final RemoteEventTable remoteEventTable;
    private final Map<String, ImageIcon> toolBarImageMap = new HashMap<String, ImageIcon>();
    private final JTabbedPane monitorTabs = new JTabbedPane();
    private final JTabbedPane mainTabs = new JTabbedPane();;
    private final Map<ProvisionMonitor, ProvisionMonitorPanel> monitorPanelMap =
            new HashMap<ProvisionMonitor, ProvisionMonitorPanel>();
    private int orientation;
    private final ProgressPanel progressPanel;
    private final JComponent glassPaneComponent;

    public Main(final Configuration config, final boolean exitOnClose, final Properties startupProps) throws ExportException, ConfigurationException {
        this.config = config;

        progressPanel = new ProgressPanel(config);
        glassPaneComponent = new GlassPaneContainer(progressPanel);
        String lastArtifactName = startupProps.getProperty(Constants.LAST_ARTIFACT);
        if(lastArtifactName!=null)
            lastArtifact = lastArtifactName;
        String lastDirName = startupProps.getProperty(Constants.LAST_DIRECTORY);
        if(lastDirName!=null)
            lastDir = new File(lastDirName);

        ServiceAdminManager.getInstance().setAdminFrameProperties(startupProps);
        colorManager = new ColorManager(startupProps);
        utilizationColumnManager = new UtilizationColumnManager(startupProps);
        orientation = Integer.parseInt(startupProps.getProperty(Constants.GRAPH_ORIENTATION,
                                                                Constants.GRAPH_ORIENTATION_WEST));

        String defaultRefreshRate = Integer.toString(Constants.DEFAULT_CYBERNODE_REFRESH_RATE);
        String refreshRate = startupProps.getProperty(Constants.CYBERNODE_REFRESH_RATE, defaultRefreshRate);
        cybernodeRefreshRate = Integer.parseInt(refreshRate);

        try {
            String title = (String)config.getEntry(Constants.COMPONENT, "title", String.class, null);
            if(title!=null)
                setTitle(title);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        frame = this;

        toolBarImageMap.put("refreshIcon", Util.getImageIcon("org/rioproject/tools/ui/images/view-refresh.png"));
        toolBarImageMap.put("fitIcon", Util.getImageIcon("org/rioproject/tools/ui/images/view-fullscreen.png"));
        toolBarImageMap.put("westIcon", Util.getScaledImageIcon("org/rioproject/tools/ui/images/west.png", 22, 22));
        toolBarImageMap.put("westSelectedIcon", Util.getScaledImageIcon("org/rioproject/tools/ui/images/west-selected.png", 22, 22));
        toolBarImageMap.put("northIcon", Util.getScaledImageIcon("org/rioproject/tools/ui/images/north.png", 22, 22));
        toolBarImageMap.put("northSelectedIcon", Util.getScaledImageIcon("org/rioproject/tools/ui/images/north-selected.png", 22, 22));

        final JButton colorChooser = new JButton("Color ...");
        colorChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JColorChooser.showDialog(colorChooser, "Colors", colorChooser.getForeground());
            }
        });

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
                                            utilizationColumnManager.getSelectedColumns(),
                                            startupProps);
        cup.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));

        UtilitiesPanel utilities = new UtilitiesPanel(cup);

        final JPanel utilitiesPanel = makeUtilitiesPanel(utilities);

        setJMenuBar(createMenu());
        Container content = getContentPane();

        mainTabs.add("Deployments", glassPaneComponent);
        mainTabs.add("Utilization", utilitiesPanel);
        remoteEventTable = new RemoteEventTable(config, startupProps);
        JLabel label = TabLabel.create("Service Event Notifications", remoteEventTable);
        mainTabs.addTab(null, remoteEventTable);
        mainTabs.setTabComponentAt(2, label);

        content.add(mainTabs);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (exitOnClose)
                    terminateAndClose();
                else
                    terminate();
            }
        });
        progressPanel.showProgressPanel();
    }

    void setStartupLocations(final Properties startupProps) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                remoteEventTable.init(startupProps);
            }
        });
    }

    public UtilizationColumnManager getUtilizationColumnManager() {
        return utilizationColumnManager;
    }

    GraphView getGraphView() {
        if(monitorTabs.getTabCount()>0)
            return ((ProvisionMonitorPanel) monitorTabs.getSelectedComponent()).getGraphView();
        return null;
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
        menuBar.setLayout(new BoxLayout(menuBar, BoxLayout.X_AXIS));
        JMenu fileMenu = null;
        if(!MacUIHelper.isMacOS()) {
            fileMenu = new JMenu("File");
            JMenuItem preferencesMenuItem = fileMenu.add(new JMenuItem("Preferences"));
            preferencesMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PreferencesDialog prefs = new PreferencesDialog(frame, getGraphView(), cup);
                    prefs.setVisible(true);
                }
            });
            JMenuItem exitMenuItem = fileMenu.add(new JMenuItem("Exit"));
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
        addLocator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addLocator();
            }
        });
        JMenuItem remLocator = discoMenu.add(new JMenuItem("Remove Locator..."));
        remLocator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeLocator();
            }
        });
        discoMenu.addSeparator();
        //String[] gps = jiniClient.getRegistrarGroups();
        JMenuItem addGroup = discoMenu.add(new JMenuItem("Add Group..."));
        //if (gps == null)
        //    addGroup.setEnabled(false);
        addGroup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addGroup();
            }
        });
        JMenuItem remGroup = discoMenu.add(new JMenuItem("Remove Group..."));
        //if (gps == null)
        //    remGroup.setEnabled(false);
        remGroup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeGroup();
            }
        });

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem multiCastMonitor = toolsMenu.add(new JMenuItem("Multicast Monitor..."));
        multiCastMonitor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JDialog dialog = MulticastMonitor.getDialog(frame);
                dialog.setVisible(true);
            }
        });
        JMenuItem browserMenu = toolsMenu.add(new JMenuItem("Service Browser..."));
        final JFrame parent = this;
        browserMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    Browser browser = new Browser(config, parent);
                    browser.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        } /*else {
			MacUIHelper.setUIHandler(this, getGraphView(), cup);
		}*/
        if (fileMenu!=null)
            menuBar.add(fileMenu);
        menuBar.add(discoMenu);
        menuBar.add(toolsMenu);
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
                        JOptionPane.showMessageDialog(this,
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

        remoteEventTable.terminate();

        Dimension dim = getSize();
        Point point = getLocation();
        Properties props = new Properties();

        props.put(Constants.USE_EVENT_COLLECTOR, Boolean.toString(remoteEventTable.getUseEventCollector()));
        props.put(Constants.EVENTS_DIVIDER, Integer.toString(remoteEventTable.getDividerLocation()));

        props.put(Constants.FRAME_HEIGHT, Integer.toString(dim.height));
        props.put(Constants.FRAME_WIDTH, Integer.toString(dim.width));
        props.put(Constants.FRAME_X_POS, Double.toString(point.getX()));
        props.put(Constants.FRAME_Y_POS, Double.toString(point.getY()));

        props.put(Constants.TREE_TABLE_AUTO_EXPAND, Boolean.toString(cup.getExpandAll()));

        if(lastArtifact!=null)
            props.put(Constants.LAST_ARTIFACT, lastArtifact);
        props.put(Constants.LAST_DIRECTORY, lastDir.getAbsolutePath());

        props.put(Constants.FAILURE_COLOR, Integer.toString(colorManager.getFailureColor().getRGB()));
        props.put(Constants.OKAY_COLOR, Integer.toString(colorManager.getOkayColor().getRGB()));
        props.put(Constants.WARNING_COLOR, Integer.toString(colorManager.getWarningColor().getRGB()));

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

        if(getGraphView()!=null)
            props.put(Constants.GRAPH_ORIENTATION, Integer.toString(getGraphView().getOrientation()));

        props.put(Constants.CYBERNODE_REFRESH_RATE, Integer.toString(getCybernodeRefreshRate()));

        String[] cols = utilizationColumnManager.getSelectedColumns();
        for(int i=0; i<cols.length; i++) {
            props.put(cols[i], Integer.toString(i+1));
        }

        try {
            saveProperties(props, Constants.UI_PROPS);
        } catch (IOException e) {
            e.printStackTrace();  
        }
    }

    /**
     * Load properties from ${user.home}/.rio
     *
     * @param filename The name of the properties file to load
     *
     * @return A Properties object loaded from the system
     * @throws IOException If there are exceptions accessing the file system
     */
    private static Properties loadProperties(String filename) throws IOException {
        File rioHomeDir = new File(System.getProperty("user.home") +File.separator +".rio");
        Properties props = new Properties();
        if (!rioHomeDir.exists())
            return (props);
        File propFile = new File(rioHomeDir, filename);
        props.load(new FileInputStream(propFile));
        return (props);
    }


    /**
     * Save properties to a file stored in ${user.home}/.rio
     *
     * @param props The Properties object to save, must not be null
     * @param filename The file name to save the Properties to
     *
     * @throws IOException If there are exceptions accessing the file system
     */
    private void saveProperties(final Properties props, final String filename) throws IOException {
        if (props == null)
            throw new IllegalArgumentException("props is null");

        File rioHomeDir = new File(System.getProperty("user.home") +File.separator +".rio");
        if (!rioHomeDir.exists())
            rioHomeDir.mkdir();
        File propFile = new File(rioHomeDir, filename);
        props.store(new FileOutputStream(propFile), null);
    }

    /**
     * Terminate the utility
     */
    public void terminateAndClose() {
        terminate();
        if(System.getProperty(AS_SERVICE_UI)==null)
            System.exit(0);
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
        ServiceTemplate eventCollectors = new ServiceTemplate(null,
                                                              new Class[]{EventCollector.class},
                                                              new Entry[]{new OperationalStringEntry(org.rioproject.config.Constants.CORE_OPSTRING)});

        sdm = new ServiceDiscoveryManager(jiniClient.getDiscoveryManager(), new LeaseRenewalManager(), config);

        ServiceWatcher watcher = new ServiceWatcher();

        ProvisionClientEventConsumer provisionClientEventConsumer = new ProvisionClientEventConsumer();

        clientEventConsumer = new BasicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(),
                                                     provisionClientEventConsumer,
                                                     config);
        monitorCache = sdm.createLookupCache(monitors, null, watcher);
        sdm.createLookupCache(cybernodes, null, watcher);
        sdm.createLookupCache(eventCollectors, null, watcher);
        remoteEventTable.setDiscoveryManagement(jiniClient.getDiscoveryManager());
    }

    void addProvisionMonitor(ServiceItem item) throws RemoteException, OperationalStringException {
        progressPanel.systemUp();
        ProvisionMonitor monitor = (ProvisionMonitor) item.service;
        ProvisionMonitorPanel pmp = addProvisionMonitorPanel(item);
        clientEventConsumer.register(item);

        //failureEventConsumer.register(item);
        DeployAdmin da = (DeployAdmin)monitor.getAdmin();
        OperationalStringManager[] opStringMgrs = da.getOperationalStringManagers();
        if (opStringMgrs == null || opStringMgrs.length == 0) {
            return;
        }
        for (OperationalStringManager opStringMgr : opStringMgrs) {
            OperationalString ops = opStringMgr.getOperationalString();
            if(getGraphView().getOpStringNode(ops.getName())!=null)
                return;
            getGraphView().addOpString(monitor, ops);
            ServiceElement[] elems = ops.getServices();
            for(ServiceElement elem : elems) {
                ServiceBeanInstance[] instances = opStringMgr.getServiceBeanInstances(elem);
                for(ServiceBeanInstance instance : instances) {
                    GraphNode node =
                            getGraphView().serviceUp(elem, instance);
                    if(node!=null)
                        ServiceItemFetchQ.write(node, pmp.getGraphView());
                    else {
                        System.err.println("### Cant get GraphNode for ["+elem.getName()+"], " +
                                           "instance ["+instance.getServiceBeanConfig().getInstanceID()+"]");
                    }
                }
            }
            getGraphView().setOpStringState(ops.getName());
        }
    }

    private ProvisionMonitorPanel addProvisionMonitorPanel(ServiceItem item) {
        ProvisionMonitor monitor = (ProvisionMonitor) item.service;
        ProvisionMonitorPanel pmp = new ProvisionMonitorPanel(monitor,
                                                              monitorCache,
                                                              frame,
                                                              colorManager,
                                                              toolBarImageMap,
                                                              config,
                                                              orientation);
        String host = "<unknown>";
        for(Entry entry : item.attributeSets) {
            if(entry instanceof Host) {
                host = ((Host)entry).hostName;
                break;
            }
        }
        monitorPanelMap.put(monitor, pmp);
        monitorTabs.addTab(host, pmp);
        if(mainTabs.getComponentAt(0).equals(glassPaneComponent)) {
            progressPanel.systemUp();
            mainTabs.setComponentAt(0, monitorTabs);
            monitorTabs.setSelectedIndex(0);
        }
        return pmp;
    }

    /**
     * ServiceDiscoveryListener for Cybernodes and ProvisionMonitors
     */
    class ServiceWatcher extends ServiceDiscoveryAdapter {

        public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
            try {
                ServiceItem item = sdEvent.getPostEventServiceItem();
                if(item.service instanceof EventCollector) {
                    try {
                        remoteEventTable.addEventCollector((EventCollector)item.service);
                    } catch(Exception e) {
                        Util.showError(e, frame, "Cannot add Event Collector");
                    }
                }
                if(item.service instanceof ProvisionMonitor) {
                    ProvisionMonitor monitor = (ProvisionMonitor) item.service;
                    ProvisionMonitorPanel pmp = addProvisionMonitorPanel(item);

                    clientEventConsumer.register(item);

                    //failureEventConsumer.register(item);
                    DeployAdmin da = (DeployAdmin)monitor.getAdmin();
                    OperationalStringManager[] opStringMgrs = da.getOperationalStringManagers();
                    if (opStringMgrs.length == 0) {
                        return;
                    }
                    for (OperationalStringManager opStringMgr : opStringMgrs) {
                        OperationalString ops = opStringMgr.getOperationalString();
                        if(pmp.getGraphView().getOpStringNode(ops.getName())!=null)
                           return;
                        pmp.getGraphView().addOpString(monitor, ops);
                        ServiceElement[] elems = ops.getServices();
                        for(ServiceElement elem : elems) {
                            ServiceBeanInstance[] instances = opStringMgr.getServiceBeanInstances(elem);
                            for(ServiceBeanInstance instance : instances) {
                                GraphNode node = pmp.getGraphView().serviceUp(elem, instance);
                                if(node!=null)
                                    ServiceItemFetchQ.write(node, pmp.getGraphView());
                                else {
                                    System.err.println("### Cant get GraphNode " +
                                                       "for ["+elem.getName()+"], " +
                                                       "instance ["+instance.getServiceBeanConfig().getInstanceID()+"]");
                                }
                            }
                        }
                        pmp.getGraphView().setOpStringState(ops.getName());
                    }
                }
                if(item.service instanceof Cybernode) {
                    Cybernode c = (Cybernode)item.service;
                    CybernodeAdmin cAdmin;
                    try {
                        cAdmin = (CybernodeAdmin)c.getAdmin();
                        cup.addCybernode(item,
                                         cAdmin,
                                         getComputeResourceUtilization(cAdmin, item),
                                         utilizationColumnManager);
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
                ProvisionMonitorPanel pmp = monitorPanelMap.get(item.service);
                monitorTabs.removeTabAt(monitorTabs.indexOfComponent(pmp));
                if(monitorCache.lookup(null, Integer.MAX_VALUE).length==0) {
                    progressPanel.systemDown();
                    mainTabs.setComponentAt(0, glassPaneComponent);
                    mainTabs.repaint();
                }
            }

            if(item.service instanceof Cybernode) {
                cup.removeCybernode((Cybernode)item.service);
            }
            if(item.service instanceof EventCollector) {
                remoteEventTable.removeEventCollector((EventCollector)item.service);
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
    class ProvisionClientEventConsumer implements RemoteServiceEventListener<ProvisionMonitorEvent> {
        final List<ProvisionMonitorEvent> eventQ = new LinkedList<ProvisionMonitorEvent>();

        public void notify(ProvisionMonitorEvent pme) {
            ProvisionMonitorEvent.Action action = pme.getAction();
            ProvisionMonitor monitor = (ProvisionMonitor)pme.getSource();
            GraphView graphView = monitorPanelMap.get(monitor).getGraphView();
            switch (action) {
                case SERVICE_ELEMENT_UPDATED:
                case SERVICE_BEAN_INCREMENTED:
                    graphView.serviceIncrement(pme.getServiceElement());
                    processQueued(pme.getOperationalStringName(), graphView);
                    break;
                case SERVICE_BEAN_DECREMENTED:
                    graphView.serviceDecrement(pme.getServiceElement(), pme.getServiceBeanInstance());
                    refreshCybernodes(pme);
                    break;
                case SERVICE_ELEMENT_ADDED:
                    if(pme.getServiceElement()==null) {
                        System.err.println("Unable to add ServiceElement, " +
                                           "ServiceElement property in ProvisionMonitorEvent is null");
                        break;
                    }
                    graphView.addServiceElement(pme.getServiceElement(),
                                                monitor);
                    processQueued(pme.getOperationalStringName(), graphView);
                    break;
                case SERVICE_ELEMENT_REMOVED:
                    if(pme.getServiceElement()==null) {
                        System.err.println("Unable to remove ServiceElement, " +
                                           "ServiceElement property in ProvisionMonitorEvent is null");
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
                    processQueued(pme.getOperationalStringName(), graphView);
                    graphView.setOpStringState(pme.getOperationalStringName());
                    break;
                case OPSTRING_UNDEPLOYED:
                    graphView.removeOpString(pme.getOperationalStringName());
                    break;
                case SERVICE_PROVISIONED:
                case EXTERNAL_SERVICE_DISCOVERED:
                    handleServiceProvisioned(pme, graphView);
                    graphView.setOpStringState(pme.getOperationalStringName());
                    refreshCybernodes(pme);
                    break;
                case SERVICE_FAILED:
                case SERVICE_TERMINATED:
                    graphView.serviceDown(pme.getServiceElement(), pme.getServiceBeanInstance());
                    graphView.setOpStringState(pme.getOperationalStringName());
                    refreshCybernodes(pme);
                    break;
            }
        }

        void handleServiceProvisioned(ProvisionMonitorEvent pme, GraphView graphView) {
            if(graphView.getOpStringNode(pme.getOperationalStringName())==null) {
                eventQ.add(pme);
            } else {
                GraphNode node = null;
                try {
                    node = graphView.serviceUp(pme.getServiceElement(), pme.getServiceBeanInstance());
                    if(node==null)
                        eventQ.add(pme);
                } finally {
                    ServiceItemFetchQ.write(node, graphView);
                }
            }
        }

        void processQueued(String opStringName, GraphView graphView) {
            ProvisionMonitorEvent[] events = getEvents(opStringName);
            for(ProvisionMonitorEvent e : events) {
                handleServiceProvisioned(e, graphView);
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
			if (pme == null) {
				System.err.println("refreshCybernodes(): Null ProvisionMonitorEvent!!");
            } else if (pme.getServiceBeanInstance() == null) {
                System.err.println("refreshCybernodes(): Can't fetch ServiceBeanInstance from ProvisionMonitorEvent");

            } else {
            	cup.updateCybernodesAt(pme.getServiceBeanInstance().getHostAddress());
            }
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
                    ComputeResourceUtilization cru = getComputeResourceUtilization(cAdmin, item);
                    cup.update(item, cAdmin, cru, utilizationColumnManager);
                } catch (Throwable e) {
                    if(!ThrowableUtil.isRetryable(e) &&
                       !(e instanceof NullPointerException)) {
                        removals.add(item);
                        cup.removeCybernode(cybernode);
                        System.err.println(e.getClass().getName()+": "+e.getMessage()+", remove Cybernode from table");
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
                ServiceItemFetchQ.Request request;
                try {
                    request = ServiceItemFetchQ.take();
                    GraphNode node = request.node;
                    if(node.getInstance()!=null) {
                        Uuid uuid = node.getInstance().getServiceBeanID();
                        ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(),
                                                            uuid.getLeastSignificantBits());
                        ServiceTemplate template = new ServiceTemplate(serviceID, null, null);
                        ServiceItem item = sdm.lookup(template, null, 1000);
                        if(item!=null) {
                            request.graphView.setGraphNodeServiceItem(node, item);
                        } else {
                            ServiceItemFetchQ.write(request);
                        }
                    } else {
                        ServiceItemFetchQ.write(request);
                    }
                } catch (InterruptedException e) {
                    System.err.println("ServiceItemFetcher ["+id+"] InterruptedException, exiting");
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
                                               rioHome+File.separator+"logs");
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

    @SuppressWarnings("unchecked")
    public static void main(final String[] args) {
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
            if(commandArgs.isEmpty()) {
                override = generateGroovyOverride(multicastAnnouncementInterval);
            } else {
                if(commandArgs.get(0).endsWith(".groovy")) {
                    override = generateGroovyOverride(multicastAnnouncementInterval);
                } else {
                    override = generateJiniOverride(multicastAnnouncementInterval);
                }
            }
            commandArgs.add(override);
            String[] newArgs = commandArgs.toArray(new String[commandArgs.size()]);
                        
            final Configuration config = ConfigurationProvider.getInstance(newArgs);
            final Properties props  = new Properties();
            try {
                props.putAll(loadProperties(Constants.UI_PROPS));
            } catch(IOException e) {
                //
            }
            PrivilegedExceptionAction<Main> createViewer =
                new PrivilegedExceptionAction<Main>() {
                public Main run() throws Exception {
                    System.setSecurityManager(new SecurityManager());
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

                    Main.redirect();
                    Installer.install();
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
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();

            String s = props.getProperty(Constants.FRAME_WIDTH);
            int width = (s==null?700:Integer.parseInt(s));
            s = props.getProperty(Constants.FRAME_HEIGHT);
            int height = (s==null?690:Integer.parseInt(s));
            frame.setSize(new Dimension(width, height));

            s = props.getProperty(Constants.FRAME_X_POS);
            if(s!=null) {
                double xPos = Double.parseDouble(s);
                double yPos = Double.parseDouble(props.getProperty(Constants.FRAME_Y_POS));
                frame.setLocation((int)xPos, (int)yPos);
            }
            frame.setVisible(true);
            frame.setStartupLocations(props);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

