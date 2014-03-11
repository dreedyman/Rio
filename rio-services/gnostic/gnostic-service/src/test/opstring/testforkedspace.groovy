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

import org.rioproject.config.Constants
import org.rioproject.system.SystemWatchID
import java.util.logging.Level

deployment(name: 'Test Forked Space') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'outrigger-dl', "org.apache.river:outrigger-dl:2.2.2"
    artifact id: 'outrigger-impl', "org.apache.river:outrigger:2.2.2"

    service(name: 'Space', fork:'yes') {
        interfaces {
            classes 'net.jini.space.JavaSpace05'
            artifact ref: 'outrigger-dl'
        }

        implementation(class: 'com.sun.jini.outrigger.TransientOutriggerImpl') {
            artifact ref: 'outrigger-impl'
        }

        maintain 1
    }

    rules {
        rule{
            resource 'file:'+System.getProperty('user.dir')+'/src/test/resources/SpaceUtilization'
            serviceFeed(name: "Space") {
                watches "${SystemWatchID.PROC_CPU}, ${SystemWatchID.JVM_MEMORY}"
            }
        }
    }
}
