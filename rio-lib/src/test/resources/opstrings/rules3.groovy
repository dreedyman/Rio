import org.rioproject.config.Constants

import java.util.logging.Level

deployment(name: 'Test using service and rules decl') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'hello', 'org.rioproject.examples:events:1.0'
    artifact id: 'client', 'org.rioproject.examples:events:dl:1.0'

    logging {
        logger 'org.rioproject.gnostic', Level.FINE
    }

    service(name: 'Hello') {
        interfaces {
            classes 'org.rioproject.examples.events.Hello'
            artifact ref: 'client'
        }

        implementation(class: 'org.rioproject.examples.events.service.HelloImpl') {
            artifact ref: 'hello'
        }

        comment: 'Hello World Event Producer Example'

        maintain 1

        /* Load configuration as a resource to get the Service UI */
        configuration file: 'classpath:EventConfig.groovy'

    }

    rules {
        rule{
            resource 'file:src/test/resources/CounterNotification'
            ruleClassPath 'org.rioproject.examples:events:dl:1.0'
            serviceFeed(name: "Hello") {
                watches "notification"
            }
        }
    }

}