package opstring
import org.rioproject.config.Constants

import java.util.logging.Level

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Scaling Service Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'SettableLoadService') {
        interfaces {
            classes 'org.rioproject.test.scaling.SettableLoadService'
            resources 'integrationTest/'
        }
        implementation(class: 'org.rioproject.test.scaling.SettableLoadServiceImpl') {
            resources 'integrationTest/'
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

