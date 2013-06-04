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
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    /* The artifact the back end service requires to instantiate can
     * be done once and referenced using the identifier 'service' */
    artifact id: 'service', 'org.rioproject.examples.events:events-service:2.1'

    /* Declaring the artifact clients need to communicate with the service can
     * be done once and referenced using the identifier 'client'. Note the
     * classifier of 'dl' */
    artifact id: 'client', 'org.rioproject.examples.events:events-proxy:2.1'

    /* The event producer */
    service(name: 'Hello') {
        interfaces {
            classes 'org.rioproject.examples.events.Hello'
            artifact ref: 'client'
        }

        implementation(class: 'org.rioproject.examples.events.service.HelloImpl') {
            artifact ref: 'service'
        }

        comment: 'Hello World Event Producer Example'

        maintain 1

        /* Load configuration as a resource to get the Service UI */
        configuration file: 'classpath:EventConfig.groovy'

    }

    /* The event consumer */
    service(name: 'Hello Event Consumer') {
        implementation(class:'org.rioproject.examples.events.service.HelloEventConsumer') {
            artifact ref: 'service'
        }

        association name:'Hello', type:'uses', property:'eventProducer'

        maintain 1

        comment 'Hello World Event Consumer Example'
    }

}
