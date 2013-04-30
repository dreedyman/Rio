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

/*
 * Configuration for a Lookup Service
 */
import org.rioproject.config.Component
import org.rioproject.config.Constants
import net.jini.export.Exporter
import org.rioproject.net.HostUtil

@Component('com.sun.jini.reggie')
class ReggieConfig {
    //int initialUnicastDiscoveryPort = 10500

    String[] getInitialMemberGroups() {
        def groups = [System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                                         System.getProperty('user.name'))]
        return groups as String[]
    }

    String getUnicastDiscoveryHost() {
        return HostUtil.getHostAddressFromProperty("java.rmi.server.hostname")
    }

    Exporter getServerExporter() {
        return ExporterConfiguration.getDefaultExporter()
    }
}
