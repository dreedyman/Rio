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
 * This configuration is used to start Webster
 */

import com.sun.jini.start.ServiceDescriptor

import org.rioproject.config.Component
import org.rioproject.resolver.maven2.Repository
import org.rioproject.start.util.ServiceDescriptorUtil
import org.rioproject.util.RioHome

@SuppressWarnings("unused")
@Component('org.rioproject.start')
class StartWebsterConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String m2Repo = Repository.getLocalRepository().absolutePath
        String rioHome = RioHome.get()
        def websterRoots = [rioHome + '/lib-dl', ';',
                            rioHome + '/lib',    ';',
                            m2Repo]

        String policyFile = rioHome + '/policy/policy.all'
        def options = ["-join"]
        ServiceDescriptorUtil.getWebster(policyFile,
                '0',
                websterRoots as String[],
                ["-join"] as String[],
                true) as ServiceDescriptor[]
    }

}