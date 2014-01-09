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
package org.rioproject.tools.ui.cybernodeutilization;

import net.jini.core.lookup.ServiceItem;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.impl.jmx.JMXUtil;
import org.rioproject.tools.ui.Constants;
import org.rioproject.tools.ui.GraphViewAdapter;
import org.rioproject.tools.ui.UtilizationColumnManager;
import org.rioproject.tools.ui.servicenotification.DeploymentNode;
import org.rioproject.ui.Util;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.tools.ui.serviceui.ServiceAdminManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * The CybernodeUtilizationPanel displays graphed and tabular information about
 * Cybernode Utilization
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class CybernodeUtilizationPanel extends JPanel {
    /**
     * The model for the Cybernode utilization table
     */
    private UtilizationTreeModel utilizationModel;
    private final JXTreeTable treeTable;
    /**
     * For showing cybernode stats
     */
    private JPanel statusPanel;
    private final ServiceAdminManager adminManager = ServiceAdminManager.getInstance();
    /* a reference to 'self' */
    private JPanel component;
    private boolean expandAll = false;
    private final GraphViewAdapter graphViewAdapter;

    public CybernodeUtilizationPanel(final GraphViewAdapter graphViewAdapter,
                                     final String[] selectedColumns,
                                     final Properties props) {
        super(new BorderLayout(8, 8));
        this.graphViewAdapter = graphViewAdapter;
        String s = props.getProperty(Constants.TREE_TABLE_AUTO_EXPAND);
        expandAll = (s != null && Boolean.parseBoolean(s));

        List<String> columns = getColumns(selectedColumns);

        treeTable = /*new JXTreeTable();*/
        new JXTreeTable() {
            @Override public TableCellRenderer getCellRenderer(int row, int column) {
                Object value;
                try {
                    value = super.getValueAt(row, column);
                } catch (IllegalArgumentException e) {
                    value = null;
                }
                if(value != null) {
                    if(value instanceof JLabel) {
                        return new JLabelCellRenderer(((JLabel)value));
                    }
                    return super.getCellRenderer(row, column);
                }
                return super.getCellRenderer(row, column);
            }
        };
        utilizationModel = createModel(columns);

        treeTable.setTreeTableModel(utilizationModel);
        treeTable.setRootVisible(false);
        treeTable.setShowsRootHandles(true);
        treeTable.setAutoCreateColumnsFromModel(false);
        treeTable.getTableHeader().setReorderingAllowed(false);

        //no icons
        treeTable.setLeafIcon(null);
        treeTable.setOpenIcon(null);
        treeTable.setClosedIcon(null);

        component = this;

        treeTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int row = treeTable.getSelectedRow();
                if(row==-1) {
                    return;
                }
                if(e.isPopupTrigger()) {
                    showPopup(e, row);
                }
            }

            public void mouseReleased(MouseEvent e) {
                int row = treeTable.getSelectedRow();
                if(!e.isPopupTrigger() || row==-1) {
                    return;
                }
                showPopup(e, row);
            }

            private void showPopup(MouseEvent e, int row) {
                CybernodeNode cNode = utilizationModel.getCybernodeNode(row);
                ServiceNode sNode = null;
                final String label;
                boolean showJConsoleOption = true;
                if(cNode == null) {
                    sNode = utilizationModel.getServiceNode(row);
                    if(sNode==null)
                        return;
                    label = sNode.getName();
                    if(!sNode.isForked())
                        showJConsoleOption=false;
                } else {
                    label = "Cybernode Admin";
                }
                final ServiceItemAccessor serviceItemAccessor = new ServiceItemAccessor(cNode==null?sNode:cNode);
                JPopupMenu popup = new JPopupMenu();
                JMenuItem serviceUI = new JMenuItem("Show "+label+" UI");
                serviceUI.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        adminManager.doShowAdminUI(serviceItemAccessor.getServiceItem(), graphViewAdapter.getMain());
                    }
                });

                JMenuItem jconsole = null;
                if(showJConsoleOption) {
                    jconsole = new JMenuItem("Launch JConsole");
                    final ServiceItem serviceItem = serviceItemAccessor.getServiceItem();
                    if(serviceItem!=null) {
                        jconsole.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                String jmxConn = JMXUtil.getJMXConnection(serviceItem.attributeSets);
                                if(jmxConn==null) {
                                    JOptionPane.showMessageDialog(null,
                                                                  "There is no JMX connectivity information for ["+label+"], " +
                                                                  "unable to start a JConsole",
                                                                  "Missing JMX Entries",
                                                                  JOptionPane.WARNING_MESSAGE);
                                    return;
                                }
                                try {
                                    Runtime.getRuntime().exec("jconsole "+jmxConn);
                                } catch (IOException e1) {
                                    Util.showError(e1, component, "Creating jconsole");
                                }
                            }
                        });
                    } else {
                        JOptionPane.showMessageDialog(null,
                                                      "There is no ServiceItem for ["+label+"]",
                                                      "Missing ServiceItem",
                                                      JOptionPane.WARNING_MESSAGE);
                    }
                }
                popup.add(serviceUI);
                if(jconsole!=null) {
                    popup.addSeparator();
                    popup.add(jconsole);
                }
                popup.pack();
                popup.show(e.getComponent(), e.getX(), e.getY());
            }

            public void mouseClicked(MouseEvent e) {
                int row = treeTable.getSelectedRow();
                if (row == -1)
                    return;
                if (e.getClickCount() == 2) {
                    AbstractMutableTreeTableNode node = utilizationModel.getNode(row);
                    if(node instanceof ServiceNode) {
                        ServiceItem item = graphViewAdapter.getServiceItem(((ServiceNode)node).getServiceElement(),
                                                                           ((ServiceNode)node).getUuid());
                        if(item!=null)
                            adminManager.doShowAdminUI(item, graphViewAdapter.getMain());
                    } else if(node instanceof CybernodeNode) {
                        ServiceItem item = ((CybernodeNode)node).getServiceItem();
                        if(item!=null)
                            adminManager.doShowAdminUI(item, graphViewAdapter.getMain());
                    }
                } else if(e.getClickCount()==1) {
                    AbstractMutableTreeTableNode node = utilizationModel.getNode(row);
                    if(node instanceof DeploymentNode) {
                        if(treeTable.isExpanded(row)) {
                            treeTable.collapseRow(row);
                        } else {
                            treeTable.expandRow(row);
                        }
                    }
                }
            }
        });

        //treeTable.setColumnSelectionAllowed(false);
        //treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorderPainted(false);
        ImageIcon collapseIcon = Util.getImageIcon("org/rioproject/tools/ui/images/collapseall.gif");
        ImageIcon expandIcon = Util.getImageIcon("org/rioproject/tools/ui/images/expandall.gif");
        ImageIcon refreshIcon = null;
        if(expandIcon!=null) {
            refreshIcon = Util.getScaledImageIcon("org/rioproject/tools/ui/images/view-refresh.png",
                                                  expandIcon.getIconWidth(),
                                                  expandIcon.getIconHeight());
        }
        JButton collapse = new JButton();
        collapse.setIcon(collapseIcon);
        collapse.setPreferredSize(new Dimension(22, 22));
        collapse.setMaximumSize(new Dimension(22, 22));
        collapse.setToolTipText("Collapse all Cybernodes");
        collapse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = treeTable.getRowCount() - 1;
                while (row >= 0) {
                    treeTable.collapseRow(row);
                    row--;
                }
                expandAll = false;
            }
        });

        JButton expand = new JButton();
        expand.setIcon(expandIcon);
        expand.setPreferredSize(new Dimension(22, 22));
        expand.setMaximumSize(new Dimension(22, 22));
        expand.setToolTipText("Expand all Cybernodes");
        expand.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = 0;
                while (row < treeTable.getRowCount()) {
                    treeTable.expandRow(row);
                    row++;
                }
                expandAll = true;
            }
        });

        final JButton refresh = new JButton(refreshIcon);
        refresh.setPreferredSize(new Dimension(22, 22));
        refresh.setMaximumSize(new Dimension(22, 22));
        refresh.getAccessibleContext().setAccessibleName("refresh utilization values");
        refresh.setToolTipText("Refresh the utilization values");
        
        toolBar.add(collapse);
        toolBar.add(expand);
        toolBar.add(refresh);
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                graphViewAdapter.refreshCybernodeTable();
            }
        });


        JScrollPane sp2 = new JScrollPane(treeTable);
        sp2.getViewport().setBackground(Color.WHITE);
        statusPanel = new StatusPanel(utilizationModel);
        statusPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 8));

        String bannerIcon = props.getProperty("bannerIcon");
        if(bannerIcon!=null) {
            ImageIcon icon = Util.getImageIcon(bannerIcon);
            JLabel l = new JLabel(icon);
            //l.setBorder(BorderFactory.createEtchedBorder());
            statusPanel.add(l, BorderLayout.NORTH);
        }
        statusPanel.add(toolBar, BorderLayout.NORTH);
        
        //statusPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 70));
        statusPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        add(statusPanel, BorderLayout.NORTH);
        add(sp2, BorderLayout.CENTER);
    }

    public void setSelectedColumns(String... selectedColumns) {
        java.util.List<TableColumn> currentCols = new ArrayList<TableColumn>();
        for(Enumeration<TableColumn> cols =
                treeTable.getColumnModel().getColumns(); cols.hasMoreElements();) {
            TableColumn col = cols.nextElement();
            currentCols.add(col);
        }

        for(TableColumn col : currentCols) {
            if(!col.getHeaderValue().equals(ColumnValueHelper.FIXED_COLUMN))
                treeTable.getColumnModel().removeColumn(col);
        }
        for(String col : selectedColumns) {
            insertColumn(col, treeTable.getColumnCount());
        }
    }

    private List<String> getColumns(String... column) {
        List<String> columns = new ArrayList<String>();
        columns.add(ColumnValueHelper.FIXED_COLUMN);
        Collections.addAll(columns, column);
        return columns;
    }

    private UtilizationTreeModel createModel(List<String> columns) {
        if(utilizationModel==null) {
            utilizationModel = new UtilizationTreeModel(new RootNode(), columns, treeTable);
            utilizationModel.setExpandAll(expandAll);
        } else {
            utilizationModel.setColumnIdentifiers(columns);
        }
        return utilizationModel;
    }

    private TableColumn insertColumn(String headerLabel, int vColIndex) {
        TableColumn col = betterAddColumn(treeTable, headerLabel);
        treeTable.moveColumn(treeTable.getColumnCount()-1, vColIndex);
        return col;
    }

    private TableColumn betterAddColumn(JXTreeTable table, String headerLabel) {
        TableColumn col = new TableColumn(treeTable.getColumnCount());

        // Ensure that auto-create is off
        if (table.getAutoCreateColumnsFromModel()) {
            throw new IllegalStateException();
        }
        col.setHeaderValue(headerLabel);
        table.addColumn(col);
        return col;
    }

    public boolean getExpandAll() {
        return expandAll;
    }

    public int getCount() {
        return treeTable.getRowCount();
    }

    public void addCybernode(final ServiceItem item,
                             final CybernodeAdmin admin,
                             final ComputeResourceUtilization cru,
                             final UtilizationColumnManager utilizationColumnManager) {
        CybernodeNode node = new CybernodeNode(item,
                                               admin,
                                               cru,
                                               new ColumnValueHelper(utilizationColumnManager, treeTable));
        utilizationModel.addCybernodeNode(node);
        statusPanel.repaint();
    }

    public void removeCybernode(Cybernode item) {
        utilizationModel.removeCybernode(item);
        statusPanel.repaint();
    }    

    public void update(final ServiceItem item,
                       final CybernodeAdmin admin,
                       final ComputeResourceUtilization cru,
                       final UtilizationColumnManager utilizationColumnManager) {
        CybernodeNode node = new CybernodeNode(item,
                                               admin,
                                               cru,
                                               new ColumnValueHelper(utilizationColumnManager, treeTable));
        utilizationModel.updateCybernode(node);
    }

    public void updateCybernodesAt(final String hostAddress) {
        utilizationModel.updateCybernodesAt(hostAddress);
    }

    /**
     * Renderer for nodes that use a JLabel
     */
    class JLabelCellRenderer extends DefaultTableCellRenderer {
        final JLabel label;
        public JLabelCellRenderer(JLabel label) {
            super();
            this.label = label;

        }
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            setValue(value);
            return label;
        }

    }

    class ServiceItemAccessor {
        TreeTableNode node;

        ServiceItemAccessor(TreeTableNode node) {
            this.node = node;
        }

        ServiceItem getServiceItem() {
            ServiceItem item;
            if(node instanceof CybernodeNode) {
                item = ((CybernodeNode)node).getServiceItem();
            } else {
                item = graphViewAdapter.getServiceItem(((ServiceNode)node).getServiceElement(),
                                                       ((ServiceNode)node).getUuid());
            }
            return item;
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
}
