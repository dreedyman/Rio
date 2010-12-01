/*
 * Configuration for a Cybernode
 */
import java.util.logging.Level
import org.rioproject.config.Component
import org.rioproject.log.LoggerConfig
import org.rioproject.fdh.FaultDetectionHandlerFactory
import org.rioproject.resources.client.JiniClient

import net.jini.core.discovery.LookupLocator
import net.jini.jeri.BasicILFactory
import net.jini.jeri.BasicJeriExporter
import net.jini.jeri.tcp.TcpServerEndpoint
import org.rioproject.boot.BootUtil
import net.jini.export.Exporter
import org.rioproject.core.ClassBundle
import org.rioproject.config.Constants
import java.util.logging.Logger
import org.rioproject.log.ServiceLogEventHandler
import org.rioproject.log.LoggerConfig.LogHandlerConfig
import java.util.logging.ConsoleHandler

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
        ['org.rioproject.cybernode' : Level.INFO,
            'org.rioproject.cybernode.loader' : Level.FINEST,
            'org.rioproject.config' : Level.INFO,
            'org.rioproject.resources.servicecore': Level.INFO,
            'net.jini.discovery.LookupDiscovery' : Level.OFF].each { name, level ->
            loggers << new LoggerConfig(name,
                                        level,
                                        new LogHandlerConfig(new ConsoleHandler()))
        }
        return loggers as LoggerConfig[]
    }

    ClassBundle getFaultDetectionHandler() {
        def fdh = org.rioproject.fdh.HeartbeatFaultDetectionHandler.class.name
        def fdhConf = ['-', fdh+'.heartbeatPeriod=10000', fdh+'.heartbeatGracePeriod=10000']
        return FaultDetectionHandlerFactory.getClassBundle(fdh, fdhConf)
    }
}

/*
 * The exporter to declare as the *default* exporter for services and utilities
 */
@Component('org.rioproject')
class ExporterConfig {

    Exporter getDefaultExporter() {
        String host = BootUtil.getHostAddressFromProperty("java.rmi.server.hostname")
        return new BasicJeriExporter(TcpServerEndpoint.getInstance(host, 0),
                                     new BasicILFactory())
    }
}

/*
 * Configure the watchDataSourceExporter
 */
@Component('org.rioproject.watch')
class WatchConfig extends ExporterConfig {
    Exporter getWatchDataSourceExporter() {
        return getDefaultExporter()
    }
}

/*
 * Default exporter to use for the ServiceDiscoveryManager is the same as the
 * exporter in the ExporterConfig class
 */
@Component('net.jini.lookup.ServiceDiscoveryManager')
class SDMConfig extends ExporterConfig {
    Exporter getEventListenerExporter() {
        return getDefaultExporter()
    }
}

/*
 * Test the liveness of  multicast announcements from previously discovered
 * lookup services every 5 seconds
 */
@Component('net.jini.discovery.LookupDiscovery')
class LookupDiscoConfig {
    long getMulticastAnnouncementInterval() {
        return 5000
    }
}