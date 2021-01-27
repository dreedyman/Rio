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
package org.rioproject.tools.ui.serviceui;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceRegistrar;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.SystemComponent;
import org.rioproject.opstring.ServiceElement.ProvisionType;
import org.rioproject.deploy.SystemRequirements;
import org.rioproject.log.LoggerConfig;
import org.rioproject.opstring.*;
import org.rioproject.sla.SLA;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.ui.Util;
import org.rioproject.tools.ui.util.SwingWorker;
import org.rioproject.watch.ThresholdValues;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


/**
 * A JPanel which displays ServiceElement attributes and provides support to
 * increase, decrease, relocate and redeploy instances
 *
 * @author Dennis Reedy
 */
public class ServiceElementPanel extends javax.swing.JPanel {
    ServiceElement sElem;
    private static String[] serviceElementProps =
        new String[] {"Interfaces",
                      "Service Implementation",
                      "Planned Instances",
                      "Groups",
                      "Locators",
                      "System Requirements",
                      "SLAs",
                      "Fault Detection Handler",
                      "Max Per Machine",
                      "Cluster",
                      "Provision Type",
                      "Match On Name",
                      "Auto Advertise",
                      "DiscoveryManagement Pooling",
                      "Comment",
                      "Organization",
                      "Parameters",
                      "Associations",
                      "Configuration",
                      "Loggers"};
    private static String instanceLabelText = "Instances";
    private JButton more;
    private OperationalStringManager opMgr;
    private Component parent;
    static final Font COMMON_FONT = new Font("Lucida Grande", 0, 10);

