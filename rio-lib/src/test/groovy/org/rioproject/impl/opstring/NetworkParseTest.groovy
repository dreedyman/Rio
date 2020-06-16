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
package org.rioproject.impl.opstring

import org.rioproject.opstring.ServiceElement
/**
 * Test rule parsing
 */
class NetworkParseTest extends GroovyTestCase {
    def OpStringParser dslParser = new GroovyDSLOpStringParser()

    void testNetwork() {
        File file = new File("src/test/resources/opstrings/networkDecl.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 1 service", 1, elems.length

    }


}
