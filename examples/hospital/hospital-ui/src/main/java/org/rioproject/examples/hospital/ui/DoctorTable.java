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

import org.rioproject.examples.hospital.Doctor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;

/**
 * Table of doctors.
 */
public class DoctorTable extends JPanel {
    private DoctorModel doctorModel;

    public DoctorTable() {
        super(new BorderLayout());
        this.doctorModel = new DoctorModel();
        JTable doctorTable = new JTable();        
        doctorTable.setModel(doctorModel);
        doctorTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setPreferredSize(new Dimension(500, 100));
        add("Center", new JScrollPane(doctorTable));
    }

    void addDoctor(Doctor d) {
        doctorModel.addDoctor(d);
    }

    void tableDataUpdated() {
        doctorModel.fireTableDataChanged();
    }

    void updateDoctors() {
        doctorModel.update();
        doctorModel.fireTableDataChanged();
    }

    static class DoctorModel extends AbstractTableModel {
        private final String[] columnNames = {"Doctor Name", "Status", "Num Assigned", "Patients Assigned"};

        private final java.util.List<LocalDoctor> doctors =
            new ArrayList<LocalDoctor>();

        public int getRowCount() {
            int rows;
            synchronized (doctors) {
                rows = doctors.size();
            }
            return rows;
        }

        void addDoctor(Doctor d) {
            synchronized (doctors) {
                doctors.add(new LocalDoctor(d));
            }
        }

        void update() {
            synchronized (doctors) {
                for(LocalDoctor ld : doctors) {
                    ld.getPatientsAssigned();
                }
            }
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            Object value = null;
            LocalDoctor d;
            synchronized (doctors) {
                d = doctors.get(row);
            }
            switch (col) {
                case 0:
                    value = d.getName();
                    break;
                case 1:
                    value = d.getStatus();
                    break;
                case 2:
                    value = d.getNumPatientsAssigned();
                    break;
                case 3:
                    value = d.getPatientsAssigned();
            }
            return value;
        }
    }

}
