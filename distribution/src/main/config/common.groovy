/*
 * Configuration for core system properties
 */

import net.jini.export.Exporter
import net.jini.jeri.BasicILFactory
import net.jini.jeri.BasicJeriExporter
import org.rioproject.bean.BeanInvocationLayerFactory
import org.rioproject.config.Component
import org.rioproject.config.ExporterConfig

/*
 * The exporter to declare as the *default* exporter for services and utilities
 */
@Component('org.rioproject')
class ExporterConfiguration {
    static  Exporter getDefaultExporter() {
        return new BasicJeriExporter(ExporterConfig.getServerEndpoint(), new BasicILFactory())
    }
}

/*
 * Configure the POJO Exporter
 */
@Component('org.rioproject.bean')
class POJOExporter extends ExporterConfiguration {
    Exporter getServerExporter() {
        return new BasicJeriExporter(ExporterConfig.getServerEndpoint(),
                                     new BeanInvocationLayerFactory(),
                                     false,
                                     true)
    }
}

/*
 * Configure the WatchDataSourceExporter
 */
@Component('org.rioproject.watch')
class WatchConfig extends ExporterConfiguration {
    Exporter getWatchDataSourceExporter() {
        return getDefaultExporter()
    }
}

/*
 * Default exporter to use for the ServiceDiscoveryManager is the same as the
 * exporter in the ExporterConfig class
 */
@Component('net.jini.lookup.ServiceDiscoveryManager')
class SDMConfig extends ExporterConfiguration {
    Exporter getEventListenerExporter() {
        return getDefaultExporter()
    }
}

/*
 * Test the liveness of multicast announcements from previously discovered
 * lookup services every 5 seconds
 */
@Component('net.jini.discovery.LookupDiscovery')
class LookupDiscoConfig {
    long getMulticastAnnouncementInterval() {
        return 5000
    }
}
