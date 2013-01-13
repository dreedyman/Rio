package org.rioproject.examples.hospital.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Introduction panel for the hospital example.
 */
@SuppressWarnings("unused")
public class HospitalIntro extends JPanel {

    public HospitalIntro(final Object obj) {
        super(new BorderLayout(8, 8));
        getAccessibleContext().setAccessibleName("Hospital Client");
        JEditorPane text = new JEditorPane("text/html", getIntro());
        text.setEditable(false);
        JPanel introPanel = new JPanel(new BorderLayout());
        introPanel.add(text, BorderLayout.CENTER);
        JButton hb = new JButton("<html><body><big>Go to Hospital User Interface</big></body></html>",
                                 Util.getImageIcon("images/hospital-icon.png"));
        add(introPanel, BorderLayout.CENTER);
        add(hb, BorderLayout.SOUTH);
        hb.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                HospitalUI ui = new HospitalUI(obj);
                ui.setVisible(true);
            }
        });
    }

    private String getIntro() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>\n");
        sb.append("<h2>Introduction</h2>\n");
        sb.append("Welcome to the Hospital demonstration user interface. This example " +
                  "combines Rio and Drools through the use of the Gnostic service.");
        sb.append("To interact with this demo, press the select the \"Hospital\" button.");
        sb.append("</p><br>\n");
        sb.append("<h2>Using the User Interface</h2>\n");
        sb.append("<h3>Admitting Patients</h3>\n");
        sb.append("You need to admit patients to the hospital, You can admit them " +
                  "one by one, or several at a time.  The Hospital has a rule (DoctorRule.drl) that when " +
                  "a Doctor has more than <bold><font color=blue>3</font></bold> patients, an ON CALL Doctor is changed to ON " +
                  "DUTY. You can optionally go to the \"Calamity\" tab and select a " +
                  "larger scale event that will admit multiple patients.\n");
        sb.append("<h3>Patient Monitoring</h3>\n");
        sb.append("As patients are admitted they are assigned beds. Each bed has a pulse " +
                  "and a temperature monitor. \n");
        sb.append("<h3>Available Beds</h3>\n");
        sb.append("As patients are assigned beds, if the number of available beds is 0, " +
                  "more beds are allocated (AvailableBedRule.drl).");
        sb.append("</body></html>");
        return sb.toString();
    }
}
