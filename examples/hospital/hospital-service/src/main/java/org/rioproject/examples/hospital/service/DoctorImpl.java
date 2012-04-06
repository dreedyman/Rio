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

import net.jini.config.ConfigurationException;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.examples.hospital.Doctor;
import org.rioproject.examples.hospital.Patient;
import org.rioproject.watch.GaugeWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a {@link Doctor}
 */
public class DoctorImpl implements Doctor {
    protected Status status = Status.OFF_DUTY;
    protected String name;
    private String specialty;
    private final List<Patient> patients = new ArrayList<Patient>();
    private static final String COMPONENT = DoctorImpl.class.getName();
    private GaugeWatch numPatients;
    private static Logger logger = Logger.getLogger(DoctorImpl.class.getName());

    public void setServiceBeanContext(ServiceBeanContext context) throws
                                                                  ConfigurationException {
        Integer instanceID = context.getServiceBeanConfig().getInstanceID().intValue();
        name = (String)context.getConfiguration().getEntry(COMPONENT,
                                                           "name",
                                                           String.class,
                                                           null,
                                                           instanceID);
        String sStatus = (String)context.getConfiguration().getEntry(COMPONENT,
                                                                     "status",
                                                                     String.class,
                                                                     null,
                                                                     name);
        if(logger.isLoggable(Level.CONFIG)) {
            logger.log(Level.CONFIG, "Status for {0} is {1}", new Object[]{name, sStatus});
        }
        this.status = Status.valueOf(sStatus);
        numPatients = new GaugeWatch("numPatients");
        context.getWatchRegistry().register(numPatients);
    }    

    public String getSpecialty() {
        return specialty;
    }

    public Status getStatus() {
        return status;
    }

    public void onCall() {
        status = Status.ON_CALL;
        logStatusChange();
    }

    public void onDuty() {
        status = Status.ON_DUTY;
        logStatusChange();
    }

    public void offDuty() {
        status = Status.OFF_DUTY;
        logStatusChange();
    }

    private void logStatusChange() {
        logger.log(Level.INFO, "Set {0} to {1}", new Object[]{name, status.name()});
    }

    public String getName() {
        return name;
    }

    public void assignPatient(Patient p) {
        synchronized(patients) {
            if(patients.add(p)) {
                numPatients.addValue(patients.size());
                logger.log(Level.INFO, "{0}, total patients: {1}, added {2} ",
                           new Object[]{name, patients.size(), p.getPatientInfo().getName()});
            }
        }        
    }

    public void removePatient(Patient p) {
        synchronized(patients) {
            if(patients.remove(p)) {
                numPatients.addValue(patients.size());
            }
        }
    }

    public List<Patient> getPatients() {
        List<Patient> p;
        synchronized(patients) {
            p = Collections.unmodifiableList(patients);
        }
        return p;
    }
    
}
