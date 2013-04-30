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
 * This configuration is used to start a service that will exec a single service bean
 */

import org.rioproject.start.RioServiceDescriptor
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.util.FileHelper
import org.rioproject.util.ServiceDescriptorUtil

@Component('org.rioproject.start')
class StartServiceBeanExecConfig {

    String[] getConfigArgs(String rioHome) {
        ServiceDescriptorUtil.checkForLoopback()
        File common = new File(rioHome + '/config/compiled/common')
        File cybernode = new File(rioHome + '/config/compiled/cybernode')
        File computeResource = new File(rioHome + '/config/compiled/compute_resource')

        def configArgs = []
        configArgs.addAll(FileHelper.getIfExists(common, rioHome + '/config/common.groovy'))
        configArgs.addAll(FileHelper.getIfExists(cybernode, rioHome + '/config/forked_service.groovy'))
        configArgs.addAll(FileHelper.getIfExists(computeResource, rioHome + '/config/compute_resource.groovy'))
        return configArgs as String[]
    }

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')
        String codebase = ServiceDescriptorUtil.getCybernodeCodebase()
        String classpath = ServiceDescriptorUtil.getCybernodeClasspath()
        
        String policyFile = rioHome + '/policy/policy.all'
        def configArgs = getConfigArgs(rioHome)

        def serviceDescriptors = [
            new RioServiceDescriptor(codebase,
                                     policyFile,
                                     classpath,
                                     'org.rioproject.cybernode.ServiceBeanExecutorImpl',
                                     (String[]) configArgs)
        ]

        return (ServiceDescriptor[]) serviceDescriptors
    }
}
