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

/**
 * The deployment configuration for the Calculator example
 *
 * @author Dennis Reedy
 */

deployment(name:'Calculator') {

    codebase "file://${System.getProperty("user.dir")}/src/test/"

    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    service(name: 'Calculator') {
        interfaces {
            classes 'org.rioproject.cybernode.test.calculator.Calculator'
            resources 'build/classes/java/test/'
        }
        implementation(class:'org.rioproject.cybernode.test.calculator.service.CalculatorImpl') {
            resources 'build/classes/java/test/'
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
                classes "org.rioproject.cybernode.test.calculator.$s"
                resources 'build/classes/java/test/'
            }
            implementation(class: "org.rioproject.cybernode.test.calculator.service.${s}Impl") {
                resources 'build/classes/java/test/'
            }
            maintain 1
        }
    }
}

