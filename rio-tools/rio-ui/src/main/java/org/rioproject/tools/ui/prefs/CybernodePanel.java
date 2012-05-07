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
package org.rioproject.tools.ui.prefs;

import org.rioproject.tools.ui.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Option to declare the ammount of time the Cybenode table should poll
 *
 * @author Dennis Reedy
 */
public class CybernodePanel extends JPanel {
    private JTextField refreshRate;
    private JTable columnChoices;

    //"Utilization" Still needed?

    public CybernodePanel(int rate, String[] selectedColumns) {
        super();
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        //c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        JLabel l1 = new JLabel("<html>Utilization Table<br>" +
                           "Refresh Rate (in seconds)</html>");

        add(l1, c);
        refreshRate = new IntegerTextField();
        refreshRate.setText(Integer.toString(rate));
        c.gridwidth = GridBagConstraints.REMAINDER;

        add(refreshRate, c);

        JLabel l2 = new JLabel("<html>" +
                               "Select the columns the Utilization table " +
                               "will display for system utilization" +
                               "</html>");
        c.gridy = 2;
        add(l2, c);
        add(Box.createVerticalStrut(8));

        JPanel p1 = new JPanel();
        String[][] system = new String[][]{
                {Constants.UTIL_PERCENT_CPU, "Percentage of CPU utilization on the machine"},
                {Constants.UTIL_PERCENT_MEMORY, "Percentage of used memory on the machine"},
                {Constants.UTIL_TOTAL_MEMORY, "Total memory (in MB) on the machine"},
                {Constants.UTIL_FREE_MEMORY, "Amount (in MB) of free memory on the machine"},
                {Constants.UTIL_USED_MEMORY, "Amount (in MB) of used memory on the machine"},
                {Constants.UTIL_PERCENT_DISK, "Percentage of used disk space"},
                {Constants.UTIL_AVAIL_DISK, "Amount (in GB) of available disk space"},
                {Constants.UTIL_TOTAL_DISK, "Amount (in GB) of total disk space"}
        };

        addCheckboxes(system, selectedColumns, p1);


        c.gridy = 4;
        c.gridheight = 2;
        c.weighty = 1.0;
        add(p1, c);

        JLabel l3 = new JLabel("<html>" +
                               "Select the columns the Utilization table " +
                               "will display for process utilization.</html>");        
        c.gridy = 6;
        c.gridheight = 1;
        c.weighty = 0;
        add(l3, c);

        JPanel p2 = new JPanel();
        String[][] process = new String[][]{
                {Constants.UTIL_PERCENT_CPU_PROC,
                 "Percentage of CPU utilization for the process " +
                 "(cybernode and/or forked services)"},
                {Constants.UTIL_PERCENT_HEAP_JVM,
                 "Percentage of Memory (heap) utilization for the " +
                 "JVM (cybernode and/or forked java services)"},
                {Constants.UTIL_HEAP_MEM_JVM,
                 "Amount of heap memory (in MB) the JVM is using"},
                {Constants.UTIL_HEAP_MEM_AVAIL,
                 "Amount of heap memory (in MB) the JVM has available"},
                {Constants.UTIL_REAL_MEM_PROC,
                 "Amount of real memory (in MB) the process has " +
                 "allocated"}};

        addCheckboxes(process, selectedColumns, p2);

        c.gridy = 7;
        c.gridheight = 2;
        c.weighty = 1.0;
        add(p2, c);

        columnChoices = new JTable();
        columnChoices.setRowSelectionAllowed(false);
        columnChoices.setAutoCreateColumnsFromModel(false);
        JScrollPane sp = new JScrollPane(columnChoices);

        for(String s : selectedColumns)
            insertColumn(s);
        c.gridy = 9;
        c.gridheight = 1;
        c.weighty = 1.0;
        add(sp, c);
    }

    private void addCheckboxes(String[][] items,
                               String[] selectedColumns,
                               JPanel panel) {
        for (String[] item : items) {
            JCheckBox cb = new JCheckBox(item[0]);
            cb.setSelected(selectCheckbox(item[0], selectedColumns));
            cb.setToolTipText(item[1]);
            panel.add(cb);
            cb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    JCheckBox cb = (JCheckBox)event.getSource();
                    if(cb.isSelected())
                        insertColumn(cb.getText());
                    else
                        removeColumn(cb.getText());
                }
            });
        }
    }

    private boolean selectCheckbox(String column, String[] selected) {
        boolean select = false;
        for(String s : selected) {
            if(column.equals(s)) {
                select = true;
                break;
            }
        }
        return select;
    }

    private void insertColumn(String headerLabel) {
        int colIndex = columnChoices.getColumnCount();
        betterAddColumn(columnChoices, headerLabel);
        if(colIndex>0)
            columnChoices.moveColumn(columnChoices.getColumnCount()-1, colIndex);
    }

    private void removeColumn(String headerLabel) {
        java.util.List<TableColumn> currentCols = getTableColumns();
        TableColumn removeColumn = null;
        for(TableColumn col : currentCols) {
            String header = (String)col.getHeaderValue();
            if(header.equals(headerLabel)) {
                removeColumn = col;
                break;
            }
        }
        if(removeColumn!=null)
            columnChoices.getColumnModel().removeColumn(removeColumn);
    }

    private void betterAddColumn(JTable table, String headerLabel) {
        DefaultTableModel model = (DefaultTableModel)table.getModel();
        TableColumn col = new TableColumn(model.getColumnCount());

        // Ensure that auto-create is off
        if (table.getAutoCreateColumnsFromModel()) {
            throw new IllegalStateException();
        }
        col.setHeaderValue(headerLabel);
        table.addColumn(col);
        model.addColumn(headerLabel);
    }

    public String[] getSelectedColumns() {
        java.util.List<String> selected = new ArrayList<String>();
        java.util.List<TableColumn> cols = getTableColumns();
        for(TableColumn col : cols) {
            String header = (String)col.getHeaderValue();
            selected.add(header);
        }
        return selected.toArray(new String[selected.size()]);
    }

    public int getRefreshRate() {
        return Integer.parseInt(refreshRate.getText());
    }

    private java.util.List<TableColumn> getTableColumns() {
        java.util.List<TableColumn> currentCols = new ArrayList<TableColumn>();
        for(Enumeration<TableColumn> cols =
                columnChoices.getColumnModel().getColumns(); cols.hasMoreElements();) {
            TableColumn col = cols.nextElement();
            currentCols.add(col);
        }
        return currentCols;
    }

    class ColumnChoiceTableModel extends DefaultTableModel {

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return 1;
        }

        public String getColumnName(int col) {
            return null;
        }

        public Object getValueAt(int row, int col) {
            return null;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {
            return row == 0;
        }

    }

    class IntegerTextField extends JTextField {
        final static String badchars = "`~!@#$%^&*()_+=\\|\"':;?/>.<, ";

        public void processKeyEvent(KeyEvent ev) {
            char c = ev.getKeyChar();
            if ((Character.isLetter(c) && !ev.isAltDown())
                || badchars.indexOf(c) > -1) {
                ev.consume();
                return;
            }
            if (c == '-' && getDocument().getLength() > 0)
                ev.consume();
            else
                super.processKeyEvent(ev);
        }
    }
}
