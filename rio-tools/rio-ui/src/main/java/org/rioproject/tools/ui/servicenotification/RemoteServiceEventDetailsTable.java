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

import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.tools.ui.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A table that shows details related to a {@code RemoteServiceEvent}.
 *
 * @author Dennis Reedy
 */
public class RemoteServiceEventDetailsTable extends JPanel {
    private final NumberFormat numberFormatter;
    private final DetailsModel model = new DetailsModel();
    private final JTable table = new JTable();

    public RemoteServiceEventDetailsTable() {
        super(new BorderLayout(8, 8));

        table.setModel(model);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new MultiLineCellRenderer());
        TableColumn column = table.getColumnModel().getColumn(0);
        column.setPreferredWidth(100);
        column.setMaxWidth(130);
        numberFormatter = NumberFormat.getNumberInstance();
        numberFormatter.setGroupingUsed(false);
        numberFormatter.setMaximumFractionDigits(2);
        add(new JScrollPane(table));
    }

    public void setRemoteServiceEventNode(RemoteServiceEventNode eventNode) {
        model.setRemoteServiceEventNode(eventNode);
    }

    class DetailsModel extends AbstractTableModel {
        private final String[] columnNames = {"Field", "Value"};

        private final Map<String, String> tableData = new LinkedHashMap<String, String>();

        public int getRowCount() {
            return tableData.size();
        }

        void setRemoteServiceEventNode(RemoteServiceEventNode eventNode) {
            if(eventNode==null) {
                tableData.clear();
                this.fireTableDataChanged();
                return;
            }
            RemoteServiceEvent event = eventNode.getEvent();
            tableData.clear();
            String impl = "<not declared>";
            Throwable thrown;
            if(event instanceof ProvisionFailureEvent) {
                String label = "ProvisionFailureEvent."+eventNode.getValueAt(0);
                ProvisionFailureEvent pfe = (ProvisionFailureEvent)event;
                ServiceElement elem = pfe.getServiceElement();
                if(elem.getComponentBundle()!=null)
                    impl = elem.getComponentBundle().getClassName();
                thrown = pfe.getThrowable();
                String exception = getExceptionText(thrown);
                tableData.put("Event", label);
                tableData.put("When", Constants.DATE_FORMAT.format(event.getDate()));
                tableData.put("Deployment", elem.getOperationalStringName());
                tableData.put("Service", elem.getName());
                tableData.put("Class", impl);
                StringBuilder builder = new StringBuilder();
                for(String reason : pfe.getFailureReasons()) {
                    if(builder.length()>0)
                        builder.append("\n    ");
                    builder.append(reason);
                }
                tableData.put("Reason", builder.toString());
                if(exception!=null) {
                    tableData.put("Exception", exception);
                    table.setRowHeight(tableData.size(), table.getRowHeight() * 20);
                }

            } else if(event instanceof ProvisionMonitorEvent) {
                String label = "ProvisionMonitorEvent."+eventNode.getValueAt(0);
                tableData.put("Event", label);
                ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
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
                    tableData.put("When", Constants.DATE_FORMAT.format(event.getDate()));
                    tableData.put("Deployment", pme.getOperationalStringName());
                    tableData.put("Services", builder.toString());
                } else {
                    tableData.put("When", Constants.DATE_FORMAT.format(event.getDate()));
                    tableData.put("Deployment", pme.getOperationalStringName());
                    tableData.put("Description", eventNode.getDescription());
                }

            } else if(event instanceof ServiceLogEvent) {
                String label = "ServiceLogEvent."+eventNode.getValueAt(0);
                tableData.put("Event", label);
                ServiceLogEvent sle = (ServiceLogEvent)event;
                thrown = sle.getLogRecord().getThrown();
                String exception = getExceptionText(thrown);
                tableData.put("When", Constants.DATE_FORMAT.format(event.getDate()));
                tableData.put("Deployment", sle.getOpStringName());
                tableData.put("Service", sle.getServiceName());
                tableData.put("Machine", sle.getAddress().getHostName());
                tableData.put("Message", sle.getLogRecord().getLevel()+": "+sle.getLogRecord().getMessage());
                if(exception!=null) {
                    tableData.put("Exception", exception);
                    table.setRowHeight(tableData.size(), table.getRowHeight() * 20);
                }
            } else {
                String label = "SLAThresholdEvent."+eventNode.getValueAt(0);
                tableData.put("Event", label);
                SLAThresholdEvent slaEvent = (SLAThresholdEvent)event;
                StringBuilder builder = new StringBuilder();
                builder.append("low=").append(slaEvent.getSLA().getCurrentLowThreshold());
                builder.append(" high=").append(slaEvent.getSLA().getCurrentHighThreshold());
                tableData.put("When", Constants.DATE_FORMAT.format(event.getDate()));
                tableData.put("Deployment", slaEvent.getServiceElement().getOperationalStringName());
                tableData.put("Service", slaEvent.getServiceElement().getName());
                tableData.put("Machine", slaEvent.getHostAddress());
                tableData.put("Value", numberFormatter.format(slaEvent.getCalculable().getValue()));
                tableData.put("Threshold Values", builder.toString());
                tableData.put("Policy Handler", slaEvent.getSLAPolicyHandlerDescription());
            }
            this.fireTableDataChanged();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            int i = 0;
            String value = null;
            for(Map.Entry<String, String> entry : tableData.entrySet()) {
                if(row==i) {
                    value = col==0?entry.getKey():entry.getValue();
                    break;
                }
                i++;
            }
            return value;
        }

        private String getExceptionText(Throwable thrown) {
            String exception = null;
            if(thrown!=null) {
                StringBuilder builder = new StringBuilder();
                builder.append(thrown.getClass().getName()).append(": ").append(thrown.getLocalizedMessage() ).append("\n");
                StackTraceElement[] trace = thrown.getStackTrace();
                for (StackTraceElement aTrace : trace)
                    builder.append("    at ").append(aTrace).append("\n");
                exception = builder.toString();
            }
            return exception;
        }
    }

    /**
     * A multi-line cell renderer
     *
     * @author http://blog.botunge.dk/post/2009/10/09/JTable-multiline-cell-renderer.aspx
     */
    class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {
        private java.util.List<java.util.List<Integer>> rowColHeight = new ArrayList<java.util.List<Integer>>();

        public MultiLineCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            }
            else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setFont(table.getFont());
            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
                if (table.isCellEditable(row, column)) {
                    setForeground(UIManager.getColor("Table.focusCellForeground"));
                    setBackground(UIManager.getColor("Table.focusCellBackground"));
                }
            }
            else {
                setBorder(new EmptyBorder(1, 2, 1, 2));
            }

            setText((value == null) ? "" : value.toString());
            if(getLineCount()>1) {
                adjustRowHeight(table, row, column);
            }
            return this;
        }

        /**
         * Calculate the new preferred height for a given row, and sets the height on the table.
         */
        private void adjustRowHeight(JTable table, int row, int column) {
            //The trick to get this to work properly is to set the width of the column to the
            //textarea. The reason for this is that getPreferredSize(), without a width tries
            //to place all the text in one line. By setting the size with the with of the column,
            //getPreferredSize() returns the proper height which the row should have in
            //order to make room for the text.
            int cWidth = table.getTableHeader().getColumnModel().getColumn(column).getWidth();
            setSize(new Dimension(cWidth, 1000));
            int prefH = getPreferredSize().height;
            while (rowColHeight.size() <= row) {
                rowColHeight.add(new ArrayList<Integer>(column));
            }
            java.util.List<Integer> colHeights = rowColHeight.get(row);
            while (colHeights.size() <= column) {
                colHeights.add(0);
            }
            colHeights.set(column, prefH);
            int maxH = prefH;
            for (Integer colHeight : colHeights) {
                if (colHeight > maxH) {
                    maxH = colHeight;
                }
            }
            if (table.getRowHeight(row) != maxH) {
                table.setRowHeight(row, maxH);
            }
        }
    }

}
