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
package org.rioproject.tools.ui;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.util.RioManifest;
import org.rioproject.RioVersion;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * About box dialog
 *
 * @author Dennis Reedy
 */
public class RioAboutBox extends JDialog {
    public RioAboutBox(final JFrame frame) {
        super(frame,
              frame.getTitle().equals("")?"Rio Project":frame.getTitle(),
              true);

        JPanel substrate = new JPanel();
        substrate.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JPanel surface = new JPanel(new BorderLayout(2, 2));
        surface.setBackground(Color.white);
        surface.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED,
                                            new Color(228, 228, 228), Color.gray),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        String aboutIcon = null;
        Configuration config = ((Main)frame).getConfiguration();
        try {
            aboutIcon = (String)config.getEntry(Constants.COMPONENT,
                                                "aboutIcon",
                                                String.class,
                                                null);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        ClassLoader cl = getClass().getClassLoader();
        ImageIcon rioIcon=null;
        try {
            if(aboutIcon==null)
                aboutIcon = "org/rioproject/tools/ui/images/rio.png";
            URL url = cl.getResource(aboutIcon);
            if(url != null)
                rioIcon = new ImageIcon(url);
        } catch(Exception e) {
            e.printStackTrace();
        }

        String build = null;
        try {
            build = (String)config.getEntry(Constants.COMPONENT,
                                            "build",
                                            String.class,
                                            null);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        if(build==null) {
            URL implUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            try {
                RioManifest rioManifest = new RioManifest(implUrl);
                build = rioManifest.getRioBuild();
            } catch (Exception e) {
                e.printStackTrace();
                build = e.getClass().getName()+": "+e.getMessage();
            }
        }

        String version = null;
        try {
            version = (String)config.getEntry(Constants.COMPONENT,
                                              "version",
                                              String.class,
                                              null);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        if(version==null)
            version = RioVersion.VERSION;

        String aboutInfo = null;
        try {
            aboutInfo = (String)config.getEntry(Constants.COMPONENT,
                                                "aboutInfo",
                                                String.class,
                                                null);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        if(aboutInfo==null)
            aboutInfo = Constants.INFO;
        
        JLabel l1 = new JLabel(rioIcon);
        surface.add(l1, BorderLayout.NORTH);

        JLabel l2 = new JLabel("<html><center><h3><font color=#000000>"+
                                aboutInfo+
                                "<br>Version: "+ version+
                                "<br>Build: "+build+
                                "</font></h3>"+
                                "</center></html>");
        l2.setHorizontalAlignment(SwingConstants.CENTER);
        surface.add(l2, BorderLayout.CENTER);
        substrate.add(surface);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
                                 public void actionPerformed(ActionEvent e) {
                                     System.out.println(getSize());
                                     dispose();
                                 }
                             });
        JPanel okPanel = new JPanel();
        okPanel.add(ok);
        Container contentPane = getContentPane();
        contentPane.add(substrate, BorderLayout.CENTER);
        contentPane.add(okPanel, BorderLayout.SOUTH);
        int width=316;
        int height=259;
        setSize(new Dimension(width, height));
        setResizable(false);
        setLocationRelativeTo(frame);
        setVisible(true);
    }

}
