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
import net.jini.io.UnsupportedConstraintException;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.system.ComputeResourceUtilization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * A ServiceUI for the Cybernode
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class CybernodeUI extends JPanel implements Runnable {
    private ServiceTable serviceTable;
    private ServiceTable execServiceTable;
    private JTextField maxSvcCount, currentSvcCount, utilizationTF;
    private Cybernode cybernode;
    private CybernodeAdmin cybernodeAdmin;
    private JButton apply;
    private Integer svcCount, maxCount;
    private boolean keepAlive=true;
    private transient Thread thread;

    public CybernodeUI(final Object arg) {
        super();
        getAccessibleContext().setAccessibleName("Cybernode admin");
        ServiceItem item = (ServiceItem) arg;
        cybernode = (Cybernode) item.service;
        ComputeResourceUtilization computeResource;
        try {
            cybernodeAdmin = (CybernodeAdmin)cybernode.getAdmin();
            computeResource = getComputeResourceUtilization(cybernodeAdmin);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //idTable = new Hashtable();
        JPanel qPanel = new JPanel();
        qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.X_AXIS));
        qPanel.setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Compute Resource"),
                             BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        qPanel.add(new JLabel("Description"));
        qPanel.add(Box.createHorizontalStrut(8));

        String field="unknown: system error";
        if(computeResource!=null)
            field = computeResource.getDescription();
        JTextField descTF = new JTextField(field);
        descTF.setEnabled(false);
        qPanel.add(descTF);
        qPanel.add(Box.createHorizontalStrut(8));
        qPanel.add(new JLabel("Utilization"));
        qPanel.add(Box.createHorizontalStrut(8));
        if(computeResource!=null)
            field = computeResource.getUtilization().toString();
        utilizationTF = new JTextField(field);
        utilizationTF.setEnabled(false);
        qPanel.add(utilizationTF);

        JPanel cPanel = new JPanel();
        cPanel.setLayout(new GridLayout(0, 3, 4, 4));
        cPanel.setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Cybernode Attributes"),
                             BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        maxSvcCount = new JTextField();
        currentSvcCount = new JTextField();
        currentSvcCount.setEnabled(false);
        apply = new JButton("Apply");
        apply.addActionListener(new ApplyHandler());
        maxCount = 0;
        svcCount = 0;
        try {
            maxCount = cybernodeAdmin.getServiceLimit();
            maxSvcCount.setText(maxCount.toString());
            svcCount = cybernodeAdmin.getServiceCount();
            currentSvcCount.setText(svcCount.toString());
        } catch(RemoteException re) {
            re.printStackTrace();
        }
        cPanel.add(new JLabel("Services Created"));
        cPanel.add(currentSvcCount);
        cPanel.add(new JLabel(""));
        cPanel.add(new JLabel("Service Limit"));
        cPanel.add(maxSvcCount);
        cPanel.add(apply);

        JPanel cq = new JPanel();
        cq.setLayout(new BorderLayout());
        cq.add(BorderLayout.NORTH, qPanel);
        cq.add(BorderLayout.CENTER, cPanel);

        serviceTable = new ServiceTable("Contained Services", false);
        execServiceTable = new ServiceTable("Forked/Exec'd Services", true);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        /*
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.add(serviceTable);
        tablePanel.add(Box.createHorizontalStrut(8));
        tablePanel.add(execServiceTable);
        */
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setTopComponent(serviceTable);
        splitPane.setBottomComponent(execServiceTable);
        splitPane.setDividerLocation(150);

        add(BorderLayout.NORTH, cq);
        //add(BorderLayout.CENTER, tablePanel);
        add(BorderLayout.CENTER, splitPane);

        thread = new Thread(this);
        thread.start();
    }

    /**
     * Override to ensure the thread that is created is interrupted
     */
    public void removeNotify() {
        terminate();
    }

    public void run() {
        if(cybernode==null) {
            showError("Reference to Cybernode is null");
            return;
        }
        while(!thread.isInterrupted() && keepAlive) {
            try {
                setUIFields();
                try {
                    Thread.sleep(1000*60);
                } catch(InterruptedException e) {
                    if(!keepAlive)
                        break;
                }
            } catch(Exception e) {
                if(!keepAlive)
                    break;
                e.printStackTrace();
                showError("Cybernode Connectivity Error",e);
                if(apply!=null)
                    apply.setEnabled(false);
                break;
            }
        }
    }

    void setUIFields() throws RemoteException {
        serviceTable.clear();
        execServiceTable.clear();
        ServiceRecord[] records =
            cybernode.getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD);
        for (ServiceRecord record : records) {
            if(record.getServiceElement().forkService() || record.getServiceElement().getExecDescriptor()!=null) {
                execServiceTable.addService(record);
            } else {
                serviceTable.addService(record);
            }
        }
        currentSvcCount.setText(getCybernodeCount().toString());
        Double utilization = cybernodeAdmin.getResourceCapability().getUtilization();
        utilizationTF.setText(utilization.toString());
    }

    public void terminate() {
        keepAlive=false;
        if(thread!=null) {
            thread.interrupt();
            thread = null;
        }
        if(apply!=null)
            apply.setEnabled(false);
    }


    protected void showInformation(String text) {
        JOptionPane.showMessageDialog(this, text,
                                      "Information Message",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    protected void showError(String text) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(this, text,
                                      "System Error", JOptionPane.ERROR_MESSAGE);
    }

    protected void showError(String title, Throwable t) {
        Toolkit.getDefaultToolkit().beep();
        showError(t, this, title);
    }

    JTextField  createAttrTextField() {
        JTextField tf = new JTextField();
        tf.setEnabled(false);
        return(tf);
    }

    Integer getCybernodeCount() {
        try {
            svcCount = cybernodeAdmin.getServiceCount();
        } catch(RemoteException re) {
            re.printStackTrace();
        }
        return(svcCount);
    }

    private ComputeResourceUtilization getComputeResourceUtilization(CybernodeAdmin cAdmin)
    throws RemoteException {
        return(cAdmin.getComputeResourceUtilization());
    }

    class ApplyHandler implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            try {
                Integer newCount;
                Integer count = cybernodeAdmin.getServiceLimit();
                String s = maxSvcCount.getText();
                try {
                    newCount = Integer.valueOf(s);
                } catch(Exception e) {
                    showError("You must enter a valid number");
                    return;
                }
                if(count.equals(newCount))
                    return;

                String message = "Set service limit to ["+s+"] ?";
                String title = "Cybernode Service Configuration Change Confirmation";
                int answer = JOptionPane.showConfirmDialog(
                                                          null, message, title,
                                                          JOptionPane.YES_NO_OPTION);
                if(answer==JOptionPane.NO_OPTION)
                    return;
                cybernodeAdmin.setServiceLimit(newCount);
                maxCount = cybernodeAdmin.getServiceLimit();
            } catch(RemoteException e) {
                e.printStackTrace();
                showError("Cybernode Connectivity Error", e);
            }
        }
    }

    /**
     * Show an exception in a Dialog
     *
     * @param e The exception
     * @param comp The parent component
     * @param title The title to use
     */
    public static void showError(Throwable e, Component comp, String title) {
        if (e.getCause() != null &&
            e.getCause() instanceof UnsupportedConstraintException) {
            Throwable cause = e.getCause();
            JOptionPane.showMessageDialog(null,
                                          "<html>" +
                                          "Exception: <font color=red>" +
                                          cause.getClass().getName() +
                                          "</font><br><br> " +
                                          "You do not have permission to perform " +
                                          "the action</html>",
                                          "Action Denied",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        StringBuilder buffer = new StringBuilder();
        Throwable thrown = e;
        if (thrown.getCause() != null)
            thrown = thrown.getCause();
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement aTrace : trace) {
            buffer.append("at ").append(aTrace).append("<br>");
        }
        showError("<html>Exception : <font color=red>" +
                  thrown.getClass().getName() + "</font>" +
                  " : " +
                  "<font color=blue>" +
                  thrown.getLocalizedMessage() + "</font>" +
                  "<br>" +
                  buffer.toString() +
                  "</html>",
                  comp,
                  title);
    }

    /**
     * Show an error in a Dialog
     *
     * @param text The text to show
     * @param comp The parent component
     * @param title The title to use
     */
    public static void showError(String text, Component comp, String title) {
        JDialog dialog;
        if (comp instanceof Dialog) {
            dialog = new JDialog((Dialog) comp);
        } else if (comp instanceof Frame) {
            dialog = new JDialog((Frame) comp);
        } else {
            dialog = new JDialog();
        }
        JEditorPane errorPane = new JEditorPane();
        errorPane.setEditable(false);
        errorPane.setContentType("text/html");
        errorPane.setText(text);
        errorPane.setCaretPosition(0);
        errorPane.moveCaretPosition(0);
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout(8, 8));
        pane.add(new JScrollPane(errorPane), BorderLayout.CENTER);
        JButton dismiss = new JButton("Close");
        dismiss.addActionListener(new DisposeActionListener(dialog));
        JPanel buttonPane = new JPanel();
        buttonPane.add(dismiss);
        pane.add(buttonPane, BorderLayout.SOUTH);
        dialog.getContentPane().add(pane);
        int width = 650;
        int height = 300;
        dialog.pack();
        dialog.setSize(width, height);
        if(comp==null) {
            Dimension screenSize =
                Toolkit.getDefaultToolkit().getScreenSize();
            int widthLoc = screenSize.width / 2 - width / 2;
            int heightLoc = screenSize.height / 2 - height / 2;
            dialog.setLocation(widthLoc, heightLoc);
        } else {
            dialog.setLocationRelativeTo(comp);
        }
        dialog.setTitle(title);
        dialog.setVisible(true);
    }

    public static class DisposeActionListener implements ActionListener {
        JDialog dialog;

        public DisposeActionListener(JDialog dialog) {
            this.dialog = dialog;
        }

        public void actionPerformed(ActionEvent ae) {
            dialog.dispose();
        }
    }
}
