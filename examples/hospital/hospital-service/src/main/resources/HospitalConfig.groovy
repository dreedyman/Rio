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
import org.rioproject.config.Component
import org.rioproject.entry.UIDescriptorFactory
import org.rioproject.resolver.ResolverHelper
import org.rioproject.resolver.Resolver
import net.jini.core.entry.Entry

@Component('org.rioproject.examples.hospital.service')
class HospitalConfig {

    Entry[] getServiceUIs(String codebase) {
        def entry = []
        if(codebase!=null) {
            Resolver r = ResolverHelper.getInstance()
            String uiClass = 'org.rioproject.examples.hospital.ui.HospitalIntro'
            def classpath = []
            for(String s : r.getClassPathFor("org.rioproject.examples.hospital:hospital-ui:2.0",
                                             (File)null,
                                             true)) {
                if(s.startsWith(ResolverHelper.M2_HOME))
                    s = s.substring(ResolverHelper.M2_HOME.length()+1)
                classpath << s
            }
            entry = [UIDescriptorFactory.getJComponentDesc(codebase,
                                                           classpath as String[],
                                                           uiClass)]
        }
        return entry as Entry[]
    }
}