/*
 * Configuration for a Lookup Service
 */
import org.rioproject.boot.BootUtil 
import org.rioproject.config.Component
import org.rioproject.config.Constants
import net.jini.discovery.DiscoveryGroupManagement
import net.jini.export.Exporter

@Component('com.sun.jini.reggie')
class ReggieConfig {
    int initialUnicastDiscoveryPort = 10500

    String[] getInitialMemberGroups() {
        /*def groups = [System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                                         System.getProperty('user.name'))]*/
        def groups = DiscoveryGroupManagement.NO_GROUPS
        return groups as String[]
    }

    String getUnicastDiscoveryHost() {
        return BootUtil.getHostAddress()
    }

    Exporter getServerExporter() {
        return ExporterConfig.getDefaultExporter()
    }
}
