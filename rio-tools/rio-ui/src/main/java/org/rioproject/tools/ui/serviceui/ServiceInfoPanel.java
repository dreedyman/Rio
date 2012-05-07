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

import net.jini.lookup.entry.ServiceInfo;

import javax.swing.*;
import java.awt.*;

/**
 * The ServiceInfoPanel is a JPanel that details information about the ServiceInfo entry
 * a service has as part of its attribute set
 *
 * @author Dennis Reedy
 */
public class ServiceInfoPanel extends JPanel {
    JTextField  name, serialno, model, manufacturer, vendor, version;

    public ServiceInfoPanel() {
        super();
        setLayout(new GridLayout(0, 2, 4, 4));

        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Service Info Attributes"),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        setLayout(new GridLayout(0, 2, 4, 4));
        name = createAttrTextField();
        serialno = createAttrTextField();
        vendor = createAttrTextField();
        version = createAttrTextField();
        model = createAttrTextField();
        manufacturer = createAttrTextField();

        add(new JLabel("Name"));            add(name);
        add(new JLabel("Serial Number"));   add(serialno);
        add(new JLabel("Model"));           add(model);
        add(new JLabel("Manufacturer"));    add(manufacturer);
        add(new JLabel("Vendor"));          add(vendor);
        add(new JLabel("Version"));         add(version);
    }

    public void setServiceInfo(ServiceInfo info) {
        if(info==null) {
            name.setText("Unavailable");
            serialno.setText("Unavailable");
            vendor.setText("Unavailable");
            version.setText("Unavailable");
            model.setText("Unavailable");
            manufacturer.setText("Unavailable");
            return;
        }
        name.setText(info.name);
        name.setCaretPosition(0);
        serialno.setText(info.serialNumber);
        serialno.setCaretPosition(0);
        vendor.setText(info.vendor);
        vendor.setCaretPosition(0);
        version.setText(info.version);
        version.setCaretPosition(0);
        model.setText(info.model);
        model.setCaretPosition(0);
        manufacturer.setText(info.manufacturer);
        manufacturer.setCaretPosition(0);
    }

    private JTextField createAttrTextField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        return(tf);
    }
}

