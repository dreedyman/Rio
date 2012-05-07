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
package org.rioproject.tools.ui.serviceui;

import org.rioproject.entry.ComputeResourceInfo;

import javax.swing.*;
import java.awt.*;

/**
 * The ComputeResourceInfoPanel is a JPanel that details information about the
 * ComputeResourceInfo entry object
 *
 * @see org.rioproject.entry.ComputeResourceInfo
 *
 * @author Dennis Reedy
 */
public class ComputeResourceInfoPanel extends JPanel {
    JTextField jvmVendor, jvmVersion, hostaddr, hostname, osinfo, arch;

    public ComputeResourceInfoPanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel appl = new JPanel();
        appl.setLayout(new GridLayout(0, 2, 4, 4));

        JPanel jvm = new JPanel();
        jvm.setLayout(new GridLayout(0, 2, 4, 4));

        jvm.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "JVM Information"),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        jvmVendor = createAttrTextField();
        jvmVersion = createAttrTextField();

        jvm.add(new JLabel("JVM Version"));
        jvm.add(jvmVersion);
        jvm.add(new JLabel("JVM Vendor"));
        jvm.add(jvmVendor);

        appl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Host Attributes"),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        hostname = createAttrTextField();
        hostaddr = createAttrTextField();
        osinfo = createAttrTextField();
        arch = createAttrTextField();

        appl.add(new JLabel("Host Name"));
        appl.add(hostname);
        appl.add(new JLabel("Host Address"));
        appl.add(hostaddr);
        appl.add(new JLabel("Operating System"));
        appl.add(osinfo);
        appl.add(new JLabel("Architecture"));
        appl.add(arch);

        add(appl);
        add(Box.createVerticalStrut(8));
        add(jvm);
        add(Box.createVerticalGlue());
    }

    public void setComputeResourceInfo(ComputeResourceInfo info) {
        if (info == null) {
            jvmVersion.setText("Unknown");
            jvmVendor.setText("Unknown");
            hostname.setText("Unknown");
            hostaddr.setText("Unknown");
            osinfo.setText("Uknown");
            arch.setText("Unknown");
            return;
        }
        jvmVersion.setText(info.jvmVersion);
        jvmVersion.setCaretPosition(0);

        jvmVendor.setText(info.jvmVendor);
        jvmVendor.setCaretPosition(0);

        hostname.setText(info.hostName);
        hostname.setCaretPosition(0);

        hostaddr.setText(info.hostAddress);
        hostaddr.setCaretPosition(0);

        String s = info.osName;
        s = s + " " + info.osVersion;
        osinfo.setText(s);
        osinfo.setCaretPosition(0);

        arch.setText(info.arch);
        arch.setCaretPosition(0);
    }

    private JTextField createAttrTextField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        return (tf);
    }
}

