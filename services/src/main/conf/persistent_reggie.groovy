/*
 * Configuration for a Persistent Lookup Service
 */
import org.rioproject.config.Component

/*
 * Declare Persistent Lookup Service properties
 */
@Component('com.sun.jini.reggie')
class PersistentReggieConfig extends ReggieConfig {

    String getPersistenceDirectory() {
        String rioHome = System.getProperty('RIO_HOME')
        return rioHome+'/logs/reggie.log.dir'
    }

}

