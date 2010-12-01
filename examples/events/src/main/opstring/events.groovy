import java.util.logging.Level
import org.rioproject.config.Constants

deployment(name:'Events Example') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    /* The artifact the back end service requires to instantiate can
     * be done once and referenced using the identifier 'service' */
    artifact id: 'service', 'org.rioproject.examples.events:events-service:2.0'

    /* Declaring the artifact clients need to communicate with the service can
     * be done once and referenced using the identifier 'client'. Note the
     * classifier of 'dl' */
    artifact id: 'client', 'org.rioproject.examples.events:events-proxy:2.0'

    /*
     * The following declaration sets the logger level for the example
     */
    logging {
        logger 'org.rioproject.examples.events', Level.FINE
    }

    /* The event producer */
    service(name: 'Hello') {
        interfaces {
            classes 'org.rioproject.examples.events.Hello'
            artifact ref: 'client'
        }

        implementation(class: 'org.rioproject.examples.events.service.HelloImpl') {
            artifact ref: 'service'
        }

        comment: 'Hello World Event Producer Example'

        maintain 1

        /* Load configuration as a resource to get the Service UI */
        configuration file: 'classpath:EventConfig.groovy'

    }

    /* The event consumer */
    service(name: 'Hello Event Consumer') {
        implementation(class:'org.rioproject.examples.events.service.HelloEventConsumer') {
            artifact ref: 'service'
        }

        association name:'Hello', type:'uses', property:'eventProducer'

        maintain 1

        comment 'Hello World Event Consumer Example'
    }

}
