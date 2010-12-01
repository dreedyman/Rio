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

import org.rioproject.log.LoggerConfig
import org.rioproject.log.LoggerConfig.LogHandlerConfig
import org.rioproject.log.LoggerConfig.FormalArgument
import java.util.logging.Level

/**
 * Handles the parsing for Logging elements
 *
 * @author Jerome Bernard
 */
class LoggingHandler implements Handler {
    public parse(Object element, Object options) {
        def sDescriptor = options.serviceDescriptor
        def global = options.global
        def parent = options.parent

        /* Ensure that we dont process the Element if the Element's parent
         * is a ServiceBean and the ParsedService is null. This will happen if
         * the Element is declared as a child of the OperationalString
         * (global configuration) and overridden in the child Element */
        if(!((parent.name() == "ServiceBean" || parent.name() == "SpringBean") && sDescriptor == null)) {
            String includeGlobalDecl = element.'@IncludeGlobalDecl'
            boolean append = includeGlobalDecl == "yes"
            def logConfigs = parseLogging(element) as LoggerConfig[]
            if (sDescriptor)
                sDescriptor.setLogConfigs(logConfigs, append)
            else
                global.setLogConfigs(logConfigs, append)
        }
    }

    /*
     * Parse the Logging element
     * @param element The Logging Element
     */
    def parseLogging(element) throws Exception {
        return element.Logger.collect {
            def name = it.'@Name'
            def level = Level.parse(it.'@Level')
            def useParent = Boolean.valueOf(it.'@UseParent')
            def rBundle = it.'@ResourceBundle'
            def handlerList = it.Handler.collect {
                def className = it.'@ClassName'
                def handlerLevel = Level.parse(it.'@Level')
                if(handlerLevel.intValue()==0)
                    handlerLevel = level;
                def params = it.Parameters.collect {
                    it.Parameter.collect {
                        return new FormalArgument(it.'@Name', it.'@Value')
                    }
                }.flatten()
                def formatter = element.Formatter[0]
                return new LogHandlerConfig(className, handlerLevel, params, formatter)
            }
            return new LoggerConfig(name, level, useParent, rBundle, handlerList as LogHandlerConfig[])
        }
    }
}
