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

import net.jini.core.discovery.LookupLocator
import org.rioproject.associations.AssociationDescriptor
import org.rioproject.associations.AssociationType
import org.rioproject.deploy.SystemComponent
import org.rioproject.deploy.SystemRequirements
import org.rioproject.exec.ExecDescriptor
import org.rioproject.log.LoggerConfig
import org.rioproject.sla.RuleMap
import org.rioproject.sla.SLA
import org.rioproject.system.capability.software.SoftwareSupport

import java.util.concurrent.TimeUnit

import static org.rioproject.opstring.OpStringParserGlobals.*

/**
 * Provides helper methods to create {@code OperationalString} classes.
 *
 * @author Dennis Reedy
 */
class OpStringParserHelper {
    RuleMap.RuleDefinition inFlightRuleDefinition
    RuleMap.ServiceDefinition inFlightRuleServiceDefinition
    List<RuleMap.ServiceDefinition> inFlightRuleServiceDefinitions = []
    List<RuleMap> inFlightRuleMaps = []

    ServiceElement applyServiceElementAttributes(ServiceElement service, Map attributes, Map globalSettings) {
        if(attributes.type!=null) {
            service.setProvisionType(ServiceElement.ProvisionType.valueOf(attributes.type.toUpperCase()))
        }
        if(attributes.fork!=null) {
            boolean fork = attributes.fork == "yes"
            if(fork) {
                service.setFork(fork)
                if(attributes.jvmArgs!=null || attributes.environment!=null) {
                    ExecDescriptor execDescriptor = new ExecDescriptor()
                    if(attributes.jvmArgs!=null) {
                        execDescriptor.setInputArgs(attributes.jvmArgs)
                    }
                    if(attributes.environment!=null) {
                        Map<String, String> env = new HashMap<String, String>()
                        for(String s : attributes.environment.tokenize()) {
                            String[] parts = s.split("=")
                            env.put(parts[0], parts[1])
                        }
                        execDescriptor.setEnvironment(env)
                    }
                    service.execDescriptor = execDescriptor
                }
            }
        }

        service.serviceBeanConfig = new ServiceBeanConfig()
        service.serviceBeanConfig.setName(attributes.name)
        service.operationalStringName = globalSettings[OPSTRING]

        if(globalSettings[GROUPS]!=null)
            service.serviceBeanConfig.groups = globalSettings[GROUPS] as String[]
        if(globalSettings.get(LOCATORS)!=null)
            service.serviceBeanConfig.locators = globalSettings[LOCATORS] as LookupLocator[]
        if(globalSettings[CONFIGURATION])
            service.serviceBeanConfig.configArgs = globalSettings[CONFIGURATION] as String[]
        if(globalSettings[LOGGERS])
            service.serviceBeanConfig.addLoggerConfig(globalSettings[LOGGERS] as LoggerConfig[])
        if (globalSettings[ASSOCIATIONS])
            service.addAssociationDescriptors(globalSettings[ASSOCIATIONS] as AssociationDescriptor[])

        if(attributes.jmxName!=null)
            service.serviceBeanConfig.getConfigurationParameters().put(ServiceBeanConfig.JMX_NAME, attributes.jmxName)

        service
    }

    void processResources(def parent, String codebase, ServiceElement service, String... resources) {
        if(parent=="interfaces") {
            for(ClassBundle classBundle : service.getExportBundles()) {
                classBundle.codebase = codebase
                resources.each { jar ->
                    classBundle.addJAR(jar)
                }
            }
        } else {
            service.componentBundle.codebase = codebase
            resources.each { jar ->
                service.componentBundle.addJAR(jar)
            }
        }
    }

    AssociationDescriptor createAssociationDescriptor(Map attributes, opStringName) {
        AssociationDescriptor association = new AssociationDescriptor(AssociationType.valueOf(attributes.type.toUpperCase()),
                                                                      attributes.name,
                                                                      /*opStringName,
                                                                      attributes.property*/)
        boolean matchOnName = attributes.matchOnName == null? true: (attributes.matchOnName? true : false)
        association.matchOnName = matchOnName
        /* If the association declares a service interface there is no need to use the operational string name */
        if (attributes.serviceType) {
            association.interfaceNames = [attributes.serviceType]
        } else {
            association.operationalStringName = opStringName
        }
        association.propertyName = attributes.property
        attributes.put("association", association)
        association
    }

    AssociationDescriptor createAssociationManagement(Map attributes, AssociationDescriptor associationDescriptor) {
        if(attributes.proxy)
            associationDescriptor.proxyClass = attributes.proxy
        if(attributes.strategy)
            associationDescriptor.serviceSelectionStrategy = attributes.strategy
        if(attributes.serviceDiscoveryTimeout)
            associationDescriptor.serviceDiscoveryTimeout = attributes.serviceDiscoveryTimeout
        if(attributes.serviceDiscoveryTimeoutUnits || attributes.units) {
            def value
            if(attributes.serviceDiscoveryTimeoutUnits)
                value = attributes.serviceDiscoveryTimeoutUnits
            else
                value = attributes.units
            TimeUnit timeUnit
            if (value instanceof TimeUnit)
                timeUnit = value
            else if (value instanceof String)
                timeUnit = TimeUnit.valueOf(value.toUpperCase())
            else
                throw new DSLException("unknown serviceDiscoveryTimeoutUnits type ${value}")
            associationDescriptor.serviceDiscoveryTimeUnits = timeUnit
        }
        if (attributes.filter)
            associationDescriptor.associationMatchFilter = attributes.filter
        if(attributes.proxy)
            associationDescriptor.proxyClass = attributes.proxy
        if(attributes.inject)
            associationDescriptor.lazyInject = attributes.inject.equalsIgnoreCase("lazy")
        associationDescriptor
    }

