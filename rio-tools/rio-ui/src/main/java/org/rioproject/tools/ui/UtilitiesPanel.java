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
package org.rioproject.tools.ui;

import net.jini.config.ConfigurationException;
import org.rioproject.tools.ui.cybernodeutilization.CybernodeUtilizationPanel;

import javax.swing.*;
import java.awt.*;
import java.rmi.server.ExportException;

/**
 * Container for utilities
 */
public class UtilitiesPanel extends JPanel {

    public UtilitiesPanel(final CybernodeUtilizationPanel cup) throws ExportException, ConfigurationException {
        super(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Utilization", cup);
        add(tabs, BorderLayout.CENTER);
    }
}
