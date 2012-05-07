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

import net.jini.admin.Administrable;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.ServiceInfo;
import net.jini.lookup.entry.ServiceType;
import net.jini.lookup.ui.factory.JComponentFactory;
import net.jini.lookup.ui.factory.JDialogFactory;
import net.jini.lookup.ui.factory.JFrameFactory;
import net.jini.lookup.ui.factory.JWindowFactory;
import org.rioproject.entry.ComputeResourceInfo;
import org.rioproject.serviceui.UILoader;
import org.rioproject.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;

/**
 * The ServiceUIPanel class aggregates service ui objects that have been
 * added as attributes to a service's attribute set using a JTabbedPane.
 * The ServiceUIPanel looks for all UI entries that have a role of Admin set.
 *
 * <p> The ServiceUIPanel class additionally has some built in panels that it
 * displays via tabs that provide:
 * <br>
 * <ul>
 * <li>ServiceType details
 * <li>ServiceInfo details
 * <li>Join details
 * </ul>
 *
 * @author Dennis Reedy
 */
public class ServiceUIPanel extends JPanel {
    /** Various push buttons */
    JButton dismissB, terminateB;
    /** The ServiceItem for the service */
    ServiceItem item=null;
    /** A tabbed pane to display JComponents */
    JTabbedPane tabpane;
    /** Optional image to display */
    Image image=null;
    /** base JPanel to layout components onto */
    JPanel base;
    Object[] uiComponents;
    /* List of loaded JComponent instances */
    java.util.List<JComponent> jComponentList = new ArrayList<JComponent>();
    Container parent;

    public ServiceUIPanel(ServiceItem item, long startupDelay, Container parent)
        throws Exception {
        super(new BorderLayout());
        this.item = item;
        this.parent = parent;
        Entry[] attrs = item.attributeSets;

        base = new JPanel();
        base.setLayout(new BorderLayout(8, 8));
        tabpane = new JTabbedPane();

        String svcName;
        ServiceType type = getServiceType(attrs);
        if(type!=null) {
            svcName = type.getDisplayName();
        } else {
            svcName = item.service.getClass().getName();
        }

        ComputeResourceInfo computeResourceInfo = getComputeResourceInfo(attrs);

        java.util.Timer taskTimer = new java.util.Timer(true);
        long now = System.currentTimeMillis();
        UIComponentFetcher uiFetcher = new UIComponentFetcher();
        WaitingToLoadTask waitingToLoadTask;
        if(computeResourceInfo!=null)
            waitingToLoadTask = new WaitingToLoadTask(svcName,
                                                      computeResourceInfo.hostName,
                                                      computeResourceInfo.hostAddress);
        else
            waitingToLoadTask = new WaitingToLoadTask(svcName,
                                                      "unknown",
                                                      "unknown");
        TimeoutTask timeoutTask = new TimeoutTask(uiFetcher);
        taskTimer.schedule(waitingToLoadTask,
                           new Date(now+Math.min(1000*5, startupDelay)));
        taskTimer.schedule(timeoutTask, new Date(now+startupDelay));
        uiFetcher.start();
        try {
            uiFetcher.join(startupDelay);
        } catch(InterruptedException e) {
            throw new Exception("Unable to obtain Service UI attributes for "+
                                svcName+" in allotted time of "+
                                (startupDelay/1000)+" seconds");
        }
        waitingToLoadTask.cancel();
        if(waitingToLoadTask.dialog!=null)
            waitingToLoadTask.dialog.dispose();
        if(uiFetcher.isInterrupted())
            throw new Exception("Unable to obtain Service UI attributes for "+
                                svcName+" in allotted time of "+
                                (startupDelay/1000)+" seconds");
        timeoutTask.cancel();
        if(uiFetcher.exception!=null)
            throw uiFetcher.exception;

        ServiceInfo info = getServiceInfo(attrs);
        if(type!=null || info!=null) {
            JPanel svcTypePanel = new JPanel();
            svcTypePanel.setLayout(new BoxLayout(svcTypePanel, BoxLayout.Y_AXIS));

            ServiceTypePanel typePanel = new ServiceTypePanel();
            typePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

            if(type!=null) {
                image = type.getIcon(0);
                typePanel.setServiceType(type);
            }

            svcTypePanel.add(Box.createVerticalStrut(8));
            svcTypePanel.add(typePanel);
            ServiceInfoPanel sInfoPanel = new ServiceInfoPanel();
            sInfoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
            svcTypePanel.add(Box.createVerticalStrut(8));
            svcTypePanel.add(sInfoPanel);

            if(info!=null)
                sInfoPanel.setServiceInfo(info);

            svcTypePanel.add(Box.createVerticalGlue());
            tabpane.add("Service Information", svcTypePanel);
        }

        if(computeResourceInfo!=null) {
            ComputeResourceInfoPanel computeResourceInfoPanel = new ComputeResourceInfoPanel();
            computeResourceInfoPanel.setComputeResourceInfo(computeResourceInfo);
            tabpane.add("Host Attributes", computeResourceInfoPanel);
        }

        JPanel buttonPanel = new JPanel();
        //buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        if(!(parent instanceof JInternalFrame)) {
            dismissB = new JButton("Close");
            dismissB.setToolTipText("Close the dialog");
            dismissB.addActionListener(new DismissHandler());
            buttonPanel.add(dismissB);

            if(item.service instanceof Administrable) {
                terminateB = new JButton("Terminate");
                terminateB.setToolTipText("Terminate (destroy) the service");
                terminateB.addActionListener(new ServiceTerminator(item,
                                                                   parent));
                buttonPanel.add(terminateB);
            }
        }

