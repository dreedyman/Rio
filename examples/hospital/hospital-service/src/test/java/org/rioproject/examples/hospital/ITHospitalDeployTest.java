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
package org.rioproject.examples.hospital;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Testing the hospital example using the Rio test framework
 */
@RunWith (RioTestRunner.class)
public class ITHospitalDeployTest {
    @SetTestManager
    static TestManager testManager;
    private Hospital hospital;
    private List<Doctor> docs;
    private static final Logger logger = LoggerFactory.getLogger(ITHospitalDeployTest.class.getName());

    @Before
    public void setup() throws Exception {
	    Assert.assertNotNull(testManager);
        testManager.waitForDeployment(testManager.getOperationalStringManager());
        hospital = (Hospital)testManager.waitForService(Hospital.class);
        Assert.assertNotNull(hospital);
        /* Wait for rules to load */
        Thread.sleep(10*1000);
        List<Bed> beds = hospital.getBeds();
        Assert.assertEquals("Should have 10 Beds", 10, beds.size());
        docs = hospital.getDoctors();
        Assert.assertEquals("Should have 4 Doctors", 4, docs.size());
        int onDuty = getNumDoctorsForStatus(docs, Doctor.Status.ON_DUTY);
        Assert.assertTrue("1 Doctor should be ON_DUTY, have "+onDuty, onDuty==1);
        int onCall = getNumDoctorsForStatus(docs, Doctor.Status.ON_CALL);
        Assert.assertTrue("2 Doctor should be ON_CALL, have "+onCall, onCall==2);
        int offDuty = getNumDoctorsForStatus(docs, Doctor.Status.OFF_DUTY);
        Assert.assertTrue("1 Doctor should be OFF_DUTY, have "+offDuty, offDuty==1);
    }

