/*
 * Copyright 2008 the original author or authors.
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

import java.io.IOException;
import java.util.List;

public interface Hospital {

    /**
     * Admit a {@link Patient}
     *
     * @param patient The Patient to admit
     *
     * @return The admitted Patient
     * 
     * @throws AdmissionException if there are problems admitting the patient
     * @throws IOException if communication errors occur
     */
    Patient admit(Patient patient) throws AdmissionException, IOException;

    /**
     * Release a {@link Patient}
     *
     * @param patient The Patient to release
     *
      * @return The released Patient
     *
     * @throws AdmissionException if there are problems releasing the patient
     * @throws IOException if communication errors occur
     */
    Patient release(Patient patient) throws AdmissionException, IOException;

    /**
     * Get all {@link Bed}s
     *
     * @return An immutable list of all of the hospital beds
     *
     * @throws IOException if communication errors occur
     */
    List<Bed> getBeds() throws IOException;

    /**
     * Get all {@link Doctor}s
     *
     * @return An immutable list of all of the doctors. If there are no
     * doctors, an empty list is returned
     *
     * @throws IOException if communication errors occur
     */
    List<Doctor> getDoctors() throws IOException;

    /**
     * Get all {@link Doctor}s on duty
     *
     * @return An immutable list of all of the doctors on call. If there are no
     * doctors on call, an empty list is returned
     *
     * @throws IOException if communication errors occur
     */
    List<Doctor> getDoctorsOnCall() throws IOException;

    /**
     * Get all {@link Doctor}s on duty
     *
     * @return An immutable list of all of the on duty doctors. If there are no
     * doctors on duty, an empty list is returned
     * 
     * @throws IOException if communication errors occur
     */
    List<Doctor> getDoctorsOnDuty() throws IOException;

     /**
     * Get all admitted {@link Patient}s
     *
     * @return An immutable list of all {@link Patient}s that have been admitted
     *
     * @throws IOException if communication errors occur
     */
    List<Patient> getAdmittedPatients() throws IOException;

    /**
     * Get all {@link Patient}s in the waiting room
     *
     * @return An immutable list of all {@link Patient}s in the waiting room
     *
     * @throws IOException if communication errors occur
     */
    List<Patient> getWaitingRoom() throws IOException;
}
