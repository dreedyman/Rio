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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static Logger logger = LoggerFactory.getLogger(DoctorImpl.class.getName());

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
        logger.debug("Status for {} is {}", name, sStatus);
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
        logger.info("Set {} to {}", name, status.name());
    }

    public String getName() {
        return name;
    }

    public void assignPatient(Patient p) {
        synchronized(patients) {
            if(patients.add(p)) {
                numPatients.addValue(patients.size());
                logger.info("{}, total patients: {}, added {} ", name, patients.size(), p.getPatientInfo().getName());
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
