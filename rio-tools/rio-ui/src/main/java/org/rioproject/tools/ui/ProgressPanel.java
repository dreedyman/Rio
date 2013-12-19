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

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.tools.ui.progresspanel.SingleComponentInfiniteProgress;
import org.rioproject.ui.GlassPaneContainer;

import javax.swing.*;

/**
 * Progress spinning
 *
 * @author Dennis Reedy
 */
public class ProgressPanel extends JPanel {
    private SingleComponentInfiniteProgress progressPanel;
    private final Configuration config;
    private static final String COMPONENT = ProgressPanel.class.getPackage().getName();

    public ProgressPanel(final Configuration config) {
        this.config = config;
    }

    public void showProgressPanel() {
        if(progressPanel==null) {
            progressPanel = new SingleComponentInfiniteProgress(false);
            GlassPaneContainer.findGlassPaneContainerFor(getParent()).setGlassPane(progressPanel);
            /*GlassPaneContainer glassPaneContainer = new GlassPaneContainer();
            glassPaneContainer.setGlassPane(progressPanel);*/
        }
        String waitMessage = "Waiting to discover the Rio system ...";
        try {
            waitMessage = (String)config.getEntry(COMPONENT,
                                                  "waitMessage",
                                                  String.class,
                                                  waitMessage);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        progressPanel.setText(waitMessage);
        progressPanel.start();
    }

    public void systemDown() {
        showProgressPanel();
    }

    public void systemUp() {
        if(progressPanel!=null) {
            progressPanel.stop();
        }
    }
}
