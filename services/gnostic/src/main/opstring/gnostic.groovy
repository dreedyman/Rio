import java.util.logging.Level

deployment(name: 'Gnostic') {
    groups System.getProperty("org.rioproject.groups", System.getProperty('user.name'))

    artifact id: 'service', 'org.rioproject.gnostic:gnostic-service:4.3-SNAPSHOT'
    artifact id: 'service-api', 'org.rioproject.gnostic:gnostic-api:4.3-SNAPSHOT'

    logging {
        logger 'org.rioproject.gnostic', Level.FINE
    }

    service(name: 'Gnostic') {
        interfaces {
            classes 'org.rioproject.gnostic.Gnostic'
            artifact ref: 'service-api'
        }
        implementation(class: 'org.rioproject.gnostic.GnosticImpl') {
            artifact ref: 'service'
        }
        maintain 1
    }
}
