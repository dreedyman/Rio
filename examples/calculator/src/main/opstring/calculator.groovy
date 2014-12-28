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

def artifactTable = ["api":"org.rioproject.examples.calculator:calculator-api:2.2",
                     "service":"org.rioproject.examples.calculator:calculator-service:2.2"]
/**
 * The deployment configuration for the Calculator example
 *
 * @author Dennis Reedy
 */
deployment(name:'Calculator') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    service(name: 'Calculator') {
        interfaces {
            classes 'org.rioproject.examples.calculator.Calculator'
            artifact artifactTable.api
        }
        implementation(class:'org.rioproject.examples.calculator.service.CalculatorImpl') {
            artifact artifactTable.service
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
                artifact artifactTable.api
            }
            implementation(class: "org.rioproject.examples.calculator.service.${s}Impl") {
                artifact artifactTable.service
            }
            maintain 1
        }
    }
}

