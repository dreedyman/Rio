import org.rioproject.config.Constants

import java.util.logging.Level
import org.rioproject.system.SystemWatchID

deployment(name: 'Test Deploy 3') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    logging {
        logger 'org.rioproject.gnostic', Level.FINE        
    }
    
    service(name: 'S3') {
        interfaces {
            classes 'org.rioproject.gnostic.test.TestService'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.gnostic.test.TestServiceImpl') {
             resources 'test-classes/'
        }
        maintain 1
    }

    rules {
        rule{
            resource 'file:'+System.getProperty('user.dir')+'/src/test/resources/SystemUtilization'
            ruleClassPath 'file:target/test-classes/'
            serviceFeed(name: "S3") {
                watches "${SystemWatchID.SYSTEM_CPU}, ${SystemWatchID.JVM_MEMORY}"
            }
        }
    }

}
