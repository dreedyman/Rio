/*
 * Configuration for a Lookup Service
 */
import org.rioproject.config.Component
import org.rioproject.config.Constants
import net.jini.export.Exporter
import org.rioproject.net.HostUtil

@Component('com.sun.jini.reggie')
class ReggieConfig {
    //int initialUnicastDiscoveryPort = 10500

    String[] getInitialMemberGroups() {
        def groups = [System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                                         System.getProperty('user.name'))]
        return groups as String[]
    }

    String getUnicastDiscoveryHost() {
        return HostUtil.getHostAddressFromProperty("java.rmi.server.hostname")
    }

    Exporter getServerExporter() {
        return ExporterConfig.getDefaultExporter()
    }
}
