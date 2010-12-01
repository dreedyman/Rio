/*
 * Configuration for a Provision Monitor
 */
import java.util.logging.Level

import net.jini.core.discovery.LookupLocator
import net.jini.export.Exporter
import net.jini.jrmp.JrmpExporter

import org.rioproject.config.Component
import org.rioproject.config.Constants
import org.rioproject.core.ClassBundle
import org.rioproject.log.LoggerConfig
import org.rioproject.monitor.LeastActiveSelector
import org.rioproject.monitor.ServiceResourceSelector
import org.rioproject.fdh.FaultDetectionHandlerFactory
import org.rioproject.resources.client.JiniClient
import java.util.logging.ConsoleHandler
import org.rioproject.log.LoggerConfig.LogHandlerConfig
import net.jini.security.BasicProxyPreparer
import net.jini.core.constraint.InvocationConstraint
import net.jini.core.constraint.InvocationConstraints
import net.jini.constraint.BasicMethodConstraints
import net.jini.core.constraint.ConnectionRelativeTime
import net.jini.security.ProxyPreparer
import net.jini.core.constraint.MethodConstraints

/*
 * Declare Provision Monitor properties
 */
@Component('org.rioproject.monitor')
class MonitorConfig {
    String serviceName = 'Monitor'
    String serviceComment = 'Dynamic Provisioning Agent'
    String jmxName = 'org.rioproject.monitor:type=Monitor'

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

    ServiceResourceSelector getServiceResourceSelector() {
        return new LeastActiveSelector()
    }

    /*
     * Use a JrmpExporter for the OpStringManager.
     */
    Exporter getOpStringManagerExporter() {
        return new JrmpExporter()
    }
    
    ProxyPreparer getInstantiatorPreparer() {
        MethodConstraints serviceListenerConstraints =
                new BasicMethodConstraints(new InvocationConstraints(new ConnectionRelativeTime(30000),
                                                                     null))
        return  new BasicProxyPreparer(false, serviceListenerConstraints, null);        
    }

    LoggerConfig[] getLoggerConfigs() {
        def loggers = []
        ['org.rioproject.monitor' : Level.FINER,
         'org.rioproject.monitor.sbi' : Level.FINE,
         'org.rioproject.monitor.feedback' : Level.OFF,
         'org.rioproject.monitor.provision': Level.FINER,
         'org.rioproject.monitor.selector' : Level.OFF,
         'org.rioproject.monitor.services' : Level.FINEST,

        /*
         'org.rioproject.monitor.peer' : Level.FINE,
         */
         'net.jini.lookup.JoinManager' : Level.OFF].each { name, level ->
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
