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
package org.rioproject.tools.ui.prefs;

import javax.swing.*;

/**
 * A panel that displays options for sorting the cybernode table
 *
 * @author Dennis Reedy
 */
public class TableSortPanel extends JPanel {
    private JCheckBox autoSort;

    public TableSortPanel(boolean isAutoSort) {
        //super(new BorderLayout(8, 8));
        super();
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        autoSort = new JCheckBox("Autosort Table", isAutoSort);

        add(Box.createVerticalStrut(16));
        add(autoSort);
        add(Box.createVerticalStrut(8));
        add(new JLabel("<html>Setting the auto sort to true may impact " +
                       "table performance. The utilization table is a " +
                       "tree-table and re-sorting the table involves " +
                       "rebuilding the tree each time.</html>"));

    }

    public boolean getAutoSort() {
        return autoSort.isSelected();
    }
}
