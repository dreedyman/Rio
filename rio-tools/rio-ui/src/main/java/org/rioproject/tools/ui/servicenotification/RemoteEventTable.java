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
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.tools.ui.AbstractNotificationUtility;
import org.rioproject.tools.ui.ChainedRemoteEventListener;
import org.rioproject.tools.ui.Constants;
import org.rioproject.ui.Util;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.rmi.server.ExportException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Properties;

public class RemoteEventTable extends AbstractNotificationUtility {
    private final JXTreeTable eventTable;
    private final RemoteEventTreeModel dataModel;
    private final ChainedRemoteEventListener remoteEventListener;
    private final JCheckBox autoRemove;
    private final JCheckBox useEventCollector;
    private final RemoteEventConsumerManager eventConsumerManager;
    private DiscoveryManagement dMgr;
    private final Configuration config;
    private final NumberFormat numberFormatter;

    public RemoteEventTable(Configuration config, Properties props) throws ExportException, ConfigurationException {
        super();
        this.config = config;
        setLayout(new BorderLayout());

        numberFormatter = NumberFormat.getNumberInstance();
        numberFormatter.setGroupingUsed(false);
        numberFormatter.setMaximumFractionDigits(2);

        eventConsumerManager = new RemoteEventConsumerManager();

        java.util.List<String> columns = new ArrayList<String>();
        columns.add("Deployment");
        columns.add("Description");
        columns.add("When");
        dataModel = new RemoteEventTreeModel(new RootNode(), columns);

        UIDefaults defaults = UIManager.getDefaults( );
        Icon openIcon   = defaults.getIcon("Tree.expandedIcon");
        Icon closedIcon = defaults.getIcon("Tree.collapsedIcon");

        eventTable = new JXTreeTable(dataModel);
        eventTable.setRootVisible(false);
        dataModel.setTreeTable(eventTable);
        eventTable.setShowsRootHandles(false);
        eventTable.setAutoCreateColumnsFromModel(false);
        eventTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
        add(scroller, BorderLayout.CENTER);

        final JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        autoRemove = new JCheckBox("Auto remove provision failure events once a service is provisioned or undeployed");
        setCheckBox(autoRemove, props, Constants.AUTO_REMOVE_PROVISION_FAILURE_EVENTS, true);

        useEventCollector = new JCheckBox();
        setUseEventCollectorCheckBoxText();
        setCheckBox(useEventCollector, props, Constants.USE_EVENT_COLLECTOR, false);
        useEventCollector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    createEventListener();
                } catch (Exception e) {
                    Util.showError(e, bottom, "Could not create Event Listener");
                }
            }
        });

        bottom.add(useEventCollector);
        bottom.add(autoRemove);

        add(bottom, BorderLayout.SOUTH);

        /* Create the event consumer for EventCollector notification */
        remoteEventListener = new ChainedRemoteEventListener(new RemoteEventConsumer(this), config);
    }

    private void setCheckBox(JCheckBox checkBox, Properties props, String propertyName, boolean defaultValue) {
        String s = props.getProperty(propertyName);
        boolean value = defaultValue;
        if(s!=null)
            value = Boolean.parseBoolean(s);
        checkBox.setSelected(value);
        checkBox.setFont(Constants.ITEM_FONT);
    }

    public boolean getAutoRemove() {
        return autoRemove.isSelected();
    }

    public boolean getUseEventCollector() {
        return useEventCollector.isSelected();
    }

    public void expandAll() {
        eventTable.expandAll();
    }

    public RemoteEventTreeModel getDataModel() {
        return dataModel;
    }

    public void setDiscoveryManagement(DiscoveryManagement dMgr) throws Exception {
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
            eventConsumerManager.registerForAllServiceNotification(new RemoteEventConsumer(this),
                                                                   dMgr);
        }
    }

    public void addEventCollector(EventCollector eventCollector) throws LeaseDeniedException,
                                                                        IOException,
                                                                        UnknownEventCollectorRegistration {
        eventConsumerManager.addEventCollector(eventCollector);
        setUseEventCollectorCheckBoxText();
    }

    private void setUseEventCollectorCheckBoxText() {
        useEventCollector.setText(String.format("Use EventCollector (discovered %d EventCollectors)",
                                                eventConsumerManager.getEventControllerCount()));

    }

    private void removeEvent(int row) {
        dataModel.removeItem(row);
        notifyListeners();
    }

    public void terminate() {
        remoteEventListener.terminate();
        if(eventConsumerManager!=null)
            eventConsumerManager.terminate();
    }
    
    private RemoteServiceEventNode getRemoteServiceEvent(int row) {
        if(row==-1)
            return null;
        TreePath path = eventTable.getPathForRow(row);
        if(path==null)
            return null;
        return dataModel.getRemoteServiceEventNode(row);
    }

    public int getTotalItemCount() {
        return dataModel.getRowCount();
    }

    class RowListener extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            int clickCount = e.getClickCount();
            int row = eventTable.rowAtPoint(new Point(e.getX(), e.getY()));
            if(row==-1)
                return;
            if(clickCount==2) {
                showDetails(getRemoteServiceEvent(row));
            } else if(clickCount==1) {
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

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        void maybeShowPopup(final MouseEvent e) {
            if(e.isPopupTrigger()) {
                final int row = eventTable.rowAtPoint(new Point(e.getX(), e.getY()));
                JPopupMenu popup = new JPopupMenu();
                JMenuItem delete = new JMenuItem("Delete");
                delete.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            removeEvent(row);
                            notifyListeners();
                        }
                    });
                JMenuItem details = new JMenuItem("Show Details");
                details.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            int row = eventTable.rowAtPoint(new Point(e.getX(), e.getY()));
                            showDetails(getRemoteServiceEvent(row));
                        }
                    });
                popup.add(details);
                popup.addSeparator();
                popup.add(delete);
                popup.pack();
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    void showDetails(final RemoteServiceEventNode eventNode) {
        if(eventNode==null)
            return;
        Component parent = SwingUtilities.getAncestorOfClass(JFrame.class, this);
        JDialog dialog = new JDialog((JFrame)parent);
        final String[] columns = {"Field", "Value"};
        String impl = "<not declared>";
        final Object[][] data;
        String label;
        Throwable thrown = null;
        RemoteServiceEvent event = eventNode.getEvent();
        if(event instanceof ProvisionFailureEvent) {
            label = "Provision Failure Event";
            ProvisionFailureEvent pfe = (ProvisionFailureEvent)event;
            ServiceElement elem = pfe.getServiceElement();
            if(elem.getComponentBundle()!=null)
                impl = elem.getComponentBundle().getClassName();
            thrown = pfe.getThrowable();
            String exception = getExceptionText(thrown);
            data = new Object[][] {
                {"When", Constants.DATE_FORMAT.format(event.getDate())},
                {"Deployment", elem.getOperationalStringName()},
                {"Service", elem.getName()},
                {"Class", impl},
                {"Reason", pfe.getReason()},
                {"Exception", exception}
            };
        } else if(event instanceof ProvisionMonitorEvent) {
            label = "Provision Monitor Event";
            ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
            thrown = null;
            StringBuilder builder = new StringBuilder();
            if(pme.getAction().equals(ProvisionMonitorEvent.Action.OPSTRING_DEPLOYED) ||
               pme.getAction().equals(ProvisionMonitorEvent.Action.OPSTRING_UNDEPLOYED)) {
                StringBuilder serviceNameBuilder = new StringBuilder();
                for(ServiceElement service : pme.getOperationalString().getServices()) {
                    if(serviceNameBuilder.length()>0) {
                        serviceNameBuilder.append(", ");
                    }
                    serviceNameBuilder.append(service.getName());
                }
                builder.append(serviceNameBuilder.toString());
                data = new Object[][] {
                                          {"When", Constants.DATE_FORMAT.format(event.getDate())},
                                          {"Deployment", pme.getOperationalStringName()},
                                          {"Action", pme.getAction().toString()},
                                          {"Services", builder.toString()}
                };
            } else {
                data = new Object[][] {
                                          {"When", Constants.DATE_FORMAT.format(event.getDate())},
                                          {"Deployment", pme.getOperationalStringName()},
                                          {"Action", pme.getAction().toString()},
                                          {"Description", eventNode.getDescription()}
                };
            }

        } else if(event instanceof ServiceLogEvent) {
            label = "Service Log Event";
            ServiceLogEvent sle = (ServiceLogEvent)event;
            thrown = sle.getLogRecord().getThrown();
            String exception = getExceptionText(thrown);
            data = new Object[][] {
                {"When", Constants.DATE_FORMAT.format(event.getDate())},
                {"Deployment", sle.getOpStringName()},
                {"Service", sle.getServiceName()},
                {"Machine", sle.getAddress().getHostName()},
                {"Message", sle.getLogRecord().getLevel()+": "+sle.getLogRecord().getMessage()},
                {"Exception", exception}
            };
        } else {
            label = "SLA Threshold Event";
            SLAThresholdEvent slaEvent = (SLAThresholdEvent)event;
            StringBuilder builder = new StringBuilder();
            builder.append("low=").append(slaEvent.getSLA().getCurrentLowThreshold());
            builder.append(" high=").append(slaEvent.getSLA().getCurrentHighThreshold());
            data = new Object[][] {
                                      {"When", Constants.DATE_FORMAT.format(event.getDate())},
                                      {"Deployment", slaEvent.getServiceElement().getOperationalStringName()},
                                      {"Service", slaEvent.getServiceElement().getName()},
                                      {"Machine", slaEvent.getHostAddress()},
                                      {"Value", numberFormatter.format(slaEvent.getCalculable().getValue())},
                                      {"Threshold Values", builder.toString()},
                                      {"Policy Handler", slaEvent.getSLAPolicyHandlerDescription()}
            };
        }
        JTable table = new JTable(data, columns);
        table.setModel(new AbstractTableModel() {
            public String getColumnName(int col) {
                return columns[col];
            }
            public int getRowCount() {
                return data.length;
            }
            public int getColumnCount() {
                return columns.length;
            }
            public Object getValueAt(int row, int col) {
                return data[row][col];
            }
            public boolean isCellEditable(int row, int col){
                return false;
            }
            public void setValueAt(Object value, int row, int col) {
                data[row][col] = value;
                fireTableCellUpdated(row, col);
            }
        });

        table.addMouseListener(new DetailsRowListener(table, thrown, (JFrame)parent, label));
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        TableColumn column = table.getColumnModel().getColumn(0);
        column.setPreferredWidth(100);
        column.setMaxWidth(130);

        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout(8, 8));
        pane.add(scrollPane, BorderLayout.CENTER);
        JButton dismiss = new JButton("Close");
        dismiss.setToolTipText("Close the details dialog");
        JButton details = new JButton("Show Details");
        details.setToolTipText("Show stack trace for the Exception");
        dismiss.addActionListener(new Util.DisposeActionListener(dialog));
        JPanel buttonPane = new JPanel();
        details.addActionListener(new ShowDetailsListener(thrown, (JFrame)parent, label));
        if(thrown!=null)
            buttonPane.add(details);
        buttonPane.add(dismiss);
        pane.add(buttonPane, BorderLayout.SOUTH);
        dialog.getContentPane().add(pane);
        int width = 675;
        int height = 200;
        dialog.pack();
        dialog.setSize(width, height);
        dialog.setTitle(label+" Details");
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private String getExceptionText(Throwable t) {
        String exception;
        if(t==null)
            exception = "No Exception";
        else
            exception = "<html><body>" +
                        "<font face=monospace><font size=3><font color=red>" +
                        t.getClass().getName()+
                        "</font></font></font>" +
                        "</body></html>";
        return exception;
    }

    private void showDetails(JFrame parent, String label, Throwable thrown) {
        Throwable cause = thrown.getCause();
        if(cause != null) {
            Throwable nested = cause.getCause();
            Util.showError((nested==null?cause:nested),
                           parent,
                           "Stacktrace for "+label);
        } else {
            Util.showError(thrown,
                           parent,
                           "Stacktrace for " + label);
        }
    }

    class ShowDetailsListener implements ActionListener  {
        Throwable thrown;
        JFrame parent;
        String label;

        ShowDetailsListener(Throwable thrown, JFrame parent, String label) {
            this.thrown = thrown;
            this.parent = parent;
            this.label = label;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            showDetails(parent, label, thrown);
        }
    }

    class RootNode extends AbstractMutableTreeTableNode {

        @Override
        public Object getValueAt(int i) {
            return null;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }
    }

    class DetailsRowListener extends MouseAdapter {
        JTable table;
        Throwable thrown;
        JFrame parent;
        String label;

        DetailsRowListener(JTable table, Throwable thrown, JFrame parent, String label) {
            this.table = table;
            this.thrown = thrown;
            this.parent = parent;
            this.label = label;
        }

        public void mouseClicked(MouseEvent e) {
            int clickCount = e.getClickCount();
            if(clickCount==2) {
                int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                String field = (String)table.getModel().getValueAt(row, 0);
                if(field.equals("Exception") && thrown!=null) {
                    showDetails(parent, label, thrown);
                }
            }
        }
    }

}

