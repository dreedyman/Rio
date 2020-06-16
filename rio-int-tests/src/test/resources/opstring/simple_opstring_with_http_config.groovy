package opstring

import org.rioproject.net.HostUtil

def getAddressAndPort() {
    return "${HostUtil.getInetAddress().hostAddress}:${System.getProperty("TEST_PORT")}"
}
deployment(name:'Simple Test') {

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            artifact "org.rioproject:rio-int-tests:LATEST"
        }
        implementation(class: 'org.rioproject.test.simple.service.SimpleImpl') {
            artifact "org.rioproject:rio-int-tests:LATEST"
        }

        parameters {
            parameter name: "food", value: "fries"
        }
        maintain 1        

        configuration file: "http://${getAddressAndPort()}/simple_config.groovy"
    }
}