    @After
    public void release() {
        Assert.assertNotNull("Should not have a null Hospital", hospital);
        try {
            for(Patient patient : hospital.getAdmittedPatients()) {
                hospital.release(patient);
            }
            int numAdmitted = hospital.getAdmittedPatients().size();
            Assert.assertTrue("Should have no admitted Patients, have "+numAdmitted, numAdmitted==0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeployment() {
        Throwable thrown = null;
        try {
            for(Patient p : first8Presidents()) {
                hospital.admit(p);
                /* Give the admitting room a chance to dole out assignments */
                Thread.sleep(100);
            }
            int numAssigned = waitForDocs(2);
            Assert.assertTrue("Should have at least 2 Doctors with assigned patients, have "+numAssigned+". " +
                              "Check that the DoctorRule fired", numAssigned>=2);
            getAssignedDocs();

            List<Patient> patients = next8Presidents();

            int onDuty = getNumDoctorsForStatus(docs, Doctor.Status.ON_DUTY);
            if(onDuty<3) {
                Thread.sleep(500);
                onDuty = getNumDoctorsForStatus(docs, Doctor.Status.ON_DUTY);
            }
            Assert.assertTrue("3 Doctors should be ON_DUTY, have " + onDuty, onDuty == 3);

            try {
                hospital.admit(patients.remove(0));
                /* Give the admitting room a chance to dole out assignments */
                Thread.sleep(100);
            } catch(Exception e) {
                System.out.println(e.getClass().getName()+": "+e.getLocalizedMessage());
            }
            try {
                hospital.admit(patients.remove(0));
            } catch(Exception e) {
                System.out.println(e.getClass().getName()+": "+e.getLocalizedMessage());
            }
            int found = waitForBeds(11);
            Assert.assertTrue("Should have at least 11 Beds, have "+found, 11<=found);
            hospital.admit(patients.remove(0));
            found = waitForBeds(12);
            Assert.assertTrue("Should have at least 12 Beds, have "+found, 12<=found);
            for(Patient p : patients) {
                try {
                    hospital.admit(p);
                } catch(AdmissionException e) {
                    List<Patient> patientsInWaitingRoom = hospital.getWaitingRoom();
                    int wSize = patientsInWaitingRoom.size();
                    logger.info("AdmissionException: {}, waiting room size={}", e.getLocalizedMessage(), wSize);
                }
            }
            found = waitForBeds(16);
            Assert.assertTrue("Should have at least 16 Beds, have "+found, 16 <= found);

            onDuty = getNumDoctorsForStatus(docs, Doctor.Status.ON_DUTY);
            Assert.assertTrue("3 Doctors should be ON_DUTY, have "+onDuty, onDuty==3);
            int onCall = getNumDoctorsForStatus(docs, Doctor.Status.ON_CALL);
            Assert.assertTrue("0 Doctors should be ON_CALL, have "+onCall, onCall==0);
            int offDuty = getNumDoctorsForStatus(docs, Doctor.Status.OFF_DUTY);
            Assert.assertTrue("1 Doctor should be OFF_DUTY, have "+offDuty, offDuty==1);

            Doctor d = getOffDutyDoctor(docs);
            Assert.assertNotNull(d);
            d.onDuty();
            onDuty = getNumDoctorsForStatus(docs, Doctor.Status.ON_DUTY);
            Assert.assertTrue("4 Doctors should be ON_DUTY, have "+onDuty, onDuty==4);

        } catch (Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull("Should not have thrown an exception", thrown);
    }

    private int getAssignedDocs() throws IOException {
        int numAssigned = 0;
        for(Doctor d : hospital.getDoctors()) {
            logger.info("{}, {}, num patients: {}", d.getName(), d.getStatus(), d.getPatients().size());
            if(d.getPatients().size()>0) {
                numAssigned++;
            }
        }
        return numAssigned;
    }

    private int waitForDocs(int num) throws Exception {
        int found = 0;
        int iterations = 120;
        long t0 = System.currentTimeMillis();

        for(int i=0; i<iterations; i++) {
            for(Doctor d :  hospital.getDoctors()) {
                logger.info("{}  has {} patients", d.getName(), d.getPatients().size());
                if(d.getPatients().size()>0) {
                    found++;
                }
            }
            if(found<num) {
                Thread.sleep(500);
                found = 0;
            } else {
                break;
            }
        }
        long t1 = System.currentTimeMillis();
        logger.info("Waited ({}) millis for {} Doctors with assigned patients, found {}", (double)(t1-t0), num, found);
        return found;
    }

    private int waitForBeds(int num) throws Exception {
        int found = 0;
        int iterations = 120;
        long t0 = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            List <Bed> beds = hospital.getBeds();
            found = beds.size();
            if(found<num) {
                Thread.sleep(500);
            } else {
                break;
            }
        }
        long t1 = System.currentTimeMillis();
        logger.info("Waited ({}) millis for {} Beds, found {}", (double)(t1-t0), num, found);
        return found;
    }

    private int getNumDoctorsForStatus(List<Doctor> docs, Doctor.Status status) throws IOException {
        int count = 0;
        for(Doctor d : docs) {
            if(d.getStatus().equals(status)) {
                count++;
            }
        }
        String dr = count==1?"Doctor":"Doctors";
        logger.info("Found {} {} {}", count, dr, status.name());
        return count;
    }

    private Doctor getOffDutyDoctor(List<Doctor> docs) throws IOException {
        Doctor doc = null;
        for(Doctor d : docs) {
            if(d.getStatus().equals(Doctor.Status.OFF_DUTY)) {
                doc = d;
                break;
            }
        }
        return doc;
    }

    private List<Patient> first8Presidents() {
        List<Patient> patients = new ArrayList<Patient>();
        patients.add(create("George Washington", new GregorianCalendar(1732, 1, 22)));
        patients.add(create("John Adams", new GregorianCalendar(1735, 9, 30)));
        patients.add(create("Thomas Jefferson", new GregorianCalendar(1743, 3, 13)));
        patients.add(create("James Madison", new GregorianCalendar(1751, 2, 16)));
        patients.add(create("James Monroe", new GregorianCalendar(1758, 3, 28)));
        patients.add(create("John Quincy Adams", new GregorianCalendar(1767, 6, 11)));
        patients.add(create("Andrew Jackson", new GregorianCalendar(1767, 2, 15)));
        patients.add(create("Martin Van Buren", new GregorianCalendar(1782, 11, 5)));
        return patients;
    }

    private List<Patient> next8Presidents() {
        List<Patient> patients = new ArrayList<Patient>();
        patients.add(create("William Henry Harrison", new GregorianCalendar(1773, 1, 9)));
        patients.add(create("John Tyler", new GregorianCalendar(1790, 2, 29)));
        patients.add(create("James Knox Polk", new GregorianCalendar(1795, 10, 2)));
        patients.add(create("Zachary Taylor", new GregorianCalendar(1784, 10, 24)));
        patients.add(create("Millard Fillmore", new GregorianCalendar(1800, 0, 7)));
        patients.add(create("Franklin Pierce", new GregorianCalendar(1804, 10, 23)));
        patients.add(create("James Buchanan", new GregorianCalendar(1791, 3, 23)));
        patients.add(create("Abraham Lincoln", new GregorianCalendar(1809, 1, 12)));
        return patients;
    }

    private Patient create(String name, Calendar cal) {
        return new Patient(new Patient.PatientInfo(name, "Male", cal.getTime()));
    }
}
