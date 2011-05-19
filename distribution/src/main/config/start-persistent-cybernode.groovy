/*
 * This configuration is used by the com.sun.jini.start utility to start a 
 * Cybernode, including an embedded Webster
 */
import org.rioproject.config.Component

@Component('com.sun.jini.start')
class StartPersistentCybernodeConfig extends StartCybernodeConfig {

    String[] getConfigArgs(String rioHome) {
        def configArgs = ["${rioHome}/config/common.groovy",
                          "${rioHome}/config/cybernode.groovy",
                          "${rioHome}/config/persistent_cybernode.groovy",
                          "${rioHome}/config/compute_resource.groovy"]
        return configArgs as String[]
    }
}