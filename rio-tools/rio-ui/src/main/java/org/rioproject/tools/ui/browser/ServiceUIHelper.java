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
package org.rioproject.tools.ui.browser;

import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.factory.JComponentFactory;
import net.jini.lookup.ui.factory.JDialogFactory;
import net.jini.lookup.ui.factory.JFrameFactory;
import net.jini.lookup.ui.factory.JWindowFactory;
import org.rioproject.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * utility to help using service user interface entries.
 * @author Dennis Reedy
 */
public class ServiceUIHelper {

    static void handle(final UIDescriptor uiDescriptor, final ServiceItem serviceItem, final Component parent) {
        try {
            Object factory = uiDescriptor.getUIFactory(Thread.currentThread().getContextClassLoader());
            Class factoryClass = factory.getClass();
            if(JFrameFactory.class.isAssignableFrom(factoryClass)) {
                JFrameFactory uiFactory = (JFrameFactory)factory;
                JFrame frame = uiFactory.getJFrame(serviceItem);
                frame.validate();
                frame.setVisible(true);
            }
            else if(JWindowFactory.class.isAssignableFrom(factoryClass)) {
                JWindowFactory uiFactory = (JWindowFactory)factory;
                JWindow window = uiFactory.getJWindow(serviceItem);
                window.validate();
                window.setVisible(true);
            } else if(JComponentFactory.class.isAssignableFrom(factoryClass)) {
                JComponentFactory uiFactory = (JComponentFactory)factory;
                JComponent component = uiFactory.getJComponent(serviceItem);
                String name = component.getAccessibleContext().getAccessibleName();
                if(name==null) {
                    component.getAccessibleContext().setAccessibleName(component.getClass().getName());
                }
                JComponentFrame componentFrame = new JComponentFrame(component, name);
                componentFrame.setLocationRelativeTo(parent);
                componentFrame.setVisible(true);
            } else if(JDialogFactory.class.isAssignableFrom(factoryClass)) {
                JDialogFactory uiFactory = (JDialogFactory)factory;
                JDialog dialog = uiFactory.getJDialog(serviceItem);
                dialog.validate();
                dialog.setVisible(true);
            }
        } catch (Exception e) {
            Util.showError(e, parent, "Service UI Exception");
        }
    }

    static class JComponentFrame extends JFrame {
        public JComponentFrame(JComponent component, String name)  {
            super();
            setTitle("Service UI for "+name);
            Container container = getContentPane();
            if(container!=null)
                container.add(component);
            WindowListener l = new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            };
            addWindowListener(l);
            // Set dimensions and show
            setSize(565, 588);
        }
    }
}