        base.add(tabpane,BorderLayout.CENTER);
        base.add(buttonPanel, BorderLayout.SOUTH);

        add("Center", base);
        add("South", buttonPanel);
    }

    public Object[] getUIComponents() {
        if(uiComponents==null)
           return(new Object[0]);
        Object[] comps = new Object[uiComponents.length];
        System.arraycopy(uiComponents, 0, comps, 0, uiComponents.length);
        return(comps);
    }

    public class DismissHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Util.dispose(parent);
        }
    }

    private JComponent getComponent(Object factory, Object item) throws Exception {
        JComponent uiComponent = null;
        Class factoryClass = factory.getClass();
        if(JFrameFactory.class.isAssignableFrom(factoryClass))
            uiComponent = new LaunchPanel(factory, item);
        else if(JWindowFactory.class.isAssignableFrom(factoryClass))
            uiComponent = new LaunchPanel(factory, item);
        else if(JComponentFactory.class.isAssignableFrom(factoryClass)) {
            JComponentFactory fac = (JComponentFactory)factory;
            uiComponent = fac.getJComponent(item);
            String name = uiComponent.getAccessibleContext().getAccessibleName();
            if(name==null)
                uiComponent.getAccessibleContext().setAccessibleName(
                                                  uiComponent.getClass().getName());

        } else if(JDialogFactory.class.isAssignableFrom(factoryClass))
            uiComponent = new LaunchPanel(factory, item);

        return(uiComponent);
    }

    class UIComponentFetcher extends Thread {
        Exception exception;

        public void run() {
            try {
                uiComponents = UILoader.loadUI(item);
                if(uiComponents!=null) {
                    String name;
                    for(int i=uiComponents.length-1; i>=0; i--) {
                        if(isInterrupted())
                            break;
                        JComponent jComp = getComponent(uiComponents[i], item);
                        if(jComp instanceof JPanel) {
                            name =
                                jComp.getAccessibleContext().getAccessibleName();
                            tabpane.add(name, jComp);
                            jComponentList.add(jComp);
                        } else {
                            System.err.println("Unsupported UI class");
                        }
                    }
                }
            } catch(Exception e) {
                exception = e;
            }
        }
    }

    class TimeoutTask extends TimerTask {
        Thread thread;

        TimeoutTask(Thread thread) {
            super();
            this.thread = thread;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            thread.interrupt();
            System.out.println("Cancelled Admin Viewer startup");
        }
    }

    class WaitingToLoadTask extends TimerTask {
        String svcName;
        String hostName;
        String hostAddress;
        JDialog dialog;

        WaitingToLoadTask(String svcName, String hostName, String hostAddress) {
            super();
            this.svcName = svcName;
            this.hostName = hostName;
            this.hostAddress = hostAddress;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            JOptionPane pane = new JOptionPane("<html>"+
                                               "<font color=black>"+
                                               "Waiting to load UI Components for "+
                                               "the </font><br>"+
                                               "<font color=blue>"+svcName+"</font>"+
                                               "<font color=black> service running "+
                                               "on<br>"+
                                               "machine: <font color=blue>"+
                                               hostName+
                                               "</font><font color=black> address: "+
                                               "</font><font color=blue>"+
                                               hostAddress+"</font><br>"+
                                               "<font color=black>"+
                                               "The timeframe is longer then "+
                                               "expected,<br>"+
                                               "verify network connections."+
                                               "</font></html>",
                                               JOptionPane.WARNING_MESSAGE);
            dialog = pane.createDialog(null, "Network Delay");
            dialog.setModal(false);
            dialog.setVisible(true);
        }
    }

    class LaunchPanel extends JPanel {
        Object uiObj;
        LaunchPanel(final Object factory, final Object item) {
            super();
            String info=factory.getClass().getName();
            getAccessibleContext().setAccessibleName(info);
            JButton button = new JButton("Launch "+info);
            button.addActionListener(
                new ActionListener() {
                    final Class factoryClass = factory.getClass();
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            if(JFrameFactory.class.isAssignableFrom(factoryClass))
                                launchFrameUI(factory, item);
                            else if(JWindowFactory.class.isAssignableFrom(factoryClass))
                                launchWindowUI(factory, item);
                            else if(JDialogFactory.class.isAssignableFrom(factoryClass))
                                launchDialogUI(factory, item);
                        } catch(Exception e) {
                            e.printStackTrace();
                            Util.showError("Exception loading "+
                                           "["+factoryClass.getName()+"]\n"+
                                           e.getMessage(),
                                           null,
                                           "Could not launch " +
                                           "["+factoryClass.getName()+"]");
                        }
                    }
                });
            add(button);
        }

        private void launchFrameUI(Object factory, Object item) throws
                                                                IOException, ClassNotFoundException {
            JFrameFactory fac = (JFrameFactory)factory;
            JFrame frame = fac.getJFrame(item);
            frame.validate();
            frame.setSize(frame.getSize());
            frame.setLocation(frame.getLocation());
            frame.pack();
            frame.setVisible(true);
            uiObj = frame;
        }

        private void launchWindowUI(Object factory, Object item) throws IOException, ClassNotFoundException {
            JWindowFactory fac = (JWindowFactory)factory;
            JWindow win = fac.getJWindow(item);
            win.validate();
            win.pack();
            win.setVisible(true);
            uiObj = win;
        }

        private void launchDialogUI(Object factory, Object item) throws IOException, ClassNotFoundException {
            JDialogFactory fac = (JDialogFactory)factory;
            JDialog d = fac.getJDialog(item);
            d.validate();
            d.pack();
            d.setVisible(true);
            uiObj = d;
        }
    }

    private ComputeResourceInfo getComputeResourceInfo(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ComputeResourceInfo) {
                return (ComputeResourceInfo) attr;
            }
        }
        return(null);
    }

    private ServiceInfo getServiceInfo(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ServiceInfo) {
                return (ServiceInfo) attr;
            }
        }
        return(null);
    }

    private ServiceType getServiceType(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ServiceType) {
                return (ServiceType) attr;
            }
        }
        return(null);
    }
}
