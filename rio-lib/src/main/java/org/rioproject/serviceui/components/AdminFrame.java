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
package org.rioproject.serviceui.components;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.ServiceType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Method;

/**
 * The AdminFrame class creates a ServiceUIPanel in a JFrame
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class AdminFrame extends JFrame {    
    private ServiceUIPanel serviceUIPanel;

    public AdminFrame(final Object arg) {
        super();
        ServiceItem item = (ServiceItem) arg;
        serviceUIPanel = new ServiceUIPanel(item, 1000*30, this);
        ServiceType sType = getServiceType(item.attributeSets);
        if(sType!=null && sType.getDisplayName()!=null)
            setTitle("Service UI for "+sType.getDisplayName());
        Container container = getContentPane();
        if(container!=null)
            container.add(serviceUIPanel);
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        };
        addWindowListener(l);
        // Set dimensions and show
        setSize(565, 588);
    }

    public AdminFrame(Object arg, Component component) {
        this(arg);
        display(component);
    }

    private void display(Component component) {
        if(component!=null)
            setLocationRelativeTo(component);
        setVisible(true);
    }

    private ServiceType getServiceType(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ServiceType) {
                return (ServiceType) attr;
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        //System.out.println(getSize());
        for (Object comp : serviceUIPanel.getUIComponents()) {
            try {
                Method terminate = comp.getClass().getMethod("terminate", (Class[]) null);
                terminate.invoke(comp, (Object[]) null);
            } catch (Exception e) {
                /* ignore */
            }
        }
        super.dispose();
    }
}

