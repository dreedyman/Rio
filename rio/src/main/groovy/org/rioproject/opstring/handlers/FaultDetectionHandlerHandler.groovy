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

import org.rioproject.core.ClassBundle
import org.rioproject.opstring.OpStringParser

/**
 * Handles the parsing for FaultDetectionHandler elements
 *
 * @author Jerome Bernard
 */
class FaultDetectionHandlerHandler implements Handler {
    def OpStringParser parser

    public parse(Object element, Object options) {
        def opString = options['opstring']
        def global = options['global']
        def sDescriptor = options['serviceDescriptor']

        /* Ensure that we dont process the Element if the Element's parent
         * is a ServiceBean and the ParsedService is null. This will happen if
         * the Element is declared as a child of the OperationalString
         * (global configuration) and overridden in the child Element */
        if (!((element.parent().name() == "ServiceBean" ||
              element.parent().name() == "SpringBean") && sDescriptor == null)) {
            String codebase = sDescriptor == null ? global.codebase : sDescriptor.codebase
            ClassBundle fdhBundle = parser.parseFDH(element, codebase, global, sDescriptor, opString)
            if (sDescriptor)
                sDescriptor.faultDetectionHandlerBundle = fdhBundle
            else
                global.faultDetectionHandlerBundle = fdhBundle
        }
    }
}
