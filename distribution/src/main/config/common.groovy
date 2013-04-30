/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.jini.export.Exporter
import net.jini.jeri.BasicILFactory
import net.jini.jeri.BasicJeriExporter
import org.rioproject.bean.BeanInvocationLayerFactory
import org.rioproject.config.Component
import org.rioproject.config.ExporterConfig

/*
 * Configuration for core system properties
 */

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
