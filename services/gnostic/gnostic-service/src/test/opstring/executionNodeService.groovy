import org.rioproject.config.Constants
import java.util.logging.Level

deployment(name:'executionNodeService',  debug: 'true') {

    logging {
        logger 'org.rioproject.gnostic', Level.FINEST

    }
    /* Configuration for the discovery group that the service should join.
     * This first checks if the org.rioproject.groups property is set, if not
     * the user name is used */
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'test', 'org.rioproject.gnostic:test:1.0'

    /*
     * Declare the service to be deployed. The number of instances deployed
     * defaults to 1. If you require > 1 instances change as needed
     */
    service(name: 'ExecutionNodeService', fork: 'no') { //fork yes, works only in unix machines
        interfaces {
            classes 'org.rioproject.gnostic.test.ExecutionNodeService'
            artifact ref:'test'
        }
        implementation(class:'org.rioproject.gnostic.test.ExecutionNodeServiceImpl') {
            artifact ref:'test'
        }
        maintain 1
    }


    rules {
        rule{
            resource 'SLAKsessions'
            ruleClassPath 'org.rioproject.gnostic:test:1.0'
            serviceFeed(name: "ExecutionNodeService") {
                watches "kSessionCounter"
                //, ${SystemWatchID.SYSTEM_CPU}, ${SystemWatchID.JVM_MEMORY}"
            }
        }
    }


}