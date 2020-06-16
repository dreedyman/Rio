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
import org.rioproject.associations.AssociationDescriptor
import org.rioproject.opstring.ServiceElement
/**
 * Test changing the name of an opstring and make sure all embedded opstring
 * name refs are changed
 */
class OpStringNameChangeTest extends GroovyTestCase {

    def OpStringParser dslParser = new GroovyDSLOpStringParser()

    void testEmptyResources() {
        File file = new File("src/test/resources/opstrings/calculator.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        String opStringName = opstring.getName()
        String newOpStringName = "${opStringName}-1"
        opstring.setName(newOpStringName)
        ServiceElement[] elems = opstring.getServices()
        for(ServiceElement elem : elems) {
            assertEquals elem.getServiceBeanConfig().operationalStringName, newOpStringName
            if(elem.associationDescriptors.length>0) {
                for(AssociationDescriptor ad : elem.associationDescriptors) {
                    assertEquals ad.operationalStringName, newOpStringName
                }
            }
        }
    }
}
