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

import net.jini.core.lookup.ServiceItem;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.platform.ByteOrientedDevice;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import java.util.Vector;

/** 
 * The PlatformCapabilityUI displays PlatformCapability instances for a Cybernode and
 * allows administrators to enable/disable persistent provisioning
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class PlatformCapabilityUI extends JPanel {
    /** The CybernodeAdmin instance */
    private CybernodeAdmin cybernodeAdmin;
    /** JTable for PlatformCapability display */
    private final JTable capabilityTable;
    /** The model for the table */
    private final PlatformCapabilityModel dataModel;
    /** Checkbox for persistent provisioning support */
    private final JCheckBox supportsProvisioning;

    public PlatformCapabilityUI(final Object arg) {
        super();
        getAccessibleContext().setAccessibleName("Platform Capabilities");
        ServiceItem item = (ServiceItem)arg;
        Cybernode cybernode = (Cybernode) item.service;
        try {
            cybernodeAdmin = (CybernodeAdmin) cybernode.getAdmin();
        } catch (Exception e) {
            showError(e);
        }
        setLayout(new BorderLayout(2, 4));
        setBorder(BorderFactory.createCompoundBorder(
                                  BorderFactory.createRaisedBevelBorder(), 
                                  BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JPanel provPanel = new JPanel();
        provPanel.setLayout(new BorderLayout(8, 8));
        provPanel.setBorder(BorderFactory.createCompoundBorder(
                                  BorderFactory.createTitledBorder(
                                                BorderFactory.createEtchedBorder(), 
                                                "Persistent Provisioning"),
                                  BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel label = new JLabel("<html>Set whether the Cybernode supports "+
                                  "persistent provisioning of qualitative "+
                                  "capabilities</html>"); 
        provPanel.add(label, BorderLayout.NORTH);

        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
        supportsProvisioning = new JCheckBox("Supports persistent provisioning");
        label.setLabelFor(supportsProvisioning);
        subPanel.add(supportsProvisioning);
        JButton apply = new JButton("Apply");
        apply.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            cybernodeAdmin.setPersistentProvisioning(supportsProvisioning.isSelected());
                        } catch(Exception e) {
                            showError(e);
                        }
                    }
                });
        subPanel.add(Box.createHorizontalGlue());
        subPanel.add(apply);
        provPanel.add(subPanel, BorderLayout.CENTER);
        add(provPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout(8, 8));
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
                                  BorderFactory.createTitledBorder(
                                                BorderFactory.createEtchedBorder(), 
                                                "Current Platform Capabilities"),
                                  BorderFactory.createEmptyBorder(8, 8, 8, 8)));        
        capabilityTable = new JTable();
        capabilityTable.addMouseListener(new RowListener());

        dataModel = new PlatformCapabilityModel();
        capabilityTable.setModel(dataModel);
        TableColumnModel cm = capabilityTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(30);        

        capabilityTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        JScrollPane scroller = new JScrollPane(capabilityTable);
        tablePanel.add(scroller, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JButton details = new JButton("Details");
        details.setToolTipText("Get details on the selected PlatformCapability");
        details.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int row = capabilityTable.getSelectedRow();
                if(row==-1)
                    return;
                showDetails(getPlatformCapability(row));
            }
        });
        buttons.add(details);
        tablePanel.add(buttons, BorderLayout.SOUTH);

        add(tablePanel, BorderLayout.CENTER);

        Thread thread = new Thread(new PlatformCapabilityInformationFetcher());
        thread.start();
    }

    private Component getComponent() {
        return this;
    }

    class PlatformCapabilityInformationFetcher implements Runnable {
        public void run() {
            try {
                getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                supportsProvisioning.setSelected(cybernodeAdmin.getPersistentProvisioning());
                PlatformCapability[] pCaps = cybernodeAdmin.getResourceCapability().getPlatformCapabilities();
                for (PlatformCapability pCap : pCaps) {
                    addPlatformCapability(pCap);
                }
                getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } catch(Exception e) {
                getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                showError(e);
            }
        }
    }

    void addPlatformCapability(PlatformCapability pCap) {
        dataModel.addOrReplace(pCap);
        dataModel.fireTableDataChanged();
    }

    void removePlatformCapability(int row) {
        dataModel.removeItem(row);
    }

    void clearPlatformCapabilities() {
        dataModel.clear();
    }

    PlatformCapability getPlatformCapability(int row) {
        if(row==-1)
            return(null);
        return((PlatformCapability)dataModel.getItem(row));
    }

    class PlatformCapabilityModel extends AbstractTableModel {
        private final Vector<PlatformCapability> tableData = new Vector<PlatformCapability>();
        final String[] columnNames = {"Name", "Description"};

        public PlatformCapabilityModel() {
            super();
        }

        public Object getValueAt(int index, int columnIndex) {
            try {
                PlatformCapability pCap = tableData.elementAt(index);
                if(columnIndex==0) {
                    return (pCap.getValue(PlatformCapability.NAME));
                } else if(columnIndex==1) {
                    return(pCap.getDescription());
                } else {
                    return null;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            return(null);
        }

        void clear() {
            tableData.clear();
            fireTableDataChanged();
        }

        void addOrReplace(PlatformCapability item) {
            int ndx = tableData.indexOf(item);
            if(ndx!=-1) {
                setValueAt(item, ndx);
            } else {
                addItem(item);
            }
        }

        public void addItem(PlatformCapability item) {
            int rowNum = tableData.size();
            tableData.insertElementAt(item, rowNum);
            fireTableRowsInserted(rowNum, rowNum);
        }

        public void removeItem(int row) {
            tableData.removeElementAt(row);
            fireTableDataChanged();
        }

        public Object getItem(int row) {
            return(tableData.elementAt(row));
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

        public void setValueAt(PlatformCapability item, int rowNum) {
            tableData.setElementAt(item, rowNum);
            fireTableRowsUpdated(rowNum, rowNum);
        }
    }   

    class RowListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            int clickCount = e.getClickCount();
            if(clickCount==2) {
                int row = capabilityTable.rowAtPoint(new Point(e.getX(), e.getY()));
                if(row==-1)
                    return;
                showDetails(getPlatformCapability(row));
            }
        }        
    }

    private void showError(Exception e) {
        StringBuilder buffer = new StringBuilder();
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement aTrace : trace) {
            buffer.append("at ").append(aTrace).append("<br>");
        }
        showError("<html>Exception : <font color=red>"+
                  e.getClass().getName()+"</font><br>"+
                  buffer.toString()+"</html>");
    }

    private void showError(String text) {
        JOptionPane.showMessageDialog(this, 
                                      text, 
                                      "System Error", 
                                      JOptionPane.ERROR_MESSAGE);
    }

    private void showDetails(PlatformCapability pCap) {
        PlatformCapabilityDetails details = new PlatformCapabilityDetails(pCap, getComponent());
    }

    /**
     * Shows the details of a PlatformCapability object
     */
    class PlatformCapabilityDetails extends JDialog {
        private JPanel base;
        private PropertiesTable propsTable;

        PlatformCapabilityDetails(final PlatformCapability pCap, Component parent) {
            super((JFrame)null, "Platform PlatformCapabilityConfig Details", true);
            base = new JPanel();
            base.setLayout(new BorderLayout(2, 4));
            base.setBorder(BorderFactory.createCompoundBorder(
                                         BorderFactory.createRaisedBevelBorder(), 
                                         BorderFactory.createEmptyBorder(8, 8, 8, 8)));            
            JPanel basics = new JPanel();
            basics.setLayout(new GridLayout(3, 2, 4, 4));
            basics.setBorder(BorderFactory.createCompoundBorder(
                                      BorderFactory.createTitledBorder(
                                            BorderFactory.createEtchedBorder(), 
                                            "General Information"),
                                      BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            basics.add(new JLabel("Description")); 
            basics.add(createTextField(pCap.getDescription()));
            basics.add(new JLabel("Class"));       
            basics.add(createTextField(pCap.getClass().getSimpleName()));
            basics.add(new JLabel("Type"));        
            basics.add(createTextField(pCap.getType()==PlatformCapability.STATIC?
                                                        "STATIC":"PROVISIONABLE"));
            base.add(basics, BorderLayout.NORTH);

            JPanel capabilityPanel = new JPanel();
            capabilityPanel.setLayout(new BorderLayout(4, 4));
            capabilityPanel.setBorder(BorderFactory.createCompoundBorder(
                                                BorderFactory.createTitledBorder(
                                                BorderFactory.createEtchedBorder(), 
                                                "PlatformCapabilityConfig Support Mappings"),
                                      BorderFactory.createEmptyBorder(8, 8, 8, 8)));

            Properties props = new Properties();
            String[] keys = pCap.getPlatformKeys();
            for (String key : keys) {
                props.put(key, pCap.getValue(key));
            }
            
            if(pCap.getPath()!=null) {
                props.put("Path", pCap.getPath());
            }

            if(pCap instanceof ByteOrientedDevice) {
                formatByteOrientedDevice(pCap, props);
            }
            propsTable = new PropertiesTable(props);
            capabilityPanel.add(propsTable, BorderLayout.CENTER);

            base.add(capabilityPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

            JButton dismiss = new JButton("Dismiss");
            dismiss.setToolTipText("Dismiss the dialog");
            dismiss.addActionListener(new ActionListener() {
                                          public void actionPerformed(ActionEvent e) {
                                              dispose();
                                          }
                                      });
            buttonPanel.add(dismiss);
            base.add(buttonPanel, BorderLayout.SOUTH);

            getContentPane().add(base);
            pack();
            showDialog(parent);
        }

        private void formatByteOrientedDevice(PlatformCapability pCap, Properties props) {
            Double dVal = (Double)props.get("Available");
            if(dVal!=null)
                props.put("Available", formatSizeValue(dVal));
            dVal = (Double)props.get("Capacity");
            if(dVal!=null)
                props.put("Capacity", formatSizeValue(dVal));
        }

        /**
         * Create a JTextField
         * 
         * @param text The text for the JTextField
         *
         * @return An un-editable JTextField
         */
        private JTextField  createTextField(String text) {
            JTextField tf = new JTextField(text);
            tf.setEditable(false);
            return(tf);
        }

        /**
         * Format a size value
         * 
         * @param dVal The Double value to format
         * 
         * @return A formatted String
         */
        private String formatSizeValue(Double dVal) {
            String result = dVal.toString();
            try {
                double value;
                String size;
                if(dVal>ByteOrientedDevice.MB) {
                    if(dVal>ByteOrientedDevice.GB) {
                        if(dVal>ByteOrientedDevice.TB) {
                            value = dVal/ByteOrientedDevice.TB;
                            size = "TB";
                        } else {
                            value = dVal/ByteOrientedDevice.GB;
                            size = "GB";
                        }
                    } else {
                        value = dVal/ByteOrientedDevice.MB;
                        size = "MB";
                    }
                } else {
                    value = dVal/ByteOrientedDevice.KB;
                    size = "KB";
                }
                result = new Double(value).intValue()+" "+size;
            } catch(Exception e) {
                System.err.println("Bad StorageCapability value ["+dVal+"]");
            }
            return(result);
        }

        /**
         * Override show to determine size and placement
         */
        private void showDialog(Component component) {
            int width = 400;
            int height = 370;
            pack();
            setSize(width, height);
            setLocationRelativeTo(component);
            getContentPane().add(base);
            java.awt.event.WindowListener l = new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            };
            addWindowListener(l);
            setVisible(true);
        }
    }

    /**
     * Add a Key and Value
     */
    class KeyValueDialog extends JDialog {
        private JPanel base;
        private JTextField key;
        private JTextField value;

        /*
         * Create a KeyValueDialog instance
         */
        KeyValueDialog(JDialog parent) {
            this(parent, "", "");
        }

        /*
         * Create a KeyValueDialog instance
         */
        KeyValueDialog(JDialog parent, String keyText, String valueText) {
            super(parent, "Platform PlatformCapabilityConfig Mapping", true);
            base = new JPanel();
            base.setLayout(new BorderLayout(6, 6));            
            base.setBorder(BorderFactory.createCompoundBorder(
                                      BorderFactory.createRaisedBevelBorder(), 
                                      BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            JPanel basics = new JPanel();
            basics.setLayout(new GridLayout(4, 1, 8, 8));

            JLabel keyLabel = new JLabel("<html>Enter a value for the key</html>");
            key = new JTextField();
            key.setText(keyText);
            keyLabel.setLabelFor(key);
            JLabel valueLabel = new JLabel("<html>Enter a value for the value"+
                                           "</html>");
            value = new JTextField();
            value.setText(valueText);
            valueLabel.setLabelFor(value);

            JButton accept = new JButton("Accept");
            accept.setToolTipText("Accept entries for key,value pair");
            accept.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if(key.getText().length()==0) {
                                showError("You must provide a value for the key");
                                
                                return;
                            }
                            if(value.getText().length()==0) {
                                showError("You must provide a value for the value");
                                return;
                            }
                            dispose();
                        }
                    });

            JButton dismiss = new JButton("Dismiss");
            dismiss.setToolTipText("Dismiss the dialog");
            dismiss.addActionListener(
                         new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                                 dispose();
                             }
                         });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(accept);
            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(dismiss);

            basics.add(keyLabel);
            basics.add(key);
            basics.add(valueLabel);
            basics.add(value);            
            basics.add(buttonPanel);
            base.add(basics, BorderLayout.CENTER);
            base.add(buttonPanel, BorderLayout.SOUTH);
            getContentPane().add(base);
            pack();
            display();
        }        

        /**
         * Get the key
         * 
         * @return The key
         */
        public String getKey() {
            return(key.getText());
        }

        /**
         * Get the value
         * 
         * @return The value
         */
        public String getValue() {
            return(value.getText());
        }

        /**
         * Override show to determine size and placement
         */
        private void display() {
            int width = 365;
            int height = 205;
            pack();
            setSize(width, height);
            getContentPane().add(base);
            java.awt.event.WindowListener l = new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            };
            addWindowListener(l);
            Rectangle rect = getParent().getBounds();
            int widthLoc = ((int)rect.getWidth()/2)+getParent().getX() - width/2;
            setLocation(widthLoc, getParent().getY()+20);
            setVisible(true);
        }

        /**
         * Show a user input error
         *
         * @param text The text to display
         */
        void showError(String text) {
            JOptionPane.showMessageDialog(this, text, "User Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
