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

import org.rioproject.opstring.Schedule;
import org.rioproject.util.TimeUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays redeploy options
 *
 * @author Dennis Reedy
 */
public class RedeployPanel extends JPanel {
    public static final int REDEPLOY_OPTION=1;
    public static final int DISMISS_OPTION=-1;
    public static final long REDEPLOY_IMMEDIATELY = 0;
    private static long SECOND = 1000;
    private static long MINUTE = SECOND*60;
    private static long HOUR = MINUTE*60;
    private static long DAY = HOUR*24;
    private int option;
    long calculatedRemaining;
    private Timer timer;

    public static void main(String[] args) {
        JDialog dialog = new JDialog((JFrame)null, "Redeploy Options", true);
        RedeployPanel redeployPanel = new RedeployPanel(dialog, 10000);
        dialog.getContentPane().add(redeployPanel);
        int width = 302;
        int height = 300;
        dialog.pack();
        dialog.setSize(width, height);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int widthLoc = screenSize.width / 2 - width / 2;
        int heightLoc = screenSize.height / 2 - height / 2;
        dialog.setLocation(widthLoc, heightLoc);
        dialog.setVisible(true);
        redeployPanel.stopTimer();
        System.exit(0);
    }

    /* Creates new form JPanel */
    public RedeployPanel(final JDialog dialog, final long remaining) {
        initComponents();
        calculatedRemaining = remaining;
        if(remaining!= Schedule.INDEFINITE) {
            /* Create a 1-second timer and action listener for it. */
            timer = new Timer(1000,
                                          new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    nextDeploymentTime.setText(TimeUtil.format(calculatedRemaining));
                    if(calculatedRemaining==0) {
                        timer.stop();
                        redeployButton.setEnabled(false);
                        enableScheduling(false);
                    } else {
                        calculatedRemaining -= 1000;
                    }
                }
            });
            timer.start();
        } else {
            nextDeploymentTime.setText("Undefined");
        }

        redeploymentScheduling.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    enableScheduling(true);
                }
            });
        immediateRedeployment.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    enableScheduling(false);
                }
            });
        redeployButton.addActionListener(new ActionListener() {
                 public void actionPerformed(ActionEvent ae) {
                     long delay = getDelay();
                     if(calculatedRemaining!=Schedule.INDEFINITE &&
                         delay>calculatedRemaining) {
                         JOptionPane.showMessageDialog(dialog,
                                                       "<html>"+
                                                       "The Redeployment Delay "+
                                                       "<font color=blue>["+
                                                       TimeUtil.format(delay)+"]"+
                                                       "</font><br>"+
                                                       "you have requested is "+
                                                       "greater then the<br>"+
                                                       "deployment time remaining"+
                                                       "</html>",
                                                       "Redeployment Delay Error",
                                                       JOptionPane.ERROR_MESSAGE);
                         return;
                     }
                     option = REDEPLOY_OPTION;
                     dialog.dispose();
                 }
            });
        dismissButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    option = DISMISS_OPTION;
                    System.out.println(dialog.getSize());
                    dialog.dispose();
                }
           });
        days.setModel(new SpinnerNumberModel(0, 0, 365, 1));
        days.enableInputMethods(false);
        hours.setModel(new SpinnerNumberModel(0, 0, 23, 1));
        hours.enableInputMethods(false);
        minutes.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        minutes.enableInputMethods(false);
        seconds.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        seconds.enableInputMethods(false);
        immediateRedeployment.setSelected(true);
        enableScheduling(false);
    }

    private void enableScheduling(boolean enable) {
        days.setEnabled(enable);
        hours.setEnabled(enable);
        minutes.setEnabled(enable);
        seconds.setEnabled(enable);
    }

    public int getOption() {
        return(option);
    }

    public void stopTimer() {
        if(timer!=null)
            timer.stop();
    }

    public boolean getClean() {
        return(redeployClean.isSelected());
    }

    //public boolean getSticky() {
    //    return(redeploySticky.isSelected());
    //}

    public long getDelay() {
        long delay = REDEPLOY_IMMEDIATELY;
        if(!immediateRedeployment.isSelected()) {
            delay = (getValue(days)*DAY)+
                    (getValue(hours)*HOUR)+
                    (getValue(minutes)*MINUTE)+
                    (getValue(seconds)*SECOND);
        }
        return(delay);
    }

    private long getValue(JSpinner spinner) {
        Integer i = (Integer)spinner.getValue();
        long lv = 0;
        try {
            lv = i.longValue();
        } catch(NumberFormatException e) {
        }
        return(lv);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        GridBagConstraints gridBagConstraints;

        ButtonGroup deployGroup = new ButtonGroup();
        JPanel jPanel2 = new JPanel();
        redeploymentScheduling = new JRadioButton();
        redeployClean = new JCheckBox();
        //redeploySticky = new JCheckBox();
        JPanel jPanel1 = new JPanel();
        JLabel jLabel1 = new JLabel();
        days = new JSpinner();
        JLabel jLabel2 = new JLabel();
        hours = new JSpinner();
        JLabel jLabel3 = new JLabel();
        minutes = new JSpinner();
        JLabel jLabel4 = new JLabel();
        seconds = new JSpinner();
        immediateRedeployment = new JRadioButton();
        JLabel nextDeploymentLabel = new JLabel();
        nextDeploymentTime = new JTextField();
        JPanel jPanel3 = new JPanel();
        redeployButton = new JButton();
        dismissButton = new JButton();

        setLayout(new BorderLayout());

        setPreferredSize(new Dimension(300, 302));
        jPanel2.setLayout(new GridBagLayout());

        redeploymentScheduling.setText("Schedule redeployment");
        deployGroup.add(redeploymentScheduling);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        jPanel2.add(redeploymentScheduling, gridBagConstraints);

        redeployClean.setText("Redeploy Clean");
        redeployClean.setToolTipText("Redeploy using original attributes");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        jPanel2.add(redeployClean, gridBagConstraints);

        /*
        redeploySticky.setSelected(true);
        redeploySticky.setText("Redeploy Sticky");
        redeploySticky.setToolTipText("Redeploy to the same Cybernode");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        jPanel2.add(redeploySticky, gridBagConstraints);
        */
        jPanel1.setLayout(new GridLayout(2, 4, 4, 4));

        jPanel1.setBorder(new CompoundBorder(new EmptyBorder(new Insets(4, 4, 4, 4)), new EtchedBorder()));
        jLabel1.setLabelFor(days);
        jLabel1.setText("Days");
        jPanel1.add(jLabel1);

        jPanel1.add(days);

        jLabel2.setLabelFor(hours);
        jLabel2.setText("Hours");
        jPanel1.add(jLabel2);

        jPanel1.add(hours);

        jLabel3.setLabelFor(minutes);
        jLabel3.setText("Minutes");
        jPanel1.add(jLabel3);

        jPanel1.add(minutes);

        jLabel4.setLabelFor(seconds);
        jLabel4.setText("Seconds");
        jPanel1.add(jLabel4);

        jPanel1.add(seconds);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.1;
        jPanel2.add(jPanel1, gridBagConstraints);

        immediateRedeployment.setText("Redeploy immediately");
        deployGroup.add(immediateRedeployment);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        jPanel2.add(immediateRedeployment, gridBagConstraints);

        nextDeploymentLabel.setText("Deployment time remaining");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        jPanel2.add(nextDeploymentLabel, gridBagConstraints);

        nextDeploymentTime.setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        jPanel2.add(nextDeploymentTime, gridBagConstraints);

        add(jPanel2, BorderLayout.CENTER);

        jPanel3.setBorder(new EmptyBorder(new Insets(4, 4, 4, 4)));
        redeployButton.setText("Redeploy");
        jPanel3.add(redeployButton);

        dismissButton.setText("Dismiss");
        jPanel3.add(dismissButton);

        add(jPanel3, BorderLayout.SOUTH);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JSpinner days;
    private JButton dismissButton;
    private JSpinner hours;
    private JRadioButton immediateRedeployment;
    private JCheckBox redeployClean;
    //private JCheckBox redeploySticky;
    private JSpinner minutes;
    private JTextField nextDeploymentTime;
    private JButton redeployButton;
    private JRadioButton redeploymentScheduling;
    private JSpinner seconds;
    // End of variables declaration//GEN-END:variables

}
