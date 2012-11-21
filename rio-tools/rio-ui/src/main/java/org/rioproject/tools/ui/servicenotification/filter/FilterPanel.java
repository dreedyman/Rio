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
package org.rioproject.tools.ui.servicenotification.filter;

import org.rioproject.tools.ui.servicenotification.RefreshListener;
import org.rioproject.tools.ui.servicenotification.TreeExpansionListener;
import org.rioproject.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Provides filtering options for service notifications.
 *
 * @author Dennis Reedy
 */
public class FilterPanel extends JPanel {
    private final JComboBox<String> filterQuery;
    private final FilterParser filterParser = new FilterParser();

    public FilterPanel(final FilterListener filterListener,
                       final TreeExpansionListener treeExpansionListener,
                       final RefreshListener refreshListener) {
        super(new BorderLayout());

        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                     BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        filterQuery = new JComboBox<String>();
        filterQuery.setEditable(true);
        filterQuery.addItem("");
        filterQuery.setSelectedIndex(0);

        JLabel syntaxLink = new JLabel("<html><a href=\"file://\">Syntax help</a></html>");
        final JPanel parent = this;
        syntaxLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JOptionPane.showMessageDialog(parent,
                                              getSyntaxHelp(),
                                              "Syntax Help",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(syntaxLink, BorderLayout.SOUTH);

        /*java.util.List<String> autoCompleteElements = new ArrayList<String>();
        autoCompleteElements.add("type");
        autoCompleteElements.add("PROVISION_FAILURE");
        for(ProvisionMonitorEvent.Action action : ProvisionMonitorEvent.Action.values()) {
            autoCompleteElements.add(action.name());
        }
        for(ThresholdType type : ThresholdType.values()) {
            autoCompleteElements.add(type.name());
        }

        AutoCompleteDecorator.decorate(filterQuery, autoCompleteElements, true);*/

        add(new JLabel("Filter Using "), BorderLayout.WEST);
        add(filterQuery, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorderPainted(false);
        ImageIcon collapseIcon = Util.getImageIcon("org/rioproject/tools/ui/images/collapseall.gif");
        ImageIcon expandIcon = Util.getImageIcon("org/rioproject/tools/ui/images/expandall.gif");
        ImageIcon refreshIcon = Util.getScaledImageIcon("org/rioproject/tools/ui/images/view-refresh.png",
                                                        expandIcon.getIconWidth(),
                                                        expandIcon.getIconHeight());

        ImageIcon colorOptionsIcon = Util.getScaledImageIcon("org/rioproject/tools/ui/images/color-options.png",
                                                        expandIcon.getIconWidth(),
                                                        expandIcon.getIconHeight());

        JButton collapse = new JButton();
        collapse.setIcon(collapseIcon);
        collapse.setPreferredSize(new Dimension(22, 22));
        collapse.setMaximumSize(new Dimension(22, 22));
        collapse.setToolTipText("Collapse all nodes");
        collapse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                treeExpansionListener.collapse();
            }
        });

        JButton expand = new JButton();
        expand.setIcon(expandIcon);
        expand.setPreferredSize(new Dimension(22, 22));
        expand.setMaximumSize(new Dimension(22, 22));
        expand.setToolTipText("Expand all nodes");
        expand.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                treeExpansionListener.expand();

            }
        });
        final JButton refresh = new JButton(refreshIcon);
        refresh.setPreferredSize(new Dimension(22, 22));
        refresh.setMaximumSize(new Dimension(22, 22));
        refresh.getAccessibleContext().setAccessibleName("refresh events");
        refresh.setToolTipText("Refresh the events");

        final JButton colorOptions = new JButton(colorOptionsIcon);
        colorOptions.setPreferredSize(new Dimension(22, 22));
        colorOptions.setMaximumSize(new Dimension(22, 22));
        colorOptions.getAccessibleContext().setAccessibleName("event tree color options");
        colorOptions.setToolTipText("Event tree color options");

        toolBar.add(collapse);
        toolBar.add(expand);
        toolBar.add(refresh);
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                refreshListener.refresh();
            }
        });
        toolBar.add(colorOptions);
        add(toolBar, BorderLayout.EAST);
        filterQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //if(e.getActionCommand().equals("comboBoxEdited")) {
                    handleFilterQueryInput(filterListener);
                //}
            }
        });
    }

    private void handleFilterQueryInput(final FilterListener filterListener) {
        String query = (String) filterQuery.getSelectedItem();
        if(query!=null) {
            query = query.trim();
            if(query.length()>0) {
                boolean inHistory = false;
                for(int i=0; i<filterQuery.getItemCount(); i++) {
                    if(filterQuery.getItemAt(i).equals(query)) {
                        inHistory = true;
                        break;
                    }

                }
                if(!inHistory)
                    filterQuery.addItem(query);
            }
        }
        filterListener.notify(filterParser.parse(query));
    }

    private String getSyntaxHelp() {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body>");
        builder.append("Help for filter query syntax<br><br>");
        builder.append("<p>Operands");
        builder.append("<ul>");
        builder.append("<li>type</li>");
        builder.append("<li>desc</li>");
        builder.append("</ul>");
        builder.append("<p>Keywords");
        builder.append("<ul>");
        builder.append("<li>=</li>");
        builder.append("<li>is</li>");
        builder.append("<li>contains</li>");
        builder.append("<li>~</li>");
        builder.append("</ul>");
        builder.append("<p>Need examples and more details");
        builder.append("</body></html>");
        return  builder.toString();
    }

}