    void addAssociationDescriptor(AssociationDescriptor associationDescriptor,
                                  ServiceElement service,
                                  def opStringAssociations) {
        if(service==null) {
            opStringAssociations << associationDescriptor
        } else {
            service.addAssociationDescriptors(associationDescriptor)
            if (associationDescriptor.associationType == AssociationType.REQUIRES)
                service.autoAdvertise = false
        }
    }

    SystemComponent createSystemComponent(Map attributes, String... skip) {
        def componentClass = attributes.type == null ? SoftwareSupport.class.simpleName : attributes.type
        Map attributeMap = [:]
        if(skip.length==0)
            skip = ["type"]
        for(Map.Entry<String, String> entry : attributes.entrySet()) {
            if(entry.key in skip) {
                continue
            }
            attributeMap.put(capitalizeFirstLetter(entry.key), entry.value)
        }
        return new SystemComponent(componentClass, componentClass, attributeMap)
    }

    void createAndAddSystemComponent(def parent, Map attributes, Map systemRequirementsTable, ServiceElement service) {
        SystemComponent systemComponent = createSystemComponent(attributes)
        addSystemComponent(parent, systemComponent, systemRequirementsTable, service)
    }

    void addSystemComponent(parent, systemComponent, systemRequirementsTable, service) {
        if(isSystemRequirementGlobal(parent)) {
            String id =  getSystemRequirementID(parent)
            SystemRequirements systemRequirements = systemRequirementsTable[id]
            systemRequirements.addSystemComponent(systemComponent)
        } else {
            service.serviceLevelAgreements.systemRequirements.addSystemComponent(systemComponent)
        }
    }

    SLA createSLA(Map attributes, boolean asRelative) {
        String identifier = attributes.id
        def sLow = attributes.low
        def sHigh = attributes.high

        double low = Double.NaN
        if (sLow!=null) {
            if(sLow instanceof String)
                low = Double.parseDouble(sLow)
            else
                low = sLow.doubleValue()
        }

        if (asRelative && sLow!=null) {
            if (low < 0.0 || low > 1.0) {
                throw new IllegalArgumentException("Bad low value, must be between 0.0 and 1.0")
            }
        }
        double high = Double.NaN
        if (sHigh!=null) {
            if(sHigh instanceof String)
                sHigh = Double.parseDouble(sHigh)
            else
                high = sHigh.doubleValue()
        }

        if (asRelative && sHigh!=null) {
            if (high < 0.0 || high > 1.0) {
                throw new IllegalArgumentException("Bad high value, must be between 0.0 and 1.0")
            }
        }
        if (!Double.isNaN(low) && low > high)
            throw new IllegalArgumentException("Bad range, low value must be less then the high range value")
        return new SLA(identifier, low, high)
    }

    SystemRequirements merge(SystemRequirements base, SystemRequirements sysRequirements) {
        sysRequirements.addSystemComponent(base.systemComponents)
        base.getSystemThresholds().each { key, value ->
            sysRequirements.addSystemThreshold(key, value)
        }
        return sysRequirements;
    }

    String getSLAPolicyHandler(String type) {
        String handler = "org.rioproject.sla."
        if (type == "scaling") {
            handler = handler + "ScalingPolicyHandler"
        } else if (type == "relocation") {
            handler = handler + "RelocationPolicyHandler"
        } else if (type == "restart") {
            handler = handler + "RedeployPolicyHandler"
        } else if (type == "notify") {
            handler = handler + "SLAPolicyHandler"
        } else {
            handler = type
        }
        return handler
    }

    String getSystemRequirementID(String s) {
        return s.substring(s.lastIndexOf("=")+1)
    }

    boolean isSystemRequirementGlobal(String s) {
        return s.startsWith("systemRequirements:id")
    }

    def toArray(String s) {
        StringTokenizer tok = new StringTokenizer(s, ",")
        String[] array = new String[tok.countTokens()]
        int i=0
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken().trim()
            i++
        }
        return(array);
    }

    Map capitalizeFirstLetterOfEachKey(Map map) {
        Map attributes = [:]
        for(Map.Entry<String, Object> entry : map.entrySet()) {
            attributes.put(capitalizeFirstLetter(entry.key), entry.value)
        }
        return attributes
    }

    String capitalizeFirstLetter(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    void handleCommandLine(ExecDescriptor execDescriptor, String command) {
        def String[] cmd = command.tokenize()
        StringBuilder argsBuilder = new StringBuilder()
        if (cmd.size() - 1 > 0) {
            cmd[1..cmd.size() - 1].each { arg ->
                if (argsBuilder.length()>0)
                    argsBuilder.append(" ")
                argsBuilder.append(arg)
            }
        }
        execDescriptor.commandLine = cmd[0]
        execDescriptor.inputArgs = argsBuilder.toString()
    }

    String[] getServiceInterfaceNames(String serviceName, OpString opString) {
        String[] interfaceNames = null
        for(ServiceElement serviceElement : opString.services) {
            if (serviceElement.name == serviceName) {
                List<String> list = new ArrayList<String>();
                for(ClassBundle b : serviceElement.exportBundles) {
                    list << b.className
                }
                interfaceNames = list.toArray(new String[list.size()])
            }
        }
        return interfaceNames
    }

}
