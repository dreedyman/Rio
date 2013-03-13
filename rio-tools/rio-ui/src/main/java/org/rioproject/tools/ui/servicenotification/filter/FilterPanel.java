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

import org.rioproject.tools.ui.Constants;
import org.rioproject.tools.ui.servicenotification.EventCollectorListener;
import org.rioproject.tools.ui.servicenotification.EventColorManager;
import org.rioproject.tools.ui.servicenotification.TreeExpansionListener;
import org.rioproject.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides filtering options for service notifications.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unchecked")
public class FilterPanel extends JPanel {
    private final JComboBox filterQuery;
    private final FilterParser filterParser = new FilterParser();
    private final EventCollectorListener eventCollectorListener;
    private final EventColorManager eventColorManager = new EventColorManager();
    private final JCheckBox useEventCollector;

    public FilterPanel(final FilterListener filterListener,
                       final TreeExpansionListener treeExpansionListener,
                       final EventCollectorListener eventCollectorListener,
                       final Properties props) {
        super(new BorderLayout());
        this.eventCollectorListener = eventCollectorListener;
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                     BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        filterQuery = new JComboBox();
        filterQuery.setEditable(true);
        filterQuery.addItem("");
        filterQuery.setSelectedIndex(0);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        try {
            final JEditorPane syntaxHelp = new JEditorPane(getSyntaxHelp());
            syntaxHelp.setEditable(false);
            syntaxHelp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            syntaxHelp.setVisible(false);

            final JLabel syntaxLink = new JLabel("<html><a href=\"\">Syntax help</a></html>");
            syntaxLink.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    syntaxLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    syntaxLink.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    /*Window parent = SwingUtilities.windowForComponent(syntaxHelp);
                    if(parent!=null) {
                        showSyntaxHelpDialog(parent, syntaxHelp);
                    } else {*/
                        if(syntaxHelp.isVisible()) {
                            syntaxHelp.setVisible(false);
                        } else {
                            syntaxHelp.setVisible(true);
                        }
                    //}
                }
            });
            p.add(syntaxLink, BorderLayout.WEST);
            p.add(syntaxHelp, BorderLayout.SOUTH);

        } catch(IOException e) {
            e.printStackTrace();
        }

        useEventCollector = new JCheckBox();
        setUseEventCollectorCheckBoxText();
        setCheckBox(useEventCollector, props, Constants.USE_EVENT_COLLECTOR, false);
        useEventCollector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                eventCollectorListener.handleEventCollectorRegistration(useEventCollector.isSelected());
            }
        });

        p.add(useEventCollector, BorderLayout.EAST);
        add(p, BorderLayout.SOUTH);

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
        colorOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showColorOptionsDialog(SwingUtilities.windowForComponent(colorOptions));
            }
        });

        toolBar.add(collapse);
        toolBar.add(expand);
        toolBar.add(refresh);
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                eventCollectorListener.refresh();
            }
        });
        toolBar.add(colorOptions);
        add(toolBar, BorderLayout.EAST);
        filterQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleFilterQueryInput(filterListener);
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

    private void setCheckBox(JCheckBox checkBox, Properties props, String propertyName, boolean defaultValue) {
        String s = props.getProperty(propertyName);
        boolean value = defaultValue;
        if(s!=null)
            value = Boolean.parseBoolean(s);
        checkBox.setSelected(value);
        checkBox.setFont(Constants.ITEM_FONT);
    }

    private void showColorOptionsDialog(Window parent) {
        final JDialog dialog = new JDialog(parent, "Event Color Options");
        dialog.setModal(true);

        JPanel panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        /* Turn on automatically adding gaps between components */
        layout.setAutoCreateGaps(true);

        /* Turn on automatically creating gaps between components that touch
         * the edge of the container and the container. */
        layout.setAutoCreateContainerGaps(true);

        /* Create a sequential group for the horizontal axis. */
        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        /* The sequential group in turn contains two parallel groups.
         * One parallel group contains the labels, the other the text fields.
         * Putting the labels in a parallel group along the horizontal axis
         * positions them at the same x location. */

        /* Variable indentation is used to reinforce the level of grouping. */
        final Map<JLabel, JComponent> eventLabelButtonMap = getEventLabelButtonMap();

        GroupLayout.ParallelGroup labelGroup = layout.createParallelGroup();
        GroupLayout.ParallelGroup buttonGroup = layout.createParallelGroup();

        for(Map.Entry<JLabel, JComponent> eventTypes : eventLabelButtonMap.entrySet()) {
            labelGroup.addComponent(eventTypes.getKey());
            buttonGroup.addComponent(eventTypes.getValue());
        }

        hGroup.addGroup(labelGroup);
        hGroup.addGroup(buttonGroup);

        layout.setHorizontalGroup(hGroup);

        /* Create a sequential group for the vertical axis. */
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        /* The sequential group contains two parallel groups that align
         * the contents along the baseline. The first parallel group contains
         * the first label and text field, and the second parallel group contains
         * the second label and text field. By using a sequential group
         * the labels and text fields are positioned vertically after one another.*/
        for(Map.Entry<JLabel, JComponent> eventTypes : eventLabelButtonMap.entrySet()) {
            GroupLayout.ParallelGroup group = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
            group.addComponent(eventTypes.getKey()).addComponent(eventTypes.getValue());
            vGroup.addGroup(group);
        }
        layout.setVerticalGroup(vGroup);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        //p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JScrollPane(panel), BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        final JButton close = new JButton("Close");
        close.setToolTipText("Close this dialog");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        final JButton reset = new JButton("Reset");
        reset.setToolTipText("Reset event colors");
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(Map.Entry<JLabel, JComponent> eventTypes : eventLabelButtonMap.entrySet()) {
                    eventTypes.getValue().setBackground(eventColorManager.getEventColor(eventTypes.getKey().getText()));
                }
            }
        });
        buttonPane.add(close);
        buttonPane.add(reset);

        p.add(buttonPane, BorderLayout.SOUTH);

        dialog.getContentPane().add(p);
        dialog.pack();
        dialog.setSize(400, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    @SuppressWarnings("unused")
    private void showSyntaxHelpDialog(Window parent, JEditorPane syntaxHelp) {
        syntaxHelp.setVisible(true);
        final JDialog dialog = new JDialog(parent, "Filter Syntax Help");
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(syntaxHelp), BorderLayout.CENTER);
        final JButton close = new JButton("Close");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        JPanel buttonPane = new JPanel();
        buttonPane.add(close);
        panel.add(buttonPane, BorderLayout.SOUTH);

        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setSize(850, 405);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public boolean getUseEventCollector() {
        return useEventCollector.isSelected();
    }

    public void setUseEventCollectorCheckBoxText() {
        useEventCollector.setText(String.format("Use EventCollector (discovered %d EventCollectors)",
                                                eventCollectorListener.getEventControllerCount()));

    }

    private URL getSyntaxHelp() {
        return Thread.currentThread().getContextClassLoader().getResource("filter-syntax-help.html");
    }

    private Map<JLabel, JComponent> getEventLabelButtonMap() {
        Map<JLabel, JComponent> eventTypeMap = new HashMap<JLabel, JComponent>();
        for(Map.Entry<String, Color> entry : eventColorManager.getEventColorMap().entrySet()) {
            JComponent comp = makeColorComponent(entry.getValue(), "Color for "+entry.getKey());
            eventTypeMap.put(new JLabel(entry.getKey()), comp);
        }
        return eventTypeMap;
    }

    private JComponent makeColorComponent(final Color color, final String desc) {
        /*Vector<JLabel> labels = new Vector<JLabel>();
        int index = 0;
        for(Map.Entry<String, Color> entry : eventColorManager.getColorMap().entrySet()) {
            StringBuilder labelBuilder = new StringBuilder();
            labelBuilder.append("<html>");
            labelBuilder.append("<small>").append(entry.getKey()).append("</small>");
            labelBuilder.append("</html>");
            final JLabel comp = new JLabel(labelBuilder.toString());
            comp.setOpaque(true);
            comp.setBackground(entry.getValue());
            comp.setForeground(Color.GRAY);
            labels.add(comp);
            if(color.equals(entry.getValue())) {
                index = labels.indexOf(comp);
            }
        }
        JComboBox<JLabel> labelJComboBox = new JComboBox<JLabel>(labels);
        labelJComboBox.setSelectedIndex(index);
        labelJComboBox.setRenderer(new ColorCellRenderer());
        return labelJComboBox;*/
        StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append("<html>");
        labelBuilder.append("<small>").append(eventColorManager.getColorName(color)).append("</small>");
        labelBuilder.append("</html>");
        final JLabel comp = new JLabel(labelBuilder.toString());
        comp.setOpaque(true);
        comp.setBackground(color);
        comp.setForeground(Color.GRAY);
        comp.setToolTipText(desc);
        comp.setBorder(BorderFactory.createRaisedBevelBorder());
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(final MouseEvent event) {
                JLabel c = (JLabel)event.getComponent();
                comp.setBorder(BorderFactory.createLoweredBevelBorder());
                Color color = c.getBackground();
                color = JColorChooser.showDialog(comp, desc, color);
                comp.setBorder(BorderFactory.createRaisedBevelBorder());
                if(color!=null) {
                    c.setBackground(color);
                }
            }
        });
        return comp;
    }


    /*class ColorCellRenderer extends BasicComboBoxRenderer {
         @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText(((JLabel)value).getText());
            //if(isSelected)
            //    setBackground(list.getSelectionBackground());
            //else
                setBackground(((JLabel)value).getBackground());
            return this;
        }
    }*/
}