    /*
     * Creates new form ServiceElementPanel
     */
    public ServiceElementPanel(final JDialog parent) {
        this.parent = parent;
        JTabbedPane tabs = new JTabbedPane();

        JPanel serviceElementProps = new JPanel();
        serviceElementProps.setBorder(BorderFactory.createEmptyBorder(4, 4, 4,44));
        JPanel serviceInstanceProps = new JPanel();
        serviceInstanceProps.setBorder(BorderFactory.createEmptyBorder(4, 4, 4,44));
        JPanel jars = new JPanel();
        initComponents(serviceElementProps,
                       serviceInstanceProps,
                       jars);
        
        tabs.add(serviceInstanceProps, "Service Instance Management");
        tabs.add(serviceElementProps, "Service Element Properties");
        tabs.add(jars, "Service JARs");
        setLayout(new java.awt.BorderLayout());
        add(tabs, java.awt.BorderLayout.CENTER);


        JPanel control = new JPanel();
        JButton close = new JButton("Close");
        close.setToolTipText("Close the dialog");
        close.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                parent.dispose();
            }
        });
        JButton refresh = new JButton("Refresh");
        refresh.setToolTipText("Refresh ServiceElement attributes");
        /* Refresh action listener */
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Component root = SwingUtilities.getRoot(parent);
                if(root != null)
                    root.setCursor(
                        Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                refresh();
                if(root != null)
                    root.setCursor(
                        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });
        control.add(refresh);
        control.add(close);
        add(control, BorderLayout.SOUTH);

        exportJARTable.setFont(COMMON_FONT);
        serviceAttributeTable.setFont(COMMON_FONT);

        implJARTable.setFont(COMMON_FONT);
        instanceTable.setFont(COMMON_FONT);

        /* Button to display in service attributes table 3rd column for properties
         * which have more info */
        more = new JButton("...");
        /* Redeploy behavior */
        RedeployHandler handler = new RedeployHandler();
        serviceRedeploy.addActionListener(handler);
        serviceElementRedeploy.addActionListener(handler);
        /* Trim services behavior */
        trimServices.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    int planned = sElem.getPlanned();
                    int trimmed = opMgr.trim(sElem, -1);
                    sElem.setPlanned(planned-trimmed);
                    planned = sElem.getPlanned();
                    String value = Integer.toString(planned);
                    ServiceAttributeTableItem tableItem =
                        new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    DefaultTableModel tableModel =
                        (DefaultTableModel)serviceAttributeTable.getModel();
                    tableModel.setValueAt(tableItem,
                                          2,   /*row*/
                                          1);  /*column*/
                    setInstanceTableLabel(sElem,
                                          initializeInstanceTable(
                                             opMgr.getServiceBeanInstances(sElem)));
                } catch (Exception e) {
                    Util.showError(e, parent, "Unable to trim instance count");
                }
            }
        });

        /* Increase instance behavior */
        increaseService.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                SwingWorker worker = new SwingWorker() {
                    public Object construct() {
                        try {
                            int planned = sElem.getPlanned();
                            opMgr.increment(sElem, true, null);
                            planned++;
                            sElem.setPlanned(planned);
                            String value = Integer.toString(planned);
                            ServiceAttributeTableItem tableItem =
                                    new ServiceAttributeTableItem();
                            tableItem.attributeDescription = new JLabel(value);
                            DefaultTableModel tableModel =
                                    (DefaultTableModel)serviceAttributeTable.getModel();
                            tableModel.setValueAt(tableItem,
                                                  2,   /*row*/
                                                  1);  /*column*/
                            setInstanceTableLabel(sElem,
                                                  initializeInstanceTable(
                                                          opMgr.getServiceBeanInstances(sElem)));
                        } catch (Exception e) {
                            Util.showError(e, parent, "Unable to increase instance count");
                        }

                        return null;
                        }
                    };
                    worker.start();
            }
        });
        /* Decrease instance behavior */
        decreaseService.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    int planned = sElem.getPlanned();
                    int row = instanceTable.getSelectedRow();
                    ServiceBeanInstance instance =
                        (ServiceBeanInstance)instanceTable.getModel().
                                                           getValueAt(row, 1);
                    opMgr.decrement(instance, true, true);
                    if(planned>0)
                        planned--;
                    sElem.setPlanned(planned);
                    String value = Integer.toString(planned);
                    ServiceAttributeTableItem tableItem =
                        new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    DefaultTableModel tableModel =
                        (DefaultTableModel)serviceAttributeTable.getModel();
                    tableModel.setValueAt(tableItem,
                                          2,   /*row*/
                                          1);  /*column*/
                    ((DefaultTableModel)instanceTable.getModel()).removeRow(row);
                    setInstanceTableLabel(sElem,
                                          (DefaultTableModel)instanceTable.getModel());
                } catch (Exception e) {
                    Util.showError(e, parent, "Unable to decrease instance count");
                }
            }
        });

        /* Relocation instance behavior */
        relocateService.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    ServiceBeanInstance instance =
                        (ServiceBeanInstance)instanceTable.getModel().getValueAt(instanceTable.getSelectedRow(), 1);
                    opMgr.relocate(instance, null, null);
                } catch (Exception e) {
                    Util.showError(e, parent, "Unable to relocate");
                }
            }
        });

        /* Listener for the instance table */
        ListSelectionModel rowSM = instanceTable.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                //Ignore extra messages.
                if (e.getValueIsAdjusting())
                    return;
                ListSelectionModel lsm =
                    (ListSelectionModel)e.getSource();
                if (lsm.isSelectionEmpty()) {
                    setControls(false);
                } else {
                    if(sElem.getProvisionType()==ProvisionType.DYNAMIC)
                        setControls(true);
                }
            }
        });
        /* Set column color for the serviceAttributeTable */
        TableColumnModel tcm = serviceAttributeTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(40);
        /* Set renderer for column 1 for a ServiceAttributeTableItem */
        tcm.getColumn(1).setCellRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                if(value instanceof ServiceAttributeTableItem) {
                    return(((ServiceAttributeTableItem)value).attributeDescription);
                }
                return((Component)value);
            }
        });
        /* Set renderer for column 2 for a JButton */
        tcm.getColumn(2).setCellRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                if(value instanceof JButton) {
                    JButton button = (JButton)value;
                   return(button);
                }
                return((Component)value);
            }
        });
        /* Listener for the serviceAttribute table */
        serviceAttributeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int column = serviceAttributeTable.getSelectedColumn();
                int row = serviceAttributeTable.getSelectedRow();
                if(column==2) {
                    JDialog dialog = new JDialog((JFrame)null, "Property Details", true);
                    Details details = new Details(dialog);
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<html>");
                    buffer.append("<body>");
                    buffer.append("<strong>");
                    buffer.append(serviceAttributeTable.getModel().getValueAt(row, 0));
                    buffer.append("</strong>");
                    buffer.append("<br><br>");
                    ServiceAttributeTableItem item =
                        (ServiceAttributeTableItem)serviceAttributeTable.getModel().getValueAt(row, 1);
                    if(item.detailsComponent instanceof String[]) {
                        String[] sArray = (String[])item.detailsComponent;
                        for (String aSArray : sArray) {
                            if (aSArray.equals("-"))
                                continue;
                            buffer.append(aSArray).append("<br>");
                        }
                    } else if(item.detailsComponent instanceof Map) {
                        Map map = (Map)item.detailsComponent;
                        Set keys = map.keySet();
                        for (Object key : keys) {
                            buffer.append(key).append(" = ").append(map.get(key)).append("<br>");
                        }
                    } else if(item.detailsComponent instanceof SLA[]) {
                        SLA[] slas = (SLA[])item.detailsComponent;
                        for (SLA sla : slas) {
                            buffer.append(sla.toString()).append("<br>");
                        }
                    } else if(item.detailsComponent instanceof LoggerConfig[]) {
                        LoggerConfig[] loggerConfigs =
                            (LoggerConfig[])item.detailsComponent;
                        for (LoggerConfig loggerConfig : loggerConfigs) {
                            buffer.append(loggerConfig.toString()).append("<br><br>");
                        }
                    } else if(item.detailsComponent instanceof AssociationDescriptor[]) {
                        AssociationDescriptor[] aDescs = (AssociationDescriptor[])item.detailsComponent;
                        for (AssociationDescriptor aDesc : aDescs)
                            buffer.append(aDesc.toString()).append("<br>");
                    } else if(item.detailsComponent instanceof ClassBundle) {
                        ClassBundle bundle = (ClassBundle)item.detailsComponent;
                        buffer.append(bundle.toString()).append("<br>");
                    } else {
                        buffer.append(item.attributeDescription.getText());
                        buffer.append("<br<br>");
                        buffer.append("More details needed ...");
                    }
                    buffer.append("</body>");
                    buffer.append("</html>");
                    details.textArea.setText(buffer.toString());
                    details.textArea.setCaretPosition(0);
                    details.textArea.moveCaretPosition(0);
                    dialog.getContentPane().add(details);
                    int width = 450;
                    int height = 300;
                    dialog.pack();
                    dialog.setSize(width, height);
                    //dialog.setResizable(false);
                    Dimension screenSize =
                        Toolkit.getDefaultToolkit().getScreenSize();
                    int widthLoc = screenSize.width / 2 - width / 2;
                    int heightLoc = screenSize.height / 2 - height / 2;
                    dialog.setLocation(widthLoc, heightLoc);
                    dialog.setVisible(true);
                }
            }
        });

        tcm.getColumn(2).setPreferredWidth(15);
        tcm.getColumn(2).setMaxWidth(15);
        tcm.getColumn(2).setResizable(false);

        /* Set column size for the instanceTable */
        tcm = instanceTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(85);
        tcm.getColumn(0).setMaxWidth(300);

        /* Set column size for the exportTable */
        tcm = exportJARTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(100);
        tcm.getColumn(0).setMaxWidth(300);

        /* Set column size for the importTable */
        tcm = implJARTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(100);
        tcm.getColumn(0).setMaxWidth(300);
    }

    /*
     * Refresh the display
     */
    void refresh() {
        try {
            OperationalString opString = opMgr.getOperationalString();
            ServiceElement[] elems = opString.getServices();
            for (ServiceElement elem : elems) {
                if (elem.equals(sElem)) {
                    sElem = elem;
                    break;
                }
            }
            showServiceElement(sElem, opMgr.getServiceBeanInstances(sElem), opMgr);
            invalidate();
        } catch(java.rmi.NoSuchObjectException e) {
            /* Reset all panels */
            DefaultTableModel tableModel = (DefaultTableModel)serviceAttributeTable.getModel();
            cleanTable(tableModel);
            tableModel = (DefaultTableModel)exportJARTable.getModel();
            cleanTable(tableModel);

            tableModel = (DefaultTableModel)instanceTable.getModel();
            cleanTable(tableModel);            
            serviceInstancesLabel.setText(instanceLabelText+" unknown");
        } catch(Exception e) {
            Util.showError(e, parent, "Could not refresh");
        }
    }

    /*
     * Set the ServiceElement to display
     */
    public void showServiceElement(ServiceElement sElem,
                                   ServiceBeanInstance[] instances,
                                   OperationalStringManager opMgr) {
        this.sElem = sElem;
        this.opMgr = opMgr;
        setInstanceTableLabel(sElem, (DefaultTableModel)instanceTable.getModel());
        DefaultTableModel tableModel = (DefaultTableModel)serviceAttributeTable.getModel();
        cleanTable(tableModel);
        String value="";
        ServiceAttributeTableItem tableItem = null;
        /*
        "Interfaces",
        "Service Implementation",
        "Planned Instances",
        "Groups",
        "Locators",
        "System Requirements",
        "SLAs",
        "Fault Detection Handler",
        "Max Per Machine"
        "Cluster",
        "Provision Type",
        "Match On Name";
        "Auto Advertise",
        "DiscoveryManagement Pooling",
        "Comment",
        "Organization",
        "Parameters",
        "Associations",
        "Configuration",
         "Loggers"
         */
        boolean addMore = false;
        String NOT_DECLARED = "{not declared}";
        for(int i=0; i<serviceElementProps.length; i++) {
            switch(i) {
                case 0: /* Interfaces */
                    ClassBundle[] exportBundle = sElem.getExportBundles();
                    String[] interfaceNames = new String[exportBundle.length];
                    for(int j = 0; j < interfaceNames.length; j++)
                        interfaceNames[j] = exportBundle[j].getClassName();
                    value = flatten(interfaceNames);
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    addMore = false;
                    break;
                case 1: /* Service Implementation */
                    if(sElem.getComponentBundle()!= null)
                        value = sElem.getComponentBundle().getClassName();
                    else
                        value = NOT_DECLARED;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    addMore = false;
                    break;
                case 2: /* Planned Instances */
                    int planned = sElem.getPlanned();
                    value = Integer.toString(planned);
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    addMore = false;
                    break;
                case 3: /* Groups */
                    value = flatten(sElem.getServiceBeanConfig().getGroups());
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    addMore = false;
                    break;
                case 4: /* Locators */
                    LookupLocator[] locators = sElem.getServiceBeanConfig().getLocators();
                    if(locators!=null && locators.length>0) {
                        String[] s = new String[locators.length];
                        for(int j=0; j<s.length; j++)
                            s[j] = locators[j].toString();
                        value = flatten(s);
                    } else {
                        value = NOT_DECLARED;
                    }
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    addMore = false;
                    break;
                case 5: /* System Requirements */
                    ServiceLevelAgreements sl = sElem.getServiceLevelAgreements();
                    SystemRequirements s = sl.getSystemRequirements();
                    tableItem = new ServiceAttributeTableItem();
                    if(s==null) {
                        value = NOT_DECLARED;
                        addMore = false;
                    } else {
                        String[] ids = s.getSystemThresholdIDs();
                        SystemComponent[] systemComponents = s.getSystemComponents();
                        if(ids.length>0 || systemComponents.length>0) {
                            java.util.List<String> sysRequirements = new ArrayList<String>();
                            StringBuilder buff = new StringBuilder();
                            for (String id : ids) {
                                if (buff.length() > 0) {
                                    buff.append(", ");
                                }
                                ThresholdValues tVal = s.getSystemThresholdValue(id);
                                String val = String.format("ID: %s, High: %s, Low: %s",
                                                           id,
                                                           tVal.getHighThreshold(),
                                                           tVal.getLowThreshold());
                                sysRequirements.add(val);
                                buff.append("[").append(val).append("]");
                            }
                            for (SystemComponent systemComponent : systemComponents) {
                                if (buff.length() > 0) {
                                    buff.append(", ");
                                }
                                sysRequirements.add(systemComponent.toString());
                                buff.append("[").append(systemComponent.toString()).append("]");
                            }
                            value = buff.toString();
                            addMore = true;
                            tableItem.detailsComponent = sysRequirements;
                        } else {
                            value = NOT_DECLARED;
                            addMore = false;
                        }
                    }
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 6: /* SLAs */
                    sl = sElem.getServiceLevelAgreements();
                    tableItem = new ServiceAttributeTableItem();
                    if(sl==null) {
                        value = NOT_DECLARED;
                        addMore = false;
                    } else {
                        SLA[] slas = sElem.getServiceLevelAgreements().getServiceSLAs();
                        if(slas.length>0) {
                            value = flattenSLAs(slas);
                            addMore = true;
                            tableItem.detailsComponent = slas;
                        } else {
                            value = NOT_DECLARED;
                            addMore = false;
                        }
                    }
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 7: /* Fault Detection Handler */
                    if(sElem.getFaultDetectionHandlerBundle()!=null) {
                        value = sElem.getFaultDetectionHandlerBundle().getClassName();
                        addMore = true;
                        tableItem = new ServiceAttributeTableItem();
                        tableItem.detailsComponent =
                            sElem.getFaultDetectionHandlerBundle();
                        tableItem.attributeDescription = new JLabel(value);
                    } else {
                        value = NOT_DECLARED;
                    }
                    break;
                case 8: /* Max Per Machine */
                    int maxPerMachine = sElem.getMaxPerMachine();
                    if(maxPerMachine==-1)
                        value = NOT_DECLARED;
                    else
                        value = Integer.toString(maxPerMachine);
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 9: /* Cluster */
                    String[] cluster = sElem.getCluster();
                    if(cluster.length>0)
                        value = flatten(cluster);
                    else
                        value = NOT_DECLARED;
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 10: /* Provision Type */
                    ProvisionType type =
                        sElem.getProvisionType();
                    value = "Dynamic";
                    if(type==ProvisionType.FIXED)
                        value = "Fixed";
                    else if(type==ProvisionType.EXTERNAL)
                        value = "External";
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 11: /* Match On Name */
                    value = (sElem.getMatchOnName()?"Yes":"No");
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 12: /* Auto Advertise */
                    value = (sElem.getAutoAdvertise()?"Yes":"No");
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 13: /* DiscoveryManagement Pooling */
                    value = (sElem.getDiscoveryManagementPooling()?"Yes":"No");
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 14: /* Comment */
                    value = (sElem.getServiceBeanConfig().getComment());
                    value = (value==null?NOT_DECLARED:value);
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 15: /* Organization */
                    value = (sElem.getServiceBeanConfig().getOrganization());
                    value = (value==null?NOT_DECLARED:value);
                    addMore = false;
                    tableItem = new ServiceAttributeTableItem();
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 16: /* Parameters */
                    tableItem = new ServiceAttributeTableItem();
                    if(sElem.getServiceBeanConfig().getInitParameters().size()>0) {
                        value = sElem.getServiceBeanConfig().getInitParameters().toString();
                        addMore = true;
                        tableItem.detailsComponent = sElem.getServiceBeanConfig().getInitParameters();
                    } else {
                        addMore = false;
                    }
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 17: /* Associations */
                    tableItem = new ServiceAttributeTableItem();
                    AssociationDescriptor[] aDescs =
                        sElem.getAssociationDescriptors();
                    if(aDescs.length==0) {
                        value = NOT_DECLARED;
                        addMore = false;
                    } else {
                        value = aDescs.length+" Associations";
                        addMore = true;
                        tableItem.detailsComponent = aDescs;
                    }
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 18: /* Configuration */
                    tableItem = new ServiceAttributeTableItem();
                    String[] config = sElem.getServiceBeanConfig().getConfigArgs();
                    if(config.length>1) {
                        value = flatten(config);
                        value = value.substring(3);
                        addMore = true;
                        tableItem.detailsComponent = config;
                    } else {
                        value = NOT_DECLARED;
                        addMore = false;
                    }
                    tableItem.attributeDescription = new JLabel(value);
                    break;
                case 19: /* Loggers */
                    tableItem = new ServiceAttributeTableItem();
                    LoggerConfig[] loggerConfigs = sElem.getServiceBeanConfig().getLoggerConfigs();
                    if(loggerConfigs == null) {
                        value = NOT_DECLARED;
                        addMore = false;
                    } else {
                        value = "Logging declared";
                        addMore = true;
                        tableItem.detailsComponent = loggerConfigs;
                    }
                    tableItem.attributeDescription = new JLabel(value);
                    break;

            }
            Object toAdd = new JLabel("");
            if(addMore)
                toAdd = more;
            tableModel.insertRow(i, new Object[] {serviceElementProps[i], tableItem, toAdd});
        }
        tableModel = (DefaultTableModel)exportJARTable.getModel();
        cleanTable(tableModel);
        URL[] jars = new URL[0];
        try {
            jars = sElem.getExportURLs();
        } catch(MalformedURLException e) {
            Util.showError(e, null, "Error getting export URLs for service");
        }
        String NO_VALUE = "--";
        if(jars.length==0)
            tableModel.insertRow(0, new Object[] {"Not Available", NO_VALUE, "", ""});
        else {
            addTableData(tableModel, jars);
        }
        tableModel = (DefaultTableModel)implJARTable.getModel();
        cleanTable(tableModel);
        /* If we have a dynamic service, set the Trim capability on */
        if(sElem.getProvisionType()==ProvisionType.DYNAMIC)
            trimServices.setEnabled(true);
        else
            trimServices.setEnabled(false);
        /* If we have an External service, disable increase, decrease, redeploy &
         * relocate */
        if(sElem.getProvisionType()==ProvisionType.EXTERNAL) {
            increaseService.setEnabled(false);
            serviceElementRedeploy.setEnabled(false);
            setControls(false);
            tableModel.insertRow(0, new Object[] {"Not Available", NO_VALUE, "", ""});
        } else {
            try {                
                increaseService.setEnabled(true);
                serviceElementRedeploy.setEnabled(true);
                setControls(false);                
                ClassBundle bundle = sElem.getComponentBundle();
                if(bundle!=null)
                    addTableData(tableModel, bundle.getJARs());
            } catch (Exception e) {
                Util.showError(e, null, "Problem reading attributes for service");
            }
        }
        setInstanceTableLabel(sElem, initializeInstanceTable(instances));
    }

    void setInstanceTableLabel(ServiceElement sElem, DefaultTableModel tableModel) {
        StringBuilder suffix = new StringBuilder();

        if(sElem.getProvisionType()==ProvisionType.DYNAMIC) {
            suffix.append("Planned (")
                .append(sElem.getPlanned())
                .append(")")
                .append("<br>");
        } else if(sElem.getProvisionType()==ProvisionType.FIXED) {
            suffix.append("Planned (")
                .append(sElem.getPlanned())
                .append(") per Cybernode");
        }

        if(sElem.getMaxPerMachine()!=-1) {
            String boundary = sElem.getMachineBoundary().toString().toLowerCase();
            suffix.append("<br>")
                .append("Max Per ")
                .append(boundary)
                .append(" machine (")
                .append(sElem.getMaxPerMachine())
                .append(")");
        }

        serviceInstancesLabel.setText("<html>"+instanceLabelText+
                                      " ("+tableModel.getRowCount()+"), "+
                                      suffix+"</html>");
    }

    DefaultTableModel initializeInstanceTable(ServiceBeanInstance[] instances) {
        DefaultTableModel tableModel = (DefaultTableModel)instanceTable.getModel();
        cleanTable(tableModel);
        if(instances!=null) {
            for(int i=0; i<instances.length; i++) {
                String hostAddress = instances[i].getHostAddress();
                if(hostAddress==null) {
                    hostAddress = "Unknown";
                    try {
                        if(instances[i].getService() instanceof ServiceRegistrar) {
                            hostAddress = ((ServiceRegistrar)instances[i].
                                                             getService()).
                                                             getLocator().getHost();
                        }
                    } catch (Exception e) {
                        hostAddress = "Unknown";
                    }

                }
                tableModel.insertRow(i,
                                     new Object[] {hostAddress, instances[i]});
            }
        }
        return(tableModel);
    }

    /**
     * Data structure holding a descriptive name and
     * an optional Object that may have details processing
     */
    class ServiceAttributeTableItem {
        JLabel attributeDescription;
        Object detailsComponent;
    }

    /**
     * An implementation of an ActionListener to handle redeployment requests
     */
    class RedeployHandler implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            JDialog dialog = new JDialog((JFrame)null, "Redeploy Options", true);
            RedeployPanel redeployPanel;
            try {                                
                redeployPanel = new RedeployPanel(dialog, -1);
            } catch (Exception e) {
                Util.showError(e, null,
                               "Problem getting schedule duration from OpStringManager");
                return;
            }
            dialog.getContentPane().add(redeployPanel);
            int width = 302;
            int height = 260;
            dialog.pack();
            dialog.setSize(width, height);
            //dialog.setResizable(false);
            //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            //int widthLoc = screenSize.width / 2 - width / 2;
            //int heightLoc = screenSize.height / 2 - height / 2;
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);

            if(redeployPanel.getOption()==RedeployPanel.DISMISS_OPTION)
                return;
            try {
                if(ae.getActionCommand().equals("ServiceElement")) {
                    opMgr.redeploy(sElem,
                                   null,
                                   redeployPanel.getClean(),
                                   redeployPanel.getDelay(),
                                   null);

                } else {
                    ServiceBeanInstance instance =
                        (ServiceBeanInstance)instanceTable.getModel().
                                      getValueAt(instanceTable.getSelectedRow(), 1);
                    opMgr.redeploy(null,
                                   instance,
                                   redeployPanel.getClean(),
                                   redeployPanel.getDelay(),
                                   null);
                }
            } catch (Exception e) {
                Util.showError(e, null, "Could not redeploy service");
            }
        }
    }

    /**
     * Display details for a ServiceElement property
     */
    class Details extends javax.swing.JPanel {
        javax.swing.JEditorPane textArea;
        Details(final JDialog dialog) {
            super();
            setLayout(new GridBagLayout());
            textArea = new javax.swing.JEditorPane();
            textArea.setEditable(false);
            textArea.setContentType("text/html");
            java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.weighty = 0.1;
            gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);

            add(new javax.swing.JScrollPane(textArea), gridBagConstraints);
            JButton dismiss = new JButton("Dismiss");
            dismiss.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                   dialog.dispose();
               }
            });

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            add(dismiss, gridBagConstraints);
        }
    }


    /*
     * Enable/disable service control buttons
     */
    void setControls(boolean enabled) {
        decreaseService.setEnabled(enabled);
        serviceRedeploy.setEnabled(enabled);
        relocateService.setEnabled(enabled);
    }

    /*
     * Remove all entries in a TableModel
     */
    void cleanTable(DefaultTableModel tableModel) {
        int rows = tableModel.getRowCount();
        for(int i=rows-1; i>=0; i--)
            tableModel.removeRow(i);
    }

    /*
     * Add row to a table which lists JARs
     */
    void addTableData(DefaultTableModel tableModel, URL[] jars) {
        String jarName;
        String codebase;
        for(int i=0; i<jars.length; i++) {
            String url = jars[i].toExternalForm();
            int index = url.lastIndexOf('/');
            if(url.startsWith("http:")) {
                jarName = url.substring(index+1);
            } else if(url.startsWith("file:")) {
                jarName = url.substring(index+1);
            } else {
                jarName = url;
            }
            codebase = url.substring(0, index);
            tableModel.insertRow(i, new Object[] {jarName, codebase});
        }
    }

    /*
     * Convert an Array od SLA instances to a String
     */
    String flattenSLAs(SLA[] slas) {
        StringBuilder buff = new StringBuilder();
        for(int i=0; i<slas.length; i++) {
            if(i>0)
                buff.append(", ");
            buff.append("[ID=").append(slas[i].getIdentifier()).append(" " + "High=").append(slas[i].getHighThreshold()).append(" " + "Low=").append(slas[i].getLowThreshold()).append("]");
        }
        return(buff.toString());
    }

    /*
     * Convert a String array to a comma separated String
     */
    String flatten(String[] in) {
        if (in == null)
            return "";
        StringBuilder buff = new StringBuilder();
        for(int i=0; i<in.length; i++) {
            if(i>0)
                buff.append(", ");
            buff.append(in[i]);
        }
        return(buff.toString());
    }


    /*
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents(javax.swing.JPanel serviceElementPropsPanel,
                                javax.swing.JPanel serviceInstanceProps,
                                javax.swing.JPanel jarPanel) {
        java.awt.GridBagConstraints gridBagConstraints;

        JLabel exportJARLabel = new JLabel();
        JLabel implJARLabel = new JLabel();
        JScrollPane jScrollPane1 = new JScrollPane();
        exportJARTable = new javax.swing.JTable();
        JScrollPane jScrollPane2 = new JScrollPane();
        instanceTable = new javax.swing.JTable();
        JScrollPane jScrollPane3 = new JScrollPane();
        implJARTable = new javax.swing.JTable();
        JPanel jPanel1 = new JPanel();
        increaseService = new javax.swing.JButton();
        decreaseService = new javax.swing.JButton();
        trimServices = new javax.swing.JButton();
        relocateService = new javax.swing.JButton();
        serviceRedeploy = new javax.swing.JButton();
        serviceElementRedeploy = new javax.swing.JButton();
        serviceInstancesLabel = new javax.swing.JLabel();

        //serviceElementPropertiesLabel = new javax.swing.JLabel();
        JScrollPane jScrollPane4 = new JScrollPane();
        serviceAttributeTable = new javax.swing.JTable();

        jarPanel.setLayout(new java.awt.GridBagLayout());
        serviceElementPropsPanel.setLayout(new java.awt.GridBagLayout());
        serviceInstanceProps.setLayout(new java.awt.GridBagLayout());

        jarPanel.setBorder(new javax.swing.border.CompoundBorder(
                               new javax.swing.border.EtchedBorder(
                                   javax.swing.border.EtchedBorder.RAISED),
                                   new javax.swing.border.EmptyBorder(
                                       new java.awt.Insets(8, 8, 8, 8))));
        serviceElementPropsPanel.setBorder(
                       new javax.swing.border.CompoundBorder(
                           new javax.swing.border.EtchedBorder(
                               javax.swing.border.EtchedBorder.RAISED),
                               new javax.swing.border.EmptyBorder(
                                                 new java.awt.Insets(8, 8, 8, 8))));
        serviceInstanceProps.setBorder(
                       new javax.swing.border.CompoundBorder(
                           new javax.swing.border.EtchedBorder(
                               javax.swing.border.EtchedBorder.RAISED),
                               new javax.swing.border.EmptyBorder(
                                                 new java.awt.Insets(8, 8, 8, 8))));
        jarPanel.setPreferredSize(new java.awt.Dimension(500, 500));
        serviceElementPropsPanel.setPreferredSize(new java.awt.Dimension(500, 500));
        exportJARLabel.setText("Export (download) JARs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jarPanel.add(exportJARLabel, gridBagConstraints);

        implJARLabel.setText("Implementation JARs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jarPanel.add(implJARLabel, gridBagConstraints);

        exportJARTable.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        exportJARTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {"JAR", "Codebase"}) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        exportJARTable.setToolTipText("List of Export (download) JARs for the service");
        jScrollPane1.setViewportView(exportJARTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jarPanel.add(jScrollPane1, gridBagConstraints);

        serviceInstancesLabel.setText("Service Instances");
        serviceInstancesLabel.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        //TODO
        serviceInstanceProps.add(serviceInstancesLabel, gridBagConstraints);

        jScrollPane2.setPreferredSize(new java.awt.Dimension(454, 65));
        instanceTable.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        instanceTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Compute Resource", "Instance"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        instanceTable.setToolTipText("Service instance table");
        jScrollPane2.setViewportView(instanceTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 2);
        // TODO
        serviceInstanceProps.add(jScrollPane2, gridBagConstraints);

        implJARTable.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        implJARTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "JAR", "Classpath"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        jScrollPane3.setViewportView(implJARTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jarPanel.add(jScrollPane3, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridLayout(3, 2));

        increaseService.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        increaseService.setText("Increase");
        increaseService.setToolTipText("Increase the number of planned service instances");
        jPanel1.add(increaseService);

        decreaseService.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        decreaseService.setText("Decrease");
        decreaseService.setToolTipText("Decrease the number of planned service");
        jPanel1.add(decreaseService);

        trimServices.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        trimServices.setText("Trim");
        trimServices.setToolTipText("Trim pending service requests");
        jPanel1.add(trimServices);

        relocateService.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        relocateService.setText("Relocate");
        relocateService.setToolTipText("Relocate a selected service instance");
        jPanel1.add(relocateService);

        serviceRedeploy.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        serviceRedeploy.setText("Redeploy");
        serviceRedeploy.setToolTipText("Redeploy a selected service instance");
        serviceRedeploy.setActionCommand("ServiceBeanInstance");
        serviceRedeploy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serviceRedeployActionPerformed(evt);
            }
        });
        jPanel1.add(serviceRedeploy);

        serviceElementRedeploy.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        serviceElementRedeploy.setText("Redeploy All");
        serviceElementRedeploy.setToolTipText("Redeploy all service instances");
        serviceElementRedeploy.setActionCommand("ServiceElement");
        jPanel1.add(serviceElementRedeploy);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        //TODO
        serviceInstanceProps.add(jPanel1, gridBagConstraints);

        jScrollPane4.setBackground(new java.awt.Color(204, 204, 255));
        serviceAttributeTable.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(4, 4, 4, 4)));
        serviceAttributeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Property", "Value", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        serviceAttributeTable.setGridColor(new java.awt.Color(218, 227, 254));
        jScrollPane4.setViewportView(serviceAttributeTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        serviceElementPropsPanel.add(jScrollPane4, gridBagConstraints);

    }
    // </editor-fold>//GEN-END:initComponents

    private void serviceRedeployActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }//GEN-LAST:event_serviceRedeployActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
 private javax.swing.JButton decreaseService;
    private javax.swing.JTable exportJARTable;
    private javax.swing.JTable implJARTable;
    private javax.swing.JButton increaseService;
    private javax.swing.JTable instanceTable;
    private javax.swing.JButton relocateService;
    private javax.swing.JTable serviceAttributeTable;
    //private javax.swing.JLabel serviceElementPropertiesLabel;
    private javax.swing.JButton serviceElementRedeploy;
    private javax.swing.JLabel serviceInstancesLabel;
    private javax.swing.JButton serviceRedeploy;
    private javax.swing.JButton trimServices;
    // End of variables declaration//GEN-END:variables

}

