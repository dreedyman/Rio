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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

public class Patient implements Serializable {
    private Doctor doctor;
    private PatientInfo patientInfo;
    private Bed bed;
    private Random pulse = new Random();
    private Random temperature = new Random();
    private double[] pulseValues = new double[]{72, 74, 76, 78,
                                                10, 9, 8, 7, 6,
                                                5, 3, 2, 1, 0,
                                                89, 90, 92, 93,
                                                97, 55, 100, 70};
    private double[] temperatureValues = new double[]{97.1, 97.0, 97.6,
                                                      98.1, 98.2, 98.6,
                                                      99.0, 99.1, 99.8,
                                                      100.0, 100.1, 100.2,
                                                      101.0, 101.2, 101.3};

    public Patient(PatientInfo patientInfo) {
        this.patientInfo = patientInfo;
    }

    public PatientInfo getPatientInfo() {
        return patientInfo;
    }    

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Bed getBed() {
        return bed;
    }

    public void setBed(Bed bed) {
        this.bed = bed;
    }

    public Double getCurrentPulse() {
        return pulseValues[pulse.nextInt(pulseValues.length)];
    }

    public Double getCurrentTemperature() {
        return temperatureValues[temperature.nextInt(temperatureValues.length)];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Patient patient = (Patient) o;
        return !(patientInfo != null
                 ? !patientInfo.equals(patient.patientInfo)
                 : patient.patientInfo != null);

    }

    @Override
    public int hashCode() {
        return patientInfo != null ? patientInfo.hashCode() : 0;
    }

    public static class PatientInfo implements Serializable {
        private String name;
        private String gender;
        private Date birthday;

        public PatientInfo(String name, String gender, Date birthday) {
            this.name = name;
            this.gender = gender;
            this.birthday = birthday;
        }

        public String getName() {
            return name;
        }

        public String getGender() {
            return gender;
        }

        public Date getBirthday() {
            return birthday;
        }

        public int getAge() {
            if (birthday == null) {
                return -1;
            }
            Calendar cal = new GregorianCalendar();
            cal.setTime(birthday);
            Calendar now = new GregorianCalendar();
            int res = now.get(Calendar.YEAR) - cal.get(Calendar.YEAR);
            if ((cal.get(Calendar.MONTH) > now.get(Calendar.MONTH))
                || (cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                    && cal.get(Calendar.DAY_OF_MONTH) > now.get(Calendar.DAY_OF_MONTH))) {
                res--;
            }
            return res;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            PatientInfo that = (PatientInfo) o;
            return !(birthday != null
                     ? !birthday.equals(that.birthday)
                     : that.birthday != null) && !(gender != null
                                                   ? !gender.equals(that.gender)
                                                   : that.gender != null) &&
                   !(name != null
                     ? !name.equals(that.name)
                     : that.name != null);

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (gender != null ? gender.hashCode() : 0);
            result = 31 * result + (birthday != null ? birthday.hashCode() : 0);
            return result;
        }
    }
}
