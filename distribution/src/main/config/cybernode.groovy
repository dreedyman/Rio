/*
 * Configuration for a Cybernode
 */
import java.util.logging.Level
import org.rioproject.config.Component
import org.rioproject.log.LoggerConfig
import org.rioproject.fdh.FaultDetectionHandlerFactory
import org.rioproject.resources.client.JiniClient

import net.jini.core.discovery.LookupLocator
import org.rioproject.opstring.ClassBundle
import org.rioproject.config.Constants
import java.util.logging.Logger
import org.rioproject.log.ServiceLogEventHandler

/*
 * Declare Cybernode properties
 */
@Component('org.rioproject.cybernode')
class CybernodeConfig {
    String serviceName = 'Cybernode'
    String serviceComment = 'Dynamic Agent'
    String jmxName = 'org.rioproject.cybernode:type=Cybernode'
    boolean provisionEnabled = true
    //long provisionerLeaseDuration = 1000*60

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

    String getServiceLogRootDirectory() {
        String logExt = System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                                           System.getProperty('user.name'))
        String logDir = '/tmp/logs/rio/'
        String opSys = System.getProperty('os.name')
        if(opSys.startsWith("Windows"))
            logDir = '${java.io.tmpdir}/logs/rio/'
        return logDir+logExt
    }

    String getNativeLibDirectory() {
        return System.getProperty("RIO_NATIVE_DIR")
    }

    LoggerConfig[] getLoggerConfigs() {
        Logger.getLogger("").addHandler new ServiceLogEventHandler()
        def loggers = []
        return loggers as LoggerConfig[]
    }

    ClassBundle getFaultDetectionHandler() {
        def fdh = org.rioproject.fdh.HeartbeatFaultDetectionHandler.class.name
        def fdhConf = ['-', fdh+'.heartbeatPeriod=10000', fdh+'.heartbeatGracePeriod=10000']
        return FaultDetectionHandlerFactory.getClassBundle(fdh, fdhConf)
    }
}


/*
 * Configure the invocation delay duration for the JMXFaultDetectionHandler.
 * This is used (by default) for forked service to monitor the presence of the
 * Cybernode that created it. If the forked service detects that it's parent
 * Cybernode has orphaned it, it will terminate.
 */
@Component('org.rioproject.fdh.JMXFaultDetectionHandler')
class JMXFaultDetectionHandlerConfig {
    /*
     * Set the invocation delay (in milliseconds) to be 5 seconds. Default is 60 seconds.
     */
    long invocationDelay = 5000;

    /*
     * Set the number of times to retry connecting to the service. If the service cannot
     * be reached within the retry count specified the service will be determined to be
     * unreachable. Default is 3
     */
    //int retryCount = 3

    /*
     * Set the amount of time to wait between retries (in milliseconds).
     * Set how long to wait between retries (in milliseconds). This value
     * will be used between retry attempts, waiting the specified amount of
     * time to retry. Default is 1 second (1000 milliseconds)
     */
    //long retryTimeout = 1000
}
