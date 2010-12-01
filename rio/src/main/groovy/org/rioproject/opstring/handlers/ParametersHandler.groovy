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

import java.util.logging.Logger

/**
 * Handles the parsing for the Parameters element
 *
 * @author Jerome Bernard
 */
class ParametersHandler implements Handler {
    /** A suitable Logger */
    def logger = Logger.getLogger("org.rioproject.opstring")

    public parse(Object element, Object options) {
        logger.fine "Parsing parameters $element"

        def parent = options['parent']
        def global = options['global']
        def sDescriptor = options['serviceDescriptor']

        /* Ensure that we don't process the Element if the Element's parent
         * is a ServiceBean and the ParsedService is null. This will happen if
         * the Element is declared as a child of the OperationalString
         * (global configuration) and overridden in the child Element */
        if (!((parent.name() == "ServiceBean" || parent.name() == "SpringBean") && sDescriptor == null)) {
            String includeGlobalDecl = element.'@IncludeGlobalDecl'
            boolean append = (includeGlobalDecl == "yes")
            Properties props = parseParameters(element)
            if (sDescriptor)
                sDescriptor.setParameters(props, append)
            else
                global.setParameters(props, append)
            return props
        } else {
            return parseParameters(element)
        }
    }

    def static parseParameters(element) {
        Properties props = new Properties()
        element.Parameter.each {
            def name = it.'@Name'
            def elementValue = it.text()
            def node = it.'@Value'
            if (elementValue && node)
                throw new Exception("Declare either a Value attribute "+
                                    "or a <Parameter> value, not both")
            def value
            if (node == null) {
                if (elementValue)
                    value = elementValue
                else
                    throw new Exception("You must declare a Value "+
                                        "attribute or a <Parameter> value")
            } else {
                value = node
            }
            props.put(name, value)
        }
        return props
    }
}
