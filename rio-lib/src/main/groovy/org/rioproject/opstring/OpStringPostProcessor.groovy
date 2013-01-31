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
package org.rioproject.opstring

import org.rioproject.associations.AssociationDescriptor
import org.rioproject.resources.servicecore.Service

/**
 * 
 * @author Dennis Reedy
 */
class OpStringPostProcessor {
    private static OpStringParserHelper helper = new OpStringParserHelper()

    def static process(def opStrings) {
        opStrings.each { opString ->
            /* Check that services have a component bundle */
            checkForComponentBundles(opString)

            /* Check for services that do not have any declared interfaces. If they do, add
             * default interface*/
            processServiceInterfaceDeclarations(opString)

            /* Fix up and validate associations as needed */
            processAssociations(opString)
        }
    }

    def static checkForComponentBundles(OpString opString) {
        for (ServiceElement service : opString.services) {
            if(service.componentBundle == null)
                throw new DSLException("The ${service.name} does not have a declared implementation")
        }
    }

    def static processAssociations(OpString opString) {
        for (ServiceElement service : opString.services) {
            for(AssociationDescriptor associationDescriptor : service.associationDescriptors) {
                if(associationDescriptor.interfaceNames.length==0) {
                    String[] interfaceNames = helper.getServiceInterfaceNames(associationDescriptor.name, opString)
                    if(interfaceNames) {
                        associationDescriptor.interfaceNames = interfaceNames
                    }
                }

                def associatedName = associationDescriptor.name
                def svcName = service.name
                if (associatedName.equals(svcName))
                    throw new IllegalArgumentException("Invalid AssociationDescriptor : A service cannot have an association to itself")

                /* Check to see if an association has been declared with no opstring name, that an interface
                 * is declared. This will allow service discovery to occur. */
                String assocOpStringName = associationDescriptor.operationalStringName
                if (assocOpStringName == null) {
                    if (associationDescriptor.interfaceNames.length == 0)
                        throw new IllegalArgumentException("Invalid AssociationDescriptor : Unknown service interface")
                }

                /*
                 * The Association has declared an interface className, no need to find matching ServiceElement in an
                 * opstring
                 */
                if (associationDescriptor.interfaceNames.length > 0) {
                    //TODO: FIX HOW FDH IS HANDLED
                    /*if (associationDescriptor.faultDetectionHandlerBundle == null)
                        associationDescriptor.faultDetectionHandlerBundle = OpStringLoader.getDefaultFDH()
                    */
                    associationDescriptor.groups = service.serviceBeanConfig.groups
                    associationDescriptor.locators = service.serviceBeanConfig.locators
                    continue
                }

                if (assocOpStringName && !(assocOpStringName == opString.name)) {
                    if (opString.containsOperationalString(assocOpStringName)) {
                        OpString op = opString.getNestedOperationalString(assocOpStringName) as OpString
                        def sElem1 = op.getNamedService(associatedName)
                        if (sElem1 == null)
                            throw new IllegalArgumentException("Associated service [$associatedName] not in [$assocOpStringName] OperationalString")
                        setAssociationDescriptorAttributes(sElem1, associationDescriptor)
                    } else {
                        throw new IllegalArgumentException("OperationalString [$assocOpStringName] not included in [${opString.name}] OperationalString")
                    }
                } else {
                    def sElem1 = opString.getNamedService(associatedName)
                    if (sElem1 == null)
                        throw new IllegalArgumentException("Associated service [$associatedName] not in [$assocOpStringName] OperationalString")
                    setAssociationDescriptorAttributes(sElem1, associationDescriptor)
                }
            }
        }
    }

    def static processServiceInterfaceDeclarations(opString) {
        for(ServiceElement service : opString.services) {
            if(service.exportBundles.length==0) {
                ClassBundle exportBundle = new ClassBundle(Service.class.name)
                service.exportBundles = [exportBundle]
            }
        }
    }

    /**
     * Set the attributes for the AssociationDescriptor
     *
     * @param sElem The ServiceElement of the associated service
     * @param aDesc The AssociationDescriptor to apply attributes to
     */
    def static setAssociationDescriptorAttributes(ServiceElement sElem, AssociationDescriptor aDesc) {
        aDesc.interfaceNames = sElem.exportBundles.collect { it.className } as String[]
        aDesc.faultDetectionHandlerBundle = sElem.faultDetectionHandlerBundle
        aDesc.groups = sElem.serviceBeanConfig.groups
        aDesc.locators = sElem.serviceBeanConfig.locators
        aDesc.matchOnName = sElem.matchOnName
    }

}
