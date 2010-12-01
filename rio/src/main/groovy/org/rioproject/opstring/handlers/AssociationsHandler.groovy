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

import org.rioproject.opstring.OpStringParser

/**
 * Handles the parsing for Associations elements
 *
 * @author Jerome Bernard
 */
class AssociationsHandler implements Handler {
    def OpStringParser parser

    def parse(element, options) {
        def opString = options['opstring']
        def global = options['global']
        def sDescriptor = options['serviceDescriptor']
        
        if (!((element.parent().name() == "ServiceBean" ||
                element.parent().name() == "SpringBean") && sDescriptor == null)) {
            element.Association.each {
                parser.parseElement(it, global, sDescriptor, opString)
            }
        }
    }
}
