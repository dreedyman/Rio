/*
 * Configuration for Mercury
 */
import org.rioproject.config.Component
import org.rioproject.config.Constants
import net.jini.export.Exporter
import org.rioproject.net.HostUtil
import net.jini.core.discovery.LookupLocator
import org.rioproject.resources.client.JiniClient

@Component('org.rioproject.eventcollector.service')
class EventCollectorConfig {

    String serviceComment = 'Collects system events'
    String jmxName = 'org.rioproject.eventcollector:type=Event Collector'

    String[] getInitialLookupGroups() {
        def groups = [System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                                         System.getProperty('user.name'))]
        return groups as String[]
    }

    LookupLocator[] getInitialLookupLocators() {
        String locators = System.getProperty(Constants.LOCATOR_PROPERTY_NAME)
        if(locators!=null) {
            def lookupLocators = JiniClient.parseLocators(locators)
            return lookupLocators as LookupLocator[]
        } else {
            return null
        }
    }
}
