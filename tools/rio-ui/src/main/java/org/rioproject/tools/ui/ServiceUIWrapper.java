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
package org.rioproject.tools.ui;

import net.jini.config.EmptyConfiguration;
import net.jini.core.lookup.ServiceItem;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

/**
 * Used when the Rio UI is launched as a service UI
 */
public class ServiceUIWrapper extends Main {
    public ServiceUIWrapper(Object obj) {
        super(EmptyConfiguration.INSTANCE, true, new Properties());
        System.err.println("===========================");
        getAccessibleContext().setAccessibleName("Provision Monitor UI");
        System.err.println("1");
        try {
            startDiscovery();
            System.err.println("2");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            System.err.println("3");
            pack();
            System.err.println("4");
            int width = 700;
            int height = 690;
            setSize(new Dimension(width, height));
            System.err.println("5");
            addProvisionMonitor((ServiceItem)obj);
            System.err.println("6");
            setVisible(true);
            System.err.println("7");
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.err.println("===========================");
    }
}
