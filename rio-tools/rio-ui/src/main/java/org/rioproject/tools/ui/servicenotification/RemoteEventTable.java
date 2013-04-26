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
package org.rioproject.tools.ui.servicenotification;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.discovery.DiscoveryManagement;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;
import org.rioproject.tools.ui.AbstractNotificationUtility;
import org.rioproject.tools.ui.ChainedRemoteEventListener;
import org.rioproject.tools.ui.servicenotification.filter.FilterCriteria;
import org.rioproject.tools.ui.servicenotification.filter.FilterListener;
import org.rioproject.tools.ui.servicenotification.filter.FilterPanel;
import org.rioproject.ui.Util;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility to display {@link org.rioproject.event.RemoteServiceEvent}s using either an {@link EventCollector} or
 * subscribe to services for notification(s). The utility also provides filtering support as well as other handy
 * options.
 *
 * @author Dennis Reedy
 */
public class RemoteEventTable extends AbstractNotificationUtility {
    private final JXTreeTable eventTable;
    private final RemoteEventTreeModel dataModel;
    private final ChainedRemoteEventListener remoteEventListener;
    private final RemoteEventConsumerManager eventConsumerManager;
    private DiscoveryManagement dMgr;
    private final Configuration config;
    private final EventColorManager eventColorManager = new EventColorManager();
    private final FilterPanel filterPanel;
    private final RemoteServiceEventDetailsTable detailsTable;
    private final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);

    public RemoteEventTable(final Configuration config, final Properties props) throws ExportException, ConfigurationException {
        super(new BorderLayout(8, 8));
        this.config = config;

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));

        eventConsumerManager = new RemoteEventConsumerManager();
        filterPanel = new FilterPanel(new FilterApplier(), new TreeExpander(), new EventCollectorControl(this), props);
        topPanel.add(filterPanel, BorderLayout.NORTH);

        java.util.List<String> columns = new ArrayList<String>();
        columns.add("Deployment");
        columns.add("Description");
        columns.add("When");
        dataModel = new RemoteEventTreeModel(new RootNode(), columns);

        UIDefaults defaults = UIManager.getDefaults( );
        Icon openIcon   = defaults.getIcon("Tree.expandedIcon");
        Icon closedIcon = defaults.getIcon("Tree.collapsedIcon");

        Color normalBackground = new Color(215,225, 205);
        Color warningBackground = new Color(255, 245, 205);
        Color minorBackground = new Color(255, 235, 205);
        Color criticalColor = new Color(245, 205, 205);
        Color indeterminateColor = new Color(235, 235, 205);
        eventTable = new JXTreeTable(dataModel);
        eventTable.setRootVisible(false);
        ColorHighlighter normalHighlighter = new ColorHighlighter(new HighlightPredicate() {
            @Override
            public boolean isHighlighted(Component component, ComponentAdapter componentAdapter) {
                if(!componentAdapter.isLeaf())
                    return false;
                Object value = componentAdapter.getValue(0);
                return value != null && eventColorManager.isNormal((String) value);
            }
        });
        normalHighlighter.setBackground(normalBackground);

        ColorHighlighter indeterminateHighlighter = new ColorHighlighter(new HighlightPredicate() {
            @Override
            public boolean isHighlighted(Component component, ComponentAdapter componentAdapter) {
                if(!componentAdapter.isLeaf())
                    return false;
                Object value = componentAdapter.getValue(0);
                return value != null && eventColorManager.isIndeterminate((String) value);
            }
        });
        indeterminateHighlighter.setBackground(indeterminateColor);

        ColorHighlighter minorHighlighter = new ColorHighlighter(new HighlightPredicate() {
            @Override
            public boolean isHighlighted(Component component, ComponentAdapter componentAdapter) {
                Object value = componentAdapter.getValue(0);
                return value != null && eventColorManager.isMinor((String) value);
            }
        });
        minorHighlighter.setBackground(minorBackground);

        ColorHighlighter warningHighlighter = new ColorHighlighter(new HighlightPredicate() {
            @Override
            public boolean isHighlighted(Component component, ComponentAdapter componentAdapter) {
                Object value = componentAdapter.getValue(0);
                return value != null && eventColorManager.isWarning((String)value);
            }
        });

        warningHighlighter.setBackground(warningBackground);

        ColorHighlighter criticalHighlighter = new ColorHighlighter(new HighlightPredicate() {
            @Override
            public boolean isHighlighted(Component component, ComponentAdapter componentAdapter) {
                Object value = componentAdapter.getValue(0);
                return value != null && eventColorManager.isCritical((String) value);
            }
        });
        criticalHighlighter.setBackground(criticalColor);

        eventTable.addHighlighter(normalHighlighter);
        eventTable.addHighlighter(warningHighlighter);
        eventTable.addHighlighter(criticalHighlighter);
        eventTable.addHighlighter(minorHighlighter);
        eventTable.addHighlighter(indeterminateHighlighter);

        dataModel.setTreeTable(eventTable);
        eventTable.setShowsRootHandles(false);
        eventTable.setAutoCreateColumnsFromModel(false);
        //eventTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        detailsTable = new RemoteServiceEventDetailsTable();
        JPanel detailsPanel = new JPanel(new BorderLayout(8, 8));
        detailsPanel.add(detailsTable);

        eventTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()) {
                    detailsTable.setRemoteServiceEventNode(getRemoteServiceEventNode(eventTable.getSelectedRow()));
                }
            }
        });

        //no icons
        eventTable.setLeafIcon(null);
        eventTable.setOpenIcon(openIcon);
        eventTable.setClosedIcon(closedIcon);

        eventTable.addMouseListener(new RowListener());

        TableColumnModel cm = eventTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(60);
        cm.getColumn(0).setWidth(60);
        cm.getColumn(0).setMaxWidth(500);

        cm.getColumn(1).setPreferredWidth(400);
        cm.getColumn(1).setMaxWidth(1000);
        cm.getColumn(2).setPreferredWidth(100);
        cm.getColumn(2).setMaxWidth(500);

        final JScrollPane scroller = new JScrollPane(eventTable);
        scroller.getViewport().setBackground(Color.WHITE);
        topPanel.add(scroller, BorderLayout.CENTER);

        splitPane.add(topPanel, JSplitPane.TOP);
        splitPane.add(detailsPanel, JSplitPane.BOTTOM);

        splitPane.setDividerSize(8);

        add(splitPane, BorderLayout.CENTER);

        /* Create the event consumer for EventCollector notification */
        remoteEventListener = new ChainedRemoteEventListener(new RemoteEventConsumer(this), config);
    }

    public void init(final Properties props) {
        int dividerLocation = splitPane.getHeight()-splitPane.getHeight()/6;
        splitPane.setDividerLocation(dividerLocation);
    }

    public boolean getUseEventCollector() {
        return filterPanel.getUseEventCollector();
    }

    public int getDividerLocation() {
        return splitPane.getDividerLocation();
    }

    public void expandAll() {
        try {
            eventTable.expandAll();
        } catch(IllegalArgumentException e) {
            Util.showError(e, this, "Could not expand tree");
        }
    }

    public RemoteEventTreeModel getDataModel() {
        return dataModel;
    }

    public void setDiscoveryManagement(final DiscoveryManagement dMgr) throws Exception {
        this.dMgr = dMgr;
        createEventListener();
    }

    public void createEventListener() throws Exception {
        if(getUseEventCollector()) {
            eventConsumerManager.terminate();
            eventConsumerManager.setUseEventCollector(true);
            eventConsumerManager.registerForEventCollectorNotification(remoteEventListener, config);
        } else {
            if(dMgr==null)
                throw new IllegalStateException("Cannot register for service notifications without a DiscoveryManagement instance");
            eventConsumerManager.terminate();
            eventConsumerManager.setUseEventCollector(false);
            eventConsumerManager.registerForAllServiceNotification(new RemoteEventConsumer(this), dMgr);
        }
    }

    public void addEventCollector(final EventCollector eventCollector) throws LeaseDeniedException,
                                                                              IOException,
                                                                              UnknownEventCollectorRegistration {
        eventConsumerManager.addEventCollector(eventCollector);
        filterPanel.setUseEventCollectorCheckBoxText();
    }

    public void removeEventCollector(final EventCollector eventCollector) {
        eventConsumerManager.removeEventCollector(eventCollector);
        filterPanel.setUseEventCollectorCheckBoxText();
    }

    public void terminate() {
        remoteEventListener.terminate();
        if(eventConsumerManager!=null)
            eventConsumerManager.terminate();
    }
    
    private RemoteServiceEventNode getRemoteServiceEventNode(final int row) {
        if(row==-1)
            return null;
        TreePath path = eventTable.getPathForRow(row);
        if(path==null)
            return null;
        return dataModel.getRemoteServiceEventNode(row);
    }

    public int getTotalItemCount() {
        int rowCounter = 0;
        for(DeploymentNode dNode : dataModel.getDeploymentNodes()) {
            for(int i=0; i<dNode.getChildCount(); i++) {
                RemoteServiceEventNode rNode = (RemoteServiceEventNode)dNode.getChildAt(i);
                if(eventColorManager.isCritical((String)rNode.getValueAt(0))) {
                    rowCounter++;
                }
            }
        }
        //return dataModel.getRowCount();
        return rowCounter;
    }

    class RowListener extends MouseAdapter {

        public void mouseClicked(final MouseEvent e) {
            int clickCount = e.getClickCount();
            int row = eventTable.rowAtPoint(new Point(e.getX(), e.getY()));
            if(row==-1)
                return;
            if(clickCount==1) {
                AbstractMutableTreeTableNode node = dataModel.getNode(row);
                if(node instanceof DeploymentNode) {
                    if(eventTable.isExpanded(row)) {
                        eventTable.collapseRow(row);
                    } else {
                        eventTable.expandRow(row);
                    }
                }
            }
        }

        public void mousePressed(final MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(final MouseEvent e) {
            maybeShowPopup(e);
        }

        void maybeShowPopup(final MouseEvent e) {
            if(e.isPopupTrigger()) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem delete = new JMenuItem("Delete");
                delete.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            int[] rows = eventTable.getSelectedRows();
                            for(int i=rows.length-1;i>=0;i--){
                                dataModel.removeItem(rows[i]-i);
                            }
                            notifyListeners();
                        }
                    });
                popup.add(delete);
                popup.pack();
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    class TreeExpander implements TreeExpansionListener {

        @Override
        public void expand() {
            eventTable.expandAll();
        }

        @Override
        public void collapse() {
            int row = eventTable.getRowCount() - 1;
            while (row >= 0) {
                eventTable.collapseRow(row);
                row--;
            }
        }
    }

    class EventCollectorControl implements EventCollectorListener {
        Component parent;

        EventCollectorControl(final Component parent) {
            this.parent = parent;
        }

        @Override
        public void handleEventCollectorRegistration(final boolean useEventCollector) {
            try {
                createEventListener();
            } catch (Exception e) {
                Util.showError(e, parent, "Could not create Event Listener");
            }
        }

        @Override
        public int getEventControllerCount() {
            return eventConsumerManager.getEventControllerCount();
        }

        @Override
        public void refresh() {
            try {
                Map<DeploymentNode, Boolean> nodes = new LinkedHashMap<DeploymentNode, Boolean>();
                for(DeploymentNode dNode : dataModel.getDeploymentNodes()) {
                    int row = dataModel.getDeploymentNodeRow(dNode);
                    boolean expanded = row != -1 && eventTable.isExpanded(dataModel.getDeploymentNodeRow(dNode));
                    nodes.put(dNode, expanded);
                }
                dataModel.reset();
                eventConsumerManager.refresh();
                for(Map.Entry<DeploymentNode, Boolean> entry : nodes.entrySet()) {
                    if(entry.getValue()) {
                        int row = dataModel.getDeploymentNodeRow(entry.getKey());
                        eventTable.expandRow(dataModel.getDeploymentNodeRow(entry.getKey()));
                        dataModel.updated(eventTable.getPathForRow(row));
                    }
                }

            } catch (UnknownEventCollectorRegistration e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class FilterApplier implements FilterListener {

        public void notify(final FilterCriteria filterCriteria) {
            if(filterCriteria==null && dataModel.getFilterCriteria()==null)
                return;
            dataModel.setFilterCriteria(filterCriteria);
            eventTable.expandAll();
        }
    }

}

