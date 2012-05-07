/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.watch;

import net.jini.core.lookup.ServiceItem;
import org.rioproject.ui.GlassPaneContainer;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Accumulator Viewer
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class AccumulatorViewer extends JPanel implements TreeSelectionListener {
    static final long serialVersionUID = 1L;
    private Watchable service;
    private WatchDataSource[] watchDataSources;
    private CalculableViewable defaultDataView = new DefaultCalculableView();
    private CalculableViewable dataView = defaultDataView;
    private JTree tree;
    private DefaultMutableTreeNode  root= new DefaultMutableTreeNode("Watches");
    private JComponent graphPanel;
    boolean paintFlag = false;
    private ClassLoader loader;
    private javax.swing.Timer autoRefresh;
    private GlassPaneContainer glassPane;
    private CalculableViewable.PlottedCalculable mouseOverValue = null;
    //private final static Color fillBoxColor = new Color(110, 206, 227);
    private final static Color fillBoxColor = new Color(255, 255, 204);
    private NumberFormat numberFormatter = NumberFormat.getInstance();
    private DateFormat dateFormat;
    private Point lastMousePoint;
    private boolean autoRefreshMode = true;
    
    /** 
     * Creates new form AccumulatorViewer 
     * 
     * @param item the service proxy
     *
     * @throws Exception If communication errors happen interfacing with
     * the WatchDataSource
     */
    public AccumulatorViewer(Object item) throws Exception {
        super();
        loader = getClass().getClassLoader();
        getAccessibleContext().setAccessibleName("Watchable Viewer");
        if(((ServiceItem)item).service instanceof Watchable) {
            setService((Watchable)((ServiceItem)item).service);
        }
        initComponents();
    }

    /**
     * Accessor for property service
     * 
     * @return Value of property service.
     */
    public Remote getService() {
        return(service);
    }

    /**
     * Mutator for property service.
     * 
     * @param service New value of property services.
     *
     * @throws RemoteException If communication errors happen interfacing with
     * the WatchDataSource
     */
    public void setService(Watchable service) throws RemoteException {
        this.service = service;
        if(this.service != null) {
            watchDataSources = this.service.fetch();
            if(watchDataSources == null)
                return;
            if(watchDataSources.length > 0) {
                int wdsNbr = watchDataSources.length;
                String[] wdsIDs = new String[wdsNbr];
                for(int i = 0; i < watchDataSources.length; i++) {
                    wdsIDs[i] = watchDataSources[i].getID();
                }
                setTreeWDSNodes(wdsIDs);
            }
        }
    }

    private void initComponents() {
        numberFormatter.setMinimumFractionDigits(1);
        numberFormatter.setMaximumFractionDigits(3);
        dateFormat = new SimpleDateFormat("MM/dd/yy hh:mm:ss aa");
        setLayout(new BorderLayout());
        setUI(this);

        graphPanel = createGraphPanel();
        glassPane = createGlassPane(graphPanel);
        //glassPane.setVisible(true);
        glassPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                mouseOverValue = null;
                lastMousePoint = null;
                glassPane.repaint();
            }
        });
        glassPane.addMouseMotionListener(new MouseMotionAdapter() {            
            @Override
            public void mouseMoved(MouseEvent event) {
                lastMousePoint = event.getPoint();
                mouseOverValue = dataView.getCalcForPoint(lastMousePoint);
                if(mouseOverValue!=null) {
                    glassPane.repaint();
                }
            }
        });

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                  createTreePanel(),
                                                  glassPane);
        mainSplitPane.setDividerSize(2);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setDividerLocation(100);

        add(BorderLayout.CENTER, mainSplitPane);
        autoRefresh =
            new javax.swing.Timer(
                5*1000,
                new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        try {
                            if(!autoRefreshMode)
                                return;
                            runAccumulator();
                            if(lastMousePoint!=null) {
                                mouseOverValue =
                                    dataView.getCalcForPoint(lastMousePoint);
                                glassPane.repaint();
                            }
                        } catch(RemoteException e) {
                            e.printStackTrace();
                            autoRefresh.stop();
                        }
                    }
                });
    }

    public void removeNotify() {
        if(autoRefresh != null && autoRefresh.isRunning()) {
            autoRefresh.stop();
        }
        super.removeNotify();
    }

    public void addNotify() {
        super.addNotify();
        if(autoRefresh != null) {
            if(isVisible() && !autoRefresh.isRunning()) {
                autoRefresh.start();
            }
        }
    }

    public void setVisible(boolean flag) {
        if(autoRefresh != null) {
            if(flag && !autoRefresh.isRunning())
                autoRefresh.start();
            else if(autoRefresh.isRunning())
                autoRefresh.stop();
        }
        super.setVisible(flag);
    }


    private void setUI(Component component) {
        try {
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            UIManager.put("Label.foreground", Color.black);
            UIManager.put("TitledBorder.titleColor", Color.black);
            SwingUtilities.updateComponentTreeUI(component);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private ImageIcon getScaledImageIcon(String location, int width, int height) {
        ImageIcon icon = null;
        URL url = this.getClass().getClassLoader().getResource(location);
        if (url != null) {
            icon = new ImageIcon(url);
            icon = new ImageIcon(icon.getImage().getScaledInstance(width,
                                                                   height,
                                                                   Image.SCALE_SMOOTH));
        }
        return (icon);
    }

    private JComponent createTreePanel() {
        JPanel panel = new JPanel(new BorderLayout(0,0));
        tree = new JTree(root);
        tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tree.setBackground(new Color(202,202,202));
        tree.setForeground(Color.green);
        tree.setRowHeight(-1);
        tree.addTreeSelectionListener(this);
        tree.setCellRenderer(new AVTreeCellRenderer());

        JPanel bPanel = new JPanel();
        bPanel.setLayout(new BoxLayout(bPanel, BoxLayout.X_AXIS));
        final ImageIcon playIcon = getScaledImageIcon("images/Play24.gif", 16, 16);
        final ImageIcon pauseIcon = getScaledImageIcon("images/Pause24.gif", 16, 16);
        final JButton playPause = new JButton(pauseIcon);
        playPause.setToolTipText("Stop auto feed");
        playPause.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if(autoRefreshMode) {
                    playPause.setIcon(playIcon);
                    autoRefreshMode = false;
                    playPause.setToolTipText("Enable auto feed");
                } else {
                    playPause.setIcon(pauseIcon);
                    autoRefreshMode = true;
                    playPause.setToolTipText("Stop auto feed");
                }
            }
        });
        JButton refreshB = new JButton("Refresh");
        refreshB.setToolTipText("Refresh Watch Data Source Graph");
        refreshB.addActionListener(new ActionListener() {
                                       public void actionPerformed(ActionEvent evt) {
                                           try {
                                               runAccumulator();
                                           } catch(Exception e) {
                                               e.printStackTrace();
                                           }
                                       }
                                   });
        JButton resetB = new JButton("Reset");
        resetB.setToolTipText("Reset Watches");
        resetB.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        getWatches();
                    } catch(RemoteException re) {
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(null,
                                                      "Cannot reset watches\n" +
                                                      "RemoteException: "+
                                                      re.getLocalizedMessage(),
                                                      "Reset Watch Remote Error",
                                                      JOptionPane.ERROR_MESSAGE);


                    }
                }
            });

        JButton clearB = new JButton("Clear");
        clearB.setToolTipText("Clear all entries in the WatchDataSource");
        clearB.addActionListener(new ActionListener() {
                                     public void actionPerformed(ActionEvent evt) {
                                         clearWatch();
                                     }
                                 });
        bPanel.add(playPause);
        bPanel.add(refreshB);
        bPanel.add(resetB);
        bPanel.add(clearB);
        panel.add(BorderLayout.CENTER, /*sp*/tree);
        panel.add(BorderLayout.SOUTH, bPanel);
        JScrollPane sp = new JScrollPane();
        sp.getViewport().add(panel);
        return(sp);
    }

    public void valueChanged(TreeSelectionEvent evt) {
        try {
            runAccumulator();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void getWatches() throws RemoteException {
        root.removeAllChildren();
        setService(service);
        ((DefaultTreeModel)tree.getModel()).nodeStructureChanged(root);
        tree.repaint();
    }

    private void clearWatch() {
        int[] selRows = tree.getSelectionRows();
        if(selRows == null || selRows.length == 0 || selRows[0] == 0)
            return;
        int wdsIdx = selRows[0] - 1;
        try {
            watchDataSources[wdsIdx].clear();
            runAccumulator();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void runAccumulator() throws RemoteException {
        int[] selRows = tree.getSelectionRows();
        if(selRows == null || selRows.length == 0 || selRows[0] == 0)
            return;
        int wdsIdx = selRows[0] - 1;
        // assuming we only have one here, future enhancement would build the views
        // in the JTree and let the user select the view to see.
        dataView = defaultDataView;
        String view = watchDataSources[wdsIdx].getView();
        if(view != null) {
            try {
                dataView = loadView(view);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        dataView.setWatchDataSource(watchDataSources[wdsIdx]);
        graphPanel.repaint();
    }

    private void setTreeWDSNodes(String[] wdsNames) {
        for (String wdsName : wdsNames)
            root.add(new DefaultMutableTreeNode(wdsName));
        //tree.repaint();
    }

    static class AVTreeCellRenderer extends JLabel implements TreeCellRenderer {
        final Color SelectedBackgroundColor = Color.yellow;//new Color(0, 0, 128);
        protected boolean selected;
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            String stringValue = tree.convertValueToText(value, selected, expanded, 
                                                         leaf, row, hasFocus);
            setText(stringValue);
            this.selected = selected;
            return(this);
        }

        public void paint(Graphics g) {
            Color bColor;
            if(selected)
                bColor = SelectedBackgroundColor;
            else if(getParent() != null)
                bColor = getParent().getBackground();
            else
                bColor = getBackground();
            g.setColor(bColor);
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            super.paint(g);
        }
    }

    private JComponent createGraphPanel() {
        final JPanel panel = new JPanel() {
            public Color getBackground() {
                return(Color.white);
            }
            public void paint(Graphics g) {
                paintFlag = true;
                dataView.paint(g,this.getSize());
                paintFlag = false;
            }
        };
        panel.setDoubleBuffered(true);
        return(panel);
    }

    /*
     * Draw mouse over details
     */
    private GlassPaneContainer createGlassPane(JComponent comp) {
        return new GlassPaneContainer(comp) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if(mouseOverValue!=null) {
                    g.setFont(DefaultCalculableView.defaultFont);
                    Point p = mouseOverValue.getPoint();
                    String value =
                        " "+numberFormatter.format(
                            mouseOverValue.getCalculable().getValue())+" ";
                    Graphics2D g2 = (Graphics2D)g;

                    g2.setColor(Color.BLUE);
                    Point p2 = new Point((int)p.getX()+15, (int)p.getY()-15);
                    g2.drawLine((int)p.getX(), (int)p.getY(), (int)p2.getX(), (int)p2.getY());
                    Rectangle2D rect = g2.getFontMetrics().getStringBounds(value,
                                                                           g2);
                    int startX = p2.x-(int)rect.getWidth()/2;
                    int startY = p2.y-10;
                    g2.setColor(fillBoxColor);
                    g2.fillRect(startX,
                                startY,
                                (int)rect.getWidth(),
                                (int)rect.getHeight()+g2.getFontMetrics().getMaxDescent());
                    g2.setColor(Color.BLACK);
                    g2.drawRect(startX,
                                startY,
                                (int)rect.getWidth(),
                                (int)rect.getHeight()+g2.getFontMetrics().getMaxDescent());
                    g2.drawString(value,
                                 startX,
                                 startY+(int)rect.getHeight());
                    Date date = new Date(mouseOverValue.getCalculable().getWhen());
                    value = dateFormat.format(date);
                    rect = g2.getFontMetrics().getStringBounds(value, g2);
                    startX = p.x-(int)rect.getWidth()/2;
                    startY = getSize().height-10;

                    g2.setColor(fillBoxColor);
                    g2.fillRect(startX,
                                startY-(int)rect.getHeight(),
                                (int)rect.getWidth(),
                                (int)rect.getHeight()+g2.getFontMetrics().getMaxDescent());
                    g2.setColor(Color.BLACK);
                    g2.drawRect(startX,
                                startY-(int)rect.getHeight(),
                                (int)rect.getWidth(),
                                (int)rect.getHeight()+g2.getFontMetrics().getMaxDescent());

                    g2.setColor(Color.BLACK);
                    g2.drawLine(p.x, startY-(int)rect.getHeight(), p.x, p.y);
                    g2.drawString(value, startX, startY);
                }
            }
        };
    }

    private CalculableViewable loadView(String view) throws ClassNotFoundException, 
    InstantiationException, IllegalAccessException {
        Class vuClass = loader.loadClass(view);
        return(CalculableViewable)vuClass.newInstance();
    }

    static class GridBagPanel extends JPanel {
        private GridBagLayout layout = new GridBagLayout();
        public GridBagConstraints constraints  = new GridBagConstraints();

        public GridBagPanel() {
            super();
            setLayout(layout);
            constraints.fill = GridBagConstraints.HORIZONTAL;
        }

        public void addLabeledComp(String labelText, Component comp) {
            constraints.weightx = 0.0;
            constraints.gridwidth = GridBagConstraints.RELATIVE;
            layout.setConstraints(comp, constraints);
            super.add(new JLabel(labelText));
            constraints.weightx = 1.0;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            layout.setConstraints(comp, constraints);
            super.add(comp);
        }
    }

}
