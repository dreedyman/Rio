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
public class PlatformCapabilityUI extends JPanel implements Runnable {
    /** The Cybernode instance */
    Cybernode cybernode;
    /** The CybernodeAdmin instance */
    CybernodeAdmin cybernodeAdmin;
    /** JTable for PlatformCapability display */
    JTable capabilityTable;
    /** The model for the table */
    PlatformCapabilityModel dataModel;
    /** Checkbox for persistent provisioning support */
    JCheckBox supportsProvisioning;

    public PlatformCapabilityUI(final Object arg) {
        super();
        getAccessibleContext().setAccessibleName("Platform Capabilities");
        ServiceItem item = (ServiceItem)arg;
        cybernode = (Cybernode)item.service;
        try {
            cybernodeAdmin = (CybernodeAdmin)cybernode.getAdmin();
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
                            cybernodeAdmin.setPersistentProvisioning(
                                                 supportsProvisioning.isSelected());
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

        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {
            supportsProvisioning.setSelected(
                                     cybernodeAdmin.getPersistentProvisioning());
            PlatformCapability[] pCaps =
                                     cybernodeAdmin.getPlatformCapabilties();
            for (PlatformCapability pCap : pCaps)
                addPlatformCapability(pCap);
        } catch(Exception e) {
            showError(e);
        }
    }

    void addPlatformCapability(PlatformCapability pCap) {
        dataModel.addItem(pCap);
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
        Vector tableData = new Vector();

        final String[] columnNames = {"Name", "Description", "Class", "Package"};

        public PlatformCapabilityModel() {
            super();
        }

        public Object getValueAt(int index, int columnIndex) {
            try {
                PlatformCapability pCap =
                    (PlatformCapability)tableData.elementAt(index);
                switch(columnIndex) {
                    case 0:
                        return (pCap.getValue(PlatformCapability.NAME));
                    case 1:
                        return(pCap.getDescription());
                    case 2:
                        String name = pCap.getClass().getName();
                        int ndx = name.lastIndexOf(".");
                        if(ndx!=-1)
                            name = name.substring(ndx+1, name.length());
                        return(name);
                    case 3:
                        String pkgName = pCap.getClass().getName();
                        int x = pkgName.lastIndexOf(".");
                        if(x!=-1)
                            pkgName = pkgName.substring(0, x);
                        return(pkgName);
                    default:
                        return(null);
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

        @SuppressWarnings("unchecked")
        public void addItem(Object item) {
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

        @SuppressWarnings("unchecked")
        public void setValueAt(Object item, int rowNum) {
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

    void showError(Exception e) {
        StringBuffer buffer = new StringBuffer();
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement aTrace : trace)
            buffer.append("at ").append(aTrace).append("<br>");
        showError("<html>Exception : <font color=red>"+
                  e.getClass().getName()+"</font><br>"+
                  buffer.toString()+"</html>");
    }

    void showError(String text) {
        JOptionPane.showMessageDialog(this, 
                                      text, 
                                      "System Error", 
                                      JOptionPane.ERROR_MESSAGE);
    }

    void showDetails(PlatformCapability pCap) {
        PlatformCapabilityDetails details = new PlatformCapabilityDetails(pCap);
        if(details.changed) {
            Thread thread = new Thread(this);
            clearPlatformCapabilities();
            thread.start();            
        }
    }

    /**
     * Shows the details of a PlatformCapability object
     */
    class PlatformCapabilityDetails extends JDialog {
        final PlatformCapability pCap;
        JPanel base;
        JDialog instance;
        PropertiesTable propsTable;
        boolean changed=false;
        JButton apply;

        PlatformCapabilityDetails(final PlatformCapability pCap) {
            super((JFrame)null, "Platform PlatformCapabilityConfig Details", true);
            this.pCap = pCap;
            instance = this;
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
            basics.add(createTextField(pCap.getClass().getName()));
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

            //Map capabilityMap = pCap.getMapping();
            Properties props = new Properties();
            //props.putAll(capabilityMap);
            String[] keys = pCap.getPlatformKeys();
            for (String key : keys) {
                props.put(key, pCap.getValue(key));
            }
            
            if(pCap.getPath()!=null) {
                props.put("Path", pCap.getPath());
            }
                        
            /* Check if the PlatformCapability is a 
             * org.rioproject.system.capability.system.StorageCapability. If it is make 
             * sure the org.rioproject.system.capability.system.StorageCapability.Available 
             * and org.rioproject.system.capability.system.StorageCapability.Capacity
             * are formatted for kbytes display             
             */
            String storageClass = 
                "org.rioproject.system.capability.system.StorageCapability";
            if(pCap.getClass().getName().equals(storageClass)) {
                Double dVal = (Double)props.get((Object)"Available");
                if(dVal!=null) 
                    props.put("Available", formatStorageCapabilityValue(dVal));                
                dVal = (Double)props.get((Object)"Capacity");
                if(dVal!=null)
                    props.put("Capacity", formatStorageCapabilityValue(dVal));
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
            //buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(dismiss);
            base.add(buttonPanel, BorderLayout.SOUTH);

            getContentPane().add(base);
            //setResizable(false);
            pack();
            showDialog();
        }

        /**
         * Create a JTextField
         * 
         * @param text The text for the JTextField
         *
         * @return An un-editable JTextField
         */
        JTextField  createTextField(String text) {
            JTextField tf = new JTextField(text);
            tf.setEditable(false);
            return(tf);
        }

        /**
         * Format StorageCapability value
         * 
         * @param dVal The Double value to format
         * 
         * @return A formatted String value for kbytes
         */
        String formatStorageCapabilityValue(Double dVal) {
            String result = dVal.toString();
            try {
                double value = dVal /1024;
                result = new Double(value).intValue()+" kbytes";
                //result =value+" kbytes";                
            } catch(Exception e) {
                System.err.println("Bad StorageCapability value ["+dVal+"]");
            }
            return(result);
        }

        /**
         * Override show to determine size and placement
         */
        private void showDialog() {
            int width = 400;
            int height = 370;
            pack();
            setSize(width, height);
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
        JPanel base;
        JTextField key;
        JTextField value;

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
            JOptionPane.showMessageDialog(this, 
                                          text, 
                                          "User Input Error", 
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
}
