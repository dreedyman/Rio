/*
 * Configuration for core system properties
 */

import org.rioproject.config.Component

import net.jini.jeri.BasicILFactory
import net.jini.jeri.BasicJeriExporter
import net.jini.jeri.tcp.TcpServerEndpoint
import net.jini.export.Exporter
import org.rioproject.net.PortRangeServerSocketFactory
import javax.net.ServerSocketFactory
import org.rioproject.bean.BeanInvocationLayerFactory
import net.jini.jeri.ServerEndpoint
import org.rioproject.config.Constants
import org.rioproject.net.HostUtil

/*
 * The exporter to declare as the *default* exporter for services and utilities
 */
@Component('org.rioproject')
class ExporterConfig {
    static  Exporter getDefaultExporter() {
        return new BasicJeriExporter(getServerEndpoint(), new BasicILFactory())
    }

    static ServerEndpoint getServerEndpoint() {
        String host = HostUtil.getHostAddressFromProperty("java.rmi.server.hostname")
        String range = System.getProperty(Constants.PORT_RANGE)
        ServerSocketFactory factory = null
        if(range!=null) {
            String[] parts = range.split("-")
            int start = Integer.parseInt(parts[0])
            int end = Integer.parseInt(parts[1])
            factory = new PortRangeServerSocketFactory(start, end)
        }
        return TcpServerEndpoint.getInstance(host, 0, null, factory)
    }
}

/*
 * Configure the POJO Exporter
 */
@Component('org.rioproject.bean')
class POJOExporter extends ExporterConfig {
    Exporter getServerExporter() {
        return new BasicJeriExporter(getServerEndpoint(),
                                     new BeanInvocationLayerFactory(),
                                     false,
                                     true)
    }
}

/*
 * Configure the WatchDataSourceExporter
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
 * Test the liveness of multicast announcements from previously discovered
 * lookup services every 5 seconds
 */
@Component('net.jini.discovery.LookupDiscovery')
class LookupDiscoConfig {
    long getMulticastAnnouncementInterval() {
        return 5000
    }
}
