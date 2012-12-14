import java.util.logging.Level
import org.rioproject.config.Constants
import org.rioproject.net.HostUtil

def String getCodebase() {
    return 'http://'+HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)+":9010"
}

deployment(name:'Scaling Service Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

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

