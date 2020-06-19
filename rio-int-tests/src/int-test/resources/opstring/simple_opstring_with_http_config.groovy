package opstring

import org.rioproject.config.Constants
import org.rioproject.net.HostUtil

static String getAddressAndPort() {
    return "${HostUtil.getInetAddress().hostAddress}:${System.getProperty("TEST_PORT")}"
}

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Simple Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            resources 'integrationTest/'
        }
        implementation(class: 'org.rioproject.test.simple.service.SimpleImpl') {
            resources 'integrationTest/'
        }

        parameters {
            parameter name: "food", value: "fries"
        }
        maintain 1        

        configuration file: "http://${getAddressAndPort()}/simple_config.groovy"
    }
}
