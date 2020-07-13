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
 * The deployment configuration for the Events example
 *
 * @author Dennis Reedy
 */
deployment(name:'Events Example') {
    codebase "file://${System.getProperty("user.dir")}/src/test/"
    
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
            System.getProperty('user.name'))
    
    /* The event producer */
    service(name: 'Hello') {
        interfaces {
            classes 'org.rioproject.cybernode.test.events.Hello'
            resources 'build/classes/java/test/'
        }

        implementation(class: 'org.rioproject.cybernode.test.events.service.HelloImpl') {
            resources 'build/classes/java/test/'
        }

        comment: 'Hello World Event Producer Example'

        maintain 1
    }

    /* The event consumer */
    service(name: 'Hello Event Consumer') {
        implementation(class:'org.rioproject.cybernode.test.events.service.HelloEventConsumer') {
            resources 'build/classes/java/test/'
        }

        association name:'Hello', type:'uses', property:'eventProducer'

        maintain 1

        comment 'Hello World Event Consumer Example'
    }

}