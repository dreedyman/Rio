import org.rioproject.config.Constants
import java.util.logging.Level

deployment(name: 'Gnostic') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    artifact id: 'service', 'org.rioproject:gnostic:5.0-M2'
    artifact id: 'service-dl', 'org.rioproject:gnostic:dl:5.0-M2'

    logging {
        logger 'org.rioproject.gnostic', Level.FINE
    }

    service(name: 'Gnostic') {
        interfaces {
            classes 'org.rioproject.gnostic.Gnostic'
            artifact ref: 'service-dl'
        }
        implementation(class: 'org.rioproject.gnostic.GnosticImpl') {
            artifact ref: 'service'
        }
        maintain 1
    }
}