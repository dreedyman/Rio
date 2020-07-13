package opstring
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

deployment(name:'Hello World Spring Example') {
    codebase "file://${System.getProperty("user.dir")}/src/test/"
    
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    spring(name: 'Hello', config: 'hello-spring.xml') {
        interfaces {
            classes 'org.rioproject.cybernode.test.springbean.Hello'
            resources 'build/classes/java/test/'
        }
        implementation(class: 'org.rioproject.cybernode.test.springbean.service.HelloImpl') {
            resources 'build/classes/java/test/'
        }
        maintain 1
    }
}
