/*
 * Configuration for a Persistent Provision Monitor
 */
import org.rioproject.config.Component

/*
 * Declare Persistent Provision Monitor properties
 */
@Component('org.rioproject.monitor')
class PersistentMonitorConfig extends MonitorConfig {

    String getLogDirectory() {
        String rioHome = System.getProperty('RIO_HOME')
        return rioHome+'/logs/monitor.log.dir'
    }
}

