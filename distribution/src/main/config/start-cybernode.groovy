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
 * This configuration is used to start a Cybernode, including an embedded Webster
 */

import com.sun.jini.start.ServiceDescriptor
import org.rioproject.util.FileHelper
import org.rioproject.util.ServiceDescriptorUtil
import org.rioproject.config.Component
import org.rioproject.resolver.maven2.Repository

@Component('org.rioproject.start')
class StartCybernodeConfig {

    String[] getConfigArgs(String rioHome) {
        ServiceDescriptorUtil.checkForLoopback()
        File common = new File(rioHome + '/config/compiled/common')
        File cybernode = new File(rioHome + '/config/compiled/cybernode')
        File computeResource = new File(rioHome + '/config/compiled/compute_resource')

        def configArgs = []
        configArgs.addAll(FileHelper.getIfExists(common, rioHome + '/config/common.groovy'))
        configArgs.addAll(FileHelper.getIfExists(cybernode, rioHome + '/config/cybernode.groovy'))
        configArgs.addAll(FileHelper.getIfExists(computeResource, rioHome + '/config/compute_resource.groovy'))
        return configArgs as String[]
    }

    ServiceDescriptor[] getServiceDescriptors() {
        String m2Repo = Repository.getLocalRepository().absolutePath
        String rioHome = System.getProperty('RIO_HOME')
        def websterRoots = [rioHome + '/lib', ';',
                rioHome + '/lib-dl', ';',
                m2Repo]

        String policyFile = rioHome + '/policy/policy.all'

        def serviceDescriptors = [
                ServiceDescriptorUtil.getWebster(policyFile, '0', websterRoots as String[]),
                ServiceDescriptorUtil.getCybernode(policyFile, getConfigArgs(rioHome))
        ]
        return (ServiceDescriptor[]) serviceDescriptors
    }

}