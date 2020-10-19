/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

/*
 * Configuration for a Provision Monitor
 */
import net.jini.core.discovery.LookupLocator
import net.jini.export.Exporter
import net.jini.jrmp.JrmpExporter
import net.jini.security.BasicProxyPreparer
import net.jini.core.constraint.InvocationConstraints
import net.jini.constraint.BasicMethodConstraints
import net.jini.core.constraint.ConnectionRelativeTime
import net.jini.security.ProxyPreparer
import net.jini.core.constraint.MethodConstraints

import org.rioproject.config.Component
import org.rioproject.config.Constants
import org.rioproject.impl.client.JiniClient
import org.rioproject.monitor.service.selectors.RoundRobinSelector
import org.rioproject.monitor.service.selectors.ServiceResourceSelector
import org.rioproject.resolver.RemoteRepository
import org.rioproject.util.PortUtil

/*
* Declare Provision Monitor properties
*/
@Component('org.rioproject.monitor')
class MonitorConfig {
    String serviceName = 'Provision Monitor'
    String serviceComment = 'Dynamic Provisioning Agent'
    String jmxName = 'org.rioproject.monitor:type=Monitor'

    String[] getInitialLookupGroups() {
        String groups = System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))
        groups.split(",")
    }

    RemoteRepository[] getRemoteRepositories() {
        RemoteRepository remoteRepository = new RemoteRepository()
        remoteRepository.setId(serviceName.replaceAll(" ", ""))
        remoteRepository.setUrl(System.getProperty(Constants.CODESERVER))
        def repositories = [remoteRepository]
        repositories as RemoteRepository[]
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
        new RoundRobinSelector() as ServiceResourceSelector
    }

    /*
     * Use a JrmpExporter for the OpStringManager.
     */
    Exporter getOpStringManagerExporter() {
        int port = 0
        String portRange = System.getProperty(Constants.PORT_RANGE)
        if(portRange!=null)
            port = PortUtil.getPortFromRange(portRange)
        new JrmpExporter(port) as Exporter
    }
    
    ProxyPreparer getInstantiatorPreparer() {
        MethodConstraints serviceListenerConstraints =
                new BasicMethodConstraints(new InvocationConstraints(new ConnectionRelativeTime(30000),
                                                                     null))
        new BasicProxyPreparer(false, serviceListenerConstraints, null) as ProxyPreparer
    }
}
