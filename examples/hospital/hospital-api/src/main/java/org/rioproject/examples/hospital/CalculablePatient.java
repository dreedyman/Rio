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

import org.rioproject.watch.Calculable;

/**
 * A {@link org.rioproject.watch.Calculable} that includes the {@link Patient}
 */
public class CalculablePatient extends Calculable {
    private Patient patient;

    public CalculablePatient(String id, double value, Patient patient) {
        super(id, value);
        this.patient = patient;
    }

    public Patient getPatient() {
        return patient;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getId())
            .append(", ")
            .append(patient.getPatientInfo().getName())
            .append(", value: ").append(getValue())
            .append(", when: ").append(getWhen());
        return sb.toString();
    }
}
