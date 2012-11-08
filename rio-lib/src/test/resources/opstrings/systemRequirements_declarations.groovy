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

deployment(name: 'systemRequirements declarations') {

    groups('blah')

    systemRequirements(id: "1") {
        software name: 'name1', version: '1.0', manufacturer: 'Acme', comment: 'Wily E Coyote'
    }

    systemRequirements(id: "3") {
        utilization id:'CPU', high: 0.95
        utilization id:'Memory', low: 0.1, high: 0.99

        memory available: '4g', capacity: '20g'
        diskspace available: '100g', capacity: '20t'
        
        software name: 'name3', version: '3.0', manufacturer: 'Acme', comment: 'Wily E Coyote'
        software name: 'name3.1', version: '3.1'

        operatingSystem name:'Mac OSX', version:'10.7*'

        processor available: 8

        platformRequirement type: "NativeLibrarySupport", name: 'libbrlcad.19'
    }

    ['1', '2', '3', '4'].each {s ->
        service(name: s) {

            interfaces {
                classes 'org.rioproject.test.simple.Simple'
                resources 'test-classes/'
            }

            implementation(class: 'org.rioproject.test.simple.SimpleImpl') {
                resources 'test-classes/'
            }

            if (s.equals('1')) {                
                systemRequirements ref: '1'
            }

            if (s.equals('2')) {
                systemRequirements {
                    software name: 'name2', version: '2.0', manufacturer: 'Acme', comment: 'Road Runner'
                }
            }

            if (s.equals('3')) {
                systemRequirements ref: '3'
            }

            maintain 1
        }
    }

}
