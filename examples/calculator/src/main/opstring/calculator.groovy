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
 * The deployment configuration for the Calculator example
 *
 * @author Dennis Reedy
 */
deployment(name:'Calculator') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))
    
    artifact id:'service', 'org.rioproject.examples.calculator:calculator-service:2.1'
    artifact id:'service-dl', 'org.rioproject.examples.calculator:calculator-api:2.1'

    service(name: 'Calculator') {
        interfaces {
            classes 'org.rioproject.examples.calculator.Calculator'
            artifact ref:'service-dl'
        }
        implementation(class:'org.rioproject.examples.calculator.service.CalculatorImpl') {
            artifact ref:'service'
        }
        associations {
            ['Add', 'Subtract', 'Multiply', 'Divide'].each { s ->
                association name:"$s", type:'requires', property:"${s.toLowerCase()}"
            }
        }
        maintain 1
    }

    ['Add', 'Subtract', 'Multiply', 'Divide'].each { s ->
        service(name: s) {
            interfaces {
                classes "org.rioproject.examples.calculator.$s"
                artifact ref:'service-dl'
            }
            implementation(class: "org.rioproject.examples.calculator.service.${s}Impl") {
                artifact ref:'service'
            }
            maintain 1
        }
    }
}

