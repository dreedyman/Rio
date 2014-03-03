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
 * The deployment configuration for the Workflow example that includes transactional support using Mahalo
 *
 * @author Dennis Reedy
 */
deployment(name: 'Workflow Example') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    /* The implementation and client artifacts are defined globally and referenced
     * in each service bean declaration */
    artifact id: 'service-impl',   'org.rioproject.examples.workflow:workflow-service:2.1'
    artifact id: 'service-dl',     'org.rioproject.examples.workflow:workflow-api:2.1'
    artifact id: 'outrigger-dl',   'org.apache.river:outrigger-dl:2.2.2'
    artifact id: 'outrigger-impl', 'org.apache.river:outrigger:2.2.2'
    artifact id: 'mahalo-dl',      'org.apache.river:mahalo-dl:2.2.2'
    artifact id: 'mahalo-impl',    'org.apache.river:mahalo:2.2.2'

    ['New Worker'    : 'NEW',
     'Pending Worker': 'PENDING',
     'Open Worker'   : 'OPEN',
     'Closed Worker' : 'CLOSED'].each {name, state ->

        service(name: name) {
            implementation(class: "org.rioproject.examples.workflow.Worker") {
                artifact ref: 'service-impl'
            }
            
            parameters {
                parameter name: "template", value: state
            }

            association(type: "requires",
                        serviceType: "net.jini.space.JavaSpace05",
                        property: "javaSpace", name: "Workflow Space")

            association(type: "requires",
                        serviceType: "net.jini.core.transaction.server.TransactionManager",
                        property: "transactionManager", name: "Mahalo")

            maintain 1
        }
    }

    service(name: 'Master') {
        interfaces {
            classes 'org.rioproject.examples.workflow.Master'
            artifact ref: 'service-dl'
        }

        implementation(class: "org.rioproject.examples.workflow.MasterImpl") {
            artifact ref: 'service-impl'
        }

        association(type: "requires",
                    serviceType: "net.jini.space.JavaSpace05",
                    property: "javaSpace", name: "Workflow Space")

        maintain 1
    }

    service(name: 'Workflow Space') {
        interfaces {
            classes 'net.jini.space.JavaSpace05'
            artifact ref: 'outrigger-dl'
        }

        implementation(class: 'com.sun.jini.outrigger.TransientOutriggerImpl') {
            artifact ref: 'outrigger-impl'
        }

        maintain 1

    }

    service(name:'Mahalo') {
        interfaces {
            classes 'net.jini.core.transaction.server.TransactionManager'
            artifact ref: 'mahalo-dl'
        }
        implementation(class: 'com.sun.jini.mahalo.TransientMahaloImpl') {
            artifact ref: 'mahalo-impl'
        }
        maintain 1
        maxPerMachine 1
    }

}

