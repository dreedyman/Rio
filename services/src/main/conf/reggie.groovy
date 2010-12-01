/*
 * Configuration for a Lookup Service
 */
import org.rioproject.boot.BootUtil 
import org.rioproject.config.Component
import org.rioproject.config.Constants

@Component('com.sun.jini.reggie')
class ReggieConfig {

    String[] getInitialMemberGroups() {
        def groups = [System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                                         System.getProperty('user.name'))]
        return groups as String[]
    }

    String getUnicastDiscoveryHost() {
        return BootUtil.getHostAddress()
    }

}
