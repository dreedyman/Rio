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

import net.jini.lookup.entry.ServiceType;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * The ServiceTypePanel is a JPanel that details information found in the
 * ServiceType entry found as part of a service's attribute set
 *
 * @author Dennis Reedy
 */
public class ServiceTypePanel extends JPanel {
    JTextField name, comment;
    ImageIcon icon;
    JPanel iconPanel;

    public ServiceTypePanel() {
        super();
        setLayout(new BorderLayout(28, 8));

        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Service Type Attributes"),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        iconPanel = new JPanel();
        iconPanel.setLayout(new BorderLayout());

        JPanel attrPanel = new JPanel();
        attrPanel.setLayout(new GridLayout(0, 2, 4, 4));
        name = createAttrTextField();
        comment = createAttrTextField();

        attrPanel.add(new JLabel("Name"));
        attrPanel.add(name);
        attrPanel.add(new JLabel("Comment"));
        attrPanel.add(comment);

        add("West", iconPanel);
        add("Center", attrPanel);
    }

    public void setServiceType(ServiceType type) {
        if (type == null) {
            name.setText("Unknown");
            comment.setText("Unknown");
            return;
        }
        name.setText(type.getDisplayName());
        name.setCaretPosition(0);
        comment.setText(type.getShortDescription());
        comment.setCaretPosition(0);
        Image image = type.getIcon(0);
        if (image != null) {
            icon = new ImageIcon(
                image.getScaledInstance(50, 50, Image.SCALE_SMOOTH));
            JLabel label = new JLabel(icon);
            label.setBorder(BorderFactory.createRaisedBevelBorder());
            iconPanel.add("Center", label);
        } else {
            // try and load a Jini image as a system resource
            JLabel label;
            URL url = ClassLoader.getSystemResource(
                "org/rioproject/tools/ui/images/jini-lamp.gif");
            if (url != null) {
                icon = new ImageIcon(url);
                label = new JLabel(icon);
                label.setBorder(BorderFactory.createRaisedBevelBorder());
                iconPanel.add("Center", label);
            } else {
                label = new JLabel();
                label.setBorder(BorderFactory.createRaisedBevelBorder());
            }
            iconPanel.add("Center", label);
        }
    }

    private JTextField createAttrTextField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        return (tf);
    }
}
