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

import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.tools.ui.Constants;
import org.rioproject.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

/**
 * Manages creation of service admin user interfaces
 *
 * @author Dennis Reedy
 */
public class ServiceAdminManager {
    private final Map<ServiceID, JFrame> adminFrameMap = new HashMap<ServiceID, JFrame>();
    private final Map<ServiceElement, JFrame> adminDesktopFrameMap =
        new HashMap<ServiceElement, JFrame>();
    private final AdminLaunchHelper adminLaunchHelper = new AdminLaunchHelper();
    private Dimension lastAdminFrameDim;
    private Point lastAdminFramePos;
    private String lastAdminWindowLayout;
    private static ServiceAdminManager instance = new ServiceAdminManager();

    private ServiceAdminManager() {}

    public static ServiceAdminManager getInstance() {
        return(instance);
    }

    public Dimension getLastAdminFrameSize() {
        return lastAdminFrameDim;
    }

    public Point getLastAdminFrameLocation() {
        return lastAdminFramePos;
    }

    public String getLastAdminWindowLayout() {
        return lastAdminWindowLayout;
    }

    public void setAdminFrameProperties(Properties props) {
        String s = props.getProperty(Constants.ADMIN_FRAME_WIDTH);
        int width = (s==null?490:Integer.parseInt(s));
        s = props.getProperty(Constants.ADMIN_FRAME_HEIGHT);
        int height = (s==null?505:Integer.parseInt(s));
        lastAdminFrameDim = new Dimension(width, height);

        s = props.getProperty(Constants.ADMIN_FRAME_X_POS);
        if(s!=null) {
            double xPos = Double.parseDouble(s);
            double yPos =
                Double.parseDouble(props.getProperty(Constants.ADMIN_FRAME_Y_POS));
            lastAdminFramePos = new Point((int)xPos, (int)yPos);
        } else {
            lastAdminFramePos = new Point(50, 100);
        }

        lastAdminWindowLayout =
            props.getProperty(Constants.ADMIN_FRAME_WINDOW_LAYOUT,
                              Constants.ADMIN_FRAME_WINDOW_TILE);
    }

