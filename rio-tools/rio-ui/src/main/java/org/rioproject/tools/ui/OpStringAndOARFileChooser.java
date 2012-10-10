/*
 * Copyright 2010 the original author or authors.
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

import org.rioproject.resolver.Artifact;
import org.rioproject.system.OperatingSystemType;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;

/**
 * Creates a JFileChooser filtering for both .xml, .groovy and .oar files
 *
 * @author Dennis Reedy
 */
public class OpStringAndOARFileChooser {
    private JFileChooser chooser;
    private JFrame frame;
    private JDialog dialog;
    private ChooserListener listener;
    private JTextField artifactField;
    private enum LastFocused {artifactField, other}
    private LastFocused lastFocused;
    private JButton deployButton;

    /**
     * Create a OpStringAndOARFileChooser
     *
     * @param frame The parent JFrame
     * @param path File Object for a starting point
     * @param lastArtifact The last entered artifact, may be null
     */
    public OpStringAndOARFileChooser(JFrame frame,
                                     File path,
                                     String lastArtifact) {
        this.frame = frame;
        if(path == null)
            chooser = new JFileChooser();
        else
            chooser = new JFileChooser(path);

        String title = "Deploy OperationalString";
        String approveButtonToolTip = "Deploy the selected OperationalString or OperationalString artifact";

        dialog = new JDialog(frame, title, true);
        Dimension d;
        if(OperatingSystemType.isMac())
            d = new Dimension(635, 440);
        else
            d = new Dimension(635, 497);
        dialog.setSize(d);

        JPanel panel = new JPanel();
        panel.setLayout(new  BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel artifactPanel = new JPanel(new BorderLayout(8, 8));
        artifactPanel.add(new JLabel("Enter artifact (groupId:artifactId:version) to deploy"), BorderLayout.NORTH);
        artifactField = new JTextField();
        artifactField.getDocument().addDocumentListener(new ArtifactFieldListener());
        artifactPanel.add(artifactField);

        panel.add(artifactPanel);
        panel.add(Box.createVerticalStrut(8));
        JLabel l = new JLabel("Or Select an OperationalString to deploy");

        /* Align components to make sure everything is left justified */
        artifactPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooser.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooser.setControlButtonsAreShown(false);

        panel.add(l);
        panel.add(Box.createVerticalStrut(4));
        panel.add(chooser);

        Container contentPane = dialog.getContentPane();
        contentPane.add(panel, BorderLayout.CENTER);

        listener =  new ChooserListener();

        JPanel buttons = new JPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        deployButton = new JButton("Deploy");
        deployButton.setToolTipText(approveButtonToolTip);
        deployButton.addActionListener(listener);

        JButton cancel = new JButton("Cancel");
        cancel.setToolTipText("Cancel the Deploy dialog");
        cancel.addActionListener(listener);
        buttons.add(deployButton);
        buttons.add(cancel);

        contentPane.add(buttons, BorderLayout.SOUTH);

        final ImageIcon groovy = getImageIcon(getImageToLoad("groovy.png"));
        final ImageIcon xml = getImageIcon(getImageToLoad("xml.png"));

        chooser.setFileView(new FileView() {
            @Override
            public Icon getIcon(File file) {
                if(!file.isDirectory()) {
                    if(file.getName().endsWith("groovy"))
                        return groovy;
                    if(file.getName().endsWith("xml"))
                        return xml;
                }
                return super.getIcon(file);
            }
        });
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(file.isDirectory())
                    return(true);
                return file.getName().endsWith(".xml") ||
                       file.getName().endsWith(".groovy") ||
                       file.getName().endsWith(".oar");
            }
            @Override
            public String getDescription() {
                return "Rio OperationalString files";
            }
        });
        ArtifactOrFileListener focusListener = new ArtifactOrFileListener();
        chooser.addFocusListener(focusListener);

        //deployButton.setEnabled(false);
        artifactField.addFocusListener(focusListener);
        if(lastArtifact!=null) {
            artifactField.setText(lastArtifact);
        }
    }

    private class ArtifactOrFileListener implements FocusListener {

        public void focusGained(FocusEvent event) {
            lastFocused = LastFocused.artifactField;
            /*if(artifactField.getText().length()>=0 || chooser.getSelectedFile()!=null) {
                if(!deployButton.isEnabled()) {
                    deployButton.setEnabled(true);
                }
            }*/
        }

        public void focusLost(FocusEvent event) {
            Component c = event.getOppositeComponent();
            if(!(c instanceof JButton && ((JButton)c).getText().equals("Deploy")))
                lastFocused = LastFocused.other;
        }
    }

    /**
     * Get the file name chosen
     * 
     * @return The name of the chosen file or null if none selected
     */
    public String getName() {
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        if(listener.isApproved()) {
            String selected = null;
            if(artifactHasBeenProvided()) {
                selected = artifactField.getText();
            } else {
                File file = chooser.getSelectedFile();
                if(file!=null) {
                    selected = file.getAbsolutePath();
                }
            }
            return selected;
        } else
            return(null);
    }

    private boolean artifactHasBeenProvided() {
        return lastFocused != null && lastFocused.equals(LastFocused.artifactField);
    }

    /**
     * Get the current directory
     * 
     * @return The current working directory
     */
    public File getCurrentDirectory() {
        return(chooser.getCurrentDirectory());
    }

    private class ChooserListener implements ActionListener {
        private boolean approved = false;

        boolean isApproved() {
            return approved;
        }

        public void actionPerformed(ActionEvent action) {
            if (action.getActionCommand().equals("Cancel")) {
                dialog.setVisible(false);
                dialog.dispose();
            }
            if (action.getActionCommand().equals("Deploy")) {
                boolean canApprove = true;
                if(artifactHasBeenProvided()) {
                    String a = artifactField.getText();
                    try {
                        new Artifact(a);
                    } catch(Exception e) {
                        canApprove = false;
                        StringBuilder sb = new StringBuilder();
                        sb.append("<html><body>The artifact <font color=red>")
                        .append(a).append("</font> is not valid. The artifact <br>must be in the form of " +
                                          "groupId:artifactId:version</body></html>");
                        JOptionPane.showMessageDialog(frame,
                                                      sb.toString(),
                                                      "Deployment Failure",
                                                      JOptionPane.ERROR_MESSAGE);

                    }
                }
                if(canApprove){
                    approved = true;
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        }
    }
    
    private class ArtifactFieldListener implements DocumentListener {

        public void insertUpdate(DocumentEvent event) {
            handle(event);
        }

        public void removeUpdate(DocumentEvent event) {
            handle(event);
        }

        public void changedUpdate(DocumentEvent event) {
            /* no-op */
        }

        private void handle(DocumentEvent event) {
            Document doc = event.getDocument();
            if(doc.getLength()>0) {
                if(!deployButton.isEnabled())
                    deployButton.setEnabled(true);
            } else {
                if(deployButton.isEnabled())
                    deployButton.setEnabled(false);
            }
        }
    }

    private ImageIcon getImageIcon(String location) {
        ImageIcon icon = null;
        URL url = OpStringAndOARFileChooser.class.getClassLoader().getResource(location);
        if (url != null)
            icon = new ImageIcon(url);
        return (icon);
    }

    private String getImageToLoad(String image) {
        return OpStringAndOARFileChooser.class.getPackage().getName().replace(".", "/")+"/images/"+image;
    }

}
