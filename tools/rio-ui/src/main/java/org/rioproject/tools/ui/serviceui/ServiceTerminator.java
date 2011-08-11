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

import com.sun.jini.admin.DestroyAdmin;
import net.jini.admin.Administrable;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.ServiceType;
import org.rioproject.ui.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;

/**
 * The ServiceTerminator provides the user an acknowledgement panel prior to
 * invoking a service's DestroyAdmin interface
 *
 * @author Dennis Reedy
 */
public class ServiceTerminator implements ActionListener {
    private ServiceItem item;
    private Entry[] attrs;
    private Container component;

    public ServiceTerminator(ServiceItem item, Container component) {
        this.item = item;
        this.attrs = item.attributeSets;
        this.component = component;
    }

    public void actionPerformed(ActionEvent e) {
        killService();
    }

    public void killService() {
        try {
            String name;
            ServiceType sType = getServiceType(this.attrs);
            if (sType != null)
                name = sType.getDisplayName();
            else
                name = this.item.service.getClass().getName();
            String message = "Terminate the [" + name + "] service?";
            String title = "Terminate Service Confirmation";
            int answer = JOptionPane.showConfirmDialog(
                component, message, title,
                JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.NO_OPTION)
                return;
            Administrable admin = (Administrable) this.item.service;
            DestroyAdmin destroyAdmin = (DestroyAdmin) admin.getAdmin();
            destroyAdmin.destroy();
            if (component != null) {
                Util.dispose(component);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            //Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(component,
                                          "Unable to destroy service :" +
                                          e.getMessage(),
                                          "Service Termination Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    ServiceType getServiceType(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ServiceType) {
                return (ServiceType) attr;
            }
        }
        return (null);
    }

}
