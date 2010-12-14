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

import org.rioproject.examples.hospital.AdmissionException;
import org.rioproject.examples.hospital.Hospital;
import org.rioproject.examples.hospital.Patient;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Table of patients.
 */
public class PatientTable extends JPanel {
    private JTable patientTable;
    private final PatientModel patientModel = new PatientModel();
    private PatientListener listener;

    public PatientTable(final Hospital h) {
        super(new BorderLayout());
        patientTable = new JTable();
        patientTable.setModel(patientModel);
        patientTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        patientTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if(listener!=null) {
                    int row = patientTable.getSelectedRow();
                    listener.patientSelected(patientModel.getPatient(row));                    
                }
            }

            public void mousePressed(MouseEvent e) {
                int row = patientTable.getSelectedRow();
                if(row==-1) {
                    return;
                }
                if(e.isPopupTrigger()) {
                    showPopup(e, row);
                }
            }

            private void showPopup(MouseEvent e, final int row) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem checkout = new JMenuItem("Check Patient Out");
                checkout.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        Patient p = patientModel.getPatient(row);
                        if(p!=null) {
                            try {
                                p = h.release(p);
                                if(p!=null && listener!=null) {
                                    patientModel.removePatient(row);
                                    listener.patientRemoved(p);
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (AdmissionException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
                popup.pack();
                popup.add(checkout);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        
        setPreferredSize(new Dimension(500, 100));
        add("Center", new JScrollPane(patientTable));
    }

    void addPatient(Patient p) {
        int row = patientModel.addPatient(p);
        if(row!=-1) {
            patientTable.setRowSelectionInterval(row, row);
            if(listener!=null)
                listener.patientSelected(p);
        }
    }

    void registerListener(PatientListener listener) {
        this.listener = listener;
    }

    int getNumPatients() {
        return patientModel.getRowCount();
    }    

    class PatientModel extends AbstractTableModel {
        private final java.util.List<Patient> patients = new ArrayList<Patient>();
        private final String[] columnNames = {"Patient Name", "Age", "Gender", "Room", "Doctor"};

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            int rows;
            synchronized(patients) {
                rows = patients.size();
            }
            return rows;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            Object value = null;
            Patient p;
            synchronized(patients) {
                p = patients.get(row);
            }
            switch(col) {
                case 0:
                    value = p.getPatientInfo().getName();
                    break;
                case 1:
                    value = p.getPatientInfo().getAge();
                    break;
                case 2:
                    value = p.getPatientInfo().getGender();
                    break;
                case 3:
                    try {
                        value = p.getBed().getRoomNumber();
                    } catch (IOException e) {
                        value = e.getClass().getName()+": "+e.getLocalizedMessage();
                    }
                    break;
                case 4:
                    try {
                        value = p.getDoctor().getName();
                    } catch (IOException e) {
                        value = e.getClass().getName()+": "+e.getLocalizedMessage();
                    }
            }
            return value;
        }

        Patient getPatient(int row) {
            return patients.get(row);
        }
        
        int addPatient(Patient p) {
            int row = -1;
            synchronized(patients) {
                if(!patients.contains(p)) {
                    row = patients.size();
                    patients.add(p);
                }
            }
            fireTableDataChanged();
            return row;
        }

        void removePatient(int row) {
            synchronized(patients) {
                patients.remove(row);
            }
            fireTableDataChanged();
        }

    }
}
