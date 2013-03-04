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

deployment(name: 'Test Deploy') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    include 'org.rioproject:gnostic:5.0-M3'

    service(name: 'S1') {
        interfaces {
            classes 'org.rioproject.gnostic.test.TestService'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.gnostic.test.TestServiceImpl') {
             resources 'test-classes/'
        }

        sla(id:'load', low:10, high: 40) {
            policy type: 'notify', max:5
        }

        maintain 1
    }
}
