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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Undeploy an OperationalString
 *
 * @author Dennis Reedy
 */
public class UndeployPanel extends JPanel {
    private final java.util.List<String> selectedOpStringNames = new ArrayList<String>();

    public UndeployPanel(final String[] opstrings, final JDialog dialog) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                         BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        DefaultListModel<String> listModel = new DefaultListModel<String>();
        for(int i=0; i<opstrings.length; i++)
            listModel.add(i, opstrings[i]);
        final JList<String> list = new JList<String>(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //list.setCellRenderer(new RemoveOpStringCellRenderer());
        if(listModel.getSize()==1)
            list.setSelectedIndex(0);
        list.setToolTipText("A list of OperationalString items that can be undeployed");
        JLabel l1 = new JLabel("Select an OperationalString to Undeploy");
        l1.setLabelFor(list);
        top.add(l1, BorderLayout.NORTH);
        top.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        final JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        JButton remove = new JButton("Undeploy");
        remove.setToolTipText("Undeploy an OperationalString");

        remove.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    selectedOpStringNames.addAll(list.getSelectedValuesList());
                    if(selectedOpStringNames.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog,
                                                      "You must select an "+
                                                      "OperationalString to "+
                                                      "undeploy",
                                                      "OperationalString "+
                                                      "Selection Error",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    dialog.dispose();
                }
            });
        bottom.add(remove);
        bottom.add(cancel);
        add(top, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public String[] getSelectedOpStringNames() {
        return selectedOpStringNames.toArray(new String[selectedOpStringNames.size()]);
    }
}
