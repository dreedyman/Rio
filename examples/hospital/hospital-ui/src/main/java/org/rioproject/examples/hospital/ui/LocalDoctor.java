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
import org.rioproject.examples.hospital.Patient;

import java.io.IOException;

public class LocalDoctor {
    Doctor d;

    LocalDoctor(Doctor d) {
        this.d = d;
    }

    String getSpecialty() {
        String s;
        try {
            s = d.getSpecialty();
        } catch (IOException e) {
            s = e.getClass().getName() + ": " + e.getLocalizedMessage();
        }
        return s;
    }
    
    String getStatus() {
        String s;
        try {
            s = d.getStatus().name();
        } catch (IOException e) {
            s = e.getClass().getName() + ": " + e.getLocalizedMessage();
        }
        return s;
    }

    String getName() {
        String s;
        try {
            s = d.getName();
        } catch (IOException e) {
            s = e.getClass().getName() + ": " + e.getLocalizedMessage();
        }
        return s;
    }

    String getNumPatientsAssigned() {
        String s;
        try {
            s = Integer.toString(d.getPatients().size());
        } catch (IOException e) {
            s = e.getClass().getName() + ": " + e.getLocalizedMessage();
        }
        return s;
    }
    String getPatientsAssigned() {
        String s;
        try {
            java.util.List<Patient> patients = d.getPatients();
            StringBuilder sb = new StringBuilder();
            for (Patient p : patients) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(p.getPatientInfo().getName());
            }
            s = sb.toString();
        } catch (IOException e) {
            s = e.getClass().getName() + ": " + e.getLocalizedMessage();
        }
        return s;
    }
    
}
