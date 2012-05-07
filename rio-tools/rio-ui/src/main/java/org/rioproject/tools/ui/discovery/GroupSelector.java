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
package org.rioproject.tools.ui.discovery;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.tools.discovery.RecordingDiscoveryListener;
import org.rioproject.tools.discovery.ReggieStat;

/**
 * Discovery group selector
 */
public class GroupSelector extends JPanel {
    private JTable groupTable;
    private GroupModel model;

    public GroupSelector(final DiscoveryManagement dMgr, final RecordingDiscoveryListener rdl, final JDialog dialog) {
        super(new BorderLayout(8, 8));
        if(rdl==null)
            throw new IllegalArgumentException("rdl is null");

        groupTable = new JTable();
        JButton okay = new JButton();
        JButton apply = new JButton();
        JButton dismiss = new JButton();
        JButton refresh = new JButton();

        okay.setText("OK");
        okay.setToolTipText("Apply choices and close the dialog");
        apply.setText("Apply");
        apply.setToolTipText("Apply choices");
        dismiss.setText("Close");
        dismiss.setToolTipText("Close the dialog");
        refresh.setText("Refresh");
        refresh.setToolTipText("Refresh list of groups");

        groupTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                int row = groupTable.getSelectedRow();
                GroupModel model = (GroupModel)groupTable.getModel();
                Boolean b = (Boolean)model.getValueAt(row, 0);
                GroupItem gi = (GroupItem)model.getItem(row);
                gi.include = !b;
                model.setValueAt(gi, row);
            }
        });

        model = new GroupModel();
        groupTable.setModel(model);
        groupTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn col = groupTable.getColumnModel().getColumn(0);
        col.setPreferredWidth(60);
        col.setMaxWidth(60);

        fillGroups(dMgr, rdl);

        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                fillGroups(dMgr, rdl);
            }
        });

        okay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setGroups(dMgr);
                dialog.dispose();
            }
        });
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setGroups(dMgr);
            }
        });
        dismiss.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                dialog.dispose();
            }
        });

        JPanel buttons = new JPanel();
        buttons.add(apply);
        buttons.add(okay);
        buttons.add(refresh);
        buttons.add(dismiss);

        JLabel groupsLabel = new JLabel();
        groupsLabel.setText("Advertised Groups Available:");
        add(groupsLabel, BorderLayout.NORTH);
        add(new JScrollPane(groupTable), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

    }

    private void fillGroups(DiscoveryManagement dMgr, RecordingDiscoveryListener rdl) {
        String[] configured = ((DiscoveryGroupManagement)dMgr).getGroups();
        
        boolean allGroups = false;
        if(configured == DiscoveryGroupManagement.ALL_GROUPS) {
            allGroups = true;
        }

        if(((DiscoveryGroupManagement)rdl.getDiscoveryManagement()).getGroups() != DiscoveryGroupManagement.NO_GROUPS) {
            ReggieStat[] rStats = rdl.getReggieStats(ReggieStat.DISCOVERED);
            List<String> list = new ArrayList<String>();
            for (ReggieStat rStat : rStats) {
                String[] groups = rStat.getGroups();
                for (String group1 : groups) {
                    String group = group1;
                    if (group==null)
                        group = "ALL_GROUPS";
                    if (list.contains(group))
                        continue;
                    list.add(group);
                }
            }
            for (String group : list) {
                GroupItem gi = new GroupItem(group);
                if (!allGroups) {
                    boolean include = false;
                    for (String aConfigured : configured) {
                        if (group.equals(aConfigured)) {
                            include = true;
                            break;
                        }
                    }
                    gi.include = include;
                }
                if (!model.hasItem(gi))
                    model.addItem(gi);
            }
        }
    }

    private void setGroups(final DiscoveryManagement dMgr) {
        final Runnable doSetGroup = new Runnable() {
            public void run() {
                try {
                    GroupModel model = (GroupModel)groupTable.getModel();
                    ((DiscoveryGroupManagement)dMgr).setGroups(
                        model.getSelectedGroups());
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread() {
            public void run() {
                try {
                    SwingUtilities.invokeAndWait(doSetGroup);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Get the dialog for the Group selector
     *
     * @param frame The parent frame
     * @param dMgr The DiscoveryManagement to use
     * @param rdl  The RecordingDiscoveryListener
     * @return The JDialog which will encapsulate the ReggieStatPanel
     */
    public static JDialog getDialog(JFrame frame, DiscoveryManagement dMgr, RecordingDiscoveryListener rdl) {
        JDialog dialog = new JDialog(frame, "Discovery Group Selection", true);
        GroupSelector gs = new GroupSelector(dMgr, rdl, dialog);
        dialog.getContentPane().add(gs);
        int width = 380;
        int height = 340;
        dialog.pack();
        dialog.setSize(width, height);
        //dialog.setResizable(false);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(frame);
        return (dialog);

    }

    class GroupModel extends AbstractTableModel {
        Vector<GroupItem> tableData = new Vector<GroupItem>();
        final String[] columnNames = {"Include", "Group Name"};

        public Object getValueAt(int index, int columnIndex) {
            try {
                GroupItem gi = tableData.elementAt(index);
                switch(columnIndex) {
                    case 0:
                        return (gi.include);
                    case 1:
                        return (gi.name);
                    default:
                        return (null);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            return (null);
        }

        public String[] getSelectedGroups() {
            List<String> list = new ArrayList<String>();
            GroupItem[] gis = getItems();
            for (GroupItem gi : gis) {
                if (gi.include)
                    list.add(gi.name);
            }
            return list.toArray(new String[list.size()]);
        }

        GroupItem[] getItems() {
            return tableData.toArray(new GroupItem[tableData.size()]);
        }

        boolean hasItem(GroupItem item) {
            return(tableData.contains(item));
        }

        public void addItem(GroupItem item) {
            int rowNum = tableData.size();
            tableData.insertElementAt(item, rowNum);
            fireTableRowsInserted(rowNum, rowNum);
        }

        public Object getItem(int row) {
            return (tableData.elementAt(row));
        }

        public int getColumnCount() {
            return (columnNames.length);
        }

        public int getRowCount() {
            return (tableData.size());
        }

        public String getColumnName(int column) {
            return (columnNames[column]);
        }

        public void setValueAt(GroupItem item, int rowNum) {
            tableData.setElementAt(item, rowNum);
            fireTableRowsUpdated(rowNum, rowNum);
        }

        public Class getColumnClass(int c) {
            if(c == 0)
                return (Boolean.class);
            return (super.getColumnClass(c));
        }

        public boolean isCellEditable(int row, int col) {
            return ((col == 0));
        }
    }

    class GroupItem {
        String name;
        Boolean include = true;

        GroupItem(String name) {
            this.name = name;
        }

        public int hashCode() {
            return(name.hashCode());
        }

        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass()) && (name.equals(((GroupItem) o).name));
        }

    }

}
