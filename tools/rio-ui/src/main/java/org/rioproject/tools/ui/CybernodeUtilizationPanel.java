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

import net.jini.core.lookup.ServiceItem;
import org.rioproject.core.provision.ServiceRecord;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.resources.ui.Util;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.measurable.cpu.CpuUtilization;
import org.rioproject.system.measurable.disk.DiskSpaceUtilization;
import org.rioproject.system.measurable.memory.ProcessMemoryUtilization;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;
import org.rioproject.tools.ui.serviceui.ServiceAdminManager;
import org.rioproject.tools.ui.treetable.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The CybernodeUtilizationPanel displays graphed and tabular information about
 * Cybernode Utilization
 *
 * @author Dennis Reedy
 */
public class CybernodeUtilizationPanel extends JPanel {
    /**
     * The model for the Cybernode utilization table
     */
    private final UtilizationTreeModel utilizationModel;
    private final JTreeTable treeTable;
    /**
     * For showing cybernode stats
     */
    private JPanel statusPanel;
    private double uHigh = 0;
    //private String uHighAt = "?";
    private double cpuHigh = 0;
    //private String cpuHighAt = "?";
    private final ServiceAdminManager adminManager = ServiceAdminManager.getInstance();
    /* a reference to 'self' */
    private JPanel component;
    private boolean expandAll = false;
    private ColorManager colorManager;
    private final Executor updateHandler = Executors.newSingleThreadExecutor();
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("/");
    public static final Font COMMON_FONT = new Font("Lucida Grande", 0, 10);
    private ImageIcon arrowUpIcon;
    private ImageIcon arrowDownIcon;
    private boolean autoSort;
    private static final double KB = 1024;
    //private static final double MB = Math.pow(KB, 2);
    private static final double GB = Math.pow(KB, 3);
    private TableCellRenderer iconHeaderRenderer;
    private final GraphViewAdapter graphViewAdapter;

