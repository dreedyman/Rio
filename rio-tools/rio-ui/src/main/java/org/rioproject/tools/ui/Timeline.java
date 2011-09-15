/*
 * Copyright 2008 to the original author or authors.
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

import javax.swing.*;
import java.awt.*;

/**
 * A timeline
 */
public class Timeline extends JPanel {

    public Timeline() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(8));
        add(createSlider());
        add(Box.createVerticalStrut(8));
        add(createSlider());
        add(Box.createVerticalGlue());
    }

    JSlider createSlider() {
        JSlider slider = new JSlider(JSlider.HORIZONTAL);
        slider.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));
        slider.setMaximum(100);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(5);
        slider.setBackground(Color.GREEN);
        slider.setPaintTrack(false);
        return slider;
    }
}
