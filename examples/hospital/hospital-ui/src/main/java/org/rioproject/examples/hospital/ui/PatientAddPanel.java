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
package org.rioproject.examples.hospital.ui;

import org.rioproject.examples.hospital.Patient;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Panel to add a Patient
 */
public class PatientAddPanel extends JPanel {
    JTextField name;
    JComboBox gender;
    JTextField years;
    JComboBox days;
    JComboBox month;
    final String[] months = new String[]{"January",
                                         "February",
                                         "March",
                                         "April",
                                         "May",
                                         "June",
                                         "July",
                                         "August",
                                         "September",
                                         "October",
                                         "November",
                                         "December"};
    final static String[] calamityYears = makeCalamityYears();
    PatientListener listener;
    Component parent;

    public PatientAddPanel() {
        super(new BorderLayout(8, 8));
        parent = SwingUtilities.getAncestorOfClass(JFrame.class, this);
        JPanel addSingle = createSinglePatientAdditionPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Patient Admission", addSingle);
        tabs.add("Calamity Control", createCalamityPanel());
        add(tabs, BorderLayout.CENTER);
        setMaximumSize(new Dimension(1000, 200));
    }

    void registerListener(PatientListener listener) {
        this.listener = listener;
    }

    private JPanel createSinglePatientAdditionPanel() {
        JPanel addSingle = new JPanel(new BorderLayout(8, 8));
        name = new JTextField();
        gender = new JComboBox(GENDER);
        years = new JTextField(4) {
            @Override
            protected Document createDefaultModel() {
                return new IntegerDocument(4);
            }
        };
        years.setText(null);
        years.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("clear"))
                    years.setText(null);
            }
        });
        days = new JComboBox();
        days.setEnabled(false);

        month = new JComboBox(months);
        month.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String month = (String) cb.getSelectedItem();
                String year = years.getText();
                Calendar cal = new GregorianCalendar(new Integer(year),
                                                     getMonthIndex(month),
                                                     1);
                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                for (int i = 1; i <= daysInMonth; i++)
                    days.addItem(Integer.toString(i));
                days.setEnabled(true);
            }
        });

        JPanel form = new JPanel();
        form.setLayout(new GridLayout(5, 2));
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Gender"));
        form.add(gender);
        form.add(new JLabel("Year of Birth"));
        form.add(years);
        form.add(new JLabel("Month"));
        form.add(month);
        form.add(new JLabel("Day"));
        form.add(days);

        addSingle.add(form, BorderLayout.CENTER);
        JButton add = new JButton("Admit Patient");
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String n = name.getText();
                if (n == null || n.length()==0) {
                    JOptionPane.showMessageDialog(parent,
                                                  "You must provide a patient name",
                                                  "Patient Entry Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String m = (String) month.getSelectedItem();
                if(m==null || m.length()==0) {
                    JOptionPane.showMessageDialog(parent,
                                                  "You must select a month",
                                                  "Patient Entry Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String y = years.getText().trim();
                if(y==null || y.length()==0) {
                    JOptionPane.showMessageDialog(parent,
                                                  "You must provide a year of birth",
                                                  "Patient Entry Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String d = (String)days.getSelectedItem();
                if(d==null || d.length()==0) {
                    JOptionPane.showMessageDialog(parent,
                                                  "You must select a day of birth",
                                                  "Patient Entry Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int monthIndex = getMonthIndex(m);
                Calendar cal =
                    new GregorianCalendar(new Integer(y),
                                          monthIndex,
                                          new Integer(d));
                Date birthDay = cal.getTime();

                Patient.PatientInfo pInfo =
                    new Patient.PatientInfo(name.getText(),
                                            (String) gender.getSelectedItem(),
                                            birthDay);
                Patient patient = new Patient(pInfo);
                if(listener!=null)
                    listener.patientCreated(patient);
                name.setText("");
                years.setText(null);
                days.removeAllItems();
                days.setEnabled(false);
            }
        });
        JPanel buttonPane = new JPanel();
        buttonPane.add(add);
        addSingle.add(buttonPane, BorderLayout.SOUTH);
        addSingle.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                     BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        return addSingle;
    }

    private JPanel createCalamityPanel() {
        JPanel addCalamity = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel();
        form.setLayout(new GridLayout(5, 2));
        form.add(new JLabel("Type of Calamity"));
        final JComboBox calamityType = new JComboBox(new String[]{"Multiple Car Crash",
                                                                  "Epic Flood",
                                                                  "Explosion"
                                                                  });
        form.add(calamityType);
        form.add(new JLabel("Number of Patients"));
        final JComboBox numCalamityPatients =
            new JComboBox(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"});
        form.add(numCalamityPatients);

        addCalamity.add(form, BorderLayout.CENTER);

        JButton add = new JButton("Create Calamity");
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String c = (String)calamityType.getSelectedItem();
                if(c==null || c.length()==0) {
                    JOptionPane.showMessageDialog(parent,
                                                  "You must select a calamity",
                                                  "Patient Entry Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String n = (String)numCalamityPatients.getSelectedItem();
                if(n==null || n.length()==0) {
                    JOptionPane.showMessageDialog(parent,
                                                  "You must provide a number of patients",
                                                  "Patient Entry Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int numPatients = Integer.parseInt(n);
                Random rand = new Random();
                for(int i=0; i<numPatients; i++) {
                    String gender = getRandomName(GENDER, rand);
                    String[] fNames = gender.equals("Male")?MALE_FIRST_NAMES:FEMALE_FIRST_NAMES;
                    String name = getRandomName(fNames, rand)+" "+getRandomName(LAST_NAMES, rand);
                    Patient.PatientInfo pInfo =
                        new Patient.PatientInfo(name,
                                                gender,
                                                createRandomBirthday(rand));
                    Patient patient = new Patient(pInfo);
                    if(listener!=null)
                        listener.patientCreated(patient);
                }

            }
        });
        JPanel buttonPane = new JPanel();
        buttonPane.add(add);
        addCalamity.add(buttonPane, BorderLayout.SOUTH);
        return addCalamity;
    }

    private int getMonthIndex(String month) {
        int monthIndex = 0;
        for (int i = 0; i < months.length; i++) {
            if (month.equals(months[i])) {
                monthIndex = i;
                break;
            }
        }
        return monthIndex;
    }

    private String getRandomName(String[] names, Random rand) {
        return names[rand.nextInt(names.length)];
    }

    private Date createRandomBirthday(Random rand) {
        String m = months[rand.nextInt(months.length)];
        int monthIndex = getMonthIndex(m);
        String y = calamityYears[rand.nextInt(calamityYears.length)];
        Calendar cal = new GregorianCalendar(new Integer(y),
                                             getMonthIndex(m),
                                             1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int[] daysArray = new int[daysInMonth+1];
        for (int i = 1; i <= daysInMonth; i++)
            daysArray[i] = i;
        int d = daysArray[rand.nextInt(daysArray.length)];
        cal = new GregorianCalendar(new Integer(y),
                                    monthIndex,
                                    d);

        return cal.getTime();
    }

    private static String[] makeCalamityYears() {
        java.util.List<String> l = new ArrayList<String>();
        for(int i=1929; i<2010; i++)
            l.add(Integer.toString(i));
        return l.toArray(new String[l.size()]);
    }

    class IntegerDocument extends PlainDocument {
        int width = 4;

        IntegerDocument(int width) {
            this.width = width;
        }

        public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException {

            if (str != null) {
                if ((getLength() + str.length()) <= width) {
                    try {
                        Integer.decode(str);
                        super.insertString(offs, str, a);
                    }
                    catch (NumberFormatException ex) {
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            }
        }
    }

    private static final String[] FEMALE_FIRST_NAMES = new String[] {
        "Mary", "Patricia", "Linda", "Barbara", "Elizabeth",
        "Jennifer", "Maria", "Susan", "Margaret", "Juliet",
        "Lisa", "Nancy", "Karen", "Betty", "Helen", "Sandra",
        "Donna", "Carol", "Ruth", "Sharon", "Michelle", "Laura",
        "Sara", "Christine", "Beth"
    };
    private static final String[] MALE_FIRST_NAMES = new String[] {
        "James", "John", "Robert", "Michael", "William", "David", "Richard",
        "Charles", "Joseph", "George", "Thomas", "Daniel", "Mark", "Anthony",
        "Ronald", "Shawn", "Matthew", "Ian", "Dennis", "Kevin", "Jason",
        "Jacob", "Sayid", "Hurley", "Desmond"
    };
    private static final String[] LAST_NAMES = new String[] {
        "Locke", "Linus", "Hume", "Shephard", "Jurah", "Bowers", "Ford",
        "Burke", "Austin", "Reyes", "Pace", "Nixon", "Johnson", "Carter",
        "Reagan", "Bush", "Clinton", "Eisenhower", "Truman", "Roosevelt", "Orr",
        "Howe", "Armstrong", "Mercx", "Reedy"
    };
    private static final String[] GENDER = new String[]{"Male", "Female"};
}