    public CybernodeUtilizationPanel(final GraphViewAdapter graphViewAdapter,
                                     ColorManager colorManager,
                                     String[] selectedColumns,
                                     Properties props) {
        super(new BorderLayout(8, 8));
        this.colorManager= colorManager;
        this.graphViewAdapter = graphViewAdapter;
        String s = props.getProperty(Constants.TREE_TABLE_AUTO_EXPAND);
        expandAll = (s != null && Boolean.parseBoolean(s));

        s = props.getProperty(Constants.TREE_TABLE_AUTO_SORT);
        autoSort = (s != null && Boolean.parseBoolean(s));

        String columnToSort =
            props.getProperty(Constants.TREE_TABLE_SORTED_COLUMN_NAME,
                              "Utilization");

        utilizationModel = new UtilizationTreeModel(root);
        utilizationModel.tableComparator.columnName = columnToSort;

        treeTable = new UtilizationTreeTable(utilizationModel);
        treeTable.getTree().setRootVisible(false);
        treeTable.getTree().setShowsRootHandles(true);
        treeTable.setAutoCreateColumnsFromModel(false);
        treeTable.getTableHeader().setReorderingAllowed(false);
        //no icons
        DefaultTreeCellRenderer treeRenderer =
            ((DefaultTreeCellRenderer) treeTable.getTree().getCellRenderer());
        treeRenderer.setLeafIcon(null);
        treeRenderer.setOpenIcon(null);
        treeRenderer.setClosedIcon(null);
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
                CybernodeNode cNode = utilizationModel.getCybernodeCRU(row);
                ServiceNode sNode = null;
                String label;
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
                final ServiceItemAccessor serviceItemAccessor =
                    new ServiceItemAccessor(cNode==null?sNode:cNode);
                JPopupMenu popup = new JPopupMenu();
                JMenuItem serviceUI = new JMenuItem("Show "+label+" UI");
                serviceUI.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        adminManager.doShowAdminUI(serviceItemAccessor.getServiceItem());
                    }
                });

                JMenuItem jconsole = null;
                if(showJConsoleOption) {
                    jconsole = new JMenuItem("Launch JConsole");
                    jconsole.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            String jmxConn = JMXUtil.getJMXConnection(
                                serviceItemAccessor.getServiceItem().attributeSets);
                            try {
                                Runtime.getRuntime().exec("jconsole "+jmxConn);
                            } catch (IOException e1) {
                                Util.showError(e1, component, "Creating jconsole");
                            }
                        }
                    });
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
                if (e.getClickCount() == 2) {
                    int row = treeTable.getSelectedRow();
                    if (row == -1)
                        return;
                    DefaultMutableTreeNode node = utilizationModel.getNode(row);
                    if(node instanceof ServiceNode) {
                        ServiceItem item =
                            graphViewAdapter.getServiceItem(
                                ((ServiceNode)node).getServiceElement(),
                                ((ServiceNode)node).getUuid());
                        if(item!=null)
                            adminManager.doShowAdminUI(item);
                    }                    
                }
            }
        });

        treeTable.setColumnSelectionAllowed(false);
        //treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        treeTable.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() <= 2) {
                    int ndx = treeTable.columnAtPoint(new Point(e.getX(),
                                                                e.getY()));
                    String columnName = utilizationModel.getColumnName(ndx);
                    utilizationModel.tableComparator.setColumnToSort(
                        columnName);
                    utilizationModel.sortTable();
                    e.getComponent().repaint();                    
                }
            }
        });

        /* Scale the up & down arrows from their original size of
         * 15 X 8 to 10 x 5, they look better as directional arrows
         * in the table header */
        arrowUpIcon =
            Util.getScaledImageIcon("org/rioproject/tools/ui/images/arrow-up.png",
                                    10, 5);
        arrowDownIcon =
            Util.getScaledImageIcon("org/rioproject/tools/ui/images/arrow-dn.png",
                                    10, 5);

        /* Renders sorting direction arrows */
        iconHeaderRenderer = new TCR();

        /* Set first column renderer */
        TableColumn tc = treeTable.getColumnModel().getColumn(0);
        tc.setHeaderRenderer (iconHeaderRenderer);

        setSelectedColumns(selectedColumns);                               

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorderPainted(false);
        ImageIcon collapseIcon =
            Util.getImageIcon("org/rioproject/tools/ui/images/collapseall.gif");
        ImageIcon expandIcon =
            Util.getImageIcon("org/rioproject/tools/ui/images/expandall.gif");
        ImageIcon refreshIcon =
            Util.getScaledImageIcon("org/rioproject/tools/ui/images/view-refresh.png",
                                    expandIcon.getIconWidth(),
                                    expandIcon.getIconHeight());
        JButton collapse = new JButton();
        collapse.setIcon(collapseIcon);
        collapse.setPreferredSize(new Dimension(22, 22));
        collapse.setMaximumSize(new Dimension(22, 22));
        collapse.setToolTipText("Collapse all Cybernodes");
        collapse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = treeTable.getTree().getRowCount() - 1;
                while (row >= 0) {
                    treeTable.getTree().collapseRow(row);
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
                while (row < treeTable.getTree().getRowCount()) {
                    treeTable.getTree().expandRow(row);
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
        statusPanel = createStatusPanel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 0));

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

    public void setSelectedColumns(String[] selectedColumns) {
        java.util.List<TableColumn> currentCols = new ArrayList<TableColumn>();
        for(Enumeration<TableColumn> cols =
                treeTable.getColumnModel().getColumns(); cols.hasMoreElements();) {
            TableColumn col = cols.nextElement();
            currentCols.add(col);
        }

        for(TableColumn col : currentCols) {
            if(!col.getHeaderValue().equals(UtilizationTreeModel.FIXED_COLUMN))
                treeTable.getColumnModel().removeColumn(col);
        }
        for(String col : selectedColumns) {
            TableColumn tc = insertColumn(col, treeTable.getColumnCount());
            tc.setHeaderRenderer (iconHeaderRenderer);
        }

        ((DefaultTableModel)treeTable.getModel()).fireTableDataChanged();
    }

    TableColumn insertColumn(String headerLabel, int vColIndex) {
        TableColumn col = betterAddColumn(treeTable, headerLabel);
        treeTable.moveColumn(treeTable.getColumnCount()-1, vColIndex);
        return col;
    }

    TableColumn betterAddColumn(JTable table, String headerLabel) {
        DefaultTableModel model = (DefaultTableModel)table.getModel();
        TableColumn col = new TableColumn(model.getColumnCount());

        // Ensure that auto-create is off
        if (table.getAutoCreateColumnsFromModel()) {
            throw new IllegalStateException();
        }
        col.setHeaderValue(headerLabel);
        table.addColumn(col);
        model.addColumn(headerLabel);
        return col;
    }

    public boolean getExpandAll() {
        return expandAll;
    }

    public void setAutoSort(boolean autoSort) {
        this.autoSort = autoSort;
    }

    public boolean getAutoSort() {
        return autoSort;
    }

    public String getSortedColumnName() {
        return utilizationModel.tableComparator.columnName;
    }

    /*public boolean getSortAscending() {
        return utilizationModel.tableComparator.ascending;
    }*/

    public void sortTable() {
        utilizationModel.sortTable();
    }

    public int getCount() {
        return treeTable.getRowCount();
    }

    class UtilizationTreeTable extends JTreeTable {
        public UtilizationTreeTable(TreeTableModel model) {
            super(model, colorManager, graphViewAdapter);
        }

        public Component prepareRenderer(TableCellRenderer renderer,
                                         int row,
                                         int column) {
            Component component = super.prepareRenderer(renderer, row, column);
            int[] selectedRows = treeTable.getTree().getSelectionRows();
            if (selectedRows != null) {
                for (int selectedRow : selectedRows) {
                    if (row == selectedRow) {
                        component.setForeground(UIManager.getColor(
                            "Table.focusCellForeground"));
                        return component;
                    }
                }
            }

            JTree tree = getTree();
            if(tree!=null) {
                 
                TreePath path = tree.getPathForRow(row);
                if(path!=null) {
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode)path.getLastPathComponent();
                    component.setBackground(Util.getRowColor(root,
                                                             node,
                                                             getTree(),
                                                             getBackground(),
                                                             colorManager.getAltRowColor()));
                    CRUNode cruNode = utilizationModel.getCRUNode(row);
                    if (column > 0 && cruNode != null &&
                        cruNode.getComputeResourceUtilization()!= null) {
                        ComputeResourceUtilization cru =
                                cruNode.getComputeResourceUtilization();
                        //component.setForeground(new Color(0, 100, 0));
                        String columnName = getColumnName(column);
                        if(columnName==null)
                            return null;
                        
                        if(columnName.equals(Constants.UTIL_PERCENT_CPU)      ||
                           columnName.equals(Constants.UTIL_PERCENT_MEMORY)   ||
                           columnName.equals(Constants.UTIL_PERCENT_DISK)     ||
                           columnName.equals(Constants.UTIL_PERCENT_CPU_PROC) ||
                           columnName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
                            component.setForeground(new Color(0, 100, 0));

                            MeasuredResource mRes = getMeasuredResource(columnName,
                                                                        cru);
                            if(mRes!=null && mRes.thresholdCrossed()) {
                                component.setForeground(new Color(178, 34, 34));
                                component.setFont(component.getFont().deriveFont(Font.BOLD));
                            }
                        } else {
                            component.setForeground(Color.black);
                        }

                    } else {
                        component.setForeground(Color.black);
                    }
                }
            }            
            return component;
        }

        /*
         * Returns the appropriate background color for the given row.
         */
        //protected Color colorForRow(int row) {
        //    return (row % 2 == 0) ? Color.LIGHT_GRAY : getBackground();
        //}

        MeasuredResource getMeasuredResource(String columnName,
                                             ComputeResourceUtilization cru) {
            MeasuredResource mRes = null;
            if(columnName.equals(Constants.UTIL_PERCENT_CPU)) {
                mRes = cru.getCpuUtilization();
            } else if(columnName.equals(Constants.UTIL_PERCENT_DISK)) {
                mRes = cru.getDiskSpaceUtilization();
            } else if(columnName.equals(Constants.UTIL_PERCENT_CPU_PROC)) {
                mRes = cru.getProcessCpuUtilization();
            } else if(columnName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
                mRes = cru.getProcessMemoryUtilization();
            }
            return mRes;
        }
    }

    private JPanel createStatusPanel() {
        final JPanel panel = new JPanel() {
            public void paint(final Graphics g) {
                super.paint(g);
                drawStatus(g);
            }
        };
        panel.setDoubleBuffered(true);
        return (panel);
    }

    /*
     * Draw status items
     */
    void drawStatus(Graphics g) {
        int LEFT_MARGIN = 10;
        int LINE_HEIGHT = 14;
        int BASE_LINE = 15;

        ComputeResourceUtilization[] crus = utilizationModel.getValues();
        int num = crus.length;

        //long uTime = System.currentTimeMillis();
        //long cpuTime = uTime;

        for (int i = 0; i < num; i++) {
            if (crus[i] == null)
                continue;
            double value = crus[i].getUtilization();
            if (value >= uHigh) {
                uHigh = value;
                //uHighAt = crus[i].getAddress();
                //uTime = System.currentTimeMillis();
            }
        }

        for (int i = 0; i < num; i++) {
            if (crus[i] == null)
                continue;
            double value = getMeasuredValue("CPU", crus[i]);
            if (value >= cpuHigh) {
                cpuHigh = value;
                //cpuHighAt = crus[i].getAddress();
                //cpuTime = System.currentTimeMillis();
            }
        }

        Font defaultFont = COMMON_FONT;
        FontMetrics fontMetrics = g.getFontMetrics(defaultFont);
        /* Get the length of the longest string*/
        int sLen = fontMetrics.stringWidth("Highest Utilization: ");
        g.setColor(Color.BLACK);
        g.setFont(defaultFont);
        int totalCybernodes = utilizationModel.getCybernodeCount();
        int inUse = utilizationModel.getNumInUse();
        int free = totalCybernodes - inUse;
        g.drawString("Total Cybernodes: ",
                     LEFT_MARGIN,
                     BASE_LINE);
        g.drawString("" + totalCybernodes,
                     LEFT_MARGIN + sLen,
                     BASE_LINE);

        g.drawString("Cybernodes in use: ",
                     LEFT_MARGIN,
                     BASE_LINE + LINE_HEIGHT);
        g.drawString(inUse + ",   Cybernodes Free: " + free,
                     LEFT_MARGIN + sLen,
                     BASE_LINE + LINE_HEIGHT);
        /*
        NumberFormat percentFormatter = NumberFormat.getPercentInstance();
        percentFormatter.setMaximumFractionDigits(2);

        g.drawString("Highest Utilization:",
                     LEFT_MARGIN,
                     BASE_LINE + (LINE_HEIGHT * 2));
        g.drawString(percentFormatter.format(uHigh) +
                     ",  on " + uHighAt + ",  at " +
                     new java.util.Date(uTime).toString(),
                     LEFT_MARGIN + sLen,
                     BASE_LINE + (LINE_HEIGHT * 2));

        g.drawString("Highest CPU:",
                     LEFT_MARGIN,
                     BASE_LINE + (LINE_HEIGHT * 3));
        g.drawString(percentFormatter.format(cpuHigh) +
                     ",  on " + cpuHighAt + ",  at " + new java.util.Date(
            cpuTime).toString(),
                     LEFT_MARGIN + sLen,
                     BASE_LINE + (LINE_HEIGHT * 3));
                     */
    }

    public void addCybernode(CybernodeNode item) {
        utilizationModel.addItem(item);
        statusPanel.repaint();
    }

    public void removeCybernode(Cybernode item) {
        utilizationModel.removeItem(item);
        statusPanel.repaint();
    }    

    public void update(CybernodeNode item) {
        int row = utilizationModel.getItemRow(item.getCybernode());
        if (row != -1) {
            utilizationModel.setValueAt(item, row);
        } else {
            System.err.println("Could not update Cybernode at ["+item+"], " +
                               "unable to find table row");
        }
    }

    public void updateCybernodesAt(final String hostAddress) {
        updateHandler.execute(new Runnable() {
            List<CybernodeNode> nodes = new ArrayList<CybernodeNode>();
            public void run() {
                for(int i=0; i<utilizationModel.getRoot().getChildCount(); i++) {
                    nodes.add((CybernodeNode)utilizationModel.getChild(
                        utilizationModel.getRoot(), i));
                }

                TreePath selectionPath = treeTable.getTree().getSelectionPath();
                for(CybernodeNode node : nodes) {
                    if(node.getHostName().equals(hostAddress)) {
                        utilizationModel.setServices(node);
                    }
                }
                if(selectionPath!=null) {
                    treeTable.getTree().setSelectionPath(selectionPath);
                }
            }
        });
    }

    /**
     * The UtilizationModel extends AbstractTableModel providing the model to
     * display usage information on Cybernodes
     */
    class UtilizationTreeModel extends AbstractTreeTableModel {
        final UtilizationComparator tableComparator = new UtilizationComparator();
        final static String FIXED_COLUMN = "Host Name";
        NumberFormat percentFormatter;
        NumberFormat numberFormatter;


        public UtilizationTreeModel(DefaultMutableTreeNode root) {
            super(root);
            percentFormatter = NumberFormat.getPercentInstance();
            numberFormatter = NumberFormat.getNumberInstance();
            /* Display 3 digits of precision */
            percentFormatter.setMaximumFractionDigits(3);
            /* Display 2 digits of precision */
            numberFormatter.setGroupingUsed(false);
            numberFormatter.setMaximumFractionDigits(2);
        }

        public boolean isCellEditable(Object node, int column) {
            return column == 0;
        }

        public Class getColumnClass(int column) {
            return (column == 0 ? TreeTableModel.class : String.class);
        }

        public Object getValueAt(Object node, int columnIndex) {
            if (node instanceof ServiceNode) {
                ServiceNode sn = (ServiceNode)node;
                if (columnIndex > 0) {

                    if(sn.getComputeResourceUtilization()!=null) {
                        ComputeResourceUtilization cru =
                            sn.getComputeResourceUtilization();
                        return getColumnValue(columnIndex, cru, false);
                    } else {
                        return "";
                    }
                }
                return node.toString();
            }
            String value;
            if(node!=null) {
                CybernodeNode c = (CybernodeNode) node;
                if(columnIndex==0) {
                    value =  c.getHostName();
                } else {
                    ComputeResourceUtilization cru = c.getComputeResourceUtilization();
                    value =  getColumnValue(columnIndex,  cru, true);
                }
            } else {
                value = "";
            }
            return value;
        }

        String getColumnValue(int columnIndex,
                              ComputeResourceUtilization cru,
                              boolean includeSystemInfo) {
            String cName = getColumnName(columnIndex);

            if(cName==null) {
                System.out.println(">> FAILED!!!, getColumnValue for index ("+columnIndex+")");
                return null;
            }
            String value = null;

            if(cru==null)
                return "?";

            if(includeSystemInfo) {
                if(cName.equals(Constants.UTIL_PERCENT_CPU)) {
                    CpuUtilization cpu = cru.getCpuUtilization();
                    value = (cpu == null ? "?" : formatPercent(cpu.getValue()));

                } else if(cName.equals(Constants.UTIL_PERCENT_MEMORY)) {
                    SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                    value = (mem == null ? "?" : formatPercent(mem.getValue()));

                } else if(cName.equals(Constants.UTIL_TOTAL_MEMORY)) {
                    SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                    value = (mem == null ? "?" : format(mem.getTotal()," MB"));

                } else if(cName.equals(Constants.UTIL_FREE_MEMORY)) {
                    SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                    value = (mem == null ? "?" : format(mem.getFree()," MB"));

                } else if(cName.equals(Constants.UTIL_USED_MEMORY)) {
                    SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                    value = (mem == null ? "?" : format(mem.getUsed()," MB"));

                } else if(cName.equals(Constants.UTIL_PERCENT_DISK)) {
                    DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
                    value = (disk==null?"?" : formatPercent(disk.getValue()));

                } else if(cName.equals(Constants.UTIL_AVAIL_DISK)) {
                    DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
                    value = (disk==null?"?" : format(disk.getAvailable()/GB," GB"));

                } else if(cName.equals(Constants.UTIL_TOTAL_DISK)) {
                    DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
                    value = (disk==null?"?" : format(disk.getCapacity()/GB," GB"));
                }
            }

            if(cName.equals(Constants.UTIL_PERCENT_CPU_PROC)) {
                value = formatPercent(getMeasuredValue(SystemWatchID.PROC_CPU, cru));

            } else if(cName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
                value = formatPercent(getMeasuredValue(SystemWatchID.JVM_MEMORY, cru));
                //ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
                //value = (mem==null?"?" : format(mem.getUsedHeap()));

            } else if(cName.equals(Constants.UTIL_HEAP_MEM_JVM)) {
                ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
                //value = (mem==null?"?" : format(mem.getCommittedHeap())+" MB");
                value = (mem==null?"?" : format(mem.getUsedHeap()," MB"));

            } else if(cName.equals(Constants.UTIL_HEAP_MEM_AVAIL)) {
                ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
                //value = (mem==null?"?" : format(mem.getCommittedHeap())+" MB");
                value = (mem==null?"?" : format(mem.getCommittedHeap(), " MB"));

            } else if(cName.equals(Constants.UTIL_REAL_MEM_PROC)) {
                ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
                value = (mem==null?"?" : format(mem.getResident(), " MB"));
            }

            return (value);
        }

        String formatPercent(Double value) {
            if (value != null && !Double.isNaN(value))
                return (percentFormatter.format(value.doubleValue()));
            return ("?");
        }

        String format(Double value, String units) {
            if (value != null && !Double.isNaN(value) && value!=-1)
                return (numberFormatter.format(value.doubleValue())+units);
            return ("?");
        }


        ComputeResourceUtilization[] getValues() {
            ComputeResourceUtilization[] crus =
                new ComputeResourceUtilization[getRoot().getChildCount()];
            for (int i = 0; i < root.getChildCount(); i++) {
                crus[i] = ((CybernodeNode) root.getChildAt(i)).getComputeResourceUtilization();
            }
            return (crus);
        }

        int getNumInUse() {
            int inUse = 0;
            for (int i = 0; i < root.getChildCount(); i++) {
                CybernodeNode node = (CybernodeNode) root.getChildAt(i);
                try {
                    Integer num =
                        ((CybernodeAdmin) node.getCybernode().getAdmin()).
                            getServiceCount();
                    if (num > 0)
                        inUse++;
                } catch (Throwable t) {
                    t.printStackTrace();
                    //if(!ThrowableUtil.isRetryable(t)) {
                    //    removeCybernode(node.cybernode);
                    //}
                }
            }
            return (inUse);
        }

        private synchronized void addItem(CybernodeNode item) {
            insertNodeInto(item, root, root.getChildCount());
            treeTable.getTree().makeVisible(new TreePath(item.getPath()));
            setServices(item);
            if(autoSort)
                sortTable();
        }

        void removeItem(Cybernode item) {
            for (int i = 0; i < root.getChildCount(); i++) {
                CybernodeNode node = (CybernodeNode) root.getChildAt(i);
                if (item.equals(node.getCybernode())) {
                    removeNodeFromParent(node);
                    nodeStructureChanged(root);
                    if(autoSort)
                        sortTable();
                    break;
                }
            }
        }

        int getItemRow(Cybernode item) {
            int rowCounter = 0;
            for (int i = 0; i < root.getChildCount(); i++) {
                CybernodeNode node = (CybernodeNode) root.getChildAt(i);
                if (item.equals(node.getCybernode())) {
                    return (rowCounter);
                }
                rowCounter++;
                for (int j = 0; j < node.getChildCount(); j++) {
                    DefaultMutableTreeNode t =
                        (DefaultMutableTreeNode) node.getChildAt(j);
                    if (treeTable.getTree().isVisible(new TreePath(t.getPath()))) {
                        rowCounter++;
                    }
                }
            }
            return (-1);
        }

        ServiceNode getServiceNode(int row) {
            DefaultMutableTreeNode t = getNode(row);
            return (t instanceof ServiceNode ? (ServiceNode)t : null);
        }

        public CybernodeNode getCybernodeCRU(int row) {
            DefaultMutableTreeNode t = getNode(row);
            return (t instanceof CybernodeNode ? (CybernodeNode)t : null);
        }

        public CRUNode getCRUNode(int row) {
            DefaultMutableTreeNode t = getNode(row);
            return (t instanceof CRUNode ?
                    (CRUNode)t : null);
        }

        public DefaultMutableTreeNode getNode(int row) {
            int rowCounter = 0;
            DefaultMutableTreeNode node = null;

            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode)root.getChildAt(i);
                if (rowCounter == row) {
                    node = tn;
                    break;
                }
                rowCounter++;
                for (int j = 0; j < tn.getChildCount(); j++) {
                    DefaultMutableTreeNode t =
                        (DefaultMutableTreeNode) tn.getChildAt(j);
                    if (treeTable.getTree().isVisible(new TreePath(t.getPath()))) {
                        if (rowCounter == row) {
                            node = t;
                            break;
                        }
                        rowCounter++;
                    }
                }

                if(node!=null)
                    break;
            }
            return node;
        }

        public int getColumnCount() {
            return (treeTable==null?1:
                    treeTable.getColumnModel().getColumnCount());
            //return (columnNames.length);
        }

        public int getCybernodeCount() {
            return (root.getChildCount());
        }

        public String getColumnName(int index) {
            if(index==0)
                return FIXED_COLUMN;
            String cName = null;
            if(index < treeTable.getColumnModel().getColumnCount()) {
                TableColumn column = treeTable.getColumnModel().getColumn(index);
                cName = (String)column.getHeaderValue();
            }
            return cName;
        }

        public void setValueAt(Object item, int row) {
            CybernodeNode node = getCybernodeCRU(row);
            TreePath selectionPath = treeTable.getTree().getSelectionPath();
            if (node != null) {
                node.setComputeResourceUtilization(
                    ((CybernodeNode)item).getComputeResourceUtilization());
                setServices(node);
                nodeChanged(node);
            }

            if(selectionPath!=null) {
                treeTable.getTree().setSelectionPath(selectionPath);
            }
            if(autoSort)
                sortTable();
        }

        @SuppressWarnings("unchecked")
        void sortTable() {
            Map<CybernodeNode, Boolean> expandMap = new HashMap<CybernodeNode, Boolean>();
            ArrayList<CybernodeNode> children = Collections.list(root.children());
            for (CybernodeNode child : children) {
                expandMap.put(child,
                              treeTable.getTree().isExpanded(
                                  new TreePath(child.getPath())));

            }

            ArrayList<CybernodeNode> sortedChildren =
                new ArrayList<CybernodeNode>(children);
            Collections.sort(sortedChildren, tableComparator);
            if(sortedChildren.equals(children))
                return;
            root.removeAllChildren();
            for (CybernodeNode child : sortedChildren) {
                root.add(child);
            }
            nodeStructureChanged(root);
            children = Collections.list(root.children());
            for (CybernodeNode child : children) {
                if(expandMap.get(child))
                    treeTable.getTree().expandPath(new TreePath(child.getPath()));
            }
        }

        private void setServices(CybernodeNode node) {
            java.util.List<ServiceRecord> serviceList = new ArrayList<ServiceRecord>();
            try {
                ServiceRecord[] records =
                    node.getCybernode()
                        .getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD);
                serviceList.addAll(Arrays.asList(records));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            List<ServiceNode> removals = new ArrayList<ServiceNode>();
            ServiceRecord[] cybernodeServices =
                serviceList.toArray(new ServiceRecord[serviceList.size()]);
            
            for (int i = 0; i < node.getChildCount(); i++) {
                boolean found = false;
                ServiceNode sNode = (ServiceNode) node.getChildAt(i);
                for (ServiceRecord record : cybernodeServices) {
                    if (sNode.getName().equals(record.getServiceElement().getName())) {
                        if(serviceList.remove(record)) {
                            found = true;
                            setServiceNodeUtilization(sNode, node.getAdmin());
                            break;
                        }
                    }
                }
                if (!found)
                    removals.add(sNode);
            }

            /* Remove services */
            for (ServiceNode sNode : removals) {
                utilizationModel.removeNodeFromParent(sNode);
            }

            /* Add new services */
            for (ServiceRecord record : serviceList) {
                ServiceNode sNode = new ServiceNode(record);
                setServiceNodeUtilization(sNode, node.getAdmin());
                utilizationModel.insertNodeInto(sNode, node, node.getChildCount());
                if(expandAll) {
                    treeTable.getTree().makeVisible(new TreePath(sNode.getPath()));
                }
            }
        }

        /*
         * set the compute resource utilization for a ServiceNode
         */
        void setServiceNodeUtilization(ServiceNode sNode, CybernodeAdmin cAdmin) {
            if(sNode.isForked()) {
                try {
                    ComputeResourceUtilization util =
                        cAdmin.getComputeResourceUtilization(sNode.getUuid());
                    sNode.setComputeResourceUtilization(util);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * A Comparator for the UtilizationTable. Note: this comparator imposes
     * orderings that are inconsistent with equals.
     */
    class UtilizationComparator implements Comparator<CybernodeNode> {
        String columnName;
        boolean ascending = false;

        void setColumnToSort(String columnName) {
            ascending = this.columnName.equals(columnName) && !ascending;
            this.columnName = columnName;
        }

        public int compare(CybernodeNode item1,
                           CybernodeNode item2) {
            int order;
            ComputeResourceUtilization cru1 = item1.getComputeResourceUtilization();
            ComputeResourceUtilization cru2 = item2.getComputeResourceUtilization();
            if (columnName.equals("Utilization")) {
                order = getOrder(cru1.compareTo(cru2));
            } else if (columnName.equals("Host Name")) {
                String hn1 = cru1.getHostName();
                String hn2 = cru2.getHostName();
                order = getOrder(hn1.compareTo(hn2));
            } else  {
                Double d1 = getMeasuredResourceValue(columnName, cru1);
                Double d2 = getMeasuredResourceValue(columnName, cru2);
                order = getOrder(d1.compareTo(d2));
            }
            return (order);
        }

        int getOrder(int result) {
            int order = 0;
            if (!ascending) {
                if (result < 0)
                    order = 1;
                else if (result > 0)
                    order = -1;
            } else {
                order = result;
            }
            return (order);
        }

    }

    private Double getMeasuredValue(String label,
                                    ComputeResourceUtilization cru) {
        Double value = Double.NaN;
        for (MeasuredResource mRes : cru.getMeasuredResources()) {
            if (mRes.getIdentifier().equals(label)) {
                value = mRes.getValue();
                break;
            }
        }
        return value;
    }

    private double getMeasuredResourceValue(String cName,
                                            ComputeResourceUtilization cru) {
        double value = 0;
        if(cName.equals(Constants.UTIL_PERCENT_CPU)) {
            CpuUtilization cpu = cru.getCpuUtilization();
            value = (cpu == null ? 0 : cpu.getValue());

        } else if(cName.equals(Constants.UTIL_PERCENT_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getValue());

        } else if(cName.equals(Constants.UTIL_TOTAL_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getTotal());

        } else if(cName.equals(Constants.UTIL_FREE_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getFree());

        } else if(cName.equals(Constants.UTIL_USED_MEMORY)) {
            SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
            value = (mem == null ? 0 : mem.getUsed());

        } else if(cName.equals(Constants.UTIL_PERCENT_DISK)) {
            DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
            value = (disk==null?0 : disk.getValue());

        } else if(cName.equals(Constants.UTIL_AVAIL_DISK)) {
            DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
            value = (disk==null?0 : disk.getAvailable()/GB);

        } else if(cName.equals(Constants.UTIL_TOTAL_DISK)) {
            DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
            value = (disk==null?0 : disk.getCapacity()/GB);

        } else if(cName.equals(Constants.UTIL_PERCENT_CPU_PROC)) {
            value = getMeasuredValue("CPU (JVM)", cru);

        } else if(cName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
            value = getMeasuredValue("Memory", cru);

        } else if(cName.equals(Constants.UTIL_HEAP_MEM_JVM)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            value = (mem==null?0 : mem.getUsedHeap());

        } else if(cName.equals(Constants.UTIL_HEAP_MEM_AVAIL)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            value = (mem==null?0 : mem.getCommittedHeap());

        } else if(cName.equals(Constants.UTIL_REAL_MEM_PROC)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            value = (mem==null?0 : mem.getResident());
        }

        return value;
    }

    /**
     * Renderer to draw sort direction icons
     */
    class TCR extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            /* Inherit the colors and font from the header component */
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(header.getFont());
                }
            }

            setHorizontalTextPosition(SwingConstants.LEFT);
            if (value.equals(utilizationModel.tableComparator.columnName)) {
                setIcon((utilizationModel.tableComparator.ascending ?
                         arrowUpIcon:arrowDownIcon));
                setText(value.toString());
            } else {
                setText(value.toString());
                setIcon(null);
            }
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    }


    class ServiceItemAccessor {
        DefaultMutableTreeNode node;

        ServiceItemAccessor(DefaultMutableTreeNode node) {
            this.node = node;
        }

        ServiceItem getServiceItem() {
            ServiceItem item;
            if(node instanceof CybernodeNode) {
                item = ((CybernodeNode)node).getServiceItem();
            } else {
                item = graphViewAdapter.getServiceItem(
                    ((ServiceNode)node).getServiceElement(),
                    ((ServiceNode)node).getUuid());
            }
            return item;
        }
    }
}
