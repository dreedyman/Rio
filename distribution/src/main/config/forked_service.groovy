import org.rioproject.config.Component
import org.rioproject.log.LoggerConfig
import java.util.logging.Logger
import org.rioproject.log.ServiceLogEventHandler
/*
 * Configure the invocation delay duration for the JMXFaultDetectionHandler.
 * This is used (by default) for a forked service to monitor the presence of the
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

@Component('org.rioproject.cybernode')
class ForkedCybernodeConfig {
    LoggerConfig[] getLoggerConfigs() {
        Logger.getLogger("").addHandler new ServiceLogEventHandler()
        def loggers = []
        return loggers as LoggerConfig[]
    }
}
