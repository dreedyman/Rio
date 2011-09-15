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
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;

/**
 * A panel that displays color choices
 *
 * @author Dennis Reedy
 */
public class ColorPanel extends JPanel {
    Color altRowColor;
    Color failureColor;
    Color okayColor;
    Color warningColor;
    JComponent okayColorComp;
    JComponent failureColorComp;
    JComponent warningColorComp;
    JComponent rowColorComp;
           
    public ColorPanel(Color altRowColor,
                      Color failureColor,
                      Color okayColor,
                      Color warningColor) {
        super(new GridBagLayout());
        this.altRowColor = altRowColor;
        this.warningColor = warningColor;
        this.failureColor = failureColor;
        this.okayColor = okayColor;

        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8, 8, 8, 8),
            BorderFactory.createEtchedBorder()));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        Field setOkayColor = getField("okayColor", getClass());
        okayColorComp =
            makeColorComponent(okayColor,
                               "Okay Status Color",
                               "Press to choose the color for " +
                               "okay status",
                               setOkayColor);
        Field setFailureColor = getField("failureColor", getClass());
        failureColorComp =
            makeColorComponent(failureColor,
                               "Failure Status Color",
                               "Press to choose the color for " +
                               "failure status",
                               setFailureColor);

        Field setWarningColor = getField("warningColor", getClass());
        warningColorComp =
            makeColorComponent(warningColor,
                               "Warning Status Color",
                               "Press to choose the color for " +
                               "warning status. The warning status is used if a deployed service is not registered into any lookup services",
                               setWarningColor);

        Field setAltRowColor = getField("altRowColor", getClass());
        rowColorComp =
            makeColorComponent(altRowColor,
                               "Alternate Row Color",
                               "Press to choose the color for " +
                               "alternating row color in the " +
                               "Cybernode table",
                               setAltRowColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        add(new JLabel("<html><h3>" +
                       "Select colors for elements in the UI" +
                       "</h3></html>"),
            gbc);       

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(new JLabel("Okay Status"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 2;
        add(okayColorComp, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Failure Status"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 3;
        add(failureColorComp, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("Warning Status"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 4;
        add(warningColorComp, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel("Alternate Table Row"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 5;
        add(rowColorComp, gbc);
    }

    public Color getWarningColor() {
        return warningColor;
    }

    public Color getOkayColor() {
        return okayColor;
    }

    public Color getFailureColor() {
        return failureColor;
    }

    public Color getAltRowColor() {
        return altRowColor;
    }

    public void setAltRowColor(Color altRowColor) {
        this.altRowColor = altRowColor;
        rowColorComp.setBackground(altRowColor);
    }

    public void setFailureColor(Color failureColor) {
        this.failureColor = failureColor;
        failureColorComp.setBackground(failureColor);
    }

    public void setOkayColor(Color okayColor) {
        this.okayColor = okayColor;
        okayColorComp.setBackground(okayColor);
    }

    public void setWarningColor(Color warningColor) {
        this.warningColor = warningColor;
        warningColorComp.setBackground(warningColor);
    }

    JComponent makeColorComponent(Color color,
                                  final String desc,
                                  String tooltip,
                                  final Field field) {
        final JLabel comp = new JLabel("<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</html>");
        comp.setOpaque(true);
        comp.setBackground(color);
        comp.setToolTipText(tooltip);
        comp.setBorder(BorderFactory.createRaisedBevelBorder());
        comp.addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent event) {
            }
            public void mousePressed(MouseEvent event) {
                JLabel c = (JLabel)event.getComponent();
                comp.setBorder(BorderFactory.createLoweredBevelBorder());
                Color color = c.getBackground();
                color = JColorChooser.showDialog(comp, desc, color);
                comp.setBorder(BorderFactory.createRaisedBevelBorder());
                if(color!=null) {
                    setField(field, color);
                    c.setBackground(color);
                }
            }
            public void mouseReleased(MouseEvent event) {
            }
            public void mouseEntered(MouseEvent event) {
            }
            public void mouseExited(MouseEvent event) {
            }
        });
        return comp;
    }

    private Field getField(String name, Class cl) {
        Field field = null;
        try {
            field = cl.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return field;
    }

    private void setField(Field field, Color color) {
        try {
            field.set(this, color);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
