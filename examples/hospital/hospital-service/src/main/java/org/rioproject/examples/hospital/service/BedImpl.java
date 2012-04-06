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
package org.rioproject.examples.hospital.service;

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.examples.hospital.Bed;
import org.rioproject.examples.hospital.CalculablePatient;
import org.rioproject.examples.hospital.Patient;
import org.rioproject.watch.GaugeWatch;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a {@link Bed}
 */
public class BedImpl implements Bed {
    private Patient patient;
    private GaugeWatch temperature;
    private GaugeWatch pulse;
    private ScheduledExecutorService scheduler;
    private String roomNumber;
    private static final Logger logger = Logger.getLogger(BedImpl.class.getName());

    public void setServiceBeanContext(ServiceBeanContext context) {
        if(logger.isLoggable(Level.FINE)) {
            logger.config(String.format("Creating Bed:%d...", context.getServiceBeanConfig().getInstanceID()));
        }

        int numRooms = 40;
        String sNumRooms = (String)context.getInitParameter("numRooms");
        if(sNumRooms!=null) {
            try {
                numRooms = Integer.parseInt(sNumRooms);
            } catch(NumberFormatException e) {
                logger.warning(String.format("Bad roomNumber [%s], default to 40 rooms", sNumRooms));
            }
        }
        Random random = new Random();
        int room = random.nextInt(numRooms)+1;
        roomNumber = Integer.toString(room);
        temperature = new GaugeWatch("temperature");
        pulse = new GaugeWatch("pulse");
        context.getWatchRegistry().register(temperature, pulse);
        if(logger.isLoggable(Level.CONFIG)) {
            logger.config(String.format("Created Bed:%d in room #%s",
                                        context.getServiceBeanConfig().getInstanceID(),roomNumber));
        }
    }

    public String getRoomNumber() throws IOException {
        return roomNumber; 
    }

    public Patient getPatient() {
        return patient;
    }

    public Patient removePatient() {
        if(scheduler!=null)
            scheduler.shutdownNow();
        scheduler = null;
        Patient p = new Patient(patient.getPatientInfo());
        p.setDoctor(patient.getDoctor());
        p.setBed(patient.getBed());
        patient = null;
        return p;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        if(scheduler!=null)
            scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new FeederTask(),
                                      0,
                                      5,
                                      TimeUnit.SECONDS);
    }

    public void updatePatient(Patient patient) {
        this.patient = patient;
    }

    class FeederTask implements Runnable {

        public void run() {
            pulse.addWatchRecord(new CalculablePatient(pulse.getId(),
                                                       patient.getCurrentPulse(),
                                                       patient));
            temperature.addWatchRecord(new CalculablePatient(temperature.getId(),
                                                             patient.getCurrentTemperature(),
                                                             patient));
        }
    }
}
