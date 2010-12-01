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

import org.rioproject.examples.hospital.Bed;
import org.rioproject.examples.hospital.Patient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel of beds.
 */
public class BedPanel extends JPanel {
    private final List<BedComponent> beds = new ArrayList<BedComponent>();
    private final Map<String, JPanel> roomMap = new HashMap<String, JPanel>();
    private ImageIcon availableBedIcon;
    private ImageIcon occupiedBedIcon;
    private PatientListener listener;
    private JPanel roomPanel;

    public BedPanel(List<Bed> beds) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        ImageIcon bedIcon = Util.getImageIcon("images/hospital-bed.png");
        add(new JLabel(bedIcon));
        add(Box.createVerticalStrut(8));

        roomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        availableBedIcon = Util.getImageIcon("images/empty-bed.png");
        occupiedBedIcon = Util.getImageIcon("images/occupied-bed.png");
        addBeds(beds);
        add(roomPanel);
        setPreferredSize(new Dimension(500, 50));
    }

    int getBedCount() {
        int count;
        synchronized(beds) {
            count = beds.size();
        }
        return count;
    }

    int getOccupiedBedCount() {
        int count = 0;
        synchronized(beds) {
            for(BedComponent b : beds) {
                if(b.isOccupied())
                    count++;
            }
        }
        return count;
    }

    private int addBeds(List<Bed> beds) {
        int count = 0;
        for(Bed b : beds) {
            try {
                addBed(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    void registerListener(PatientListener listener) {
        this.listener = listener;
    }

    boolean occupyBed(Patient p) {
        BedComponent b = getBedComponent(p.getBed());
        if(b==null)
            return false;
        b.button.setIcon(occupiedBedIcon);
        b.setOccupied(true);
        String s = p.getPatientInfo().getName();
        b.button.setToolTipText("Patient: "+s);
        b.button.repaint();
        return true;
    }

    boolean emptyBed(Patient p) {
        BedComponent b = getBedComponent(p.getBed());
        if(b==null)
            return false;
        b.button.setIcon(availableBedIcon);
        b.button.setToolTipText(null);
        b.button.repaint();
        b.setOccupied(false);
        return true;
    }

    void addBed(Bed b) throws IOException {
        try {
            String roomNumber = b.getRoomNumber();
            JPanel panel = roomMap.get(roomNumber);
            if(panel==null) {
                panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                panel.setBackground(new Color(222, 227, 233));
                panel.setBorder(BorderFactory.createEtchedBorder());
                roomPanel.add(panel);
                roomMap.put(roomNumber, panel);
            }
            boolean empty = b.getPatient()==null;
            addBed(panel, b, empty);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addBed(JPanel roomPanel, Bed bed, boolean empty) {
        JButton b = new JButton();
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Bed bed = findSelectedBed((JButton) actionEvent.getSource());
                if(bed!=null) {
                    try {
                        Patient p = bed.getPatient();
                        if(p!=null && listener!=null) {
                            listener.patientSelected(p);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        b.setIcon(empty?availableBedIcon:occupiedBedIcon);
        roomPanel.add(b);
        BedComponent bc = new BedComponent(b, bed);
        bc.setOccupied(!empty);
        beds.add(bc);
    }

    private BedComponent getBedComponent(Bed bed) {
        BedComponent bedC = null;
        synchronized(beds) {
            for(BedComponent b : beds) {
                if(b.bed.equals(bed)) {
                    bedC = b;
                    break;
                }
            }
        }
        return bedC;
    }

    private Bed findSelectedBed(JButton button) {
        Bed bed = null;
        synchronized(beds) {
            for(BedComponent b : beds) {
                if(b.button.equals(button)) {
                    bed = b.bed;
                    break;
                }
            }
        }
        return bed;
    }
    
    private class BedComponent {
        JButton button;
        Bed bed;
        boolean occupied = false;

        BedComponent(JButton button, Bed bed) {
            this.button = button;
            this.bed = bed;
        }

        boolean isOccupied() {
            return occupied;
        }

        void setOccupied(boolean occupied) {
            this.occupied = occupied;
        }
    }
}
