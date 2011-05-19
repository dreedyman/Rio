/*
 * Configuration for a Persistent Cybernode
 */
import org.rioproject.config.Component
import org.rioproject.cybernode.PersistentServiceStatementManager
import org.rioproject.core.provision.ServiceStatementManager
import net.jini.config.Configuration

/*
 * Declare Persistent Cybernode properties
 */
@Component('org.rioproject.cybernode')
class PersistentCybernodeConfig extends CybernodeConfig {

    String getLogDirectory() {
        String rioHome = System.getProperty('RIO_HOME')
        return rioHome+'/logs/cybernode.log.dir'
    }

    ServiceStatementManager getServiceStatementManager(Configuration config) {
        return new PersistentServiceStatementManager(config)
    }
}

