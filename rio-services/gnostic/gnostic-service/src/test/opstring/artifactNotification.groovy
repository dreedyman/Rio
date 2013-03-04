import org.rioproject.config.Constants

import java.util.logging.Level

deployment(name: 'Notification Using Artifact Test') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'service', 'org.rioproject.gnostic:gnostic-service:5.0-M3'
    artifact id: 'service-api', 'org.rioproject.gnostic:gnostic-api:5.0-M3'
    artifact id: 'test', 'org.rioproject.gnostic:test:1.0'

    logging {
        logger 'org.rioproject.gnostic', Level.FINE
    }
    
    service(name: 'Test') {
        interfaces {
            classes 'org.rioproject.gnostic.test.TestService'
            artifact ref: 'test'
        }

        implementation(class: 'org.rioproject.gnostic.test.TestServiceImpl') {
            artifact ref: 'test'
        }

        comment: 'Gnostic Test Service'

        maintain 1
    }

    rules {
        rule{
            resource 'file:'+System.getProperty('user.dir')+'/src/test/resources/CounterNotification'
            ruleClassPath 'org.rioproject.gnostic:test:1.0'
            serviceFeed(name: "Test") {
                watches "notification"
            }
        }
    }

}
