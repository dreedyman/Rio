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
 * This configuration is used to start the Event Collector
 */

import com.sun.jini.start.ServiceDescriptor
import org.rioproject.config.Component
import org.rioproject.start.RioServiceDescriptor
import org.rioproject.util.RioHome
import org.rioproject.util.ServiceDescriptorUtil

@Component('org.rioproject.start')
class StartCollectorConfig {

    String[] getConfigArgs(String rioHome) {
        def configArgs = [rioHome+'/config/common.groovy', rioHome+'/config/collector.groovy']
        return configArgs as String[]
    }

    ServiceDescriptor[] getServiceDescriptors() {
        ServiceDescriptorUtil.checkForLoopback()
        String rioHome = RioHome.get()

        String policyFile = rioHome+'/policy/policy.all'

        StringBuilder pathBuilder = new StringBuilder()
        pathBuilder.append(rioHome).append(File.separator).append("lib").append(File.separator).append("event-collector-service.jar")

        def serviceDescriptors = [
                new RioServiceDescriptor("artifact:org.rioproject.event-collector/event-collector-proxy/5.8.0",
                                         policyFile,
                                         pathBuilder.toString(),
                                         "org.rioproject.eventcollector.service.EventCollectorImpl",
                                         getConfigArgs(rioHome))
        ]

        return serviceDescriptors as ServiceDescriptor[]
    }

}
