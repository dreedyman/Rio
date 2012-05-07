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

import net.jini.core.lookup.ServiceItem;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.*;
import java.lang.reflect.Method;

/**
 * The AdminInternalFrame class creates a ServiceUIPanel in a JInternalFrame
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class AdminInternalFrame extends JInternalFrame {
    ServiceUIPanel serviceUIPanel;
    public static final long DEFAULT_DELAY=1000*30;

    public AdminInternalFrame(String name, ServiceItem item) throws Exception {
        this(name, item, DEFAULT_DELAY);
    }

    public AdminInternalFrame(String name,
                              ServiceItem item,
                              long startupDelay) throws Exception {
        super(name, true, false, true, true);
        serviceUIPanel = new ServiceUIPanel(item, startupDelay, this);
        Container container = getContentPane();
        if(container!=null)
            container.add(serviceUIPanel);
        display();
    }

    private void display() {
        InternalFrameListener l = new InternalFrameListener() {
            public void internalFrameOpened(InternalFrameEvent event) {
            }

            public void internalFrameClosing(InternalFrameEvent event) {
            }

            public void internalFrameClosed(InternalFrameEvent e) {
                dispose();
            }

            public void internalFrameIconified(InternalFrameEvent event) {
            }

            public void internalFrameDeiconified(InternalFrameEvent event) {
            }

            public void internalFrameActivated(InternalFrameEvent event) {
            }

            public void internalFrameDeactivated(InternalFrameEvent event) {
            }
        };
        addInternalFrameListener(l);

        // Set dimensions and show
        setSize(490, 505);
        setLocation(50, 100);
        setVisible(true);
    }

    @Override
    public void dispose() {
        for (Object comp : serviceUIPanel.getUIComponents()) {
            try {
                Method terminate = comp.getClass().getMethod("terminate",
                                                              (Class[]) null);
                terminate.invoke(comp, (Object[]) null);
            } catch (Exception e) {
            }
        }
        super.dispose();
    }
}
