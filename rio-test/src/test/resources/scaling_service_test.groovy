import java.net.InetAddress
import java.util.logging.Level

def String getCodebase() {
    return 'http://'+InetAddress.getLocalHost().getHostAddress()+":9010"
}

deployment(name:'Scaling Service Test') {

    codebase getCodebase()

    groups '${org.rioproject.groups}'

    service(name: 'SettableLoadService') {
        interfaces {
            classes 'org.rioproject.test.scaling.SettableLoadService'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.scaling.SettableLoadServiceImpl') {
            resources 'test-classes/'
        }

        sla(id:'load', low:0.48, high: 0.52) {
            policy type: 'scaling', lowerDampener: 5000, upperDampener: 5000
        }
        maintain 1

        logging {
            logger('org.rioproject.sla', Level.FINEST)
        }
    }
}

