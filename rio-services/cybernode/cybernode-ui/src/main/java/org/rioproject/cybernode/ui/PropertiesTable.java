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

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.*;

/**
 * The PropertiesTable displays a Properties object in a JTable
 *
 * @author Dennis Reedy
 */
public class PropertiesTable extends JPanel {
    private JTable propertiesTable;
    private PropertiesTableModel propertiesTableModel;

    /**
     * Create a PropertiesTable
     */
    public PropertiesTable() {
        super();
        setLayout(new BorderLayout());
        propertiesTable = new JTable();
        propertiesTableModel = new PropertiesTableModel();
        propertiesTable.setModel(propertiesTableModel);
        propertiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumnModel cm = propertiesTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(0).setMaxWidth(200);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setForeground(Color.gray);
        cm.getColumn(0).setCellRenderer(renderer);
        JScrollPane scroller = new JScrollPane(propertiesTable);
        add(scroller, BorderLayout.CENTER);
    }

    /**
     * Create a PropertiesTable
     * 
     * @param props The Properties to display
     */
    public PropertiesTable(Properties props) {
        this();
        setProperties(props);
    }

    /**
     * Set the Properties to display
     * 
     * @param props The Properties to display
     */
    private void setProperties(Properties props) {
        if(props == null)
            throw new IllegalArgumentException("props is null");
        propertiesTableModel.setData(props);
    }

    /**
     * Add a key,value mapping to the table
     * 
     * @param key The key value
     * @param value The value
     */
    void addMapping(String key, String value) {
        if(key == null)
            throw new IllegalArgumentException("key is null");
        if(value == null)
            throw new IllegalArgumentException("value is null");
        propertiesTableModel.addKeyValue(key, value);
    }

    /**
     * Get the value of the key for a row.
     * 
     * @return The value of the key for the row. If the row is not
     * found, then a null is returned
     */
    String getKey(int row) {
        return ((String)propertiesTableModel.getValueAt(row, 0));
    }

    /**
     * Get the value of the value for a row.
     * 
     * @return The value of the value for the row. If the row is not
     * found, then a null is returned
     */
    String getValue(int row) {
        if(row == -1)
            return (null);
        return ((String)propertiesTableModel.getValueAt(row, 1));
    }

    /**
     * Set the value for a row
     * 
     * @param key The value to set
     * @param row The row
     */
    void setKey(String key, int row) {
        if(key == null)
            throw new IllegalArgumentException("key is null");
        propertiesTableModel.setValueAt(key, row, 0);
    }

    /**
     * Set the value for a row
     * 
     * @param value The value to set
     * @param row The row
     */
    void setValue(String value, int row) {
        if(value == null)
            throw new IllegalArgumentException("value is null");
        propertiesTableModel.setValueAt(value, row, 1);
    }

    /**
     * Get all the key,value pairs as a Properties object
     * 
     * @return All the key,value pairs as a Properties object
     */
    Properties getProperties() {
        return (propertiesTableModel.getProperties());
    }

    /**
     * Clear all values in the table
     */
    public void clearTable() {
        propertiesTableModel.removeAll();
    }

    /**
     * Remove the selected row
     * 
     * @return Return true if the row was removed, if not (a row was
     * not selected), return false
     */
    boolean removeSelectedRow() {
        int row = propertiesTable.getSelectedRow();
        if(row == -1)
            return (false);
        propertiesTableModel.removeItem(row);
        return (true);
    }

    /**
     * Get the selected row
     * 
     * @return Return the selected row number, or return -1 if no row is
     * selected
     */
    int getSelectedRow() {
        return (propertiesTable.getSelectedRow());
    }
    /**
     * The PropertiesTableModel provides the model for the table
     */
    class PropertiesTableModel extends AbstractTableModel {
        private java.util.List<String> keys = new ArrayList<String>();
        private java.util.List<String> values = new ArrayList<String>();
        private final String[] columnNames = {"Key", "Value"};

        public PropertiesTableModel() {
            super();
        }

        @SuppressWarnings("unchecked")
        void setData(Properties props) {
            keys.clear();
            for(Enumeration e = props.propertyNames(); e.hasMoreElements();) {
                keys.add((String) e.nextElement());
            }
            values.clear();
            for(String key : keys) {
                values.add(props.getProperty(key));
            }
            fireTableDataChanged();
        }

        @SuppressWarnings("unchecked")
        void addKeyValue(String key, String value) {
            keys.add(key);
            values.add(value);
            fireTableDataChanged();
        }

        @SuppressWarnings("unchecked")
        public void setValueAt(Object value, int row, int column) {
            if(column == 1) {
                values.set(row, (String) value);
                fireTableRowsUpdated(row, row);
            } else {
                super.setValueAt(value, row, column);
            }
        }

        public Object getValueAt(int row, int column) {
            if(column==0) {
                return (keys.get(row));
            } else if(column==1) {
                return (values.get(row));
            }
            return (null);
        }

        public int getColumnCount() {
            return (columnNames.length);
        }

        public int getRowCount() {
            if(keys == null)
                return (0);
            return (keys.size());
        }

        public void removeAll() {
            keys.clear();
            values.clear();
            fireTableDataChanged();
        }

        //public boolean isCellEditable(int row, int col) {
        //    return((col==1?true:false));
        //}
        public String getColumnName(int column) {
            return (columnNames[column]);
        }

        public void removeItem(int row) {
            keys.remove(row);
            values.remove(row);
            fireTableDataChanged();
        }

        Properties getProperties() {
            Properties props = new Properties();
            for(int i = 0; i < keys.size(); i++) {
                props.put(keys.get(i), values.get(i));
            }
            return (props);
        }
    }
}
