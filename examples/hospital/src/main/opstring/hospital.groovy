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
import org.rioproject.config.Constants

/**
 * The deployment configuration for the Hospital example
 *
 * @author Dennis Reedy
 */
deployment(name: 'Hospital') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'service', 'org.rioproject.examples.hospital:hospital-service:2.1'
    artifact id: 'service-dl', 'org.rioproject.examples.hospital:hospital-api:2.1'

    service(name: 'Admission') {
        interfaces {
            classes 'org.rioproject.examples.hospital.Hospital'
            artifact ref: 'service-dl'
        }
        implementation(class: 'org.rioproject.examples.hospital.service.HospitalImpl') {
            artifact ref: 'service'
        }
        associations {
            ['Doctors', 'Beds'].each { s ->
                association name: "$s", type: 'requires', property: "${s.toLowerCase()}"
            }
        }

        configuration file: 'classpath:HospitalConfig.groovy'

        maintain 1

    }

    service(name: 'Doctors') {
        interfaces {
            classes "org.rioproject.examples.hospital.Doctor"
            artifact ref: 'service-dl'
        }
        implementation(class: "org.rioproject.examples.hospital.service.DoctorImpl") {
            artifact ref: 'service'
        }

        configuration file: 'classpath:DoctorConfig.groovy'

        maintain 4
    }

    service(name: 'Beds') {
        interfaces {
            classes "org.rioproject.examples.hospital.Bed"
            artifact ref: 'service-dl'
        }
        implementation(class: "org.rioproject.examples.hospital.service.BedImpl") {
            artifact ref: 'service'
        }

        parameters {
            parameter name: 'numRooms', value: '5'
        }

        maintain 10
    }

    rules {
        rule {
            resource 'DoctorRule, AvailableBedRule'
            ruleClassPath 'org.rioproject.examples.hospital:hospital-rule:2.1'
            serviceFeed(name: "Doctors") {
                watches "numPatients"
            }
            serviceFeed(name: "Admission") {
                watches "availableBeds"
            }
        }
    }
}

