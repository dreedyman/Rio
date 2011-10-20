/*
 * Copyright 2005 GigaSpaces, Inc. All Rights Reserved.
 */

package org.rioproject.tools.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.jini.config.Configuration;
import net.jini.core.lookup.ServiceItem;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.event.*;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.ui.Util;
import org.rioproject.tools.ui.treetable.DeploymentNode;
import org.rioproject.tools.ui.treetable.JTreeTable;
import org.rioproject.tools.ui.treetable.RemoteServiceEventNode;

public class ProvisionFailureEventTable extends AbstractNotificationUtility {
    JTreeTable eventTable;
    ProvisionFailureEventTreeModel dataModel;
    EC eventConsumer;
    BasicEventConsumer provisionFailureEventConsumer;
    BasicEventConsumer provisionMonitorEventConsumer;
    DynamicEventConsumer serviceLogEventConsumer;
    JScrollPane scroller;
    JCheckBox autoRemove;

    public ProvisionFailureEventTable(ColorManager colorManager,
                                      Configuration config,
                                      Properties props) {
        super();        
        setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("/");
        dataModel = new ProvisionFailureEventTreeModel(root);

        UIDefaults defaults = UIManager.getDefaults( );
        Icon openIcon   = defaults.getIcon( "Tree.expandedIcon" );
        Icon closedIcon = defaults.getIcon( "Tree.collapsedIcon" );

        eventTable = new JTreeTable(dataModel, colorManager, null);
        eventTable.getTree().setRootVisible(false);
        //eventTable.getTree().setShowsRootHandles(true);
        eventTable.setAutoCreateColumnsFromModel(false);
        eventTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //eventTable.getTableHeader().setReorderingAllowed(false);
        //no icons
        DefaultTreeCellRenderer treeRenderer =
            ((DefaultTreeCellRenderer) eventTable.getTree().getCellRenderer());
        treeRenderer.setLeafIcon(null);
        treeRenderer.setOpenIcon(openIcon);
        treeRenderer.setClosedIcon(closedIcon);

        eventTable.addMouseListener(new RowListener());
        dataModel.setTreeTable(eventTable);

        //String[] columns = new String[]{ "Status", "Description", "When" };
        String[] columns = new String[]{"Description", "When" };
        // Ensure that auto-create is off
        if (eventTable.getAutoCreateColumnsFromModel()) {
            throw new IllegalStateException();
        }
        for(String column : columns) {
            DefaultTableModel model = (DefaultTableModel)eventTable.getModel();
            TableColumn col = new TableColumn(model.getColumnCount());

            col.setHeaderValue(column);
            eventTable.addColumn(col);
            model.addColumn(column);
        }

        TableColumnModel cm = eventTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(0).setMaxWidth(500);
        //cm.getColumn(1).setPreferredWidth(50);
        //cm.getColumn(1).setMaxWidth(100);
        cm.getColumn(1).setPreferredWidth(300);
        cm.getColumn(1).setMaxWidth(500);
        cm.getColumn(2).setPreferredWidth(150);
        cm.getColumn(2).setMaxWidth(500);

        ((DefaultTableModel)eventTable.getModel()).fireTableDataChanged();

        scroller = new JScrollPane(eventTable);
        scroller.getViewport().setBackground(Color.WHITE);
        add(scroller, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        autoRemove = new JCheckBox("Auto remove provision failure events once a service is provisioned or undeployed");

        String s = props.getProperty(Constants.AUTO_REMOVE_PROVISION_FAILURE_EVENTS);
        boolean remove = true;
        if(s!=null)
            remove = Boolean.parseBoolean(s);

        autoRemove.setSelected(remove);
        autoRemove.setFont(Constants.ITEM_FONT);
        bottom.add(autoRemove);

        add(bottom, BorderLayout.SOUTH);

        /* Create the event consumer for ProvisionFailureEvent utilities */
        EventDescriptor provisionFailureEventDescriptor = ProvisionFailureEvent.getEventDescriptor();
        EventDescriptor provisionMonitorEventDescriptor = ProvisionMonitorEvent.getEventDescriptor();
            new EventDescriptor(ProvisionMonitorEvent.class, ProvisionFailureEvent.ID);
        eventConsumer = new EC();
        try {
            provisionFailureEventConsumer =
                new BasicEventConsumer(provisionFailureEventDescriptor,
                                       eventConsumer,
                                       null,
                                       config);
            provisionMonitorEventConsumer =
                new BasicEventConsumer(provisionMonitorEventDescriptor,
                                       eventConsumer,
                                       null,
                                       config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setDiscoveryManagement(DiscoveryManagement dMgr) {
        try {
            serviceLogEventConsumer = new DynamicEventConsumer(ServiceLogEvent.getEventDescriptor(),
                                                               eventConsumer,
                                                               dMgr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean getAutoRemove() {
        return autoRemove.isSelected();
    }

    class EC implements RemoteServiceEventListener {
        public void notify(RemoteServiceEvent event) {
            try {
                if(event instanceof ProvisionFailureEvent ||
                   event instanceof ServiceLogEvent) {
                    dataModel.addItem(event);
                    notifyListeners();
                } else if(event instanceof ProvisionMonitorEvent) {
                    ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
                    synchronized(this) {
                        if(pme.getAction().equals(ProvisionMonitorEvent.Action.SERVICE_PROVISIONED)) {
                            for(RemoteServiceEventNode rsn : dataModel.getRemoteServiceEventNode(pme.getServiceElement())) {
                                if(rsn instanceof ProvisionFailureEventTreeModel.EventNode) {
                                    ProvisionFailureEventTreeModel.EventNode en = (ProvisionFailureEventTreeModel.EventNode)rsn;
                                    if(en.getStatus().equals("Resolved"))
                                        continue;
                                    if(autoRemove.isSelected()) {
                                        dataModel.removeItem(en);
                                        notifyListeners();
                                    } else {
                                        en.setStatus("Resolved");
                                        dataModel.nodeChanged(en);
                                    }
                                }
                                break;
                            }
                        } else if(pme.getAction().equals(ProvisionMonitorEvent.Action.OPSTRING_UNDEPLOYED)) {
                            DeploymentNode dn = dataModel.getDeploymentNode(pme.getOperationalStringName());
                            if(dn==null)
                                return;
                            java.util.List<RemoteServiceEventNode> removals = new ArrayList<RemoteServiceEventNode>();
                            for (int i = 0; i < dn.getChildCount(); i++) {
                                RemoteServiceEventNode rsn = (RemoteServiceEventNode)dn.getChildAt(i);
                                if(rsn instanceof ProvisionFailureEventTreeModel.EventNode) {
                                    removals.add(rsn);
                                }
                            }
                            for(RemoteServiceEventNode rsn : removals)
                                dataModel.removeItem(rsn);
                            notifyListeners();
                        }
                    }
                }
                //setStatusErrorText("ProvisionFailureEvent received for "
                //        + pfe.getServiceElement().getName());
            } catch (Throwable t) {
                Util.showError(t, null, "Notification of a ProvisionFailureEvent");
            }
        }
    }
    
    public void addService(ServiceItem item) {
        provisionFailureEventConsumer.register(item);
        provisionMonitorEventConsumer.register(item);
    }

    private void removeEvent(int row) {
        dataModel.removeItem(row);
        notifyListeners();
    }

    public void terminate() {
        if(provisionFailureEventConsumer!=null)
            provisionFailureEventConsumer.terminate();
        if(serviceLogEventConsumer!=null)
            serviceLogEventConsumer.terminate();
    }
    
    private RemoteServiceEvent getRemoteServiceEvent(int row) {
        if(row==-1)
            return(null);
        return dataModel.getItem(row);
    }

    public int getTotalItemCount() {
        return dataModel.getRowCount();
    }

    class RowListener extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            int clickCount = e.getClickCount();
            if(clickCount==2) {
                int row = eventTable.rowAtPoint(new Point(e.getX(), e.getY()));
                if(row==-1)
                    return;
                showDetails(getRemoteServiceEvent(row));
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
                            int row = eventTable.rowAtPoint(new Point(e.getX(), 
                                                                      e.getY()));
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

    void showDetails(final RemoteServiceEvent event) {
        Component parent = SwingUtilities.getAncestorOfClass(JFrame.class, this);
        JDialog dialog = new JDialog((JFrame)parent);
        final String[] columns = {"Field", "Value"};
        String impl = "<not declared>";
        final Object[][] data;
        String label;
        Throwable thrown;
        if(event instanceof ProvisionFailureEvent) {
            label = "Provision Failure Event";
            ProvisionFailureEvent pfe = (ProvisionFailureEvent)event;
            ServiceElement elem = pfe.getServiceElement();
            if(elem.getComponentBundle()!=null)
                impl = elem.getComponentBundle().getClassName();
            thrown = pfe.getThrowable();
            String exception = getExceptionText(thrown);
            data = new Object[][] {
                {"When", dataModel.getDateFormat().format(event.getDate())},
                {"Deployment", elem.getOperationalStringName()},
                {"Service", elem.getName()},
                {"Class", impl},
                {"Reason", pfe.getReason()},
                {"Exception", exception}
            };
        } else {
            label = "Service Log Event";
            ServiceLogEvent sle = (ServiceLogEvent)event;
            thrown = sle.getLogRecord().getThrown();
            String exception = getExceptionText(thrown);
            data = new Object[][] {
                {"When", dataModel.getDateFormat().format(event.getDate())},
                {"Deployment", sle.getOpStringName()},
                {"Service", sle.getServiceName()},
                {"Machine", sle.getAddress().getHostName()},
                {"Message", sle.getLogRecord().getLevel()+": "+sle.getLogRecord().getMessage()},
                {"Exception", exception}
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

