import org.rioproject.net.HostUtil

def getAddressAndPort() {
    return "${HostUtil.getInetAddress().hostAddress}:${System.getProperty("TEST_PORT")}"
}
deployment(name:'Simple Test') {

    artifact id:'service', 'org.rioproject.test.simple:simple-service:2.0'
    artifact id:'service-dl', 'org.rioproject.test.simple:simple-api:2.0'

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            artifact ref:'service-dl'
        }
        implementation(class: 'org.rioproject.test.simple.SimpleImpl') {
            artifact ref:'service'
        }

        parameters {
            parameter name: "food", value: "fries"
        }
        maintain 1        

        configuration file: "http://${getAddressAndPort()}/simple_config.groovy"
    }
}
