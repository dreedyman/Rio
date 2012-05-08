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
package org.rioproject.cybernode.ui;

import org.rioproject.deploy.ServiceRecord;
import org.rioproject.util.TimeUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;

/**
 * A table of services that a Cybernode has created
 *
 * @author Dennis Reedy
 */
public class ServiceTable extends JPanel {
    private JTable serviceTable;
    private ServiceTableModel dataModel;

    public ServiceTable(String title, boolean showPid) {
        super();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(),
            title));
        serviceTable = new JTable();
        dataModel = new ServiceTableModel(showPid);
        serviceTable.setModel(dataModel);
        serviceTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        TableColumnModel cm = serviceTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(200);
        cm.getColumn(0).setMaxWidth(500);
        cm.getColumn(1).setPreferredWidth(200);
        cm.getColumn(1).setMaxWidth(350);

        add("Center", new JScrollPane(serviceTable));
    }

    public void addService(ServiceRecord record) {
        dataModel.addItem(record);
    }

    public void clear() {
        dataModel.clear();
    }

    public int getSelectedRow() {
        int elements = dataModel.tableData.size();
        if(elements==0)
            return(-1);
        return(serviceTable.getSelectedRow());
    }

    class ServiceTableModel extends AbstractTableModel {
        private java.util.List<ServiceRecord> tableData = new ArrayList<ServiceRecord>();
        private boolean showPid;
        private String[] columnNames = {"Name", "Implementation Class", "Time Active"};

        public ServiceTableModel(boolean showPid) {
            super();
            this.showPid = showPid;
            if(showPid)
                columnNames = new String[]{"Name", "Implementation Class", "PID", "Time Active"};
        }

        public Object getValueAt(int index, int columnIndex) {
            try {
                ServiceRecord record = tableData.get(index);
                if(showPid) {
                    switch(columnIndex) {
                        case 0:
                            return(record.getName());
                        case 1:
                            return(record.getServiceElement().getComponentBundle().
                                getClassName());
                        case 2:
                            return record.getPid();
                        case 3:
                            return(TimeUtil.format(record.computeElapsedTime()));
                        default:
                            return(null);
                    }
                } else {
                    switch(columnIndex) {
                        case 0:
                            return(record.getName());
                        case 1:
                            return(record.getServiceElement().getComponentBundle().
                                getClassName());
                        case 2:
                            return(TimeUtil.format(record.computeElapsedTime()));
                        default:
                            return(null);
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            return(null);
        }

        public void addItem(ServiceRecord item) {
            int rowNum = tableData.size();
            tableData.add(item);
            fireTableRowsInserted(rowNum, rowNum);
        }

        public void removeItem(int row) {
            tableData.remove(row);
            fireTableDataChanged();
        }

        public void clear() {
            tableData.clear();
            fireTableDataChanged();
        }

        public Object getItem(int row) {
            return(tableData.get(row));
        }

        public int getColumnCount() {
            return(columnNames.length); 
        }

        public int getRowCount() {
            return(tableData.size());
        }

        public String getColumnName(int column) {
            return(columnNames[column]);
        }

        public void setValueAt(ServiceRecord item, int rowNum) {
            tableData.set(rowNum, item);
            fireTableRowsUpdated(rowNum, rowNum);
        }
    }   
}
