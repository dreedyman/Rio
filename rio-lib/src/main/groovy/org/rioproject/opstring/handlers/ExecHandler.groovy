/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.opstring.handlers

import org.rioproject.exec.ExecDescriptor
import org.rioproject.opstring.ParsedService

/**
 * Handles the parsing for the Exec element
 *
 * @author Jerome Bernard
 * @author Dennis Reedy
 */
class ExecHandler implements Handler {
    public parse(Object elem, Object options) {
        def ParsedService sDescriptor = options['serviceDescriptor']
        def exec = new ExecDescriptor()
        def inputArgs = new StringBuffer()
        //exec.noHup = elem.@nohup == "yes"
        elem.children().each {
            if (it.name() == "CommandLine") {
                exec.commandLine = it.text()
            }
            if (it.name() == "Error") {
                exec.stdErrFileName = it.text()
            }
            if (it.name() == "Output") {
                exec.stdOutFileName = it.text()
            }
            if (it.name() == "Environment") {
                exec.setEnvironment(parseEnvironment(it))
            }
            if (it.name() == "WorkingDirectory") {
                exec.workingDirectory = it.text()
            }
            if (it.name() == "InputArg") {
                if (inputArgs.length() > 0)
                    inputArgs.append(" ")
                inputArgs.append(it.text())
            }
            if(it.name() == "PidFile") {
                exec.pidFile = it.text()
            }
        }
        if (inputArgs.length() > 0)
            exec.inputArgs = inputArgs.toString()
        sDescriptor.execDescriptor = exec
        return exec
    }

    Map<String, String> parseEnvironment(element) {
        Map<String, String> env = new HashMap<String, String>()
        element.Property.each {
            def name = it.'@Name'
            def value = it.'@Value'
            env.put(name, value)
        }        
        return env
    }
}
