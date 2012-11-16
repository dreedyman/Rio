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

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Provides filtering options for service notification eventTypes.
 *
 * @author Dennis Reedy
 */
public class FilterPanel extends JPanel {
    private final JTextField filterQuery;
    private final FilterParser filterParser = new FilterParser();

    public FilterPanel(final FilterListener filterListener) {
        super(new BorderLayout());

        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                     BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        filterQuery = new JTextField();
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

        filterQuery.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String query = filterQuery.getText().trim();
                    filterListener.notify(filterParser.parse(query));
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
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
        builder.append("</ul>");
        builder.append("<p>Need examples and more details");
        builder.append("</body></html>");
        return  builder.toString();
    }

}
