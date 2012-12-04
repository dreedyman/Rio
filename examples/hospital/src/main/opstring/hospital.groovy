
import org.rioproject.config.Constants

deployment(name: 'Hospital') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'service', 'org.rioproject.examples.hospital:hospital-service:2.0.2'
    artifact id: 'service-dl', 'org.rioproject.examples.hospital:hospital-api:2.0.2'

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
            ruleClassPath 'org.rioproject.examples.hospital:hospital-rule:2.0.2'
            serviceFeed(name: "Doctors") {
                watches "numPatients"
            }
            serviceFeed(name: "Admission") {
                watches "availableBeds"
            }
        }
    }
}