    /**
     * Shows the ServiceUI in a JFrame
     *
     * @param item The ServiceItem for the service
     * @param component The component to position the admin ui relative to
     */
    public void doShowAdminUI(final ServiceItem item, final Component component) {
        if(item==null) {
            JOptionPane.showMessageDialog(null,
                                          "There is no ServiceItem for []",
                                          "Missing ServiceItem",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFrame frame;
        if (adminFrameMap.containsKey(item.serviceID)) {
            frame = adminFrameMap.get(item.serviceID);
            int state = frame.getState();
            if (state == JFrame.ICONIFIED)
                frame.setState(JFrame.NORMAL);
            frame.requestFocus();
            return;
        }

        final Runnable createAdminViewer = new Runnable() {
            public void run() {
                try {
                    AdminFrame admin = new AdminFrame(item, component);
                    admin.addWindowListener(
                        new AdminFrameListener(item.serviceID, admin));
                    adminFrameMap.put(item.serviceID, admin);
                } catch (Exception ex) {
                    Util.showError(ex, null, "Could not create AdminFrame");
                }
            }
        };
        adminLaunchHelper.push(createAdminViewer);
    }

    /**
     * Shows all ServiceUIs as JInternalFrame instances inside of a JDesktop
     *
     * @param elem The ServiceElement for the instances
     * @param items Instance IDs and ServiceItems to use when constructing
     * the admin UIs
     */
    public void doShowAdminUIs(final ServiceElement elem,
                               final Map<String, ServiceItem> items) {
        if(items==null) {
            JOptionPane.showMessageDialog(null,
                                          "There is no ServiceItem for []",
                                          "Missing ServiceItem",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFrame frame;
        if (adminDesktopFrameMap.containsKey(elem)) {
            frame = adminDesktopFrameMap.get(elem);
            int state = frame.getState();
            if (state == JFrame.ICONIFIED)
                frame.setState(JFrame.NORMAL);
            frame.requestFocus();
            return;
        }

        final Runnable createAdminFrame = new Runnable() {
            public void run() {
                try {
                    DesktopAdminFrame desktopFrame =
                        new DesktopAdminFrame(elem, items);                    
                    desktopFrame.addWindowListener(
                        new AdminDesktopPaneListener(elem, desktopFrame));
                    adminDesktopFrameMap.put(elem, desktopFrame);
                    if(lastAdminFrameDim!=null)
                        desktopFrame.setSize(lastAdminFrameDim);
                    if(lastAdminFramePos!=null)
                        desktopFrame.setLocation(lastAdminFramePos);
                    desktopFrame.setVisible(true);
                    if(lastAdminWindowLayout.equals(Constants.ADMIN_FRAME_WINDOW_TILE)) {
                        desktopFrame.tile();
                    } else {
                        desktopFrame.cascade();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                                                  "<html>Exception <font color=red>"
                                                  + ex.getClass().getName()
                                                  + "</font><br>"
                                                  + "Message <font color=blue>"
                                                  + ex.getLocalizedMessage()
                                                  + "</font></html>",
                                                  "Fatal Exception: Cannot Create AdminFrame",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        adminLaunchHelper.push(createAdminFrame);
    }

    public void terminate() {
        adminLaunchHelper.terminate();
    }

    class AdminLaunchHelper extends Thread {
        boolean keepAlive = true;
        final List<Runnable> list = new ArrayList<Runnable>();

        /**
         * Create and start the AdminLaunchHelper
         */
        AdminLaunchHelper() {
            start();
        }

        /**
         * Terminate the AdminLaunchHelper
         */
        void terminate() {
            keepAlive = false;
            interrupt();
        }

        /**
         * Push a Runnable onto the list
         *
         * @param r - The Runnable to execute with SwingUtilities.invokeLater
         */
        void push(Runnable r) {
            synchronized (list) {
                list.add(r);
            }
        }

        /**
         * Pop a Runnable from the list
         *
         * @return Runnable - The Runnable to execute with
         * SwingUtilities.invokeLater
         */
        Runnable pop() {
            Runnable r;
            synchronized (list) {
                r = list.remove(0);
            }
            return (r);
        }

        /**
         * Get the size of the list
         *
         * @return int - The size of the list
         */
        int getSize() {
            int size;
            synchronized (list) {
                size = list.size();
            }
            return (size);
        }

        public void run() {
            while (!isInterrupted()) {
                if (keepAlive) {
                    int size = getSize();
                    if (size > 0) {
                        Runnable r = pop();
                        //SwingUtilities.invokeLater(r);
                        new Thread(r).start();
                    } else {
                        try {
                            sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    class AdminFrameListener extends WindowAdapter {
        ServiceID id;
        JFrame frame;

        public AdminFrameListener(ServiceID id, JFrame frame) {
            this.id = id;
            this.frame = frame;
        }

        public void windowClosed(WindowEvent e) {
            cleanup();
        }

        public void windowClosing(WindowEvent e) {
            frame.dispose();
            cleanup();
        }

        void cleanup() {
            adminFrameMap.remove(id);
        }
    }

    class AdminDesktopPaneListener extends WindowAdapter {
        ServiceElement elem;
        DesktopAdminFrame frame;

        public AdminDesktopPaneListener(ServiceElement elem,
                                        DesktopAdminFrame frame) {
            this.elem = elem;
            this.frame = frame;
        }

        public void windowClosed(WindowEvent e) {
            cleanup();
        }

        public void windowClosing(WindowEvent e) {
            lastAdminFrameDim = frame.getSize();
            lastAdminFramePos = frame.getLocation();
            lastAdminWindowLayout = frame.getLastWindowLayout();
            frame.dispose();
            cleanup();
        }

        void cleanup() {
            adminDesktopFrameMap.remove(elem);
        }
    }

}
