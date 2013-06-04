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

/**
 * The deployment configuration for the Spring example
 *
 * @author Dennis Reedy
 */
deployment(name:'Hello World Spring Example') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    spring(name: 'Hello', config: 'hello-spring.xml') {
        interfaces {
            classes 'org.rioproject.examples.springbean.Hello'
            artifact 'org.rioproject.examples.springbean:springbean-api:2.1'
        }
        implementation(class: 'org.rioproject.examples.springbean.service.HelloImpl') {
            artifact 'org.rioproject.examples.springbean:springbean-service:2.1'
        }
        maintain 1
    }
}
