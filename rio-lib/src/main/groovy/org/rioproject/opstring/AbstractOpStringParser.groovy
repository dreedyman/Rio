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
package org.rioproject.opstring

import com.sun.jini.config.ConfigUtil

import org.rioproject.associations.AssociationDescriptor
import org.rioproject.core.provision.SystemRequirements

/**
 * Provides an abstract implementation of the OpStringParser
 *
 * @author Jerome Bernard
 * @author Dennis Reedy
 */
abstract class AbstractOpStringParser implements OpStringParser {

   protected def merge(SystemRequirements base, SystemRequirements sysRequirements) {
        sysRequirements.addSystemComponent(base.systemComponents)
        base.getSystemThresholds().each { key, value ->
            sysRequirements.addSystemThreshold(key, value)
        }
        return sysRequirements;
    }

    /**
     * Create a Map object from an element that support name,value pairs
     *
     * @param el The element containing the Attribute element
     * @return Hashtable
     */
    protected def getAttributeTable(el) {
        def table = new HashMap<String, Object>();
        el.Attribute.each {
            table.put(it.'@Name', it.'@Value')
        }
        return table
    }

    /**
     * Set the attributes for the AssociationDescriptor
     *
     * @param sElem The ServiceElement of the associated service
     * @param aDesc The AssociationDescriptor to apply attributes to
     */
    protected def setAssociationDescriptorAttrs(ServiceElement sElem, AssociationDescriptor aDesc) {
        aDesc.interfaceNames = sElem.exportBundles.collect { it.className } as String[]
        aDesc.faultDetectionHandlerBundle = sElem.faultDetectionHandlerBundle
        aDesc.groups = sElem.serviceBeanConfig.groups
        aDesc.locators = sElem.serviceBeanConfig.locators
        aDesc.matchOnName = sElem.matchOnName
    }

    /**
     * Create a Configuration entry
     *
     * @param item The item to create a configuration entry for
     *
     * @return A string suitable for use in a Jini configuration
     */
    protected def createConfigEntry(String item) {
        return ConfigUtil.concat(item);
    }

    /**
     * Get the SLAPolicyHandler classname
     *
     * @param type The type, either scaling or relocation
     * @param className The class name of the policy handler
     *
     * @return String name of the SLA policy handler to use
     */
    protected def getSLAPolicyHandler(String type, String className) {
        String handler = "org.rioproject.sla."
        if (type == "scaling") {
            handler = handler + "ScalingPolicyHandler"
        } else if (type == "relocation") {
            handler = handler + "RelocationPolicyHandler"
        } else if (type == "restart") {
            handler = handler + "RedeployPolicyHandler"
        } else if (type == "notify") {
            handler = handler + "SLAPolicyHandler"
        } else if (className) {
            if(className.indexOf(".")==-1)
                handler = handler + className
            else
                handler = className
        } else {
            handler = handler + "SLAPolicyHandler"
        }
        return handler
    }

    /**
     * Verify minimum set of elements have been declared
     *
     * @param element Either the ServiceBean or Service element
     * @param global Global attributes
     */
    protected def verify(element, GlobalAttrs global) throws Exception {
        /* Check  if either groups or locators have been declared */
        if (defaultGroups == null) {
            def groups = element.Groups
            def locators = element.Locators
            if((global.groups.length == 0 && groups.size() == 0) &&
               (global.locators.length==0 && locators.size() ==0 ))
                logger.warning("Neither groups or locators have been declared")
        }
    }
}
