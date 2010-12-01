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

import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import org.rioproject.associations.AssociationDescriptor
import org.rioproject.associations.AssociationType
import org.rioproject.opstring.ParsedService

/**
 * Handles the parsing for Association elements
 * 
 * @author Jerome Bernard
 */
class AssociationHandler implements Handler {
    def static final Map<String, AssociationType> associationTypes = [
                'uses':         AssociationType.USES,
                'requires':     AssociationType.REQUIRES,
                'colocated':    AssociationType.COLOCATED,
                'opposed':      AssociationType.OPPOSED,
                'isolated':     AssociationType.ISOLATED
        ]
    /** A suitable Logger */
    def logger = Logger.getLogger("org.rioproject.opstring")

    def parse(assoc, options) {
        logger.fine "Parsing association '${assoc.'@Name'}'"
        def opString = options['opstring']
        def global = options['global']
        def ParsedService sDescriptor = options['serviceDescriptor']

        def opStringName
        def propertyName
        def className
        def matchOnName = null
        def proxyClass = null
        def proxyType = null
        def serviceSelectionStrategy = null
        def filter = null
        def inject = null
        def serviceDiscoveryTimeout = null
        def serviceDiscoveryTimeUnits = null

        [assoc.Management, assoc.management].flatten().each {
            proxyClass = it.'@Proxy'
            proxyType = it.'@ProxyType'
            serviceSelectionStrategy = it.'@Strategy'
            filter = it.'@Filter'
            inject = it.'@Inject'
            [it.get('Service-Discovery'), it.get('service-discovery')].flatten().each {
                serviceDiscoveryTimeout = it.'@timeout'
                serviceDiscoveryTimeUnits = it.'@units'
            }
        }

        def sType = assoc.'@Type'
        def name = assoc.'@Name'
        propertyName = assoc.'@Property'
        className = assoc.'@ClassName'
        if (className == null)
            className = assoc.'@Interface'
        opStringName = assoc.'@OperationalString'
        matchOnName = assoc.'@MatchOnName'

        if (opStringName == null && className == null)
            opStringName = opString.name

        AssociationType type
        if (associationTypes.containsKey(sType))
            type = associationTypes[sType]
        else
            throw new Exception("Unknown Association type: $sType")

        AssociationDescriptor association = new AssociationDescriptor(type, name, opStringName, propertyName)
        if (className)
            association.interfaceNames = [className] as String[]
        if (matchOnName)
            association.matchOnName = matchOnName == "yes"
        if (proxyClass)
            association.proxyClass = proxyClass
        if (proxyType)
            association.proxyType = proxyType
        if (serviceSelectionStrategy)
            association.serviceSelectionStrategy = serviceSelectionStrategy
        if (filter)
            association.associationMatchFilter = filter
        if (inject) {
            association.lazyInject = inject.equalsIgnoreCase("lazy")
        }
        if (serviceDiscoveryTimeout)
            association.serviceDiscoveryTimeout = Long.parseLong(serviceDiscoveryTimeout)
        if (serviceDiscoveryTimeUnits) {
            TimeUnit unit = TimeUnit.valueOf(serviceDiscoveryTimeUnits.toUpperCase())
            association.serviceDiscoveryTimeUnits = unit
        }
        logger.fine "Built association $association"
        if (sDescriptor) {
            sDescriptor.addAssociationDescriptor(association)
            if (type == AssociationType.REQUIRES)
                sDescriptor.autoAdvertise = "no"
        } else {
            global.addAssociationDescriptor(association)
            if (type == AssociationType.REQUIRES)
                global.autoAdvertise = "no"
        }
    }
}
