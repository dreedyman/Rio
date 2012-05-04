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

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.examples.hospital.*;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.resources.servicecore.Service;
import org.rioproject.admin.ServiceAdmin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User interface for hospital example
 */
public class HospitalUI extends JFrame {
    JSplitPane leftSplitPane;
    JSplitPane rightSplitPane;
    JSplitPane innerRightSplitPane;
    Hospital hospital;
    BedPanel bedPanel;
    DoctorTable doctorTable;
    PatientTable patientTable;
    PatientStatsPanel patientStats;
    JLabel bedCount;
    JLabel patientCount;
    ExecutorService bedAssignmentCheckingPool = Executors.newCachedThreadPool();

    public HospitalUI(final Object obj) {
        super("Hospital Client");
        getAccessibleContext().setAccessibleName("Hospital Client");
        try {
            ServiceItem item = (ServiceItem) obj;            
            init((Hospital)item.service);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void init(final Hospital h) throws IOException {
        this.hospital = h;
        List<Bed> beds = hospital.getBeds();
        bedPanel = new BedPanel(beds);
        patientTable = new PatientTable(h);
        doctorTable = new DoctorTable();

        for(Bed b : beds) {
            Patient p = b.getPatient();
            if(p!=null)
                patientTable.addPatient(p);
        }

        patientStats = new PatientStatsPanel();

        Listener l = new Listener();
        bedPanel.registerListener(l);
        patientTable.registerListener(l);

        bedCount = new JLabel(getBedCountLabelText());
        bedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        patientCount = new JLabel(getPatientCountLabelText());
        patientCount.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topRight = new JPanel();
        topRight.setLayout(new BoxLayout(topRight, BoxLayout.Y_AXIS));
        topRight.add(bedPanel);
        topRight.add(Box.createVerticalStrut(8));
        topRight.add(bedCount);
        topRight.add(Box.createVerticalStrut(8));
        topRight.add(patientCount);
        topRight.add(Box.createVerticalStrut(8));

        for(Doctor d : hospital.getDoctors()) {
            doctorTable.addDoctor(d);
        }

        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BorderLayout());

        bottomPane.add(patientTable, BorderLayout.CENTER);
        innerRightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                             new JScrollPane(topRight),
                                             new JScrollPane(bottomPane));
        innerRightSplitPane.setDividerLocation(218);
        innerRightSplitPane.setContinuousLayout(true);


        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        innerRightSplitPane,
                                        new JScrollPane(patientStats));
        rightSplitPane.setDividerLocation(417);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(new JLabel(Util.getImageIcon("images/physician-icon.png")));
        left.add(Box.createVerticalStrut(8));
        left.add(doctorTable);
        left.add(Box.createVerticalStrut(8));
        PatientAddPanel patientAdd =  new PatientAddPanel();
        patientAdd.registerListener(new PatientListener() {
            public void patientCreated(Patient p) {
                doAddPatient(p);
            }
            public void patientSelected(Patient p) {
            }
            public void patientRemoved(Patient p) {
            }
        });
        left.add(patientAdd);
        leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                       left,
                                       rightSplitPane);
        leftSplitPane.setDividerLocation(390);
        leftSplitPane.setContinuousLayout(true);
        Container content = getContentPane();
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        main.add(leftSplitPane, BorderLayout.CENTER);
        content.add(main);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                terminate();
            }
        });
        try {
            wireUpProvisionMonitorEventListener(h);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBedCountLabelText() {
        return "Used Beds: "+bedPanel.getOccupiedBedCount()+", " +
               "Available Beds: "+(bedPanel.getBedCount()-bedPanel.getOccupiedBedCount());
    }

    private String getPatientCountLabelText() {
        return "Patient Count: "+patientTable.getNumPatients();
    }

    @Override
    public void setVisible(final boolean show) {
        if(show) {
            int width = 1120;
            int height = 710;
            setSize(new Dimension(width, height));
        }
        super.setVisible(show);
    }

    private void doAddPatient(final Patient p) {
        try {
            Patient admittedPatient = hospital.admit(p);
            if(!bedPanel.occupyBed(admittedPatient)) {
                JOptionPane.showMessageDialog(this,
                                              "Could not get associated Bed",
                                              "Patient Assignment Error",
                                              JOptionPane.WARNING_MESSAGE);
            } else {
                updateStatusText(admittedPatient);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AdmissionException e) {
            if(!e.getMessage().equals("No available beds"))
                JOptionPane.showMessageDialog(this,
                                              "<html>Exception : <font color=red>" +
                                              e.getClass().getName() + "</font> : " +
                                              "<font color=blue>"+
                                              e.getLocalizedMessage() + "</font>"+
                                              "</html>",
                                              "Patient Assignment Error",
                                              JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateStatusText(final Patient p) {
        doctorTable.tableDataUpdated();
        patientTable.addPatient(p);
        bedCount.setText(getBedCountLabelText());
        patientCount.setText(getPatientCountLabelText());
    }

    private void terminate() {
        bedAssignmentCheckingPool.shutdownNow();
        patientStats.shutdown();
    }

    private void wireUpProvisionMonitorEventListener(final Object o) throws Exception {
        Service s = (Service)o;
        ServiceAdmin sAdmin = (ServiceAdmin) s.getAdmin();
        ServiceItem items[] = null;
        for(ServiceRegistrar sr : sAdmin.getJoinSet()) {
            ServiceMatches sm = sr.lookup(new ServiceTemplate(null, new Class[]{ProvisionMonitor.class}, null),
                                          Integer.MAX_VALUE);
            if(sm.items!=null && sm.items.length>0) {
                items = sm.items;
                break;
            }
        }
        if(items==null)
            return;
        BasicEventConsumer eventConsumer = new BasicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(),
                                                                  new BedListener());
        for(ServiceItem item : items)
            eventConsumer.register(item);
    }

    private class BedListener implements RemoteServiceEventListener {
        public void notify(RemoteServiceEvent event) {
            ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
            if(pme.getAction().equals(ProvisionMonitorEvent.Action.SERVICE_PROVISIONED)) {
                if(pme.getServiceElement().getName().equals("Beds")) {
                    ServiceBeanInstance instance = pme.getServiceBeanInstance();
                    try {
                        Bed bed = (Bed)instance.getService();
                        bedPanel.addBed(bed);
                        bedPanel.revalidate();
                        bedAssignmentCheckingPool.submit(new BedPendingAssignment(bed));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    private class BedPendingAssignment implements Runnable {
        final Bed bed;

        BedPendingAssignment(final Bed bed) {
            this.bed = bed;
        }

        public void run() {
            try {
                Patient p = null;
                while(p==null) {
                    Thread.sleep(500);
                    p = bed.getPatient();
                }
                bedPanel.occupyBed(p);
                updateStatusText(p);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class Listener implements PatientListener {
        public void patientCreated(final Patient p) {
            patientStats.setPatient(p);
            bedCount.setText(getBedCountLabelText());
            patientCount.setText(getPatientCountLabelText());
        }
        public void patientSelected(final Patient p) {
            patientStats.setPatient(p);
        }
        public void patientRemoved(final Patient p) {
            bedPanel.emptyBed(p);
            patientStats.setPatient(null);
            doctorTable.updateDoctors();
            bedCount.setText(getBedCountLabelText());
            patientCount.setText(getPatientCountLabelText());
        }
    }
}
